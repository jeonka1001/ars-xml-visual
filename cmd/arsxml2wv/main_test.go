package main

import (
	"flag"
	"os"
	"path/filepath"
	"testing"

	"github.com/jeonka1001/ars-xml-visual/internal/tsv"
)

// 기대 결과 갱신: go test ./cmd/arsxml2wv -update
var update = flag.Bool("update", false, "rewrite testdata/golden from current output")

func TestRunGolden(t *testing.T) {
	out := t.TempDir()
	if err := run("testdata/scenario.xml", out, "testdata/labels.properties"); err != nil {
		t.Fatal(err)
	}
	golden := filepath.Join("testdata", "golden")
	if *update {
		writeGolden(t, golden, out)
	}
	compareDirs(t, golden, out)
}

func writeGolden(t *testing.T, golden, out string) {
	t.Helper()
	if err := os.RemoveAll(golden); err != nil {
		t.Fatal(err)
	}
	entries, err := os.ReadDir(out)
	if err != nil {
		t.Fatal(err)
	}
	for _, e := range entries {
		b, err := os.ReadFile(filepath.Join(out, e.Name()))
		if err == nil {
			err = tsv.Save(golden, e.Name(), string(b))
		}
		if err != nil {
			t.Fatal(err)
		}
	}
}

func compareDirs(t *testing.T, wantDir, gotDir string) {
	t.Helper()
	want, err := os.ReadDir(wantDir)
	if err != nil {
		t.Fatal(err)
	}
	got, err := os.ReadDir(gotDir)
	if err != nil {
		t.Fatal(err)
	}
	if len(want) != len(got) {
		t.Errorf("file count: got %d want %d", len(got), len(want))
	}
	for _, e := range want {
		w, _ := os.ReadFile(filepath.Join(wantDir, e.Name()))
		g, err := os.ReadFile(filepath.Join(gotDir, e.Name()))
		if err != nil || string(w) != string(g) {
			t.Errorf("%s differs from golden (run with -update after review)\n--- got ---\n%s", e.Name(), g)
		}
	}
}
