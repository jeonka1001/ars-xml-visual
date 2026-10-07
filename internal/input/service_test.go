package input

import (
	"reflect"
	"strings"
	"testing"

	"github.com/jeonka1001/ars-xml-visual/internal/diagram"
)

func node(page, pre string) diagram.Node {
	return diagram.Node{ID: "7", Type: "CallPageNode", Text: "메뉴", Props: map[string]string{"TargetPage": page, "PreScript": pre}}
}

const menuScript = `

// 맞으시면 1번, 수정하시려면 2번을 눌러주십시오.
app.mentFileName = app.mentFolder + "A361416";
app.mentFileFormat = "V";

app.digitMask = "12*#";
app.digitLegth = "1";
app.termDigitMask = "";

// 오입력, 미입력 최대 오류횟수
app.nomatchMaxCnt = 5;
/* app.digitMask = "9"; */
app.InputDTMF_type = "1";`

func TestFromNodeMenu(t *testing.T) {
	s, err := FromNode(node("InputDTMF_Menu.xml", strings.ReplaceAll(menuScript, "\n", "\r\n")))
	if err != nil {
		t.Fatal(err)
	}
	want := Spec{NodeID: "7", NodeName: "메뉴", Page: "InputDTMF_Menu.xml", Kind: KindMenu, Mask: "12*#", Length: 1,
		MentCode: "A361416", Guide: "맞으시면 1번, 수정하시려면 2번을 눌러주십시오."}
	if !reflect.DeepEqual(s, want) {
		t.Fatalf("got  %+v\nwant %+v", s, want)
	}
}

func TestFromNodeNumber(t *testing.T) {
	pre := `app.digitMask = "1234567890";` + "\n" + `app.digitLength = "8";` + "\n" + `app.termDigitMask = "*#";`
	s, err := FromNode(node("inputDTMF2.xml", pre))
	if err != nil {
		t.Fatal(err)
	}
	if s.Kind != KindNumber || s.Length != 8 || s.TermMask != "*#" || s.Guide != "" {
		t.Fatalf("got %+v", s)
	}
}

func TestFromNodeRejects(t *testing.T) {
	cases := map[string]diagram.Node{
		"dynamic mask":     node("InputDTMF_Menu.xml", `app.digitMask = mask;`+"\n"+`app.digitLength = "1";`),
		"duplicate mask":   node("InputDTMF_Menu.xml", `app.digitMask = "1";`+"\n"+`app.digitMask = "2";`+"\n"+`app.digitLength = "1";`),
		"both lengths":     node("InputDTMF_Menu.xml", `app.digitMask = "1";`+"\n"+`app.digitLength = "1";`+"\n"+`app.digitLegth = "1";`),
		"missing length":   node("InputDTMF_Menu.xml", `app.digitMask = "1";`),
		"bad mask char":    node("InputDTMF_Menu.xml", `app.digitMask = "1A";`+"\n"+`app.digitLength = "1";`),
		"menu multi digit": node("InputDTMF_Menu.xml", `app.digitMask = "12";`+"\n"+`app.digitLength = "2";`),
		"unknown page":     node("Other.xml", `app.digitMask = "1";`+"\n"+`app.digitLength = "1";`),
		"control flow":     node("InputDTMF_Menu.xml", `if (x) { app.digitMask = "1"; }`+"\n"+`app.digitLength = "1";`),
		"unclosed comment": node("InputDTMF_Menu.xml", `/* app.digitMask = "1";`),
	}
	for name, n := range cases {
		if _, err := FromNode(n); err == nil {
			t.Errorf("%s: expected error", name)
		}
	}
}

func TestStripCommentsKeepsStrings(t *testing.T) {
	got, err := stripComments(`a = "http://x"; // c` + "\n" + `b = '/*y*/';`)
	if err != nil {
		t.Fatal(err)
	}
	if want := `a = "http://x"; ` + "\n" + `b = '/*y*/';`; got != want {
		t.Errorf("got %q want %q", got, want)
	}
}

func TestKeysDedup(t *testing.T) {
	if got := (Spec{Mask: "1219*#*"}).Keys(); !reflect.DeepEqual(got, []string{"1", "2", "9", "*", "#"}) {
		t.Errorf("keys = %v", got)
	}
}

func TestIsInputNode(t *testing.T) {
	if IsInputNode(diagram.Node{Type: "ScriptNode", Props: map[string]string{"PreScript": "app.digitMask"}}) {
		t.Error("ScriptNode must not be an input node")
	}
}
