# v6 Video Source Analysis — BCI01M0000 (실제 폐쇄망 정상 화면)

Evidence: `KakaoTalk_20260819_172319366.jpg`(Design 정상 화면 스크린샷), `KakaoTalk_20260819_172339844.mp4`
(동일 화면의 WebSquare XML "소스" 탭을 처음부터 끝까지 스크롤 촬영, 43.25초, 1920x1080, 29.96fps).
`frame_components_websquare-SUPERSEDED.csv`는 사용하지 않았다.

## 1. 분석 방법

- 0.5초 간격으로 87개 frame 추출(OpenCV) → 시각 diff 기준 중복(스크롤 정지) frame 11개 제거 →
  76개 후보. 코드 영역만 crop 후 1.8배 확대해 판독성 개선.
- OCR은 사용하지 않았다(이전 라운드에서 이미 이 유형의 폰카메라 촬영본에 부적합함을 확인) — 전부
  vision 기반 직접 판독. 판독 불가/저신뢰 영역은 추측하지 않고 기록에서 제외했다(전체 스캔은
  아니며, 아래 §7에 한계를 명시한다).
- 문서 전체(라인 1부터 `</html>`까지)를 대표 시점(t=0, 8, 10, 11.5, 12, 16, 21, 26, 29, 30.5,
  35, 38, 42.5초 등)에서 고해상도로 직접 판독해 문서 구조 전체를 처음부터 끝까지 커버했다:
  `w2:columnInfo`/`w2:dataMap`/`w2:dataList`(스키마, 라인 1~약 180) → `xf:submission`/`xf:model`
  → `<body>` → 공통부(단일행 form, `xf:group tagname="table" class="w2tb_tb"` 기반 semantic
  table) → 반복부 제목/grid 1 → action button 행 → grid 2(반복부 조회 결과 grid) → hidden grid
  (엑셀 export용) → `</body></html>`.

## 2. Design 이미지와 XML 구조 대응

Design 이미지(BCI01M0000.xml, Studio "디자인" 탭)에서 관측되는 역할:

| Design 상의 역할 | XML 구조 |
|---|---|
| 공통부(회계지널내역) 섹션 제목 | `xf:group class="dfbox"` > `xf:group class="fl" style="width:100%"` > `w2:textbox class="df_tit" label="공통부(회계지널내역)"` |
| 상단 다열 입력 영역(label+input 표 형태) | `xf:group class="lybox"` > `xf:group class="ly_column col_5" id="grp_copt"` > `xf:group tagname="table" class="w2tb_tb"` > (`xf:group tagname="tr"` > `xf:group tagname="th" class="w2tb_th"` + `xf:group tagname="td" class="w2tb_td"`)* |
| 반복부(회계지널상세내역) 섹션 제목 | 동일 패턴, label만 다름 |
| 반복부 grid(상단, 편집 가능) | `w2:gridView class="wq_gvw" id="grid_rppt_list" dataList="data:GRID_RPPT"` |
| 반복부 action button(행추가/행삭제/엑셀업로드/양식다운로드) | `xf:trigger class="btn_cm[...]"` 4개 |
| 하단 다수 column grid(조회 결과) | `w2:gridView class="wq_gvw" id="grd_list" dataList="data:GRID_BCI01M0000_VIEW"` |
| 하단 실행/T자원분개 버튼 | `xf:group class="fr"` 안의 `xf:trigger class="btn_cm"`/`class="btn_cm req"` 2개 |
| (Design에는 안 보이는) 엑셀 export helper grid | `xf:group id="group_hidden" style="height:0px;"` 안의 `w2:gridView class="wq_gvw" id="grid_exctList"` |

## 3. 실제 확인된 component 수 / QName별 수

이번 영상에서 **class 속성을 가진(또는 명시적으로 빈 class를 가진) UI 요소**를 문서 전체에서
전수 판독한 결과(중복 제거, id+구조 기준):

