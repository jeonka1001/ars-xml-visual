package diagram

import (
	"strings"
	"testing"
)

const testXML = "\xEF\xBB\xBF" + `<?xml version="1.0" encoding="utf-8"?>
<Diagram Version="14">
  <Nodes>
    <Node Id="1" NodeType="CallPageNode">
      <Bounds>0, 0, 10, 10</Bounds>
      <Text>메뉴
선택</Text>
      <CustomProperties>
        <PreScript><![CDATA[app.digitMask = "12";]]></PreScript>
        <TargetPage>InputDTMF_Menu.xml</TargetPage>
      </CustomProperties>
    </Node>
    <Node Id="2" NodeType="SwitchNode"><Text>입력체크</Text></Node>
  </Nodes>
  <Links>
    <Link Id="10"><Text>ok</Text><Origin Id="1" /><Destination Id="2" /></Link>
    <Link Id="11"><Text>1</Text><Origin Id="2" /><Destination Id="99" /></Link>
  </Links>
</Diagram>`

func TestParse(t *testing.T) {
	d, err := Parse(strings.NewReader(testXML))
	if err != nil {
		t.Fatal(err)
	}
	n, ok := d.Node("1")
	if !ok || n.Type != "CallPageNode" || n.Text != "메뉴\n선택" {
		t.Fatalf("node 1 = %+v", n)
	}
	if got := n.Prop("PreScript"); got != `app.digitMask = "12";` {
		t.Errorf("PreScript = %q", got)
	}
	if n.Prop("TargetPage") != "InputDTMF_Menu.xml" {
		t.Errorf("TargetPage = %q", n.Prop("TargetPage"))
	}
	if len(d.Links) != 1 || d.Links[0].Text != "ok" || len(d.LinksFrom("1")) != 1 {
		t.Errorf("links = %+v", d.Links)
	}
	if len(d.Warnings) != 1 {
		t.Errorf("warnings = %v", d.Warnings)
	}
}

func TestParseRejects(t *testing.T) {
	cases := map[string]string{
		"duplicate id": `<Diagram><Nodes><Node Id="1"/><Node Id="1"/></Nodes></Diagram>`,
		"missing id":   `<Diagram><Nodes><Node NodeType="X"/></Nodes></Diagram>`,
		"wrong root":   `<Other/>`,
		"entity":       `<!DOCTYPE d [<!ENTITY x SYSTEM "file:///etc/passwd">]><Diagram><Nodes><Node Id="1"><Text>&x;</Text></Node></Nodes></Diagram>`,
	}
	for name, src := range cases {
		if _, err := Parse(strings.NewReader(src)); err == nil {
			t.Errorf("%s: expected error", name)
		}
	}
}

func TestWriteTSVEscapesCells(t *testing.T) {
	d, err := Parse(strings.NewReader(testXML))
	if err != nil {
		t.Fatal(err)
	}
	if got := NodesTSV(d); !strings.Contains(got, "1\tCallPageNode\t메뉴 선택\tInputDTMF_Menu.xml\t\n") {
		t.Errorf("nodes.tsv =\n%s", got)
	}
}
