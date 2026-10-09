# arsxml2wv

Hansol 시나리오 XML을 읽어 음성 입력 노드마다 **보이는 ARS(WV) 화면 스크립트 초안**을 만드는 명령행 도구입니다.
생성 결과는 해당 노드의 PreScript에 붙여 넣는 JavaScript 코드 조각입니다.
도구는 원본 XML을 수정하지 않습니다.

- Java 1.8 기반이며 외부 라이브러리를 쓰지 않습니다. 인터넷 연결 없이 폐쇄망에서 사용할 수 있습니다.
- `arsxml2wv.exe`는 같은 폴더의 `arsxml2wv.jar`를 설치된 Java(1.8 이상, `JAVA_HOME` 또는 `PATH`)로 실행하는 래퍼입니다. exe를 쓸 수 없으면 `arsxml2wv.bat`을 쓰거나 `java -jar arsxml2wv.jar ...`로 실행합니다.
- 입력 XML 안의 스크립트는 실행하지 않고 문자열로만 해석합니다. 외부 엔티티(XXE)도 처리하지 않습니다.

## 실행

사용자용 상세 안내(작업 순서, review.txt 조치, 문제 해결)는 [docs/USAGE.md](docs/USAGE.md)를 참고하세요.

```bat
arsxml2wv.exe <input.xml> <output-dir> [labels.properties]
```

배포 폴더 구성: `arsxml2wv.exe`, `arsxml2wv.jar`, `arsxml2wv.bat`, `USAGE.md`, `labels.example.properties`

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
- **공통 레코드:** `S`, `BTH`, `TIT`, `TXT`, `CHA`, `BOT`와 `wvMsgType="WV2000Q"`, `wvMaxRetryCount="2"`를 넣습니다. `wvReadTimeout`은 완료 화면이 `A`, 나머지는 `B`입니다. 형식은 `src/main/resources/screen.js.tmpl`에서 바꿀 수 있습니다.
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

JDK 1.8과 Maven이 필요합니다. 실행 의존성은 없고, 테스트에 JUnit 4만 씁니다.

```sh
mvn package                     # 컴파일 + 테스트 + target/arsxml2wv.jar 생성
mvn test -Dupdate=true          # 의도한 변경이면 E2E 기대 결과(src/test/resources/e2e/golden) 갱신
scripts/regress.sh              # 로컬 samples/*.xml 회귀 확인 (samples/expected 와 비교)
scripts/regress.sh update       # 결과 검토 후 기대값 갱신
scripts/package.sh              # 배포 묶음 dist/arsxml2wv/, dist/arsxml2wv.zip 생성 (인터넷 필요)
```

- **폐쇄망에서 코드를 수정한 경우:** `mvn package`로 `target/arsxml2wv.jar`만 다시 만들어 배포 폴더의 jar를 교체합니다. exe는 다시 만들 필요가 없습니다.
- **exe 래퍼:** `mvn -P exe package`로 만듭니다. Launch4j Maven 플러그인을 내려받아야 하므로 인터넷이 되는 PC에서 한 번만 실행합니다.
- JDK 9 이상으로 빌드해도 `release 8` 옵션이 자동으로 적용되어 Java 8 API만 사용합니다.
- `samples/`(운영 XML 등), `dist/`, `target/`은 Git에 올리지 않습니다.
- Go로 작성했던 이전 구현은 `go-final` 태그에 남아 있습니다. Java 버전은 같은 입력에 대해 Go 버전과 바이트 단위로 같은 결과를 냅니다(E2E 테스트로 확인).

### 구조

도메인마다 entity(모델·규칙), `*Service`(로직), `*Controller`(진입점·입출력)로 나눕니다. 패키지는 `com.wavve.arsxml2wv`입니다.

| 패키지 | 역할 |
|---|---|
| `Main` | 명령행 진입점 |
| `diagram` | XML 파싱(XXE 차단), 노드·링크 모델 |
| `input` | 입력 노드 PreScript 해석 |
| `label` | 버튼 문구 결정, labels.properties 읽기 |
| `wv` | 화면코드 결정, 스크립트 생성 (템플릿: `src/main/resources/screen.js.tmpl`) |
| `common` | TSV 작성·저장, Go와 같은 공백·정규식·따옴표 처리 |