| QName | 관측 수 | 비고 |
|---|---|---|
| `xf:group`(tagname=`th`, class=`w2tb_th`) | 다수(정확한 수 확인 불가 — 표 행이 매우 많아 전수 카운트는 하지 않았으나, 판독한 모든 인스턴스에서 예외 0건) | TABLE_HEADER |
| `xf:group`(tagname=`td`, class=`w2tb_td`) | 위와 동일 | TABLE_CELL |
| `xf:group`(tagname=`tr`) | th/td 쌍마다 1개(class 없음) | FORM_ROW |
| `xf:group`(tagname=`table`, class=`w2tb_tb`) | 1(공통부 테이블 root) | GRID(table) |
| `w2:textbox`(th 안의 label) | 다수, class는 거의 전부 빈 문자열 | LABEL |
| `w2:textbox`(class=`req`) | **1건**(`전표일자` label) | LABEL(required 표시) |
| `xf:input` | 9개 판독(전부 공통부 td 셀 내부) | INPUT |
| `xf:select1` | 3개 판독(Y/N 선택 2개 + 코드 선택 1개) | SELECT |
| `xf:textarea` | 1개(`ibx_rmrk`, 비고) | INPUT |
| `w2:inputCalendar` | 1개(`ica_slipDt`, 전표일자) | CALENDAR |
| `w2:gridView` | **3개**(`grid_rppt_list`/`grd_list`/`grid_exctList`, 전부 `class="wq_gvw"`) | GRID |
| `xf:trigger` | **6개**(`btn_add`/`btn_delete`/`btn_excelUpload`/`btn_xcelDwld`/`btn_exct`/`btn_tjrnl`, 전부 `class`에 `btn_cm` 토큰 포함) | ACTION_BUTTON |
| `xf:group`(class=`dfbox`/`fl`/`lybox`/`ly_column col_5`/`fr`) | 섹션당 각 1~2개 | LAYOUT_BOX |

**UNKNOWN 처리**: 스키마 영역(`w2:columnInfo`/`w2:dataMap`/`w2:dataList`, 약 180줄)은 UI
component가 아니라 데이터 모델 정의라 이 표에서 제외했다(요청된 QName/class/style 스키마와
무관 — column 정의는 class를 갖지 않는다). 판독 불가로 명시적으로 제외한 영역은 없다(모든
확대 frame이 고해상도로 legible했다) — 다만 표 행 전체를 프레임 단위로 전수 스캔하지는
않았으므로(스크롤 속도상 매 행을 다 캡처하지 못했을 가능성), "판독한 범위 내에서 예외 0건"이지
"이 화면의 모든 th/td 행을 전수 확인했다"는 뜻은 아니다 — §7 한계 참고.

## 4. class별 positive/negative/exception

| Class | Positive(관측) | Negative(반례) | 판정 근거 |
|---|---|---|---|
| `w2tb_th` | tagname=`th`인 모든 `xf:group`에서 100% 관측(예외 0) | 없음 | tagname 속성과 1:1 대응 |
| `w2tb_td` | tagname=`td`인 모든 `xf:group`에서 100% 관측(예외 0) | 없음 | tagname 속성과 1:1 대응 |
| `w2tb_tb` | tagname=`table`인 `xf:group`에서 관측(n=1, 단일 인스턴스) | 없음(표본 1) | tagname 속성과 대응하나 표본 극소 |
| `btn_cm` | **6/6** `xf:trigger`에서 base token으로 항상 포함(다른 modifier가 추가되는 경우도 base는 유지) | 없음(이 화면 내에서는) | component type(Button→xf:trigger) 자체와 대응 |
| `wq_gvw` | **3/3** `w2:gridView`에서 100% 관측(숨겨진 export grid 포함) | 없음(이 화면 내에서는) | component type(Grid→w2:gridView) 자체와 대응 |
| `req`(label) | `전표일자` th-label 1건에서 관측, 대응하는 실제 input(`ica_slipDt`)도 `class="req"` | 같은 화면에서 `mandatory="true"`인 다른 8개 `xf:input`/`xf:select1`(공통부, `ibx_slipScmt`/`ibx_tmpSlipno`/`ibx_itlk_Srno`/`ibx_canTrgtSlipno`/`ibx_altrAmt`/`ibx_altrAmtVrfcYn`/`ibx_rpptNcnt`/`ibx_canTrgtSlipJrnCoptNo`/`ibx_acno`/`sbx_w0TrnYn`/`ibx_trnCd`)는 **class가 전부 빈 문자열** — `mandatory="true"`가 `req`를 보장하지 않음(반증 다수) | **`mandatory="true" → req`는 반증됨**(REFUTED) |
| `req`(trigger) | `btn_tjrnl`(T자원분개 버튼)에서도 관측 | — | `req`가 "필수 입력 표시"만이 아니라 다른 문맥(예: 강조 action)에서도 쓰이는 다의적 토큰임을 시사 — 단일 semantic으로 설명 불가 |
| `dfbox`/`fl`/`df_tit` | 섹션 제목 2곳(공통부/반복부)에서 동일 패턴 반복(n=2, 예외 0) | 없음 | 구조적 패턴(섹션 제목 wrapper)과 대응하나 표본 작음(2) |
| `lybox`/`ly_column col_5` | 공통부 테이블 wrapper 1곳 + grid wrapper 1곳(n=2) | 없음 | layout wrapper 역할과 대응하나 표본 작음 |
| `fr` | 하단 button row wrapper 1곳(n=1) | 없음 | 표본 1 |

