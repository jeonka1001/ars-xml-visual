package input

import (
	"errors"
	"fmt"
	"regexp"
	"strconv"
	"strings"

	"github.com/jeonka1001/ars-xml-visual/internal/diagram"
	"github.com/jeonka1001/ars-xml-visual/internal/tsv"
)

// PreScript는 실행하지 않고 문자열로만 해석한다.
// 단순 리터럴 대입 1회만 인정하고, 그 외(변수, 중복 대입, 분기문)는 오류로 돌려 사람이 확인하게 한다.

var (
	controlFlow = regexp.MustCompile(`\b(if|else|switch|for|while|function)\b`)
	mentCodeRe  = regexp.MustCompile(`(?m)^\s*app\.mentFileName\s*=\s*app\.mentFolder\s*\+\s*"([A-Za-z0-9_]+)"\s*;?\s*$`)
)

// IsInputNode는 digitMask를 설정하는 CallPageNode인지 확인한다.
func IsInputNode(n diagram.Node) bool {
	return n.Type == "CallPageNode" && strings.Contains(n.Prop("PreScript"), "digitMask")
}

// FromNode는 입력 노드의 PreScript를 Spec으로 해석한다.
func FromNode(n diagram.Node) (Spec, error) {
	raw := strings.ReplaceAll(n.Prop("PreScript"), "\r\n", "\n")
	code, err := stripComments(raw)
	if err != nil {
		return Spec{}, err
	}
	if controlFlow.MatchString(code) {
		return Spec{}, errors.New("control flow in PreScript")
	}
	s := Spec{NodeID: n.ID, NodeName: n.Text, Page: n.Prop("TargetPage"), Guide: leadingComment(raw)}
	if err := readSettings(code, &s); err != nil {
		return Spec{}, err
	}
	if s.Kind, err = kindOf(s.Page, s.Length); err != nil {
		return Spec{}, err
	}
	if m := mentCodeRe.FindStringSubmatch(code); m != nil {
		s.MentCode = m[1]
	}
	return s, nil
}

func readSettings(code string, s *Spec) error {
	var err error
	if s.Mask, err = literal(code, "digitMask"); err != nil {
		return err
	}
	if !validMask(s.Mask) {
		return fmt.Errorf("invalid digitMask %q", s.Mask)
	}
	if s.TermMask, err = literal(code, "termDigitMask"); err != nil {
		return err
	}
	s.Length, err = digitLength(code)
	return err
}

// digitLength는 digitLength 또는 운영 XML의 오타 digitLegth 중 하나만 허용한다.
func digitLength(code string) (int, error) {
	a, errA := literal(code, "digitLength")
	b, errB := literal(code, "digitLegth")
	if err := errors.Join(errA, errB); err != nil {
		return 0, err
	}
	if a != "" && b != "" {
		return 0, errors.New("both digitLength and digitLegth assigned")
	}
	v := a + b
	n, err := strconv.Atoi(v)
	if err != nil || n < 1 {
		return 0, fmt.Errorf("invalid digit length %q", v)
	}
	return n, nil
}

// literal은 app.<name> = "값"; 형태의 단독 대입 값을 반환한다. 대입이 없으면 빈 문자열이다.
func literal(code, name string) (string, error) {
	q := regexp.QuoteMeta(name)
	assigns := regexp.MustCompile(`\bapp\s*\.\s*`+q+`\s*=[^=]`).FindAllStringIndex(code, -1)
	if len(assigns) == 0 {
		return "", nil
	}
	if len(assigns) > 1 {
		return "", fmt.Errorf("%s assigned %d times", name, len(assigns))
	}
	re := regexp.MustCompile(`(?m)^\s*app\s*\.\s*` + q + `\s*=\s*"([^"\\\n]*)"\s*;?\s*$`)
	m := re.FindStringSubmatch(code)
	if m == nil {
		return "", fmt.Errorf("%s is not a simple string literal", name)
	}
	return m[1], nil
}

// leadingComment는 PreScript 맨 앞의 연속된 // 주석을 한 줄로 합친다.
func leadingComment(raw string) string {
	var parts []string
	for _, line := range strings.Split(raw, "\n") {
		line = strings.TrimSpace(line)
		if line == "" && len(parts) == 0 {
			continue
		}
		if !strings.HasPrefix(line, "//") {
			break
		}
		parts = append(parts, strings.TrimSpace(strings.TrimPrefix(line, "//")))
	}
	return strings.Join(parts, " ")
}

// stripComments는 문자열 리터럴 밖의 주석을 지운다. 줄 단위 매칭을 위해 줄바꿈은 남긴다.
func stripComments(s string) (string, error) {
	var b strings.Builder
	var quote rune
	rs := []rune(s)
	for i := 0; i < len(rs); i++ {
		c := rs[i]
		switch {
		case quote != 0:
			b.WriteRune(c)
			if c == '\\' && i+1 < len(rs) {
				i++
				b.WriteRune(rs[i])
			} else if c == quote {
				quote = 0
			}
		case c == '"' || c == '\'':
			quote = c
			b.WriteRune(c)
		case c == '/' && i+1 < len(rs) && (rs[i+1] == '/' || rs[i+1] == '*'):
			end, err := skipComment(rs, i, &b)
			if err != nil {
				return "", err
			}
			i = end
		default:
			b.WriteRune(c)
		}
	}
	return b.String(), nil
}

// skipComment는 rs[i]에서 시작하는 주석을 건너뛰고 마지막 위치를 반환한다.
func skipComment(rs []rune, i int, b *strings.Builder) (int, error) {
	if rs[i+1] == '/' {
		for i < len(rs) && rs[i] != '\n' {
			i++
		}
		b.WriteByte('\n')
		return i, nil
	}
	for j := i + 2; j+1 < len(rs); j++ {
		if rs[j] == '*' && rs[j+1] == '/' {
			b.WriteString(strings.Repeat("\n", strings.Count(string(rs[i:j]), "\n")))
			return j + 1, nil
		}
	}
	return 0, errors.New("unclosed block comment in PreScript")
}

// SpecsTSV는 검토용 입력 노드 목록이다.
func SpecsTSV(specs []Spec) string {
	t := tsv.New("node_id", "node_name", "kind", "page", "mask", "length", "term_mask", "ment_code", "guide")
	for _, s := range specs {
		t.Row(s.NodeID, s.NodeName, string(s.Kind), s.Page, s.Mask, strconv.Itoa(s.Length), s.TermMask, s.MentCode, s.Guide)
	}
	return t.String()
}
