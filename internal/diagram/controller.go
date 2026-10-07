package diagram

import (
	"fmt"
	"os"

	"github.com/jeonka1001/ars-xml-visual/internal/tsv"
)

// Load는 XML 파일을 열어 Parse에 위임한다.
func Load(path string) (*Diagram, error) {
	f, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer f.Close()
	d, err := Parse(f)
	if err != nil {
		return nil, fmt.Errorf("%s: %w", path, err)
	}
	return d, nil
}

// ExportInventory는 outDir에 nodes.tsv, links.tsv를 저장한다.
func ExportInventory(d *Diagram, outDir string) error {
	if err := tsv.Save(outDir, "nodes.tsv", NodesTSV(d)); err != nil {
		return err
	}
	return tsv.Save(outDir, "links.tsv", LinksTSV(d))
}