## 5. class별 판정

| Class | 판정 | 이유 |
|---|---|---|
| `btn_cm`(base) | **CONDITIONAL** | component type(xf:trigger) 자체와 1:1 대응, 이 화면 내 예외 0(6/6) — 단 단일 화면 evidence라는 한계 명시, role별 modifier(`req`/`xls_down`)는 별도 미적용 |
| `wq_gvw`(base) | **CONDITIONAL** | component type(w2:gridView) 자체와 1:1 대응, 이 화면 내 예외 0(3/3, 숨김 grid 포함) — 동일 한계 |
| `w2tb_th`/`w2tb_td`/`w2tb_tb` | **GENERALIZABLE(규칙 자체)** / **NOT_IMPLEMENTABLE_THIS_ROUND(구현 불가)** | tagname 속성과 완전히 1:1 대응하는 깨끗한 구조적 규칙이지만, 현재 Converter는 `tagname="th"/"td"/"table"` 속성이나 semantic table 계층 구조 자체를 전혀 생성하지 않는다(절대좌표 group/div 기반 레이아웃만 생성) — class 규칙 이전에 레이아웃 전략(AXIS-B, 기존 조사에서 "단일 patch 범위 초과"로 이미 판정됨) 자체가 없어 규칙을 걸 대상이 없다 |
| `req` | **UNRESOLVED** | mandatory=true와 1:1 대응 반증됨, trigger에도 나타나 다의적 — 안전하게 설명할 수 있는 단일 조건이 없음 |
| `tal`/`tac`/`tar` | **UNRESOLVED**(이번 영상에서 관측 자체 없음) | 이번 영상 전체에서 `tal`/`tac`/`tar` 토큰을 한 번도 확인하지 못했다 |
| `readOnly` | **UNRESOLVED**(이번 영상에서 관측 자체 없음) | `disabled="true"`는 여러 번 관측됐으나(별도의 실제 disabled 속성, 이미 Converter가 처리 중) `readOnly`라는 class 토큰 자체는 관측되지 않음 |
| `dfbox`/`fl`/`df_tit`/`lybox`/`ly_column`/`fr` | **NOT_IMPLEMENTABLE_THIS_ROUND** | `w2tb_th`/`td`와 동일한 이유 — semantic 섹션/레이아웃 구조 자체가 현재 Converter 모델에 없음 |

## 6. XPlatform 소스에서 role 판별 가능 여부([클래스명] 함수명 기준 source trace)

- **`xf:trigger`(Button)**: `[ComponentMappingRegistry]` static initializer에서 `"Button" → "xf:trigger"`로
  고정 매핑. `[WebSquareGenerator] convertChildren`이 `sourceTag`(XPlatform 태그명)를 이미 알고
  있으므로, "target QName == xf:trigger"라는 조건은 소스에서 100% 판별 가능하다 — **id/업무명
  불필요, component type만으로 충분**.
- **`w2:gridView`(Grid)**: 동일하게 `[ComponentMappingRegistry]`에서 `"Grid" → "w2:gridView"`로
  고정. `[WebSquareGenerator] convertChildren`의 `targetTag` 값으로 100% 판별 가능.
- **`req`(required)**: XPlatform에 `required` 속성을 읽는 코드 자체가 없다(이전 라운드 grep
  재확인, 이번에도 결과 동일: 0건). 설령 있었더라도 이번 영상에서 mandatory=true가 req와
  대응하지 않음을 반증했으므로 구현하지 않는다.
