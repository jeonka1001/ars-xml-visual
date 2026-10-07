package diagram

import (
	"fmt"
	"io"
	"os"
	"path/filepath"
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

// ExportInventory는 outDir에 nodes.tsv, links.tsv를 UTF-8로 저장한다.
func ExportInventory(d *Diagram, outDir string) error {
	if err := os.MkdirAll(outDir, 0o755); err != nil {
		return err
	}
	if err := writeFile(filepath.Join(outDir, "nodes.tsv"), d, WriteNodesTSV); err != nil {
		return err
	}
	return writeFile(filepath.Join(outDir, "links.tsv"), d, WriteLinksTSV)
}

func writeFile(path string, d *Diagram, write func(io.Writer, *Diagram) error) error {
	f, err := os.Create(path)
	if err != nil {
		return err
	}
	if err := write(f, d); err != nil {
		f.Close()
		return fmt.Errorf("write %s: %w", path, err)
	}
	return f.Close()
}
