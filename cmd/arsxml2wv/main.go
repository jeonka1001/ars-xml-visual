// arsxml2wv는 Hansol 시나리오 XML을 읽어 보이는 ARS(WV) 스크립트 초안을 만든다.
//
// 사용법: arsxml2wv <input.xml> <output-dir>
package main

import (
	"fmt"
	"os"

	"github.com/jeonka1001/ars-xml-visual/internal/diagram"
)

// 콘솔 메시지는 영문으로 둔다. 한글 Windows 콘솔(CP949)에서 UTF-8 한글이 깨지기 때문이다.
func main() {
	if len(os.Args) != 3 {
		fmt.Fprintln(os.Stderr, "usage: arsxml2wv <input.xml> <output-dir>")
		os.Exit(2)
	}
	if err := run(os.Args[1], os.Args[2]); err != nil {
		fmt.Fprintln(os.Stderr, "error:", err)
		os.Exit(1)
	}
}

func run(input, outDir string) error {
	d, err := diagram.Load(input)
	if err != nil {
		return err
	}
	if err := diagram.ExportInventory(d, outDir); err != nil {
		return err
	}
	fmt.Printf("nodes: %d, links: %d, warnings: %d\n", len(d.Nodes), len(d.Links), len(d.Warnings))
	for _, w := range d.Warnings {
		fmt.Println("warning:", w)
	}
	return nil
}
