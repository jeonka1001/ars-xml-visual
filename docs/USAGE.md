# arsxml2wv 사용 가이드

시나리오 XML에서 보이는 ARS(WV) 화면 스크립트 초안을 자동으로 만드는 Windows 프로그램의 사용 방법입니다.
폐쇄망 PC에서 `arsxml2wv.exe` 파일 하나만으로 실행됩니다.

---

## 1. 개요

| 항목 | 내용 |
|---|---|
| 입력 | Hansol 시나리오 XML (예: `sh_menu3_6_1_4.xml`) |
| 출력 | 입력 노드마다 WV 화면 스크립트(`.js`)와 검토 파일 |
| 변환 대상 | 메뉴 선택(`InputDTMF_Menu.xml`), 숫자 입력(`inputDTMF*.xml`), 종목코드 입력(`*_jmcode.xml`)을 호출하는 노드 |
| 원본 영향 | 없음. XML을 읽기만 하고 수정하지 않습니다 |

생성된 스크립트는 **초안**입니다. 반드시 `review.txt`를 확인하고 문구를 다듬은 뒤 사용하세요.

## 2. 실행 환경

- Windows 10 / Windows Server 2016 이상 (64비트)
- 별도 설치 프로그램, Java, 인터넷 연결이 필요 없습니다.
- 반입할 파일은 `arsxml2wv.exe`, 이 문서, `labels.example.properties`(선택)입니다.

> 사내 백신이나 SmartScreen이 서명되지 않은 exe를 막을 수 있습니다. 반입 승인과 예외 등록 절차는 보안 담당자와 확인하세요.

## 3. 빠른 시작

1. 작업 폴더를 만들고 exe와 변환할 XML을 넣습니다.
   ```
   C:\wv\
   ├─ arsxml2wv.exe
   └─ sh_menu3_6_1_4.xml
   ```
2. 명령 프롬프트(cmd)를 열고 작업 폴더로 이동합니다.
   ```bat
   cd C:\wv
   ```
3. 실행합니다.
   ```bat
   arsxml2wv.exe sh_menu3_6_1_4.xml out
   ```
4. 아래와 같은 메시지가 나오면 성공입니다. 결과는 `out` 폴더에 생깁니다.
   ```
   nodes: 124, links: 153, input nodes: 9, screens: 10, review items: 40
   see review.txt for items to check
   ```

## 4. 명령 형식

```bat
arsxml2wv.exe <입력 XML> <출력 폴더> [문구 보완 파일]
```

| 인자 | 필수 | 설명 |
|---|---|---|
| 입력 XML | O | 변환할 시나리오 XML 경로 |
| 출력 폴더 | O | 결과를 저장할 폴더. 없으면 새로 만듭니다 |
| 문구 보완 파일 | X | 버튼·안내 문구를 직접 지정하는 `labels.properties` 경로 (6장 참고) |

- 경로에 공백이 있으면 큰따옴표로 감쌉니다. 예: `arsxml2wv.exe "D:\시나리오 XML\a.xml" "D:\결과"`
- 같은 출력 폴더에 다시 실행하면 같은 이름의 파일은 덮어씁니다. 하지만 이전 실행에만 있던 파일은 지워지지 않고 남습니다. 깨끗한 결과가 필요하면 새 폴더를 지정하세요.

**종료 코드** (배치 파일에서 `%ERRORLEVEL%`로 확인)

| 코드 | 의미 |
|---|---|
| 0 | 정상 완료 (review 항목이 있어도 0) |
| 1 | 오류: 파일 없음, XML 형식 오류 등 |
| 2 | 인자 개수 오류 (사용법 출력) |

## 5. 권장 작업 순서

```
① 1차 실행 → ② review.txt / buttons.tsv 확인 → ③ labels.properties 작성
→ ④ 문구 보완 파일과 함께 재실행 → ⑤ 스크립트를 디자이너에 붙여 넣기 → ⑥ 테스트
```

1. **1차 실행:** 문구 보완 파일 없이 실행합니다.
2. **결과 확인:** `review.txt`에서 확인할 항목을 봅니다(8장 참고). `buttons.tsv`에서는 추정된 버튼 문구를 확인합니다.
3. **문구 보완 파일 작성:** 어색한 문구, 빠진 버튼, 화면 제목을 지정합니다.
4. **재실행:**
   ```bat
   arsxml2wv.exe sh_menu3_6_1_4.xml out2 labels.properties
   ```
5. **디자이너 반영:** 생성된 `.js` 내용을 시나리오 디자이너의 WV 화면 호출 노드 PreScript에 붙여 넣습니다.
   - 기존 WV 노드는 `COMM_WebVoice.xml`을 호출하는 `CallPageNode`이고, 화면 입력값은 `app.sInputData`로 분기합니다.
   - 노드 배치와 분기 연결은 기존 WV 노드 구성을 참고해 직접 작성합니다.
6. **테스트:** 실제 화면 표시와 버튼 동작을 확인합니다.

