package label

import (
	"bufio"
	"io"
	"regexp"
	"strings"
	"unicode/utf8"

	"github.com/jeonka1001/ars-xml-visual/internal/diagram"
	"github.com/jeonka1001/ars-xml-visual/internal/input"
	"github.com/jeonka1001/ars-xml-visual/internal/tsv"
)

// Resolve는 메뉴 노드의 키별 문구를 정한다.
func Resolve(d *diagram.Diagram, s input.Spec, overrides map[string]string) Result {
	byOverride := map[string]string{}
	for _, k := range s.Keys() {
		byOverride[k] = overrides[s.NodeID+".button."+k]
	}
	byComment := FromComment(s.Guide)
	byBranch := FromBranches(d, s.NodeID)
	r := Result{NodeID: s.NodeID}
	for _, k := range s.Keys() {
		r.Buttons = append(r.Buttons, pick(k, byOverride, byComment, byBranch))
	}
	return r
}

// 주석 문형 예: "지정가는 1번", "수정하시려면 2번", "다시듣고싶으시면 별표", "이전단계로 가시려면 우물정자".
var (
	keyPhrase = regexp.MustCompile(`^(.+?)\s*([0-9])\s*번|^(.+?)\s*(별표|\*|우물\s*정자|우물정|샵|샾|#)`)
	// 긴 어미부터 지운다. 남는 문구가 어색할 수 있으므로 결과는 항상 review 대상이다.
	endings = []string{"을 원하시면", "를 원하시면", "돌아가시려면", "가시려면", "고 싶으시면", "고싶으시면",
		"하시려면", "으시려면", "시려면", "려면", "으시면", "으면", "시면", "은", "는"}
)

// FromComment는 안내 주석에서 키별 문구를 뽑는다. 같은 키에 다른 문구가 나오면 그 키는 버린다.
func FromComment(guide string) map[string]string {
	out := map[string]string{}
	conflict := map[string]bool{}
	for _, part := range regexp.MustCompile(`[,.]`).Split(guide, -1) {
		key, text, ok := splitKeyPhrase(strings.TrimSpace(part))
		if !ok || conflict[key] {
			continue
		}
		if old, dup := out[key]; dup && old != text {
			delete(out, key)
			conflict[key] = true
			continue
		}
		out[key] = text
	}
	return out
}

func splitKeyPhrase(part string) (key, text string, ok bool) {
	m := keyPhrase.FindStringSubmatch(part)
	switch {
	case m == nil:
		return "", "", false
	case m[2] != "":
		key, text = m[2], m[1]
	case strings.Contains("별표*", m[4]):
		key, text = "*", m[3]
	default:
		key, text = "#", m[3]
	}
	return key, trimEnding(text), true
}

// trimEnding은 어미를 지운다. 남는 말이 한 글자 이하면 원문을 쓴다(예: "맞으시면").
func trimEnding(s string) string {
	s = strings.TrimSpace(s)
	for _, e := range endings {
		if !strings.HasSuffix(s, e) {
			continue
		}
		if t := strings.TrimSpace(strings.TrimSuffix(s, e)); utf8.RuneCountInString(t) > 1 {
			return t
		}
		return s
	}
	return s
}

// FromBranches는 메뉴 노드의 정상 입력("0") 링크를 따라가 입력체크 Switch의 분기별 도착 노드 이름을 반환한다.
func FromBranches(d *diagram.Diagram, nodeID string) map[string]string {
	out := map[string]string{}
	sw, ok := findInputSwitch(d, nodeID)
	if !ok {
		return out
	}
	for _, l := range d.LinksFrom(sw) {
		to, _ := d.Node(l.To)
		if branchTarget[to.Type] && out[l.Text] == "" {
			out[l.Text] = strings.Join(strings.Fields(to.Text), " ")
		}
	}
	return out
}

// 이동 목적지를 나타내는 노드만 문구로 쓴다. TTS·조건 노드 이름은 버튼 문구로 부적절하다.
var branchTarget = map[string]bool{"GotoPageNode": true, "ReturnPageNode": true, "EmptyNode": true}

// findInputSwitch는 "0" 링크에서 시작해 Script/Empty 노드를 최대 2개 거쳐 app.inputDTMF Switch를 찾는다.
func findInputSwitch(d *diagram.Diagram, nodeID string) (string, bool) {
	cur := ""
	for _, l := range d.LinksFrom(nodeID) {
		if l.Text == "0" {
			cur = l.To
		}
	}
	for hop := 0; cur != "" && hop < 3; hop++ {
		n, _ := d.Node(cur)
		switch {
		case n.Type == "SwitchNode" && strings.Contains(n.Prop("Condition"), "inputDTMF"):
			return cur, true
		case n.Type != "ScriptNode" && n.Type != "EmptyNode":
			return "", false
		}
		cur = nextOf(d, cur)
	}
	return "", false
}

// nextOf는 나가는 링크가 정확히 1개일 때 그 도착 노드를 반환한다.
func nextOf(d *diagram.Diagram, id string) string {
	links := d.LinksFrom(id)
	if len(links) != 1 {
		return ""
	}
	return links[0].To
}

// ParseProperties는 labels.properties(UTF-8)를 읽는다. 키의 \# \! \= \: \\ 이스케이프를 지원한다.
func ParseProperties(r io.Reader) (map[string]string, error) {
	out := map[string]string{}
	sc := bufio.NewScanner(r)
	for sc.Scan() {
		line := strings.TrimSpace(strings.TrimPrefix(sc.Text(), "\uFEFF"))
		if line == "" || line[0] == '#' || line[0] == '!' {
			continue
		}
		k, v := splitProperty(line)
		out[k] = v
	}
	return out, sc.Err()
}

func splitProperty(line string) (key, value string) {
	var b strings.Builder
	for i := 0; i < len(line); i++ {
		c := line[i]
		if c == '\\' && i+1 < len(line) {
			i++
			b.WriteByte(line[i])
			continue
		}
		if c == '=' || c == ':' {
			return strings.TrimSpace(b.String()), strings.TrimSpace(line[i+1:])
		}
		b.WriteByte(c)
	}
	return strings.TrimSpace(b.String()), ""
}

// ButtonsTSV는 검토용 버튼 문구 목록이다.
func ButtonsTSV(results []Result) string {
	t := tsv.New("node_id", "key", "label", "source")
	for _, r := range results {
		for _, b := range r.Buttons {
			t.Row(r.NodeID, b.Key, b.Label, string(b.Source))
		}
	}
	return t.String()
}
