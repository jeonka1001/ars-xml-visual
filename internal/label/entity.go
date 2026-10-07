// Package label은 메뉴 노드의 키별 버튼 문구를 정한다.
package label

// Source는 버튼 문구를 어디서 가져왔는지 나타낸다.
type Source string

const (
	SourceOverride Source = "override" // labels.properties
	SourceComment  Source = "comment"  // PreScript 안내 주석
	SourceBranch   Source = "branch"   // 입력체크 Switch 분기의 도착 노드 이름
	SourceNone     Source = "none"     // 찾지 못함
)

// Button은 허용 키 1개의 문구다.
type Button struct {
	Key    string
	Label  string
	Source Source
}

// Resolved는 키가 정해졌는지 여부다. 정해지지 않은 키는 화면 버튼으로 만들지 않는다.
func (b Button) Resolved() bool { return b.Source != SourceNone }

// Inferred는 사람이 문구를 다듬어야 하는지 여부다.
func (b Button) Inferred() bool { return b.Source == SourceComment || b.Source == SourceBranch }

// Result는 메뉴 노드 1개의 버튼 목록이다. 순서는 digitMask 순서를 따른다.
type Result struct {
	NodeID  string
	Buttons []Button
}

// pick은 우선순위(설정 → 주석 → 분기) 순으로 첫 문구를 고른다.
func pick(key string, sources ...map[string]string) Button {
	order := []Source{SourceOverride, SourceComment, SourceBranch}
	for i, m := range sources {
		if v := m[key]; v != "" {
			return Button{Key: key, Label: v, Source: order[i]}
		}
	}
	return Button{Key: key, Source: SourceNone}
}
