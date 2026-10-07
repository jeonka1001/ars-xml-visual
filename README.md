# arsxml2wv

Hansol 시나리오 XML을 읽어 음성 입력 노드마다 **보이는 ARS(WV) 화면 스크립트 초안**을 만드는 명령행 도구입니다.
생성 결과는 해당 노드의 PreScript에 붙여 넣는 JavaScript 코드 조각입니다.
도구는 원본 XML을 수정하지 않습니다.

- Windows 단일 exe로 실행됩니다. 런타임 설치나 인터넷 연결이 필요 없어 폐쇄망에서 사용할 수 있습니다.
- 입력 XML 안의 스크립트는 실행하지 않고 문자열로만 해석합니다. 외부 엔티티(XXE)도 처리하지 않습니다.

## 실행

사용자용 상세 안내(작업 순서, review.txt 조치, 문제 해결)는 [docs/USAGE.md](docs/USAGE.md)를 참고하세요.

```bat
arsxml2wv.exe <input.xml> <output-dir> [labels.properties]
```

- 출력 폴더가 없으면 만들고, 같은 이름의 파일은 덮어씁니다.
- 콘솔 메시지는 한글 Windows 콘솔에서 깨지지 않도록 영문으로 출력합니다.
- 결과 파일은 모두 UTF-8로 저장합니다.

## 출력

| 파일 | 내용 |
|---|---|
| `NNN_node_<Id>_<화면코드>.js` | 노드별 WV 화면 스크립트 (PreScript용) |
| `review.txt` | 사람이 확인할 항목: 추정한 문구·화면코드, 생략한 버튼, 변환하지 못한 노드 |
| `buttons.tsv` | 메뉴 노드의 키별 버튼 문구와 출처 (override / comment / branch / none) |
| `inputs.tsv` | 입력 노드의 해석 결과: 마스크, 자릿수, 종료키, 멘트 코드, 안내 주석 |
| `nodes.tsv`, `links.tsv` | 전체 노드와 링크 목록 |

생성된 스크립트는 초안입니다. 반드시 `review.txt`를 확인한 뒤 사용하세요.

## 변환 규칙

| 대상 노드 | 화면코드 | 본문 |
|---|---|---|
| `CallPageNode` → `*_jmcode.xml` | `SHKC10` 종목 검색 | `INPH`, `INBTN` 검색 |
| `CallPageNode` → `inputDTMF*.xml` (digitMask) | `SHKC21` 숫자 입력 | `INPH`(최소·최대 자릿수), `INBTN` 확인 |
| `CallPageNode` → `InputDTMF_Menu.xml`, 처리 거래 전 | `SHKD12` 확인 메뉴 | `BTN` |
| `CallPageNode` → `InputDTMF_Menu.xml`, 처리 거래 후 | `SHKE00` 완료 메뉴 | `BTN` |

- **처리 거래:** `app.trCode`가 `u01`처럼 `u+숫자`로 끝나는 노드입니다. 이 노드에서 링크로 도달할 수 있는 메뉴는 완료 메뉴로 봅니다.
- **공통 레코드:** `S`, `BTH`, `TIT`, `TXT`, `CHA`, `BOT`와 `wvMsgType="WV2000Q"`, `wvMaxRetryCount="2"`를 넣습니다. `wvReadTimeout`은 완료 화면이 `A`, 나머지는 `B`입니다. 형식은 `internal/wv/templates/screen.js.tmpl`에서 바꿀 수 있습니다.
- **구분자:** `\$`를 씁니다. 한글 Windows에서는 `₩$`로 보입니다. 문구에 `$`나 `;`가 있으면 규격이 깨지므로 해당 화면을 만들지 않고 `review.txt`에 남깁니다.
- **PreScript 해석:** `app.digitMask = "12*#";`처럼 문자열을 한 번만 대입한 경우만 해석합니다. 운영 XML의 오타 `digitLegth`도 인식합니다. 변수 대입, 중복 대입, if/switch 같은 분기문이 있으면 그 노드는 건너뜁니다.
- **버튼 문구 우선순위:**
  1. `labels.properties`
  2. PreScript 첫 주석 (예: "지정가는 1번", "별표", "우물정자")
  3. 입력체크 Switch 분기가 도착하는 이동 노드의 이름

  문구를 찾지 못한 키는 버튼을 만들지 않습니다.
- **제목(TIT):** `labels.properties` 값이 우선이고, 없으면 첫 MemoNode 이름에서 앞의 메뉴 번호를 뺀 값을 씁니다.
- **안내 문구(TXT):** `labels.properties` 값이 우선이고, 없으면 안내 주석의 첫 문장을 씁니다. 첫 문장에 키 안내가 들어 있으면 "원하시는 메뉴를 선택해 주세요."를 씁니다.

## labels.properties

UTF-8로 작성합니다. 왼쪽 숫자는 노드 `Id`입니다(Sequence가 아님). 키 이름에 들어가는 `#`는 `\#`로 씁니다.
전체 예시는 `labels.example.properties`를 참고하세요.

```properties
# 모든 화면의 제목
title=자동이체 신청
# 노드별 제목, 안내 문구, 화면코드
54.title=자동이체 완료
54.text=신청이 완료되었습니다.
54.screen=SHKE00
# 버튼 문구
54.button.1=다른 계좌 신청
54.button.*=다시 듣기
54.button.\#=이전 단계
```

## 적용 범위

다음 작업은 사람이 화면을 설계해야 하므로 도구가 만들지 않습니다.

- 여러 음성 노드를 한 화면으로 합치기 (`TSEL`, `TOTAL`)
- TTS 동적 문구를 `LIS`/`TBL`로 바꾸기
- 완료 화면의 서비스 공통 버튼 구성
- 생성한 스크립트를 XML에 자동으로 삽입하기

화면코드 규칙은 운영 XML 1건에 있는 기존 WV 노드를 보고 추정했습니다. 다른 메뉴에 적용할 때는 `review.txt`를 확인하고, 필요하면 `<Id>.screen` 값으로 화면코드를 지정하세요.

## 개발

Go 1.22 이상이 필요합니다. 외부 라이브러리는 쓰지 않습니다.

```sh
go test ./...                                  # 단위 테스트 + 합성 시나리오 회귀 테스트
go test ./cmd/arsxml2wv -update                # 의도한 변경이면 기대 결과(testdata/golden) 갱신
scripts/regress.sh                             # 로컬 samples/*.xml 회귀 확인 (samples/expected 와 비교)
scripts/regress.sh update                      # 결과 검토 후 기대값 갱신

# Windows exe 빌드 (Mac/Linux에서 교차 빌드)
GOOS=windows GOARCH=amd64 CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o dist/arsxml2wv.exe ./cmd/arsxml2wv
```

- Go 1.21 이상으로 빌드한 exe는 Windows 10 / Server 2016 이상에서 실행됩니다.
- Windows 7 / Server 2012 환경이 있다면 Go 1.20으로 빌드해야 합니다.
- `samples/`(운영 XML 등)와 `dist/`는 Git에 올리지 않습니다.

### 구조

도메인마다 `entity`(모델·규칙), `service`(로직), `controller`(진입점·입출력)로 나눕니다.

| 패키지 | 역할 |
|---|---|
| `cmd/arsxml2wv` | 명령행 진입점 |
| `internal/diagram` | XML 파싱, 노드·링크 모델 |
| `internal/input` | 입력 노드 PreScript 해석 |
| `internal/label` | 버튼 문구 결정 |
| `internal/wv` | 화면코드 결정, 스크립트 생성 (템플릿 내장) |
| `internal/tsv` | 검토용 TSV 작성, 파일 저장 |
