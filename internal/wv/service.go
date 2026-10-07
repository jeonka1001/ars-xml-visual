package wv

import (
	_ "embed"
	"fmt"
	"regexp"
	"strconv"
	"strings"
	"text/template"

	"github.com/jeonka1001/ars-xml-visual/internal/diagram"
	"github.com/jeonka1001/ars-xml-visual/internal/input"
	"github.com/jeonka1001/ars-xml-visual/internal/label"
)

//go:embed templates/screen.js.tmpl
var screenTemplate string

var screenTmpl = template.Must(template.New("screen").Parse(screenTemplate))

const (
	defaultTitle    = "보이는 ARS"
	defaultMenuText = "원하시는 메뉴를 선택해 주세요."
	stockSearchText = "종목명/종목코드 입력"
)

// Input은 화면 생성에 필요한 앞 단계 결과다.
type Input struct {
	Diagram   *diagram.Diagram
	Specs     []input.Spec
	Buttons   []label.Result
	Overrides map[string]string // labels.properties: title, <id>.title, <id>.text, <id>.screen
}

// Render는 화면을 PreScript에 붙여 넣을 JS 코드로 만든다.
func Render(s Screen) (string, error) {
	var b strings.Builder
	if err := screenTmpl.Execute(&b, s); err != nil {
		return "", err
	}
	return b.String(), nil
}

// MenuScreen은 메뉴 노드를 BTN 화면으로 만든다. 문구가 정해진 키만 버튼이 된다.
func MenuScreen(in Input, s input.Spec, buttons []label.Button, reach map[string]bool) (Screen, []string, error) {
	var body [][]string
	for _, b := range buttons {
		if b.Resolved() {
			body = append(body, []string{"BTN", strconv.Itoa(len(body)), b.Label, b.Key})
		}
	}
	if len(body) == 0 {
		return Screen{}, nil, fmt.Errorf("no resolved button")
	}
	code := Code(CodeConfirm)
	if reach[s.NodeID] {
		code = CodeComplete
	}
	text, notes := menuText(in, s)
	code, notes = screenOverride(in, s.NodeID, code, notes)
	if code == CodeComplete {
		notes = append(notes, fmt.Sprintf("node %s: complete screen buttons follow the voice menu; check against service standard", s.NodeID))
	}
	sc, err := newScreen(s.NodeID, s.NodeName, code, titleOf(in, s.NodeID), text, body)
	return sc, notes, err
}

// NumberScreen은 숫자 입력 노드를 INPH 화면으로 만든다. #로 끝내는 가변 길이는 최소 1자리다.
func NumberScreen(in Input, s input.Spec) (Screen, []string, error) {
	minLen := strconv.Itoa(s.Length)
	if strings.Contains(s.TermMask, "#") {
		minLen = "1"
	}
	name := strings.Join(strings.Fields(s.NodeName), " ")
	body := [][]string{
		{"INPH", "0", minLen, strconv.Itoa(s.Length), name, "N", "ON"},
		{"INBTN", "0", "확인"},
	}
	text, notes := in.Overrides[s.NodeID+".text"], []string(nil)
	if text == "" {
		text = firstSentence(s.Guide, name)
		notes = append(notes, fmt.Sprintf("node %s: TXT taken from voice guide; review wording", s.NodeID))
	}
	code, notes := screenOverride(in, s.NodeID, CodeNumberInput, notes)
	sc, err := newScreen(s.NodeID, s.NodeName, code, titleOf(in, s.NodeID), text, body)
	return sc, notes, err
}

// StockSearchScreen은 공통 종목코드 입력 페이지 호출 노드를 종목 검색 화면으로 만든다.
func StockSearchScreen(in Input, n diagram.Node) (Screen, []string, error) {
	title := titleOf(in, n.ID)
	body := [][]string{
		{"INPH", "0", "1", "8", stockSearchText, "N", "ON"},
		{"INBTN", "0", "검색"},
		{"TXT", "1", "L", title + " 하실 종목을 검색해주세요."},
	}
	code, notes := screenOverride(in, n.ID, CodeStockSearch, nil)
	sc, err := newScreen(n.ID, n.Text, code, title, stockSearchText, body)
	return sc, notes, err
}

// IsStockSearch는 공통 종목코드 입력 페이지(*_jmcode.xml)를 호출하는 노드인지 확인한다.
func IsStockSearch(n diagram.Node) bool {
	return n.Type == "CallPageNode" && strings.HasSuffix(strings.ToLower(n.Prop("TargetPage")), "_jmcode.xml")
}

func menuText(in Input, s input.Spec) (string, []string) {
	if t := in.Overrides[s.NodeID+".text"]; t != "" {
		return t, nil
	}
	if t := firstSentence(s.Guide, ""); t != "" && len(label.FromComment(t)) == 0 {
		return t, []string{fmt.Sprintf("node %s: TXT taken from voice guide; review wording", s.NodeID)}
	}
	return defaultMenuText, nil
}

func screenOverride(in Input, id string, code Code, notes []string) (Code, []string) {
	if c := in.Overrides[id+".screen"]; c != "" {
		return Code(c), notes
	}
	return code, append(notes, fmt.Sprintf("node %s: screen code %s inferred", id, code))
}

var memoPrefix = regexp.MustCompile(`^[0-9]+(-[0-9]+)*\s*`)

// titleOf: <id>.title → title → 첫 MemoNode 이름(메뉴 번호 제거) → 기본값.
func titleOf(in Input, id string) string {
	for _, k := range []string{id + ".title", "title"} {
		if t := in.Overrides[k]; t != "" {
			return t
		}
	}
	for _, n := range in.Diagram.Nodes {
		if n.Type == "MemoNode" && n.Text != "" {
			return memoPrefix.ReplaceAllString(strings.Join(strings.Fields(n.Text), " "), "")
		}
	}
	return defaultTitle
}

// firstSentence는 안내 문구의 첫 문장(마침표 포함)이다. 없으면 fallback이다.
func firstSentence(guide, fallback string) string {
	guide = strings.TrimSpace(guide)
	if i := strings.Index(guide, "."); i >= 0 {
		guide = guide[:i+1]
	}
	if guide == "" {
		return fallback
	}
	return guide
}

var trCodeRe = regexp.MustCompile(`app\.trCode\s*=\s*"([A-Za-z0-9]+)"`)
var updateTr = regexp.MustCompile(`u[0-9]+$`)

// ReachableAfterUpdate는 처리(update) 거래 노드 이후 링크로 도달하는 노드 집합이다.
// trCode가 u01 등으로 끝나면 처리 거래, q01·ARS012 등은 조회 거래로 본다.
func ReachableAfterUpdate(d *diagram.Diagram) map[string]bool {
	seen := map[string]bool{}
	var queue []string
	for _, n := range d.Nodes {
		if m := trCodeRe.FindStringSubmatch(n.Prop("PreScript")); m != nil && updateTr.MatchString(m[1]) {
			queue = append(queue, n.ID)
		}
	}
	for len(queue) > 0 {
		id := queue[0]
		queue = queue[1:]
		for _, l := range d.LinksFrom(id) {
			if !seen[l.To] {
				seen[l.To] = true
				queue = append(queue, l.To)
			}
		}
	}
	return seen
}