## 6. 문구 보완 파일 (labels.properties)

자동으로 추정한 값 대신 원하는 값을 직접 지정하는 파일입니다. 이 파일 값이 항상 우선합니다.
`labels.example.properties`를 복사해서 작성하면 편합니다.

### 작성 규칙

- **UTF-8로 저장합니다.** 메모장에서는 "다른 이름으로 저장 → 인코딩: UTF-8"을 선택하세요. ANSI로 저장하면 한글이 깨집니다.
- 왼쪽 숫자는 XML의 노드 **Id**입니다(Sequence가 아님). `inputs.tsv`, `nodes.tsv`의 `node_id` 열에서 확인할 수 있습니다.
- `#` 또는 `!`로 시작하는 줄은 주석입니다.
- 키 이름에 들어가는 `#`는 `\#`로 씁니다. 예: `54.button.\#=이전 단계`
- 문구에 `$`나 `;`를 넣으면 화면 규격이 깨집니다. 이 경우 해당 화면은 생성되지 않습니다.

### 설정 항목

| 키 | 설명 | 예 |
|---|---|---|
| `title` | 모든 화면의 제목(TIT) | `title=자기융자 매도예약` |
| `<Id>.title` | 해당 화면만 제목 변경 | `54.title=예약 완료` |
| `<Id>.text` | 상단 안내 문구(TXT) | `14.text=예약 주문수량을 입력해 주세요.` |
| `<Id>.button.<키>` | 버튼 문구. 키는 `0`~`9`, `*`, `\#` | `49.button.1=주문 전송` |
| `<Id>.screen` | 화면코드 직접 지정 | `54.screen=SHKE00` |

### 기본값 (지정하지 않았을 때)

| 항목 | 기본값 |
|---|---|
| 제목 | 첫 번째 메모 노드 이름에서 앞의 메뉴 번호를 뺀 값 (예: `3-6-1-4 자기융자매도예약` → `자기융자매도예약`) |
| 안내 문구 | PreScript 첫 주석의 첫 문장. 그 문장에 키 안내("1번" 등)가 있으면 `원하시는 메뉴를 선택해 주세요.` |
| 버튼 문구 | PreScript 주석에서 추출 → 없으면 입력체크 분기의 도착 노드 이름 → 둘 다 없으면 버튼 생략 |
| 화면코드 | 7장 규칙으로 추정 |

## 7. 출력 파일

| 파일 | 내용 | 여는 방법 |
|---|---|---|
| `NNN_node_<Id>_<화면코드>.js` | 노드별 WV 화면 스크립트 | 메모장, VS Code |
| `review.txt` | 사람이 확인할 항목 목록 | 메모장 |
| `buttons.tsv` | 메뉴별 키, 버튼 문구, 출처 | Excel(아래 참고) |
| `inputs.tsv` | 입력 노드의 마스크, 자릿수, 멘트 코드, 안내 주석 | Excel |
| `nodes.tsv`, `links.tsv` | 전체 노드와 연결 목록 | Excel |

> **TSV를 Excel에서 열 때:** 더블클릭으로 열면 한글이 깨질 수 있습니다. Excel의 **데이터 → 텍스트/CSV에서**를 선택하고, 파일 원본을 **65001: 유니코드(UTF-8)**, 구분 기호를 **탭**으로 지정하세요.

### 화면코드 결정 규칙

| 대상 노드 | 화면코드 | 화면 구성 |
|---|---|---|
| `*_jmcode.xml` 호출 | `SHKC10` 종목 검색 | 종목명/코드 입력란, 검색 버튼 |
| `inputDTMF*.xml` 호출 | `SHKC21` 숫자 입력 | 입력란(최소~최대 자릿수), 확인 버튼 |
| `InputDTMF_Menu.xml` 호출, 처리 거래 전 | `SHKD12` 확인 메뉴 | 버튼 목록 |
| `InputDTMF_Menu.xml` 호출, 처리 거래 후 | `SHKE00` 완료 메뉴 | 버튼 목록 |

- **처리 거래:** `app.trCode`가 `u01`처럼 `u+숫자`로 끝나는 노드입니다.
- **숫자 입력 최소 자릿수:** 종료키에 `#`가 있으면 1, 없으면 입력 자릿수와 같습니다.

### 스크립트에서 `\$`가 `₩$`로 보이는 경우

