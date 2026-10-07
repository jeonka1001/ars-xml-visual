package wv

import (
	"fmt"

	"github.com/jeonka1001/ars-xml-visual/internal/input"
	"github.com/jeonka1001/ars-xml-visual/internal/label"
	"github.com/jeonka1001/ars-xml-visual/internal/tsv"
)

// BuildAll은 다이어그램 노드 순서대로 화면을 만든다. 만들지 못한 노드는 notes에 남긴다.
func BuildAll(in Input) (screens []Screen, notes []string) {
	specs := map[string]input.Spec{}
	for _, s := range in.Specs {
		specs[s.NodeID] = s
	}
	buttons := map[string][]label.Button{}
	for _, r := range in.Buttons {
		buttons[r.NodeID] = r.Buttons
	}
	reach := ReachableAfterUpdate(in.Diagram)
	for _, n := range in.Diagram.Nodes {
		sc, ns, err := buildOne(in, n.ID, specs, buttons, reach)
		notes = append(notes, ns...)
		switch {
		case err != nil:
			notes = append(notes, fmt.Sprintf("node %s: screen skipped: %v", n.ID, err))
		case sc.Code != "":
			screens = append(screens, sc)
		}
	}
	return screens, notes
}

func buildOne(in Input, id string, specs map[string]input.Spec, buttons map[string][]label.Button, reach map[string]bool) (Screen, []string, error) {
	n, _ := in.Diagram.Node(id)
	if IsStockSearch(n) {
		return StockSearchScreen(in, n)
	}
	s, ok := specs[id]
	switch {
	case !ok:
		return Screen{}, nil, nil
	case s.Kind == input.KindMenu:
		return MenuScreen(in, s, buttons[id], reach)
	default:
		return NumberScreen(in, s)
	}
}

// Export는 화면마다 JS 파일을 outDir에 저장한다.
func Export(screens []Screen, outDir string) error {
	for i, s := range screens {
		js, err := Render(s)
		if err != nil {
			return fmt.Errorf("render node %s: %w", s.NodeID, err)
		}
		if err := tsv.Save(outDir, s.FileName(i+1), js); err != nil {
			return err
		}
	}
	return nil
}