- **`tagname`(th/td/table)/`w2tb_*`/`dfbox`/`lybox` 등 layout role**: XPlatform source에 이런
  "표 헤더/셀/섹션 wrapper" 개념 자체가 없다(XPlatform은 절대좌표 컴포넌트 배치만 기술) —
  Converter가 이 구조를 만들려면 XPlatform component들을 그룹핑해 표 형태로 재배치하는 완전히
  새로운 레이아웃 추론 로직이 필요하다 — component class mapping의 범위를 넘는다.

## 7. 한계

- 단일 화면(BCI01M0000) evidence다 — 다른 화면에서도 `btn_cm`/`wq_gvw`가 100% 유지되는지는
  검증하지 못했다(구 RAW CSV의 다른 화면에서는 btn_cm/wq_gvw가 dominant이지만 100%는 아니었다 —
  단 그 evidence는 토큰을 정확히 분리하지 못했을 가능성이 있어 직접 비교는 어렵다, 아래 §8 참고).
- 표 행(th/td)을 프레임 단위로 전수 캡처하지 못했다 — 스크롤 사이 일부 행을 건너뛰었을 가능성이
  있다. 판독한 모든 인스턴스에서는 예외가 없었다는 것이지, "이 화면의 모든 행"을 확인했다는
  뜻은 아니다.
- OCR을 쓰지 않고 vision 직접 판독만 사용했으므로 일부 세부 속성 값(특히 스타일 픽셀 값)은
  근접값으로 옮겼을 수 있다 — class 문자열 자체는 고해상도 확대로 반복 확인해 신뢰도 HIGH로
  기록했다.

## 8.5 정정 (후속 corpus 재검증 라운드)

§5의 `btn_cm`/`wq_gvw` 판정을 "CONDITIONAL"로 표기했으나, 실제 구현(`resolveVideoEvidenceBaseClass`)은
target QName만 검사하는 **전역(global) 규칙**이다(source id/화면명 조건 없음) — 149-corpus
전수 조사로도 확인(12/12, 3/3, 예외 0). "CONDITIONAL"은 부정확한 표현이었다. 정확한 상태는:
`VIDEO_SCREEN_VERIFIED=TRUE`(단일 화면 내 100%) + `GLOBAL_V6_SEMANTIC_GENERALIZABLE=UNCONFIRMED`
(다른 화면에서도 항상 성립하는지는 단일 화면 evidence로 확정 불가) → **최종 판정 `FIX_CANDIDATE`
유지, `PATCH_READY` 아님**. 상세: `corpus-vs-video-evidence-reconciliation.md`.

## 8. 구 evidence(frame_components_websquare_raw.csv, 다른 화면)와의 관계

구 RAW CSV(video 1, 다른 실제 화면)의 `xf:trigger` dominant pattern은 `btn_cm`(38%, 16/42)였고
나머지는 `btn_cm_sch`/`req`/`fl`/`w2tb_th`/`btn_cm_search`처럼 기록되어 있었다. 이번 영상에서는
`btn_cm  xls_down`처럼 **공백으로 분리된 두 개의 별도 token**(base `btn_cm` + modifier
`xls_down`)이 실제로 관측됐다 — 구 RAW CSV가 `btn_cm_xls_down`처럼 밑줄로 이어붙인 하나의
token으로 기록한 것과 다르다. 구 RAW CSV는 먼 각도의 사진에서 정확한 공백 vs 밑줄 구분이
어려웠을 가능성이 있다(당시 confidence도 MEDIUM/LOW 위주였음). 이번 영상은 훨씬 선명해 공백
구분을 신뢰도 HIGH로 직접 확인했다. 이 차이 때문에, 구 RAW CSV의 "38%/42%만 dominant"라는 결과가
실제로는 "base token(`btn_cm`/`wq_gvw`)은 더 높은 비율로 일관되지만 modifier token 조합이
다양해서 전체 문자열 기준 집계에서는 낮게 나온 것"일 가능성이 있다 — 이번 영상의 100%(6/6, 3/3)
결과와 상충하지 않는다고 판단했다. 다만 이는 추정(INFERRED)이며, 구 RAW CSV를 재검증하지는
않았다 — base token만 별도로 다시 집계하려면 구 video 1을 재분석해야 하나 이번 라운드 범위 밖이다.