정상입니다. 한글 Windows는 백슬래시(`\`)를 원화 기호(`₩`)로 표시합니다. 두 문자는 같은 문자이므로 그대로 붙여 넣으면 됩니다.

## 8. review.txt 항목별 조치

| 메시지 | 의미 | 조치 |
|---|---|---|
| `node N key K: label from comment, review wording` | 버튼 문구를 음성 안내 주석에서 추정함 | `buttons.tsv`에서 문구를 확인하고, 어색하면 `N.button.K` 지정 |
| `node N key K: label from branch, review wording` | 버튼 문구를 분기 도착 노드 이름에서 추정함 | 위와 같음 |
| `node N key K: no label; button omitted (add N.button.K)` | 문구를 찾지 못해 버튼을 만들지 않음 | 버튼이 필요하면 `N.button.K` 지정. `*`, `#`는 대부분 버튼이 필요 없음 |
| `node N: TXT taken from voice guide; review wording` | 안내 문구를 음성 안내 문장에서 가져옴 | 화면용 문구로 `N.text` 지정 (예: "우물정자를 눌러" 같은 표현 제거) |
| `node N: screen code C inferred` | 화면코드를 규칙으로 추정함 | 맞으면 그대로, 다르면 `N.screen` 지정 |
| `node N: complete screen buttons follow the voice menu; ...` | 완료 화면 버튼을 음성 메뉴 그대로 만듦 | 서비스 표준 완료 화면 버튼과 비교해 수정 |
| `node N: screen skipped: no resolved button` | 문구가 정해진 버튼이 하나도 없어 화면을 만들지 않음 | `N.button.<키>` 지정 |
| `node N: screen skipped: protocol delimiter ($ or ;) in text ...` | 문구에 `$` 또는 `;`가 있음 | 해당 문구에서 `$`, `;` 제거 |
| `node N: skipped: control flow in PreScript` | PreScript에 if/switch 등 분기가 있어 해석하지 않음 | 해당 화면은 직접 작성 |
| `node N: skipped: digitMask is not a simple string literal` | 마스크가 변수 등으로 정해짐 | 해당 화면은 직접 작성 |
| `node N: skipped: digitMask assigned 2 times` | 같은 설정을 여러 번 대입함 | 해당 화면은 직접 작성 |
| `node N: skipped: both digitLength and digitLegth assigned` | 자릿수 설정이 두 이름으로 모두 있음 | PreScript를 확인 |
| `node N: skipped: invalid digitMask / invalid digit length` | 마스크나 자릿수 값이 비정상 | PreScript를 확인 |
| `node N: skipped: menu page with length N` | 메뉴 페이지인데 자릿수가 1이 아님 | PreScript를 확인 |
| `node N: skipped: unsupported input page "..."` | 지원하지 않는 입력 페이지를 호출함 | 해당 화면은 직접 작성 |
| `node N: skipped: unclosed block comment in PreScript` | `/*` 주석이 닫히지 않음 | PreScript를 확인 |
| `link L: unresolved endpoint A -> B` | 존재하지 않는 노드를 가리키는 연결 | XML의 연결 상태를 확인 (변환은 계속 진행) |

## 9. 문제 해결

| 증상 | 원인과 조치 |
|---|---|
| `usage: arsxml2wv <input.xml> <output-dir> [labels.properties]` | 인자 개수가 맞지 않습니다. 경로에 공백이 있으면 큰따옴표로 감싸세요 |
| `error: open ...: The system cannot find the file specified.` | 파일 경로가 틀렸습니다. `dir`로 파일 이름을 확인하세요 |
| `error: ...: parse XML: ...` | XML 형식이 깨졌거나 시나리오 XML이 아닙니다 (최상위 요소가 `Diagram`이어야 함) |
| `error: ...: parse XML: xml: encoding "..." declared but Decoder.CharsetReader is nil` | XML이 UTF-8이 아닙니다. 디자이너에서 저장한 원본 XML(UTF-8)을 사용하세요 |
| `error: ...: duplicate node Id: N` | XML에 같은 Id의 노드가 두 개 있습니다. XML을 확인하세요 |
| 결과 파일의 한글이 깨짐 | 결과는 UTF-8입니다. 메모장이나 VS Code로 열고, Excel은 7장 방법으로 여세요 |
| 문구 보완이 반영되지 않음 | ① Id가 맞는지(Sequence 아님) ② 파일이 UTF-8인지 ③ `#` 키를 `\#`로 썼는지 ④ 실행할 때 세 번째 인자로 파일을 지정했는지 확인하세요 |
| exe가 실행되지 않거나 바로 삭제됨 | 백신이나 실행 정책이 차단한 경우입니다. 보안 담당자에게 예외 등록을 요청하세요 |
| "이 앱은 PC에서 실행할 수 없습니다" | 32비트 Windows이거나 Windows 10 이전 버전입니다. 개발 담당자에게 해당 환경용 빌드를 요청하세요 |

## 10. 지원하지 않는 기능

다음 작업은 사람이 화면을 설계해야 하므로 도구가 만들지 않습니다. 기존 WV 노드를 참고해 직접 작성하세요.

- 여러 음성 단계를 한 화면으로 합치기 (예: 주문구분·단가·수량·대출일을 한 화면으로 → `TSEL`, `TOTAL`)
- 조회 결과·TTS 문구를 목록이나 표로 보여 주기 (`LIS`, `TBL`)
- 완료 화면의 서비스 공통 버튼과 이미지 (`IMG` 등)
- 생성한 스크립트를 XML에 자동으로 삽입하기
