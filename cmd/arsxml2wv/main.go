// arsxml2wv는 Hansol 시나리오 XML을 읽어 보이는 ARS(WV) 스크립트 초안을 만든다.
//
// 사용법: arsxml2wv <input.xml> <output-dir> [labels.properties]
package main

import (
	"fmt"
	"os"
	"strings"

	"github.com/jeonka1001/ars-xml-visual/internal/diagram"
	"github.com/jeonka1001/ars-xml-visual/internal/input"
	"github.com/jeonka1001/ars-xml-visual/internal/label"
	"github.com/jeonka1001/ars-xml-visual/internal/tsv"
)

// 콘솔 메시지는 영문으로 둔다. 한글 Windows 콘솔(CP949)에서 UTF-8 한글이 깨지기 때문이다.
func main() {
	if len(os.Args) < 3 || len(os.Args) > 4 {
		fmt.Fprintln(os.Stderr, "usage: arsxml2wv <input.xml> <output-dir> [labels.properties]")
		os.Exit(2)
	}
	labelsPath := ""
	if len(os.Args) == 4 {
		labelsPath = os.Args[3]
	}
	if err := run(os.Args[1], os.Args[2], labelsPath); err != nil {
		fmt.Fprintln(os.Stderr, "error:", err)
		os.Exit(1)
	}
}

func run(xmlPath, outDir, labelsPath string) error {
	overrides, err := label.LoadOverrides(labelsPath)
	if err != nil {
		return err
	}
	d, err := diagram.Load(xmlPath)
	if err != nil {
		return err
	}
	specs, inputWarnings := input.Collect(d)
	buttons, labelNotes := label.ResolveAll(d, specs, overrides)
	review := concat(d.Warnings, inputWarnings, labelNotes)
	if err := export(d, specs, buttons, review, outDir); err != nil {
		return err
	}
	fmt.Printf("nodes: %d, links: %d, input nodes: %d, menus: %d, review items: %d\n",
		len(d.Nodes), len(d.Links), len(specs), len(buttons), len(review))
	fmt.Println("see review.txt for items to check")
	return nil
}

func export(d *diagram.Diagram, specs []input.Spec, buttons []label.Result, review []string, outDir string) error {
	if err := diagram.ExportInventory(d, outDir); err != nil {
		return err
	}
	if err := input.ExportSpecs(specs, outDir); err != nil {
		return err
	}
	if err := label.ExportButtons(buttons, outDir); err != nil {
		return err
	}
	return tsv.Save(outDir, "review.txt", strings.Join(review, "\n")+"\n")
}

func concat(lists ...[]string) []string {
	var out []string
	for _, l := range lists {
		out = append(out, l...)
	}
	return out
}
