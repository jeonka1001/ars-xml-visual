package label

import (
	"fmt"
	"os"

	"github.com/jeonka1001/ars-xml-visual/internal/diagram"
	"github.com/jeonka1001/ars-xml-visual/internal/input"
	"github.com/jeonka1001/ars-xml-visual/internal/tsv"
)

// LoadOverrides는 labels.properties를 읽는다. path가 비어 있으면 빈 설정이다.
func LoadOverrides(path string) (map[string]string, error) {
	if path == "" {
		return map[string]string{}, nil
	}
	f, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer f.Close()
	props, err := ParseProperties(f)
	if err != nil {
		return nil, fmt.Errorf("%s: %w", path, err)
	}
	return props, nil
}

// ResolveAll은 메뉴 노드의 버튼 문구를 정하고, 사람이 확인할 항목을 notes로 반환한다.
func ResolveAll(d *diagram.Diagram, specs []input.Spec, overrides map[string]string) (results []Result, notes []string) {
	for _, s := range specs {
		if s.Kind != input.KindMenu {
			continue
		}
		r := Resolve(d, s, overrides)
		results = append(results, r)
		notes = append(notes, notesOf(r)...)
	}
	return results, notes
}

func notesOf(r Result) []string {
	var notes []string
	for _, b := range r.Buttons {
		switch {
		case !b.Resolved():
			notes = append(notes, fmt.Sprintf("node %s key %s: no label; button omitted (add %s.button.%s)", r.NodeID, b.Key, r.NodeID, b.Key))
		case b.Inferred():
			notes = append(notes, fmt.Sprintf("node %s key %s: label from %s, review wording", r.NodeID, b.Key, b.Source))
		}
	}
	return notes
}

// ExportButtons는 outDir에 buttons.tsv를 저장한다.
func ExportButtons(results []Result, outDir string) error {
	return tsv.Save(outDir, "buttons.tsv", ButtonsTSV(results))
}
