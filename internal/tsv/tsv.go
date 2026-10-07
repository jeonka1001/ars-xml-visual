// Package tsv는 검토용 TSV 문자열을 만든다.
package tsv

import (
	"os"
	"path/filepath"
	"strings"
)

// Table은 헤더와 행을 차례로 쌓는다. 셀 안의 탭·줄바꿈은 공백으로 바꾼다.
type Table struct {
	b strings.Builder
}

var cellCleaner = strings.NewReplacer("\t", " ", "\r\n", " ", "\n", " ", "\r", " ")

// New는 헤더 행이 들어간 Table을 만든다.
func New(header ...string) *Table {
	t := &Table{}
	t.Row(header...)
	return t
}

// Row는 한 행을 추가한다.
func (t *Table) Row(cells ...string) {
	for i, c := range cells {
		if i > 0 {
			t.b.WriteByte('\t')
		}
		t.b.WriteString(cellCleaner.Replace(c))
	}
	t.b.WriteByte('\n')
}

func (t *Table) String() string { return t.b.String() }

// Save는 dir/name에 내용을 UTF-8로 저장한다. dir이 없으면 만든다.
func Save(dir, name, content string) error {
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return err
	}
	return os.WriteFile(filepath.Join(dir, name), []byte(content), 0o644)
}
