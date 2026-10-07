// Package input은 고객 키 입력을 받는 노드(digitMask 설정 노드)를 해석한다.
package input

import (
	"fmt"
	"strings"
)

// Kind는 입력 노드의 종류다.
type Kind string

const (
	KindMenu   Kind = "menu"   // 한 자리 메뉴 선택 (InputDTMF_Menu.xml)
	KindNumber Kind = "number" // 여러 자리 숫자 입력 (inputDTMF*.xml)
)

const menuPage = "inputdtmf_menu.xml"

// Spec은 입력 노드 1개의 PreScript 설정값이다.
type Spec struct {
	NodeID   string
	NodeName string
	Page     string // 호출 대상 페이지 (TargetPage)
	Kind     Kind
	Mask     string // 허용 키 (digitMask)
	Length   int    // 입력 자릿수 (digitLength, 운영 XML의 digitLegth 포함)
	TermMask string // 종료 키 (termDigitMask)
	MentCode string // 안내 멘트 파일 코드 (예: A361121)
	Guide    string // PreScript 첫 주석의 안내 문구
}

// Keys는 허용 키를 마스크 순서대로 중복 없이 반환한다.
func (s Spec) Keys() []string {
	var keys []string
	seen := map[rune]bool{}
	for _, r := range s.Mask {
		if !seen[r] {
			seen[r] = true
			keys = append(keys, string(r))
		}
	}
	return keys
}

// kindOf는 호출 페이지와 자릿수로 입력 종류를 정한다.
func kindOf(page string, length int) (Kind, error) {
	p := strings.ToLower(page)
	switch {
	case p == menuPage && length == 1:
		return KindMenu, nil
	case p == menuPage:
		return "", fmt.Errorf("menu page with length %d", length)
	case strings.HasPrefix(p, "inputdtmf") && strings.HasSuffix(p, ".xml"):
		return KindNumber, nil
	}
	return "", fmt.Errorf("unsupported input page %q", page)
}

// validMask는 마스크가 0-9, *, # 로만 구성되었는지 확인한다.
func validMask(mask string) bool {
	if mask == "" {
		return false
	}
	return strings.Trim(mask, "0123456789*#") == ""
}
