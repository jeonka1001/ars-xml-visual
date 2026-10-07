package label

import (
	"reflect"
	"strings"
	"testing"

	"github.com/jeonka1001/ars-xml-visual/internal/diagram"
	"github.com/jeonka1001/ars-xml-visual/internal/input"
)

func TestFromComment(t *testing.T) {
	guide := "메뉴를 선택해 주십시오. 잔고조회는 1번, 이체를 원하시면 2번, 처음으로 돌아가시려면 9번, " +
		"다시 들으시려면 별표, 이전단계로 가시려면 우물정자를 눌러주십시오."
	want := map[string]string{"1": "잔고조회", "2": "이체", "9": "처음으로", "*": "다시 들", "#": "이전단계로"}
	if got := FromComment(guide); !reflect.DeepEqual(got, want) {
		t.Errorf("got %v\nwant %v", got, want)
	}
}

func TestFromCommentShortStemKeepsPhrase(t *testing.T) {
	if got := FromComment("맞으시면 1번")["1"]; got != "맞으시면" {
		t.Errorf("got %q", got)
	}
}

func TestFromCommentConflictDropsKey(t *testing.T) {
	got := FromComment("조회는 1번, 이체는 1번, 취소는 2번")
	if _, ok := got["1"]; ok || got["2"] != "취소" {
		t.Errorf("got %v", got)
	}
}

const branchXML = `<Diagram><Nodes>
<Node Id="1" NodeType="CallPageNode"><Text>메뉴</Text></Node>
<Node Id="2" NodeType="ScriptNode"><Text>세팅</Text></Node>
<Node Id="3" NodeType="SwitchNode"><Text>입력체크</Text><CustomProperties><Condition>app.inputDTMF</Condition></CustomProperties></Node>
<Node Id="4" NodeType="GotoPageNode"><Text>초기
메뉴</Text></Node>
<Node Id="5" NodeType="CallPageNode"><Text>TTS 안내</Text></Node>
<Node Id="6" NodeType="GotoPageNode"><Text>오류</Text></Node>
</Nodes><Links>
<Link Id="10"><Text>0</Text><Origin Id="1"/><Destination Id="2"/></Link>
<Link Id="11"><Text>maxErr_noinput</Text><Origin Id="1"/><Destination Id="6"/></Link>
<Link Id="12"><Text>ok</Text><Origin Id="2"/><Destination Id="3"/></Link>
<Link Id="13"><Text>9</Text><Origin Id="3"/><Destination Id="4"/></Link>
<Link Id="14"><Text>1</Text><Origin Id="3"/><Destination Id="5"/></Link>
</Links></Diagram>`

func TestResolvePriority(t *testing.T) {
	d, err := diagram.Parse(strings.NewReader(branchXML))
	if err != nil {
		t.Fatal(err)
	}
	s := input.Spec{NodeID: "1", Kind: input.KindMenu, Mask: "129#", Guide: "조회는 1번, 이체는 2번"}
	got := Resolve(d, s, map[string]string{"1.button.2": "계좌이체"})
	want := []Button{
		{Key: "1", Label: "조회", Source: SourceComment},
		{Key: "2", Label: "계좌이체", Source: SourceOverride},
		{Key: "9", Label: "초기 메뉴", Source: SourceBranch},
		{Key: "#", Source: SourceNone},
	}
	if !reflect.DeepEqual(got.Buttons, want) {
		t.Errorf("got  %+v\nwant %+v", got.Buttons, want)
	}
}

func TestFromBranchesSkipsNonTargetNodes(t *testing.T) {
	d, err := diagram.Parse(strings.NewReader(branchXML))
	if err != nil {
		t.Fatal(err)
	}
	if got := FromBranches(d, "1"); got["1"] != "" || got["9"] != "초기 메뉴" {
		t.Errorf("got %v", got)
	}
}

func TestParseProperties(t *testing.T) {
	src := "\uFEFF# 주석\n! 주석\n31.title = 청약 안내\n31.button.\\#=이전 단계\n31.button.*: 다시 듣기\n\n"
	got, err := ParseProperties(strings.NewReader(src))
	if err != nil {
		t.Fatal(err)
	}
	want := map[string]string{"31.title": "청약 안내", "31.button.#": "이전 단계", "31.button.*": "다시 듣기"}
	if !reflect.DeepEqual(got, want) {
		t.Errorf("got %v", got)
	}
}
