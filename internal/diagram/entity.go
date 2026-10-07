// Package diagram은 Hansol 시나리오 XML(Diagram)의 노드와 링크를 다룬다.
package diagram

import "fmt"

// Node는 시나리오의 노드 1개다.
type Node struct {
	ID    string
	Type  string            // NodeType 속성 (예: CallPageNode)
	Text  string            // 디자이너에 표시되는 노드 이름
	Props map[string]string // CustomProperties 하위 요소 (예: PreScript, TargetPage)
}

// Prop은 CustomProperties 값을 반환한다. 없으면 빈 문자열이다.
func (n Node) Prop(name string) string { return n.Props[name] }

// Link는 노드 간 연결이다. Text는 분기 조건 값(예: "1", "default")이다.
type Link struct {
	ID   string
	From string
	To   string
	Text string
}

// Diagram은 XML 1개에서 읽은 노드와 링크 목록이다.
type Diagram struct {
	Nodes    []Node
	Links    []Link
	Warnings []string // 변환은 계속하되 사람이 확인할 항목
	index    map[string]int
}

// newDiagram은 노드 Id 중복을 거부하고, 끝점이 없는 링크는 경고로 남긴다.
func newDiagram(nodes []Node, links []Link) (*Diagram, error) {
	d := &Diagram{Nodes: nodes, index: make(map[string]int, len(nodes))}
	for i, n := range nodes {
		if n.ID == "" {
			return nil, fmt.Errorf("node without Id (position %d)", i)
		}
		if _, dup := d.index[n.ID]; dup {
			return nil, fmt.Errorf("duplicate node Id: %s", n.ID)
		}
		d.index[n.ID] = i
	}
	for _, l := range links {
		if !d.has(l.From) || !d.has(l.To) {
			d.Warnings = append(d.Warnings, fmt.Sprintf("link %s: unresolved endpoint %s -> %s", l.ID, l.From, l.To))
			continue
		}
		d.Links = append(d.Links, l)
	}
	return d, nil
}

// Node는 Id로 노드를 찾는다.
func (d *Diagram) Node(id string) (Node, bool) {
	i, ok := d.index[id]
	if !ok {
		return Node{}, false
	}
	return d.Nodes[i], true
}

// LinksFrom은 해당 노드에서 나가는 링크 목록이다.
func (d *Diagram) LinksFrom(id string) []Link {
	var out []Link
	for _, l := range d.Links {
		if l.From == id {
			out = append(out, l)
		}
	}
	return out
}

func (d *Diagram) has(id string) bool {
	_, ok := d.index[id]
	return ok
}
