package input

import (
	"fmt"

	"github.com/jeonka1001/ars-xml-visual/internal/diagram"
	"github.com/jeonka1001/ars-xml-visual/internal/tsv"
)

// Collect는 다이어그램의 입력 노드를 모두 해석한다. 해석하지 못한 노드는 경고로 반환한다.
func Collect(d *diagram.Diagram) (specs []Spec, warnings []string) {
	for _, n := range d.Nodes {
		if !IsInputNode(n) {
			continue
		}
		s, err := FromNode(n)
		if err != nil {
			warnings = append(warnings, fmt.Sprintf("node %s: skipped: %v", n.ID, err))
			continue
		}
		specs = append(specs, s)
	}
	return specs, warnings
}

// ExportSpecs는 outDir에 inputs.tsv를 저장한다.
func ExportSpecs(specs []Spec, outDir string) error {
	return tsv.Save(outDir, "inputs.tsv", SpecsTSV(specs))
}
