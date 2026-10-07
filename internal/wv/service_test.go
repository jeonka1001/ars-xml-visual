package wv

import (
	"strings"
	"testing"

	"github.com/jeonka1001/ars-xml-visual/internal/diagram"
	"github.com/jeonka1001/ars-xml-visual/internal/input"
	"github.com/jeonka1001/ars-xml-visual/internal/label"
)

const flowXML = `<Diagram><Nodes>
<Node Id="1" NodeType="MemoNode"><Text>1-2-3 잔고 조회</Text></Node>
<Node Id="2" NodeType="CallPageNode"><Text>조회</Text><CustomProperties><PreScript>app.trCode = "abc100q01";</PreScript></CustomProperties></Node>
<Node Id="3" NodeType="CallPageNode"><Text>확인 메뉴</Text></Node>
<Node Id="4" NodeType="CallPageNode"><Text>처리</Text><CustomProperties><PreScript>app.trCode = "abc200u01";</PreScript></CustomProperties></Node>
<Node Id="5" NodeType="ScriptNode"><Text>결과</Text></Node>
<Node Id="6" NodeType="CallPageNode"><Text>완료 메뉴</Text></Node>
<Node Id="7" NodeType="CallPageNode"><Text>종목</Text><CustomProperties><TargetPage>sh_menu3_jmcode.xml</TargetPage></CustomProperties></Node>
</Nodes><Links>
<Link Id="10"><Origin Id="2"/><Destination Id="3"/></Link>
<Link Id="11"><Origin Id="3"/><Destination Id="4"/></Link>
<Link Id="12"><Origin Id="4"/><Destination Id="5"/></Link>
<Link Id="13"><Origin Id="5"/><Destination Id="6"/></Link>
</Links></Diagram>`

func testInput(t *testing.T, overrides map[string]string) Input {
	t.Helper()
	d, err := diagram.Parse(strings.NewReader(flowXML))
	if err != nil {
		t.Fatal(err)
	}
	specs := []input.Spec{
		{NodeID: "3", NodeName: "확인 메뉴", Kind: input.KindMenu, Mask: "12*", Length: 1, Guide: "맞으시면 1번, 취소는 2번"},
		{NodeID: "6", NodeName: "완료 메뉴", Kind: input.KindMenu, Mask: "1", Length: 1, Guide: "내역을 확인하십시오. 처음으로는 1번"},
		{NodeID: "2", NodeName: "금액\n입력", Kind: input.KindNumber, Mask: "0123456789", Length: 8, Guide: "금액을 입력하십시오. 예시입니다."},
	}
	buttons := []label.Result{
		{NodeID: "3", Buttons: []label.Button{{Key: "1", Label: "확인", Source: label.SourceOverride}, {Key: "2", Label: "취소", Source: label.SourceComment}, {Key: "*", Source: label.SourceNone}}},
		{NodeID: "6", Buttons: []label.Button{{Key: "1", Label: "처음으로", Source: label.SourceComment}}},
	}
	return Input{Diagram: d, Specs: specs, Buttons: buttons, Overrides: overrides}
}

func TestBuildAll(t *testing.T) {
	screens, _ := BuildAll(testInput(t, map[string]string{}))
	got := map[string]Code{}
	for _, s := range screens {
		got[s.NodeID] = s.Code
	}
	want := map[string]Code{"2": CodeNumberInput, "3": CodeConfirm, "6": CodeComplete, "7": CodeStockSearch}
	if len(got) != len(want) {
		t.Fatalf("got %v", got)
	}
	for id, c := range want {
		if got[id] != c {
			t.Errorf("node %s: got %s want %s", id, got[id], c)
		}
	}
}

func TestRenderMenu(t *testing.T) {
	screens, _ := BuildAll(testInput(t, map[string]string{"3.text": `안내 "따옴표"`}))
	js, err := Render(screens[1])
	if err != nil {
		t.Fatal(err)
	}
	for _, want := range []string{
		`wvMenu += "S\$SHKD12;";`,
		`wvMenu += "TIT\$0\$잔고 조회;";`,
		`wvMenu += "TXT\$0\$C\$안내 \"따옴표\";";`,
		`wvMenu += "BTN\$0\$확인\$1;";`,
		`wvMenu += "BTN\$1\$취소\$2;";`,
		`app.wvReadTimeout = "B";`,
	} {
		if !strings.Contains(js, want) {
			t.Errorf("missing %s in\n%s", want, js)
		}
	}
	if strings.Contains(js, `\$*;`) {
		t.Error("unresolved key must not become a button")
	}
}

func TestNumberScreen(t *testing.T) {
	in := testInput(t, map[string]string{"title": "공통 제목"})
	s, _, err := NumberScreen(in, in.Specs[2])
	if err != nil {
		t.Fatal(err)
	}
	want := []string{`S\$SHKC21;`, `BTH\$0\$ON;`, `BTH\$1\$ON;`, `TIT\$0\$공통 제목;`, `TXT\$0\$C\$금액을 입력하십시오.;`,
		`INPH\$0\$8\$8\$금액 입력\$N\$ON;`, `INBTN\$0\$확인;`}
	if strings.Join(s.Records, "|") != strings.Join(want, "|") {
		t.Errorf("got  %v\nwant %v", s.Records, want)
	}
}

func TestScreenOverrideAndDelimiterReject(t *testing.T) {
	in := testInput(t, map[string]string{"6.screen": "SHKX99", "3.text": "a;b"})
	screens, notes := BuildAll(in)
	for _, s := range screens {
		if s.NodeID == "3" {
			t.Error("node 3 must be skipped for delimiter in text")
		}
		if s.NodeID == "6" && s.Code != "SHKX99" {
			t.Errorf("node 6 code = %s", s.Code)
		}
	}
	if !strings.Contains(strings.Join(notes, "\n"), "node 3: screen skipped") {
		t.Errorf("notes = %v", notes)
	}
}

func TestRecordEscapesBackslash(t *testing.T) {
	got, err := record("TXT", `a\b`, "줄\n바꿈")
	if err != nil || got != `TXT\$a\\b\$줄 바꿈;` {
		t.Errorf("got %q, %v", got, err)
	}
}
