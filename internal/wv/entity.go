// Package wv는 보이는 ARS(WV) 화면 스크립트를 만든다.
package wv

import (
	"fmt"
	"regexp"
	"strings"
)

// Code는 WV 화면코드다. 운영 XML의 기존 WV 노드에서 추정했다.
type Code string

const (
	CodeStockSearch Code = "SHKC10" // 공통 종목코드 검색
	CodeNumberInput Code = "SHKC21" // 숫자 입력
	CodeConfirm     Code = "SHKD12" // 확인·선택 메뉴 (거래 처리 전)
	CodeComplete    Code = "SHKE00" // 처리 완료 메뉴 (거래 처리 후)
)

// Screen은 노드 1개에 대응하는 WV 화면이다. Records는 JS 문자열 안에 그대로 들어갈 레코드다.
type Screen struct {
	NodeID   string
	NodeName string
	Code     Code
	Records  []string
}

// ReadTimeout은 완료 화면만 A, 나머지는 B다.
func (s Screen) ReadTimeout() string {
	if s.Code == CodeComplete {
		return "A"
	}
	return "B"
}

func backButton(c Code) string {
	if c == CodeNumberInput {
		return "ON"
	}
	return "OFF"
}

// newScreen은 공통 레코드(S, BTH, TIT, TXT) 뒤에 본문 레코드를 붙인다.
func newScreen(id, name string, code Code, title, text string, body [][]string) (Screen, error) {
	rows := [][]string{
		{"S", string(code)},
		{"BTH", "0", backButton(code)},
		{"BTH", "1", "ON"},
		{"TIT", "0", title},
		{"TXT", "0", "C", text},
	}
	s := Screen{NodeID: id, NodeName: strings.Join(strings.Fields(name), " "), Code: code}
	for _, r := range append(rows, body...) {
		rec, err := record(r[0], r[1:]...)
		if err != nil {
			return Screen{}, err
		}
		s.Records = append(s.Records, rec)
	}
	return s, nil
}

// 레코드 구분자는 JS 소스 기준 \$ (한글 Windows 표기 ₩$), 레코드 끝은 ; 이다.
const fieldSep = `\$`

var jsEscaper = strings.NewReplacer(`\`, `\\`, `"`, `\"`, "\r\n", " ", "\n", " ", "\r", " ", " ", " ", " ", " ")

// record는 TYPE\$f1\$f2; 형태를 만든다. 구분자($, ;)가 들어간 값은 규격을 깨므로 거부한다.
func record(typ string, fields ...string) (string, error) {
	parts := []string{typ}
	for _, f := range fields {
		if strings.ContainsAny(f, "$;") {
			return "", fmt.Errorf("protocol delimiter ($ or ;) in text %q", f)
		}
		parts = append(parts, jsEscaper.Replace(f))
	}
	return strings.Join(parts, fieldSep) + ";", nil
}

var fileNameUnsafe = regexp.MustCompile(`[^A-Za-z0-9_-]`)

// FileName은 출력 파일 이름이다. 순번 접두어로 정렬과 충돌 방지를 함께 한다.
func (s Screen) FileName(seq int) string {
	return fmt.Sprintf("%03d_node_%s_%s.js", seq, fileNameUnsafe.ReplaceAllString(s.NodeID, "_"), s.Code)
}
