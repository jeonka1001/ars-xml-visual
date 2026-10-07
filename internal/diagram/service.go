package diagram

import (
	"bufio"
	"bytes"
	"encoding/xml"
	"fmt"
	"io"
	"strings"
)

// 아래 xml* 타입은 파싱 전용이다. 좌표·스타일 등 화면 요소는 읽지 않는다.
type xmlDiagram struct {
	XMLName xml.Name  `xml:"Diagram"`
	Nodes   []xmlNode `xml:"Nodes>Node"`
	Links   []xmlLink `xml:"Links>Link"`
}

type xmlNode struct {
	ID    string   `xml:"Id,attr"`
	Type  string   `xml:"NodeType,attr"`
	Text  string   `xml:"Text"`
	Props xmlProps `xml:"CustomProperties"`
}

type xmlProps struct {
	Items []xmlProp `xml:",any"`
}

type xmlProp struct {
	XMLName xml.Name
	Value   string `xml:",chardata"` // CDATA 포함
}

type xmlLink struct {
	ID   string `xml:"Id,attr"`
	Text string `xml:"Text"`
	From xmlRef `xml:"Origin"`
	To   xmlRef `xml:"Destination"`
}

type xmlRef struct {
	ID string `xml:"Id,attr"`
}

var utf8BOM = []byte{0xEF, 0xBB, 0xBF}

// Parse는 Diagram XML을 읽는다. 외부 엔티티·DTD는 처리하지 않는다(Go 표준 파서 기본 동작).
func Parse(r io.Reader) (*Diagram, error) {
	br := bufio.NewReader(r)
	if head, _ := br.Peek(len(utf8BOM)); bytes.Equal(head, utf8BOM) {
		br.Discard(len(utf8BOM))
	}
	var raw xmlDiagram
	if err := xml.NewDecoder(br).Decode(&raw); err != nil {
		return nil, fmt.Errorf("parse XML: %w", err)
	}
	return newDiagram(toNodes(raw.Nodes), toLinks(raw.Links))
}

func toNodes(raw []xmlNode) []Node {
	nodes := make([]Node, 0, len(raw))
	for _, x := range raw {
		props := make(map[string]string, len(x.Props.Items))
		for _, p := range x.Props.Items {
			props[p.XMLName.Local] = strings.TrimSpace(p.Value)
		}
		nodes = append(nodes, Node{ID: x.ID, Type: x.Type, Text: strings.TrimSpace(x.Text), Props: props})
	}
	return nodes
}

func toLinks(raw []xmlLink) []Link {
	links := make([]Link, 0, len(raw))
	for _, x := range raw {
		links = append(links, Link{ID: x.ID, From: x.From.ID, To: x.To.ID, Text: strings.TrimSpace(x.Text)})
	}
	return links
}

// WriteNodesTSV는 검토용 노드 목록을 TSV로 쓴다.
func WriteNodesTSV(w io.Writer, d *Diagram) error {
	var b strings.Builder
	b.WriteString("node_id\tnode_type\tnode_name\ttarget_page\tcomment\n")
	for _, n := range d.Nodes {
		writeRow(&b, n.ID, n.Type, n.Text, n.Prop("TargetPage"), n.Prop("Comment"))
	}
	_, err := io.WriteString(w, b.String())
	return err
}

// WriteLinksTSV는 검토용 링크 목록을 TSV로 쓴다.
func WriteLinksTSV(w io.Writer, d *Diagram) error {
	var b strings.Builder
	b.WriteString("link_id\torigin_id\torigin_name\tbranch_text\tdestination_id\tdestination_name\n")
	for _, l := range d.Links {
		from, _ := d.Node(l.From)
		to, _ := d.Node(l.To)
		writeRow(&b, l.ID, l.From, from.Text, l.Text, l.To, to.Text)
	}
	_, err := io.WriteString(w, b.String())
	return err
}

var cellCleaner = strings.NewReplacer("\t", " ", "\r\n", " ", "\n", " ", "\r", " ")

func writeRow(b *strings.Builder, cells ...string) {
	for i, c := range cells {
		if i > 0 {
			b.WriteByte('\t')
		}
		b.WriteString(cellCleaner.Replace(c))
	}
	b.WriteByte('\n')
}
