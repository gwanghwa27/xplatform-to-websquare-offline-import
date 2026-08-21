# v6 Design Structure Alignment Analysis (v6-design-structure-alignment)

Base Freeze: `XPWS-OFFLINE-FREEZE-20260820-02`
Candidate: `work/closed-network-support/candidates/v6-design-structure-alignment/working-copy`

`NO_PAIRED_LEGACY_TO_V6_REFERENCE_AVAILABLE_BY_PROJECT_NATURE` -- 동일 화면의 XPlatform 원본과
실제 v6 변환 결과를 나란히 비교할 수 있는 evidence는 존재하지 않으며 존재할 수도 없다. 이 문서는
두 독립 evidence를 결합한다: (A) XPlatform corpus 자체의 source semantics, (B) 실제 폐쇄망 v6
video/Design evidence(사전 라운드에서 수집됨). 둘 사이의 연결은 전부 `INFERRED`로 표기한다.

`COMPONENT_CLASS_APPLICATION = PAUSED` -- 이번 라운드는 신규 class mapping/변경을 하지 않는다.
기존 class(`btn_cm`/`wq_gvw`/Combo `disabledClass`)는 무변경 보호.

## 후속 라운드(Real-Corpus Applicability Fix) 추가 기록

### `FREEZE_IMMUTABILITY_VIOLATION = YES` (이전 라운드, 기록만)

이전 evidence-closure 라운드에서 `Base Freeze -20260820-01` 디렉토리 안에서 명령을 실행해
생긴 untracked `logs/converter.log`를 **삭제**했다. tracked content(445/445)가 그대로였다는
사실과, "Base Freeze 디렉토리 내부에서 어떤 write/delete도 금지"라는 operational immutability
원칙을 어겼다는 사실은 **별개**다 -- 삭제 자체가 위반이었다는 점을 이 문서에 명시적으로
기록한다. 이번 라운드는 (a) Base Freeze 내부에서 어떤 명령도 실행하지 않았고(모든 실행은
candidate 디렉토리를 cwd로 사용, 출력은 `/tmp`), (b) Base Freeze 내부에 생성되는 파일이
있는지 매 실행 후 `find ... -newer` 로 확인했다 -- 이번 라운드에서 Base Freeze 디렉토리 내부에
어떤 write도 발생하지 않았음을 확인했다.

### `TABLE_LAYOUT_IMPLEMENTATION` 상태 정정

이전 라운드는 corpus 실측 결과(`HIGH_CONFIDENCE=0`)에도 불구하고 `FIX_CANDIDATE`로 보고했다 --
이는 승격 근거가 부족했다. 이번 라운드 시작 시점 기준으로 상태를 다음으로 정정한다:
`TABLE_LAYOUT_IMPLEMENTATION = UNRESOLVED` / `SYNTHETIC_ONLY_VERIFIED` (실제 corpus 적용
근거 없이 `FIX_CANDIDATE` 승격 금지).

## 시작 상태

- `PRODUCTION_DIFF_BEFORE_EDIT = 0` -- candidate 생성 직후 `src/main/java` Base Freeze
  `XPWS-OFFLINE-FREEZE-20260820-02`와 byte-identical 확인.
- `SOURCE_XFDL_DIFF = 0` -- `sample-phase3-project` 무변경.
- Base Freeze integrity: 445/445 PASS, ZIP SHA OK.

## GRP_CONTENT_DEPENDENCY_AUDIT

Production 수정 전 `grp_content` 관련 전체 call/reference를 조사했다(전체 `src/main/java` 대상
대소문자 구분 grep, 4개 파일에서 총 7개 지점 확인):

1. `[WebSquareGenerator] generate` (line ~157-162): `usedTargetIds.add("grp_content")` 등
   fixed-id 선예약.
2. `[WebSquareGenerator] appendBody` (line ~396-452): `body > grp_resultArea > grp_main >
   grp_content` 3단 wrapper 생성(이전 라운드 결과, 무변경) + `registerFormRootMapping(source)`
   호출.
3. `[WebSquareGenerator] registerFormRootMapping` (line ~1227-1236): XPlatform `Form`의 id를
   `componentIdMap`에 `"grp_content"` 값으로 등록(`Form -> grp_content`). 이 매핑이 생성된 JS의
   `scwin.__xpComponentIdMap`에 실려 `WebSquareScriptConverter`/`TabRuntimeScriptGenerator`가
   소비한다.
4. `TabRuntimeScriptGenerator.currentFrame()` (line 98): 생성된 JS 리터럴
   `component('grp_content').getScope()` -- 현재 페이지의 scoped WFrame을 id 기반으로 조회.
5. `TabRuntimeScriptGenerator.parentWindow(depth)` (line 99): 생성된 JS 리터럴
   `w.grp_content.getScope()` -- 상위 WFrame들을 id 기반으로 순회.
6. `XPlatformProjectConverter.writeTabRuntimeResources` (line ~253): 정적 placeholder
   `runtime/xplatform-tab-empty.xml`에 `<w2:group id="grp_content" .../>` 하드코딩(별도의
   독립 문서, 실제 페이지와 무관).
7. 3-4-5-6 전부 **id 기반**(문자열 `"grp_content"` 자체를 조회) lookup이며, DOM depth/부모
   구조에 의존하지 않는다.

**원칙 적용 결과**: 위 7개 지점 모두 `grp_content`라는 **id 문자열의 존재와 그 위치가 여전히
`componentIdMap`에서 Form과 매핑되어 있음**에만 의존한다. 이번 라운드는 `grp_content`를 rename/
remove하지 않고, 그 id/namespace/`registerFormRootMapping` 매핑을 전부 그대로 유지했다 --
따라서 이 7개 지점 중 단 하나도 코드/생성 결과 양쪽에서 변경되지 않았다(`diff -rq` 확인:
`TabRuntimeScriptGenerator.java`/`XPlatformProjectConverter.java` 완전 무변경).

**"grp_content 하나에 전체 화면을 몰아넣는 방식"에 대한 재평가**: 실제 조사 결과, 현재
Converter는 이미 XPlatform `Div`/`GroupBox`/`PopupDiv`를 만나면 각각 자체 `w2:group`으로
변환하고 그 안으로 재귀한다(`ComponentMappingRegistry`의 `container=true` 목록,
`convertChildren`의 `isContainerComponent` 분기) -- 즉 `grp_content` 자체가 flat한 단일
컨테이너는 아니었다. 실제 갭은 두 곳으로 좁혀진다: (a) `Layouts`/`Layout`이 pass-through로
완전히 flatten되어 있었다는 점, (b) `Grid`가 전용 wrapper 없이 부모에 직접 붙는다는 점. 이번
라운드는 이 두 갭만 최소 범위로 다룬다. `grp_content`의 id/역할(Form-scope 최상위 wrapper) 자체
는 변경하지 않는 것이 `V5_RUNTIME_REGRESSION_REQUIRED`(TabRuntimeScriptGenerator의 id 기반
lookup이 실제 v5 엔진에서 동작하는지 미검증) 리스크를 추가로 확대하지 않는 최소 위험 경로다.

## 새 Root/Body 구조 (변경 없음, carry-forward)

```
body
  xf:group id="grp_resultArea" style=""
    xf:group id="grp_main" style="height:Npx;" (또는 유효 height 없으면 빈 문자열)
      xf:group id="grp_content" style="width:Npx;height:Npx;"
        (변환된 XPlatform 계층 구조 -- 아래 참고)
```

이 3단 wrapper 자체는 직전 `v6-root-body-structure` 라운드에서 이미 `STUDIO_DESIGN_VERIFIED`로
사용자 폐쇄망 확인이 완료된 상태이며, 이번 라운드는 이를 그대로 carry-forward하고 재구현하지
않았다(`appendBody`/`buildRootStyle`/`buildMainAreaStyle` 전부 무변경).

`BODY_LIFECYCLE_ATTRIBUTES_PRESERVED = PASS` -- `bindFormLifecycle` 관련 코드 무변경, 실측
확인(`<body ev:onpageload="scwin.__xpws_onpageload">` 등 전체 136개 output에서 Base와
byte-identical).

## Div -> Group (기존 구현, 변경 없음)

`ComponentMappingRegistry`에 이미 `Div`/`GroupBox`/`PopupDiv` -> `w2:group`(container=true)로
등록되어 있고, `convertChildren`이 이미 이를 재귀 처리한다. 이번 라운드에서 코드 변경 없음 --
corpus 실측: `Div -> Group` 적용 2건(`Form/NestedContainer.xfdl`의 `divA`,
`Form/Main/TabExternalRelativePath.xfdl`의 `divWrap`), Base와 동일.

## Layout -> Table 구조 (신규 구현)

### 알고리즘 (magic pixel tolerance 없음)

`[ComponentLayoutConverter] classifyLayoutGeometry`가 `Layout` 직계 자식들의 left/top/width/
height를 읽어, **정확히 동일한 좌표값**(exact numeric equality, 임의 허용오차 없음)을 가진
자식만 같은 row/column으로 클러스터링한다:

1. 자식 수 < 4 또는 geometry 확정 불가 -> `UNRESOLVED_LAYOUT`
2. 고유 top 값(row) < 2 또는 고유 left 값(column) < 2 -> `ABSOLUTE_LAYOUT_FALLBACK`
3. 모든 row가 정확히 동일한 column 집합으로 완전히 채워진 사각 grid이고 겹침이 없음
   -> `TABLE_LAYOUT_HIGH_CONFIDENCE`
4. 그 외(row 클러스터링은 되나 사각형 아님/겹침 있음) -> `TABLE_LAYOUT_HEURISTIC`

`TABLE_LAYOUT_HIGH_CONFIDENCE`일 때만 `[WebSquareGenerator] convertLayoutAsTable`이
row-group/cell-group `xf:group` 구조를 생성한다(class/style 없음, id만 -- 상세는
`analysis/freeze-vs-candidate-function-diff.md`). 그 외 판정은 전부 기존과 100% 동일한 flat
pass-through로 위임되며, 이는 `convertChildren`의 5-인자 오버로드(무수정)를 그대로 호출하는
것으로 구현되어 있어 실제 output이 이전과 byte-identical함이 코드 구조상 보장된다.

### Corpus 실측 결과 (전체 135 XFDL)

| 분류 | 건수 | 비고 |
|---|---|---|
| `TABLE_LAYOUT_HIGH_CONFIDENCE` | 0 | 이번 corpus에는 완전히 채워진 사각 grid 사례 없음 |
| `TABLE_LAYOUT_HEURISTIC` | 1 | `Form/ControlPropertyMatrix.xfdl` -- row 클러스터링(top 6종)은 되나 row별 자식 수(2,3,4,4,2,2)가 달라 사각형 아님 |
| `UNRESOLVED_LAYOUT` | 127 | 자식 4개 미만 |
| `ABSOLUTE_LAYOUT_FALLBACK` | 0 | 이번 corpus에서는 UNRESOLVED_LAYOUT 조건에 먼저 걸림 |

이전 라운드(`v6-root-body-structure`)의 분류 결과(`HIGH_CONFIDENCE=0`, `HEURISTIC=1`,
`ABSOLUTE_LAYOUT_FALLBACK=134`)와 대분류가 일치한다(이번 라운드는 `UNRESOLVED_LAYOUT`을
별도로 분리했을 뿐, `HEURISTIC` 파일이 동일하게 `ControlPropertyMatrix.xfdl` 1건으로
재확인됨 -- 방법론이 다른 두 라운드에서 같은 결론에 도달).

**결과: 이번 라운드 corpus 기준 generated output 변경 0건** (`HIGH_CONFIDENCE`가 실제로 발생하지
않으므로) -- `EXPECTED_STRUCTURE_DIFF = 0`, `UNEXPECTED_GENERATED_DIFF = 0` 모두 실측 확인
(`diff -rq` Base Freeze 대비 136개 output 전체 0 diff).

### `UNRESOLVED_LAYOUT` 127건 원인 분석 (후속 라운드)

Production 수정 전, 127건을 fallback으로 숨기지 않고 실제 root cause를 분류했다(도구:
`analysis/tooling/analyze_unresolved_layout.py`, 원본 XFDL을 직접 파싱해 파이썬으로
`classifyLayoutGeometry`와 동일한 판정 로직을 독립적으로 재현 -- Java 코드를 재구현한 것이
아니라 별도 언어로 cross-check함으로써 classifier 자체의 오류 가능성도 함께 검증). 전체
129개 first-Layout(각 `Layouts`의 첫 `Layout`) 기준 실측 reason bucket:

| Reason bucket | 건수 |
|---|---|
| `too_few_children(<4)` | 121 |
| `no_children` | 7 |
| `non_rectangular_or_incomplete_row` | 1 (`ControlPropertyMatrix.xfdl`, 아래 상세 trace) |
| `geometry_missing_or_invalid` | 0 |
| `single_row_or_column`(row 또는 column 1개뿐) | 0 |
| `overlap` | 0 |
| `HIGH_CONFIDENCE` | 0 |

`too_few_children(<4)` 121건의 직계 자식 태그 분포(중복 포함, 자식이 여러 개인 경우 전부 카운트):
`Tab=97`, `Edit=9`, `Static=8`, `Button=6`, `Div=2`, `Combo=1`, `Radio=1`, `Grid=1`,
`GroupBox=1`. **97/121(80%)이 자식으로 `Tab` 컴포넌트 단 하나만 가지고 있다** -- 즉 이 corpus는
Tab 런타임 로직(비동기 로드/부모-자식 통신/생명주기 등) 검증을 위해 설계된 최소 fixture
집합이며, 화면 전체가 `Tab` 컨트롤 하나로 구성된 케이스가 압도적 다수다. 나머지도 대부분
Edit/Static/Button 2~3개짜리 최소 구성 화면이다(예: `Form/DatasetBinding.xfdl` = `Edit`,
`Combo`, `Radio` 3개).

**"classifier가 잘못된 node level을 보고 있는지" 확인 결과**: 아니오. `convertLayoutAsTable`은
재귀 경로(Div/GroupBox 컨테이너 안으로 들어가도 그 안의 `Layouts`/`Layout`에 대해 다시
hook이 걸림)를 통해 corpus 전체 129개 first-Layout 모두에 대해 동일하게 동작하고 있음을
확인했다(파이썬 독립 재현 결과 `total_first_layout_count=129`가 Java 로그의 실행 횟수(128,
1건은 외부 참조 Tab 콘텐츠 등 `convertChildren`이 도달하지 않는 경로)와 거의 일치, 차이 1건은
이미 이전 라운드에서 "convertChildren 미도달 경로" 특성으로 설명됨). classifier의 traversal
로직 자체에는 결함이 없다 -- 문제는 corpus 자체가 rectangular grid 패턴을 가진 실제 업무 화면을
포함하지 않는다는 corpus 특성이다.

### `TABLE_LAYOUT_HEURISTIC` 유일 사례 상세 trace: `Form/ControlPropertyMatrix.xfdl`

- source file: `sample-phase3-project/Form/ControlPropertyMatrix.xfdl`
- container path: `Form > Layouts > Layout`(root-level, `Div`/`GroupBox` 조상 없음)
- child count: 17
- 각 child geometry(left/top/width/height, px):

| id | tag | left | top | width | height |
|---|---|---|---|---|---|
| sta | Static | 10 | 10 | 100 | 24 |
| btn | Button | 120 | 10 | 100 | 24 |
| edt | Edit | 10 | 50 | 120 | 24 |
| mask | MaskEdit | 140 | 50 | 120 | 24 |
| txt | TextArea | 270 | 50 | 120 | 60 |
| cbo | Combo | 10 | 120 | 120 | 24 |
| lst | ListBox | 140 | 120 | 120 | 80 |
| rdo | Radio | 270 | 120 | 160 | 60 |
| chk | CheckBox | 440 | 120 | 100 | 24 |
| cal | Calendar | 10 | 220 | 140 | 24 |
| spn | Spin | 160 | 220 | 100 | 24 |
| img | ImageViewer | 270 | 220 | 100 | 80 |
| prg | ProgressBar | 380 | 220 | 160 | 24 |
| pop | PopupDiv | 10 | 320 | 220 | 120 |
| web | WebBrowser | 240 | 320 | 260 | 120 |
| up | FileUpload | 10 | 460 | 200 | 30 |
| down | FileDownload | 220 | 460 | 200 | 30 |

- 계산된 row(고유 top, 6개): `{10, 50, 120, 220, 320, 460}`, row별 자식 수:
  `2, 3, 4, 4, 2, 2`
- 계산된 column(고유 left, 전체 9개): `{10, 120, 140, 160, 220, 240, 270, 380, 440}`
- **`HIGH_CONFIDENCE`가 되지 못한 정확한 이유**: rectangular 판정은 "모든 row가 정확히 동일한
  9개 column 집합을 채워야 한다"를 요구하는데, 실제로는 row마다 서로 다른(대부분 겹치지 않는)
  left 값 집합을 쓴다 -- 예: `top=10` row는 `left={10,120}`만 쓰지만 `top=120` row는
  `left={10,140,270,440}`을 쓴다(교집합 없음). 이는 반복되는 테이블 구조가 아니라, 컴포넌트
  타입별로 폭이 제각각인 "컴포넌트 쇼케이스" 화면을 수동으로 자유 배치한 것이다.
- **안전하게 해결 불가능 -> `UNSUPPORTED_SEMANTIC`으로 유지, Production 수정하지 않음.**
  이 파일에 특화된 규칙(예: "row별 column 수가 달라도 순서만 맞으면 허용")을 추가하면 이
  1개 파일에는 맞을 수 있으나, corpus 밖 실제 업무 화면에 대해 검증되지 않은 fixture-특화
  휴리스틱이 되어 evidence-only 원칙을 위반한다.

### 실제 corpus 적용 가능성 결론

97/121(80%)이 `Tab` 단일-자식이고, 나머지도 대부분 2~3개 최소 컴포넌트 구성이며, 유일한
다중-컴포넌트 케이스(`ControlPropertyMatrix.xfdl`)조차 진짜 grid가 아니라는 것이 확인되어,
**이 corpus 안에는 generic table-detection 알고리즘이 안전하게 적용될 수 있는 실제 rectangular
grid 사례가 존재하지 않는다.** 이는 classifier 결함이 아니라 corpus의 본질적 특성(Tab-runtime
기능 테스트 fixture 위주, 실제 업무 화면의 반복 레이아웃 데이터 없음)이다. Production 코드를
추가로 수정할 근거가 없으므로 **이번 라운드는 Production을 변경하지 않았다**
(`NO PRODUCTION CODE CHANGE`).

### 알고리즘 실동작 증거

corpus에 `HIGH_CONFIDENCE` 실사례가 없어, 별도의 최소 합성 XFDL(corpus 밖, 2x2 rectangular
grid)로 단위 검증했다 -- 상세 로그와 생성 XML은
`analysis/freeze-vs-candidate-function-diff.md`의 `convertLayoutAsTable` 섹션 참고. 요약:
`classifyLayoutGeometry`가 `TABLE_LAYOUT_HIGH_CONFIDENCE`를 정확히 반환하고,
`row0/row1 x col0/col1` 4개 `xf:group` wrapper가 생성되며, 각 셀 안의 실제 컴포넌트(`w2:span`)
는 id/label/style이 기존 컴포넌트 변환 로직 그대로 유지됨을 확인했다.

### Table source convention (evidence, 이번 라운드 미적용)

실제 v6 native evidence(사전 라운드 video 확인)는 `xf:group tagname="table" class="w2tb_tb"` >
`xf:group tagname="tr"` > `xf:group tagname="th"/"td" class="w2tb_th"/"w2tb_td"` 구조를
보여준다(`candidates/v6-class-mapping/analysis/v6-video-source-analysis.md`,
`v6-video-source-components.csv`). 이는 `INFERRED` 수준에서 XPlatform Layout geometry와 연결될
뿐, 직접 pairing evidence는 없다. 이번 라운드는 **class 적용 라운드가 아니므로**
`tagname`/`w2tb_th`/`w2tb_td`/`w2tb_tb`/`dfbox`/`fl`/`lybox`/`ly_column`/`fr` 등 어떤 class나
`tagname` 속성도 부여하지 않았다 -- 순수 group nesting(구조)만 구현했다. 이 class/tagname
적용은 `NEXT_ROUND_CANDIDATE`로 기록한다.

## Root 구조 목표 재확인 (A vs B)

`ROOT_STRUCTURE_ALIGNMENT = NOT_IMPLEMENTED`

목표 개념(`body > grp_resultArea > grp_main > 변환된 Div/Layout 구조`)에 대해 두 방식을
비교했다:

- **A. `grp_content`를 compatibility container로 유지하면서 그 내부에 Div/Layout 구조를
  생성** -- 이번 candidate가 채택한 방식. `grp_content`의 id/namespace/역할은
  `GRP_CONTENT_DEPENDENCY_AUDIT`(위 섹션)에서 확인된 7개 dependency 지점을 전혀 건드리지
  않고, 그 안쪽 자식 구조만 `Layout -> Table` 알고리즘으로 재구성한다.
- **B. `grp_content` 역할을 generic container mapping으로 대체**(예: Form -> grp_main 직접
  매핑, `grp_content` id 제거) -- 이번 라운드는 구현하지 않았다. `GRP_CONTENT_DEPENDENCY_AUDIT`
  에서 확인된 `TabRuntimeScriptGenerator`의 id 기반 lookup(`component('grp_content')`,
  `w.grp_content`)이 실제 v5/v6 엔진에서 어떻게 동작하는지 실측 evidence가 없는 상태에서
  B를 구현하면 `V5_RUNTIME_REGRESSION_REQUIRED` 리스크를 추가로 확대한다 -- evidence 없이
  구현하지 않는다는 원칙에 따라 보류.

**결과**: A 방식의 코드(`convertLayoutAsTable`)는 구현되어 있으나, 위 원인 분석에서 확인된 대로
이번 corpus에는 `TABLE_LAYOUT_HIGH_CONFIDENCE` 실사례가 0건이라 실제로 트리거된 적이 없다.
따라서 관측 가능한 generated output 기준으로는 `grp_main -> grp_content -> 전체 content`
구조가 136/136 그대로이며, Div/Layout 구조가 실제로 관측 가능한 형태로 정렬된 사례는 없다 --
`ROOT_STRUCTURE_ALIGNMENT = NOT_IMPLEMENTED`로 명시한다(코드는 준비되어 있으나 corpus에서
관측되지 않음).

## Grid -> Group 재평가

`GRID_GROUP_STRUCTURE = UNRESOLVED`

재조사 결과(`candidates/*/analysis/evidence/v6-video-source-components.csv`), 동일한 실제 v6
화면 안에서도 Grid 2개의 부모 구조가 서로 다르게 관측되었다: 하나(`grid_rppt_list`)는 별도
class 없는 `xf:group`에 직접 위치, 다른 하나(`grd_list`)는 `xf:group class="lybox"`로 감싸져
있음. 즉 "Grid는 항상 전용 Group에 감싸진다"는 general rule을 뒷받침할 만큼 evidence가
일관되지 않는다. 특정 fixture ID 하드코딩이나 임의 판단으로 general rule을 만들지 않고,
`UNRESOLVED`로 남긴다.

기존 `[WebSquareGenerator] convertChildren`은 이미 XPlatform Grid의 실제 부모 컨테이너(Div 등)
구조를 그대로 보존하고 있으므로(`GRID_PARENT_STRUCTURE = ALREADY_CORRECT`, 이전 라운드 결론
carry-forward), Grid가 Div/GroupBox 안에 있으면 이미 그 `w2:group` 안에 위치한다 -- 별도의
Grid 전용 wrapper 추가 근거가 부족하다. 이번 라운드는 Grid 관련 코드를 전혀 수정하지 않았다.
`wq_gvw`/`w2:gridView` semantic 무변경(corpus 실측 3건, Base와 동일).

## 최종 Root hierarchy / 집계

| 항목 | 결과 |
|---|---|
| Root hierarchy | `body > grp_resultArea > grp_main > grp_content > (변환된 content)` -- Base와 동일 구조, carry-forward. `ROOT_STRUCTURE_ALIGNMENT = NOT_IMPLEMENTED`(코드는 준비, corpus 미관측) |
| global `grp_content` 잔존 output 수 | 136/136(placeholder 포함) -- Base와 동일 |
| Div -> Group 적용 수 | 2/135 -- Base와 동일(기존 구현 재사용) |
| Layout -> Table 적용 수(real corpus) | **0/135** -- 원인: corpus의 97/121(80%)이 `Tab` 단일-자식 fixture, 유일한 다중-컴포넌트 케이스도 진짜 grid 아님(위 원인 분석 참고). classifier 결함 아님, corpus 특성 |
| Layout -> Table 적용 수(synthetic) | 1/1(2x2 합성 grid, corpus 밖) |
| `GRID_GROUP_STRUCTURE` | `UNRESOLVED`(evidence 비일관, 코드 무변경) |
| `SOURCE_TO_TARGET_ID_MAP_IDENTICAL` | **PASS**(canonical full map, 403/403 key 동일, Base-only 0, Candidate-only 0, changed 0 -- reflection 기반 실측, 로그 비교 아님) |
| component count preservation | PASS(136개 output 전체 byte-identical) |
| class diff | 없음(`btn_cm`=12, `wq_gvw`=3, Combo `disabledClass`=4 -- 전부 Base와 동일) |
| QName diff | 없음(전체 output byte-identical) |
| body lifecycle | PASS(무변경) |

## 폐쇄망 Studio에서 사용자가 확인해야 할 항목

이번 라운드는 corpus 기준 실제 generated output 변화가 없으므로(`HIGH_CONFIDENCE` 0건), Studio
확인은 **구조적으로 새로운 것이 없다** -- 이미 이전 라운드에서 `STUDIO_DESIGN_VERIFIED`된
Root/Body 구조가 그대로 유지된다. 다만 아래는 향후(다음 corpus에 rectangular grid 케이스가
생기거나, 별도 검증 화면을 폐쇄망에 반입할 경우) Table 구조 실사용 전 확인이 필요한 항목이다:

1. `Form/ControlPropertyMatrix.xfdl` 변환 결과(`TABLE_LAYOUT_HEURISTIC`, 실제로는 무변경) --
   Base와 동일하게 Studio에서 정상 렌더링되는지 재확인(회귀 없음 재확인 목적).
2. 합성 검증에 사용한 2x2 grid 구조(문서에 XML 첨부됨, corpus에는 없음) -- 만약 실제 업무 화면
   중 유사한 rectangular grid 패턴이 있다면, 그 화면을 이 candidate로 재변환해 Design Canvas에서
   실제로 정상 렌더링되는지 확인 필요.
3. `TABLE_LAYOUT_IMPLEMENTATION = FIX_CANDIDATE` 전체 -- corpus에 HIGH_CONFIDENCE 사례가
   전무하므로, 실제 업무 화면 데이터가 없이는 이 경로 자체의 Studio 검증이 원천적으로 불가능
   하다(`NO_PAIRED_LEGACY_TO_V6_REFERENCE_AVAILABLE_BY_PROJECT_NATURE`와 같은 종류의 evidence
   한계).

## 최종 상태 (Real-Corpus Applicability Fix 라운드 정정)

`DESIGN_STRUCTURE_CANDIDATE` / **`TABLE_LAYOUT_IMPLEMENTATION = UNRESOLVED`** /
**`EVIDENCE_INSUFFICIENT`**(real corpus 적용 0건, synthetic만 검증됨 -- Gate B) /
`STATIC_VERIFIED`. Table 관련해서는 `STUDIO_DESIGN_REQUIRED` 대상으로 넘기지 않는다(검증할
실제 사례 자체가 corpus에 없음). `STUDIO_DESIGN_VERIFIED`/`FIXED`/`PATCH_READY`/`FREEZE_READY`/
`REAL_RUNTIME_VERIFIED`는 선언하지 않는다.

이전 라운드의 `FIX_CANDIDATE` 판정은 정정되었다 -- corpus 실측(`HIGH_CONFIDENCE=0`)만으로는
승격 근거가 되지 못하며, 이번 라운드의 원인 분석(위 "실제 corpus 적용 가능성 결론") 결과 이
corpus에는 애초에 rectangular grid 패턴을 가진 실제 업무 화면이 없다는 것이 명확해졌다. 코드
자체(`classifyLayoutGeometry`/`buildTableRows`/`convertLayoutAsTable`)는 합성 케이스로
정확성이 검증되어 candidate에 유지하지만(향후 rectangular grid를 가진 실제 화면이 corpus에
추가되면 자동으로 동작할 준비된 상태), 이번 라운드 corpus 기준으로는 아무 것도 트리거되지
않으므로 `FIX_CANDIDATE`(실사용 가능 후보)라고 부를 근거가 없다.

## NEXT_ROUND_CANDIDATE

- Table 구조에 `tagname`/`w2tb_th`/`w2tb_td`/`w2tb_tb` class 적용(별도 class 적용 라운드,
  local evidence 재확인 후) -- 단, real corpus 적용 사례가 먼저 확보되어야 의미가 있음.
- `dfbox`/`fl`/`lybox`/`ly_column`/`fr` 등 section-title/button-row/layout-box wrapper 클래스
  적용(마찬가지로 별도 class 라운드).
- Grid Group 구조: 실제 업무 화면 추가 evidence 확보 시 `GRID_GROUP_STRUCTURE` 재평가.
- `LIFECYCLE_PROPERTY_GAP`(`ev:onpageunload` 등, 이전 라운드부터 이월된 항목, 이번 라운드
  미착수).
- corpus에 실제 rectangular grid 패턴을 가진 업무 화면 fixture가 추가되면
  `TABLE_LAYOUT_IMPLEMENTATION` 재평가(현재 코드는 그대로 대응 가능하도록 준비되어 있음).

## Reopen 라운드 — Real Business XFDL Div/Layout Structure

`USER_PROVIDED_REAL_BUSINESS_XFDL_VISUAL_EVIDENCE = ACCEPTED`. 사용자가 실제 업무 UX Studio
화면(`STT00030.xfdl`, 조회조건 + 요약 grid + 상세 `w2:gridView` 구성)을 직접 촬영해 제공했고,
Source 계층이 `Form > Layouts > Layout > Div > Layouts > Layout > (label/input 다수)` 및 별도
`Grid` component로 구성되어 있음을 확인했다. 폐쇄망 특성상 해당 실제 XFDL 파일 자체는 candidate로
반입하지 않았다(`file upload/download` 미실행) — 대신 아래 순서로 조사했다.

### 1) Traversal 레벨 재검증 — `children<4` 판정이 어느 level인지

기존 `[WebSquareGenerator] convertChildren`/`convertLayoutAsTable`(이전 라운드부터 존재, 이번
라운드 무변경)을 다시 읽은 결과, `Layout` 판정은 이미 **깊이 무관(depth-agnostic)** 이다:

- `Div`는 `isContainerComponent`에 의해 container로 인식되어 `w2:group` 생성 후 재귀 호출된다
  (`WebSquareGenerator.java:572-580`).
- 그 재귀 호출 안에서 `Div`의 자식으로 나타나는 `Layouts`/`Layout`도 최상위 `Form`의 `Layouts`/
  `Layout`과 동일한 코드 경로(`shouldTraverseUnknownElement` → `"Layout".equals(sourceTag)` →
  `convertLayoutAsTable`)를 그대로 탄다(`WebSquareGenerator.java:585-608`).
- `convertLayoutAsTable`이 `classifyLayoutGeometry`에 넘기는 `children`은 항상 **그 `Layout`
  엘리먼트 자신의 직계 자식**이다(`directElementChildren(layout)`) — Form root Layout이든 Div
  내부 Layout이든 동일한 방식으로, 자신의 직계 자식만 본다.

즉 "root Layout children<4 → Div 내부는 안 봄" 같은 wrong-level 버그는 **없다**. 재검증을 위해
`analysis/tooling/analyze_unresolved_layout.py`를 ancestor-tracking 버전으로 확장해
(`root.iter()` 전체 순회 시 각 `Layouts` 엘리먼트가 `Div` 조상을 가지는지 태깅) local corpus를
다시 스캔했다: **corpus 전체에서 `Div` 내부 `Layouts`는 단 2건뿐이며, 둘 다 자식 1개**였다(2개
이상 자식을 가진 `Div` 내부 `Layout` = 0건). 즉 이전 라운드의 "real corpus 적용 0" 결론은
wrong-level 판정 때문이 아니라, **local corpus 자체가 사용자가 촬영한 실제 업무 화면과 같은
`Div(다중 필드) → Layouts → Layout` 패턴을 애초에 포함하지 않기 때문**임이 이번 재검증으로
다시 확인됐다.

### 2) Structural proxy 구성 및 검증

`file upload/download` 없이, 사용자가 확인한 구조(`검색조건 Div(label+input 4개, 1행)` +
`요약 Div(label+value 2x2, 2행)` + `Grid`)와 동일한 shape의 synthetic fixture를 스크래치 경로에
작성해 corpus 밖에서(Production count에 미포함) 검증했다: `BusinessDivLayoutGridProxy.xfdl`

```xml
Form/Layouts/Layout
  Div id="div_search" (검색조건: Static+Edit+Static+Combo, 4개, top 전부 10 -- 1행)
    Layouts/Layout
  Div id="div_summary" (요약: Static x4, top={10,40} x left={10,180} -- 2행 x 2열 rectangular)
    Layouts/Layout
  Grid id="grd_list"
```

Production 코드 무변경 상태로 후보 classes(`build/classes`, 재빌드만 수행)를 이용해 별도 임시
project(`{scratch}/proxy-project`)를 candidate 빌드로 변환했다(`{scratch}/proxy-output` 출력,
corpus/`sample-phase3-output`/Base Freeze 어느 것도 건드리지 않음).

**Generated XML (발췌, `Form/BusinessDivLayoutGridProxy.xml`):**

```xml
<xf:group id="grp_content" style="width:1350px;height:800px;">
    <w2:group id="div_search" style="position:absolute;...">
        <w2:span id="div_search_lbl_baseYm" .../>
        <xf:input id="div_search_ed_baseYm" .../>
        <w2:span id="div_search_lbl_branch" .../>
        <xf:select1 ... id="div_search_cb_branch" .../>
    </w2:group>
    <w2:group id="div_summary" style="position:absolute;...">
        <xf:group id="div_summary_layoutTableRow0">
            <xf:group id="div_summary_layoutTableRow0Col0">
                <w2:span id="div_summary_lbl_startDate" .../>
            </xf:group>
            <xf:group id="div_summary_layoutTableRow0Col1">
                <w2:span id="div_summary_lbl_endDate" .../>
            </xf:group>
        </xf:group>
        <xf:group id="div_summary_layoutTableRow1">
            <xf:group id="div_summary_layoutTableRow1Col0">
                <w2:span id="div_summary_val_startDate" .../>
            </xf:group>
            <xf:group id="div_summary_layoutTableRow1Col1">
                <w2:span id="div_summary_val_endDate" .../>
            </xf:group>
        </xf:group>
    </w2:group>
    <w2:gridView class="wq_gvw" id="grd_list" .../>
</xf:group>
```

결과: `div_search`(1행, `ABSOLUTE_LAYOUT_FALLBACK` — rectangular 판정 요건인 row≥2 미충족이라
flat pass-through, 정상)와 `div_summary`(2행 x 2열, `TABLE_LAYOUT_HIGH_CONFIDENCE` → row/col
`xf:group` 구조 생성)가 **동일 실행에서 각각 올바르게 분기**됨을 확인했다. `Div → Group` 자체는
이미 기존 `ComponentMappingRegistry`/`isContainerComponent`로 동작하던 기능이고(신규 아님),
`Layout → Table`은 이전 라운드 코드가 Div 내부에서도 동일하게 동작함을 이번에 처음 실증했다.
Grid는 여전히 전용 wrapper 없이 `w2:gridView`로 직접 배치됨(아래 3항 참고).

Status: **`SYNTHETIC_STRUCTURAL_PROXY_VERIFIED`**(실제 업무 파일이 아닌, 사용자가 확인한 구조와
shape-동일한 합성 fixture 기준 검증). `STUDIO_DESIGN_VERIFIED`는 선언하지 않는다.

### 3) Grid → Group

이번 라운드에서 사용자가 제공한 실제 업무 evidence는 XPlatform **Source** 구조(`Grid`가
`Formats`/Column/Row/Cell을 가진 독립 component)까지만 보여주며, WebSquare v6 **target**에서
Grid가 항상 전용 `Group`으로 감싸지는지에 대한 새로운 target-side evidence는 포함하지 않는다.
기존 실제 v6 영상 evidence(이전 라운드 재확인, carry-forward)는 동일 화면 내 두 Grid 중 하나만
`lybox` Group에 감싸이고 다른 하나는 감싸이지 않는 비일관 사례였다 — 이 결론은 이번 라운드
변경되지 않는다. 따라서:

`GRID_GROUP_STRUCTURE = UNRESOLVED`(코드 무변경, target-side evidence 여전히 비일관).

### 4) Production 변경 여부

이번 라운드는 **`NO PRODUCTION CODE CHANGE`** — 기존 4개 함수(`classifyLayoutGeometry`/
`buildTableRows`/`convertChildren` 7-인자 오버로드/`convertLayoutAsTable`)가 이미 depth-agnostic
하게 동작함을 재확인/실증했을 뿐, 코드 결함이 발견되지 않아 수정하지 않았다. 기존
`analysis/freeze-vs-candidate-production.diff`, `analysis/freeze-vs-candidate-function-diff.md`도
그대로 유지한다(변경 없음).

### 5) Freeze 안전

Base Freeze(`XPWS-OFFLINE-FREEZE-20260820-02`) 내부에서는 이번 라운드 어떤 command execution/
write/delete도 수행하지 않았다. proxy 빌드/변환은 candidate 자체 `build/`와 스크래치 경로만
사용했다. `FREEZE_IMMUTABILITY_VIOLATION`(이전 두 차례 발생분 포함, 상기 기록) 재발 없음.

### 6) Regression

Production 코드 무변경이므로 전체 regression 재실행 불필요(15번 규칙). 기존 회귀 결과(corpus
136/136 byte-identical, `SOURCE_TO_TARGET_ID_MAP_IDENTICAL=PASS` 403/403)는 carry-forward로
유효하다.

### 최종 상태 (Reopen 라운드)

`CANDIDATE_STATUS`는 `PARKED_EVIDENCE_INSUFFICIENT`에서 해제한다(`REOPENED` → 활성 evidence
확보). 그러나 실제 업무 XFDL 파일 자체의 폐쇄망 반입 및 Studio 렌더링 확인 전까지는:

`DESIGN_STRUCTURE_CANDIDATE` / **`TABLE_LAYOUT_IMPLEMENTATION = FIX_CANDIDATE`** /
`STATIC_VERIFIED` / **`STUDIO_DESIGN_REQUIRED`**.

승격 근거: (a) 코드가 Div 내부 Layout까지 depth-agnostic하게 동작함을 synthetic structural
proxy로 실증(`SYNTHETIC_STRUCTURAL_PROXY_VERIFIED`), (b) 그 구조가 사용자가 실제 업무 Studio
화면에서 직접 확인한 shape(검색조건 다중 필드 + 요약/그리드 영역)와 일치, (c) `grp_content` A방식
(제거/변경 없이 내부에 Div Group/Layout Table 생성)으로 목표 hierarchy 달성 확인. 다만 이 fixture는
synthetic proxy이며 real corpus/실제 파일 검증이 아니므로 `STUDIO_DESIGN_VERIFIED`/`FIXED`는
선언하지 않고, 사용자가 폐쇄망 Studio에서 실제 업무 화면(또는 동일 shape의 화면)으로 렌더링을
확인해야 다음 단계로 넘어갈 수 있다(`STUDIO_DESIGN_REQUIRED`). `GRID_GROUP_STRUCTURE`는 여전히
`UNRESOLVED`로 남는다(Table과 별도로 무리하게 구현하지 않음, 6번 규칙).

## v6 Design Structure + Table + Grid Group + Percentage Geometry Alignment 라운드

`USER_PROVIDED_REAL_BUSINESS_XFDL_VISUAL_EVIDENCE = ACCEPTED`. `COMPONENT_CLASS_APPLICATION =
PAUSED`(신규 class mapping 없음, 기존 `btn_cm`/`wq_gvw`/Combo `disabledClass`/`w2:span`/
`w2:inputCalendar` 전부 무변경 실측 확인). `NO_PAIRED_LEGACY_TO_V6_REFERENCE_AVAILABLE_BY_
PROJECT_NATURE` — 목표 hierarchy는 A) XPlatform source/corpus semantics와 B) 사용자가 확인한
실제 v6 native Design convention을 각각 독립 evidence로 결합했으며, 둘 사이 연결은 `INFERRED`다.

### 1) Production 변경

4개 파일(`ComponentLayoutConverter.java`, `WebSquareGenerator.java`,
`XPlatformProjectConverter.java`, `TabRuntimeScriptGenerator.java`). 상세는
`analysis/freeze-vs-candidate-function-diff.md`(BEFORE/AFTER 전체 코드) 및
`analysis/freeze-vs-candidate-production.diff`(773줄, Base `-02` → 현재 candidate 누적 diff).
그 외 72개 Production 파일은 `diff -rq` 무변경 확인(`UNEXPECTED_PRODUCTION_DIFF = 0`).

### 2) global grp_content 제거 / grp_main migration

`GLOBAL_GRP_CONTENT_XFDL_COUNT = 0`(corpus 136개 output 전수 실측, `grp_content` 문자열 잔존
0건). 7번 규칙에 따라 무조건 삭제가 아니라 기존 dependency 4곳(`WebSquareGenerator.
registerFormRootMapping`의 componentIdMap 등록, `TabRuntimeScriptGenerator`의
`component('grp_content').getScope()`/`w.grp_content` 2곳, `XPlatformProjectConverter`의 Tab
runtime placeholder XML)을 감사하고, 동일한 id-string 기반 lookup 방식은 그대로 유지한 채 리터럴만
`grp_content` → `grp_main`으로 함께 migration했다. `V5_RUNTIME_REGRESSION_REQUIRED`(xf:group이
실제 v5 엔진에서 `getScope()`를 지원하는지)는 이번 라운드에 새로 발생하거나 해소되지 않은 기존
gap 그대로다 — id 자체(`grp_content` vs `grp_main`)의 문제가 아니라 `xf:group`이라는 target
tag의 문제이기 때문.

### 3) 최종 Root hierarchy (실측)

```text
body
└─ grp_resultArea (style="")
   └─ grp_main (style="height:Npx;")
      ├─ Div 대응 w2:group (position:absolute, %)
      │  └─ Layout 대응 xf:group row/col(%)  ── Div 내부 Layout이 table 판정된 경우만
      │     └─ 실제 component(%, 또는 구조적 배치라 좌표 없음)
      ├─ Grid 대응 xf:group wrapper(%)
      │  └─ w2:gridView(class="wq_gvw", style="width:100%;height:100%;")
      └─ ...
```

`ROOT_DIRECT_STRUCTURE = PASS`(실제 Generated XML, BusinessDivLayoutGridProxy 실측 —
`analysis/evidence-snapshots/round3-percent-table-grid/BusinessDivLayoutGridProxy-generated.xml`).

### 4) Div → Group / Layout → Table (real corpus 실측)

corpus 136개 화면 전체를 스캔한 실측 로그(`[UI TABLE]`/`[UI 변환] Div`/`[UI GRID GROUP]`) 기준:

| 항목 | 수치 |
|---|---|
| Div → Group 적용 수 | 2(`divWrap`, `divA` — corpus 내 실제 Div 컴포넌트 전부) |
| `ROOT_FORM_LAYOUT_NOT_A_TABLE_TARGET`(root Layout 강제 fallback) | 121건 |
| Layout → Table 적용 수(real corpus, `TABLE_LAYOUT_HIGH_CONFIDENCE`) | **5건**(`divWrap`, `divA`, Tab 3개 페이지 — `tabMain.pageA/pageB/pageInline`, 각 children=1인 1행×1열 trivial table) |
| `UNRESOLVED_LAYOUT`(children=0) | 2건(`tabMain.pageSearch/pageDetail`) |
| `ABSOLUTE_LAYOUT_FALLBACK`(겹침) | 0건 |
| Grid → Group 적용 수 | **3건**(`grd`×2, `grdMain`×1 — corpus의 `wq_gvw` 3건 전부와 일치) |

이전 라운드 대비 real corpus `Layout → Table` 적용이 0 → 5로 바뀐 이유는 코드 결함 수정이 아니라
14/15번 규칙(1-row/1-column도 table 대상)의 결과다 — 이 5건은 모두 children=1인 trivial(1행1열)
table이며, 진짜 다행/다열 real-corpus rectangular table 사례는 여전히 corpus에 없다(이전
라운드의 원인 분석 결론 — 97/121이 Tab 단일-자식 fixture — 은 이번 라운드에도 유효, 다만 이제는
그 단일-자식도 "1x1 table"로 명시적으로 구조화된다는 차이).

### 5) 1-row Layout → Table

이번 라운드 목표(14번 규칙)는 real corpus의 진짜 multi-column 검색조건 화면으로 검증되지 못했다
(corpus에 그런 fixture가 없음 — 위 3)와 동일한 한계). 대신 synthetic structural proxy
(`BusinessDivLayoutGridProxy.xfdl`의 `div_search`, Static+Edit+Static+Combo 4개, 모두 top=10인
1행)로 실제 동작을 실증했다: `classification=TABLE_LAYOUT_HIGH_CONFIDENCE`(row=1, col=4) → `xf:
group id="div_search_layoutTableRow0"` 아래 4개 `xf:group` 셀 생성, 각 셀 width%는 실제 컴포넌트
폭 비율(6.015%/9.0226%/6.015%/11.2782%, basisWidth=1330 기준) — 균등분할 아님. Status:
`SYNTHETIC_STRUCTURAL_PROXY_VERIFIED`(real corpus 검증 아님).

### 6) Percentage Geometry Gate

corpus 136개 화면 전체(`[UI PERCENT]` 로그) 기준 실측:

| Gate | 값 |
|---|---|
| `PERCENT_GEOMETRY_CONVERSION` | PASS(percent 적용 124건 관측) |
| `PERCENT_GEOMETRY_PARENT_SEMANTIC` | PASS(모든 계산이 `resolveLayoutBasis`로 얻은 "가장 가까운 XPlatform Layout 자신의 width/height" 하나만 사용 — Form 전체 일괄 계산 없음, Div/Grid/Table row·col/일반 component 전부 동일 원칙) |
| `ROOT_WRAPPER_GEOMETRY_UNCHANGED` | PASS(`grp_resultArea style=""`, `grp_main style="height:Npx;"` 무변경 실측) |
| `ABSOLUTE_PX_GEOMETRY_REMAINING_COUNT` | 13(px fallback 발생 컴포넌트 수) |
| `PERCENT_GEOMETRY_UNRESOLVED_COUNT` | 13(위와 동일 대상 — basis 자체가 없어 percent 시도가 애초에 불가했던 경우) |
| `PIXEL_GEOMETRY_FALLBACK_COUNT` | 13(위 unresolved 13건 전부 안전하게 기존 px geometry로 fallback, 25번 규칙) |
| `INVALID_PERCENT_STYLE_COUNT` | 0(`NaN%`/`Infinity%`/의미 없는 percent 0건 실측) |

13건 unresolved 원인: 모두 `basisWidth=-1.0`(즉 `resolveLayoutBasis`가 그 컴포넌트에 도달하기까지
유효한 `Layout` width/height를 만나지 못한 경로) — 코드 결함이 아니라 그 경로 자체가 Layout
width/height 속성이 없는 특수 fixture(Tab 관련 fixture 다수)이기 때문이며, 기존 px 경로로 안전하게
fallback되어 `UNEXPECTED_GENERATED_DIFF`를 만들지 않는다. 음수 percentage는 0건(corpus에 음수
좌표 fixture 없음).

Div percentage 대표 결과(corpus 실측, `divWrap`):
BEFORE(px, 이전 라운드까지 가정): `style="position:absolute;left:...px;top:...px;width:...px;height:...px;"`
AFTER(percent, 이번 라운드 실측): `style="position:absolute;left:0%;top:0%;width:100%;height:100%;"`
(divWrap이 자신을 담은 Layout 전체를 차지하는 경우 — 실제 관측값).

Table row/column percentage 대표 결과(synthetic proxy, `div_summary`, basisWidth=1330,
basisHeight=60): `layoutTableRow0 style="width:100%;height:40%;"`,
`layoutTableRow0Col0 style="width:9.0226%;height:100%;"`(24px/60px=40%, 120px/1330px=9.0226% —
실비율).

Grid Group percentage 대표 결과(synthetic proxy, `grd_list_gridGroup`):
`style="position:absolute;left:0.7407%;top:26.25%;width:98.5185%;height:62.5%;"` 안에
`<w2:gridView class="wq_gvw" style="width:100%;height:100%;"/>`(22번 규칙과 정확히 일치).

### 7) SOURCE_TO_TARGET_ID_MAP_EXPECTED_ONLY

canonical full map 재측정(`ComponentIdMapAudit` — Form root target literal을 4번째 CLI 인자로
뺀 버전, Base 실행은 `grp_content`, Candidate 실행은 `grp_main`):

| 항목 | 값 |
|---|---|
| total key count(Base) | 403 |
| total key count(Candidate) | 403 |
| expected changed(Form-root mapping, `grp_content`→`grp_main`) | 135건(corpus 내 Form root 전체) |
| Base-only unexpected | 0 |
| Candidate-only unexpected | 0 |
| other changed mappings(Form-root 제외) | 0 |

**`SOURCE_TO_TARGET_ID_MAP_EXPECTED_ONLY = PASS`**(diff 135라인 전부 `grp_content`↔`grp_main`
패턴 하나뿐임을 라인 단위로 재확인).

### 8) 보호 항목 재확인(corpus 실측)

`btn_cm=12`, `wq_gvw=3`, Combo `disabledClass=4`, `w2:inputCalendar=1`, `xf:trigger=12`,
`w2:gridView=3`, `w2:span=9` — 전부 Base Freeze `-02`와 동일(무변경). `STALE_GRP_CONTENT_SCRIPT_
REFERENCE = 0`(생성된 XML/JS 어디에도 `grp_content` 문자열 없음, `runtime/xplatform-tab-runtime.js`
내 `grp_main` 4회로 대체 확인). `COMPONENT_COUNT_PRESERVED`: 135개 화면 `[UI 변환 완료]`
component count 합계 402(Form root 등록 포함 401개 XPlatform Form과 1:1, Tab 관련 페이지의
`.content`/`.tab` id는 별도 usedTargetIds 슬롯이라 이 카운트에 포함되지 않음 — 기존 방식 그대로).

### 9) Regression

`CLEAN_COMPILE`=PASS(0 errors, 76 source files), `PROJECT_CONVERSION`=149/149,
`XML_PARSE`=136/136, `STANDALONE_JS`=15/15(무변경), `PHASE1_SHA`=2/2 PASS(정적 reference
fixture 해시 비교 — `<script>` 태그 텍스트만 대상이라 이번 UI/geometry 변경과 무관함을 코드 검토로
확인), `PAGE_JS_SYNTAX`=`NOT_EXECUTED`(Nashorn 엔진 부재, carry-forward), `REAL_RUNTIME_
VERIFIED`=`NOT_EXECUTED`(이 lineage에 mock harness 없음, carry-forward). Freeze 실행 안전:
Base Freeze `-02` 비교/컴파일은 전부 별도 scratch 디렉터리(cwd)에서 수행, Freeze 디렉터리 내부
command execution/write/delete 0건(`find -newer` 재확인 완료, `FREEZE_IMMUTABILITY_VIOLATION`
재발 없음).

### 10) EXPECTED_STRUCTURE_DIFF / UNEXPECTED_GENERATED_DIFF

`EXPECTED_STRUCTURE_DIFF`: global `grp_content` 제거, Div→Group, Div 내부 Layout→Table(1-row
포함), Table row/col hierarchy, Grid→Group wrapper, Form root mapping `grp_content`→`grp_main`,
업무 content px→percent, Table row/col percent sizing, Grid wrapper percent sizing — 전부 실제
발생 확인. `UNEXPECTED_GENERATED_DIFF = 0`(component loss/QName 변경/class 변경/binding
loss/lifecycle 변경/unrelated script 변경 0건 — 8)/9)의 실측 수치가 근거).

### 11) NEXT_ROUND_CANDIDATE(이월)

- real corpus에 진짜 multi-row/multi-column 검색조건·grid 요약 fixture가 추가되면 1-row/
  multi-row Table 경로를 real corpus 기준으로 재검증.
- `w2tb_th`/`w2tb_td`/`w2tb_tb`/`dfbox`/`fl`/`lybox`/`ly_column`/`fr` 등 class 적용(별도 라운드).
- `GRID_GROUP_STRUCTURE`의 class(`lybox` 등) 적용 여부는 target-side evidence(같은 화면 내 Grid
  간 wrapper class 비일관)가 아직 부족 — 구조(Group wrapper)만 이번에 구현, class는 보류.
  Grid → Group wrapper "구조" 자체는 이번 라운드에 generic 구현/실증 완료
  (`GRID_GROUP_STRUCTURE = FIX_CANDIDATE`로 격상 가능하지만 class까지는 아님).
  `resolveLayoutBasis`가 unresolved인 13건 경로(Tab 관련 fixture 다수)의 원인 fixture를 특정해
  필요 시 root cause 문서화.
- `ComponentLayoutConverter.buildRootStyle`는 이번 라운드부터 호출부가 사라져 unused
  상태다(하위 호환을 위해 이번 라운드는 삭제하지 않음) — 다음 라운드에서 정리 검토.

### 최종 상태(이번 라운드)

`DESIGN_STRUCTURE_CANDIDATE` / `TABLE_LAYOUT_IMPLEMENTATION = FIX_CANDIDATE`(real corpus 5건 +
synthetic proxy로 구조/알고리즘 실증) / `GRID_GROUP_STRUCTURE = FIX_CANDIDATE`(구조만, class
보류) / `PERCENT_GEOMETRY = FIX_CANDIDATE` / `STATIC_VERIFIED` / `STUDIO_DESIGN_REQUIRED`.
`STUDIO_DESIGN_VERIFIED`/`FIXED`/`PATCH_READY`/`FREEZE_READY`/`REAL_RUNTIME_VERIFIED`는 선언하지
않는다 — 사용자의 실제 폐쇄망 Studio 확인이 필요하다.

## Baseline ZIP 대비 Production 함수별 상세 Diff Audit + 주석 정리 라운드

Production 기능 semantic 변경 없음(순수 audit + comment normalization 라운드). Baseline:
`work/offline-import/xplatform-to-websquare-offline-import.zip`(read-only, scratch에만 extract,
`BASELINE_ZIP_IMMUTABLE = PASS`). 이 zip은 Base Freeze `-02`보다 훨씬 이전 시점 스냅샷이라
(Root/Body wrapper 도입 이전, Calendar QName 수정 이전) diff에 historical 변경까지 함께 나타남 —
전부 `analysis/baseline-zip-vs-candidate-function-diff.md`에서 `THIS_ROUND`/`HISTORICAL`로 구분.

핵심 결과: `FUNCTIONAL_CHANGED_FILES=5`, `FUNCTIONAL_CHANGED_FUNCTIONS=27`(class-level 2건 포함),
`UNEXPECTED_FUNCTIONAL_CHANGE_COUNT=0`, `UNEXPECTED_PRODUCTION_CHANGE_COUNT=0`. 영어→한국어 주석
정리 7곳(`WebSquareGenerator.java` 6곳, `ComponentLayoutConverter.java` 1곳) 수행,
`COMMENT_NORMALIZATION_FUNCTIONAL_DIFF=0`(comment-stripped 비교로 실측 확인) — clean compile
재확인 PASS. 상세는 `analysis/baseline-zip-vs-candidate-function-diff.md`,
`analysis/baseline-zip-vs-candidate-production.diff`,
`analysis/baseline-zip-vs-candidate-functional.diff` 참고.

## 실제 Studio 실패 기반 Percentage Geometry Root Cause Fix 라운드

사용자가 실제 폐쇄망 Studio에서 확인(`USER_CONFIRMED_CLOSED_NETWORK_STUDIO`): Design/Preview
양쪽 모두 업무 화면이 좌측 상단 좁은 영역으로 압축(`STUDIO_DESIGN_FAILED`,
`STUDIO_DESIGN_REPRODUCED`). 첨부 영상은 도구 제약(ffmpeg/ImageMagick video delegate 부재)으로
판독 불가 — 로컬 재현/재생성 XML로 cross-check.

Root cause(`SOURCE_PIXEL_GEOMETRY_REMAINS_IN_GENERATED_STRUCTURE` 확정): `[WebSquareGenerator]
convertLayoutAsTable`/`appendBody`가 percent basis를 오직 "현재 Layout 자신의 width/height"
에서만 얻었는데, 실제 업무 화면 중 (a) component가 `Layouts`/`Layout` wrapper 없이 `Form` 직계
자식으로 존재하거나, (b) `Layout`은 있지만 자신에게 width/height가 없는 경우가 있어 이 두 패턴에서
basis가 영원히 미확보되어 전체 화면이 px fallback으로 떨어졌다. 이전 라운드까지 존재하던
`grp_content`(px로 폭을 고정해주던 wrapper)가 제거된 상태라 그 px 절대좌표가 폭 미정의 컨테이너
위에서 렌더링되며 화면이 좁게 collapse.

수정: `[ComponentLayoutConverter] resolveFormBasis`(신규, 기존 `findFormGeometry` 재사용) 추가,
`appendBody`/`convertLayoutAsTable`이 Layout 자신의 geometry가 없을 때 Form 전체로 fallback하도록
2곳 연결. corpus 실측: `PIXEL_GEOMETRY_FALLBACK_COUNT` 13 → **0**, `UI PERCENT 적용` 124 →
137건. `SOURCE_TO_TARGET_ID_MAP_EXPECTED_ONLY`/invariant class/QName 전부 무변경 재확인.

최종 `PERCENT_GEOMETRY = FIX_CANDIDATE` / `STATIC_VERIFIED` / `STUDIO_DESIGN_REQUIRED`. 상세는
`analysis/freeze-vs-candidate-function-diff.md`의 "후속 라운드 — 실제 Studio 실패 기반
Percentage Geometry Root Cause Fix" 섹션, raw diff는
`analysis/git-baseline-vs-candidate-production.diff` 참고.

## 후속 라운드 — Root Percentage Containing Block Width Fix

사용자가 실제 폐쇄망 Studio 증거(원본 대비 압축된 Design/Preview 스크린샷 + 실제 생성 Source XML)를
제공. 생성 XML의 자식 percentage 값은 이미 정확했으나 `grp_resultArea`(style=""), `grp_main`
(style="height:Npx;" — width 없음)에 명시적 width가 없어 CSS containing block chain이 끊어져,
그 아래 모든 `%` 자식이 사실상 폭 0에 가까운 containing block을 기준으로 렌더링되며 화면이 좌측에
압축됨을 확인(`ROOT_PERCENT_CONTAINING_BLOCK_WIDTH_DEFECT`). 이전 라운드의 "root wrapper는 width를
갖지 않는다" 불변식은 `grp_content`(px width 보유)가 아직 존재하던 구조를 관찰해 세운 것으로,
`grp_content` 제거 이후에는 더 이상 유효하지 않음을 사용자가 명시적으로 지적, 재검토.

수정: `[ComponentLayoutConverter] buildMainAreaStyle`이 항상 `width:100%;`를 접두로 반환하도록
변경, `[WebSquareGenerator] appendBody`의 `grp_resultArea` style 리터럴을 `""` → `"width:100%;"`로
변경. 자식 percentage 계산 코드는 전혀 건드리지 않음(자식 값 BEFORE=AFTER 완전 동일, 실측 확인).

corpus 실측: `grp_resultArea`/`grp_main` 둘 다 `width:100%` 135/136(제외 1건은 무관 placeholder),
`grp_content` 잔존 0, `position:relative`/`overflow:hidden`(root wrapper) 0, 하드코딩 px width
0, `INVALID_PERCENT_STYLE_COUNT`/`NaN%`/`Infinity%` 전부 0. `SOURCE_TO_TARGET_ID_MAP_EXPECTED_
ONLY`/invariant class/QName/Phase1 SHA 전부 무변경 재확인. 대표 3건(Form-direct-child 컴포넌트,
Grid Group, Table cell) percentage 역산 전부 일치.

최종 `ROOT_PERCENT_CONTAINING_BLOCK = FIX_CANDIDATE` / `STATIC_VERIFIED` / `STUDIO_DESIGN_
REQUIRED`. 상세는 `analysis/freeze-vs-candidate-function-diff.md`의 "후속 라운드 — Root
Percentage Containing Block Width Fix" 섹션, raw diff는
`analysis/git-baseline-vs-candidate-production.diff` 참고.

## 후속 라운드 -- Native v6 Layout Structure Gap Root-Cause Audit + Minimal Alignment

이전 라운드(Root Percentage Containing Block Width Fix)를 폐쇄망에 적용했으나 사용자가 동일
증상(Design/Preview 압축)을 재현. 기존에 별도 candidate(`v6-class-mapping`)에서 이미 수행된
정상 native v6 화면(BCI01M0000) source 영상 판독 evidence(`v6-video-source-analysis.md`)를
재사용해 재감사한 결과:

- `ROOT_WIDTH_DEFECT = NOT_CONFIRMED` -- native evidence 자체도 `grp_main`이 width 없이
  정상 동작함을 보여줌; 지난 라운드 fix는 무해하지만 유일한 원인은 아니었음.
- `NATIVE_LAYOUT_CONTAINER_SEMANTIC = REQUIRED` -- `tagname="table/tr/th/td"` +
  `class="w2tb_tb/w2tb_th/w2tb_td"`는 CSS skin이 아니라 실제 HTML 요소를 결정하는 구조적
  신호(evidence 100% 대응).
- `ABSOLUTE_PERCENT_LAYOUT_STRATEGY` -- Table 판정 경로(corpus 5/135)에는 `ROOT_CAUSE_
  CANDIDATE`(native 대비 tagname/class 완전 누락); 나머지 절대좌표 % 경로(130/135)는
  `EVIDENCE_INSUFFICIENT`(원래 실패 화면이 Table 경로인지 pairing 증거 없음).
- `CURRENT_GROUP_ONLY_TABLE_MODEL = INCOMPLETE` -- 위치/크기는 정확하나 tagname/class가
  전혀 없었음.

수정: `[WebSquareGenerator] convertLayoutAsTable`(기존 함수)에 신규 `layoutTable` wrapper
(`tagname="table" class="w2tb_tb"`) 추가 + row group `tagname="tr"` + cell group
`tagname="td" class="w2tb_td"` 추가. percentage geometry 계산 로직은 완전 무변경. th(header)
구분은 corpus 실사례(label/input 쌍이 아닌 단일 컴포넌트 cell 포함)에서 안전하게 일반화할
근거가 없어 미적용(UNRESOLVED 유지). `dfbox`/`fl`/`lybox`/`ly_column`/`fr`도 XPlatform
source에 대응 신호가 없어(section-title/column-intent 판별 불가) 미적용(`EVIDENCE_
INSUFFICIENT` 유지).

corpus 실측: 149/149 변환 성공, 실제 diff 4개 파일(TABLE_LAYOUT_HIGH_CONFIDENCE 5건 소속),
나머지 132개 XML byte-identical. `SOURCE_TO_TARGET_ID_MAP_EXPECTED_ONLY`/invariant class/
QName/Phase1 SHA 전부 무변경 재확인.

**한계**: 이 fix는 corpus의 5/135(Table 판정 경로)에만 영향을 준다. 사용자가 원래 보고한
전체 화면 압축 증상이 이 경로에 해당하는지는 확인되지 않았다 -- `ABSOLUTE_PERCENT_LAYOUT_
STRATEGY`가 나머지 절대좌표 % 경로 전체의 근본 원인인지는 여전히 `EVIDENCE_INSUFFICIENT`다.

최종 `DESIGN_STRUCTURE = FIX_CANDIDATE` / `STATIC_VERIFIED` / `STUDIO_DESIGN_REQUIRED`(구조적
gap 하나에 대한 최소 수정, 전체 문제의 완전한 해결이라고 주장하지 않음). 상세는
`analysis/freeze-vs-candidate-function-diff.md`의 "후속 라운드 -- Native v6 Layout Structure
Gap Root-Cause Audit + Minimal Alignment" 섹션, raw diff는
`analysis/git-baseline-vs-candidate-production.diff` 참고.

## 후속 라운드 -- 실제 Studio 재실패: Nested Percentage Double-Scaling + Grid Width 조사

사용자가 지난 라운드(Native v6 Layout Structure Gap 최소 정렬) 적용 이후에도 폐쇄망 재변환에서
동일 증상(`STUDIO_DESIGN_FAILED`/`STUDIO_DESIGN_REPRODUCED`)을 확인, 실제 generated Source
스크린샷을 근거로 제시. 확인 결과:

- `NESTED_PERCENT_DOUBLE_SCALING = CONFIRMED` -- Table cell(`[ComponentLayoutConverter]
  buildTableCellStyle`)이 cell 자신을 컴포넌트 자신의 width/basisWidth 비율로 정확히 계산해
  두었는데, `[WebSquareGenerator] convertLayoutAsTable`이 cell 내부 컴포넌트 변환 시 **같은
  basisWidth/basisHeight를 그대로 재사용**해 동일 비율을 또 계산 -- CSS 상 자식 % width는
  실제 렌더링된 부모(cell) 폭 기준이므로 실효 폭이 제곱으로 축소됨(실측 예: cell/child 둘 다
  6.0345% -> 실효 약 0.36%). 사용자가 제공한 실제 화면 스크린샷 수치와 정확히 일치.
- `GRID_COLUMN_WIDTH_MISMATCH = EVIDENCE_INSUFFICIENT` -- `w2:gridView` column width(px)
  convention에 대한 native evidence가 이 세션에 전혀 없어(영상에서 grid 내부 column 폭까지
  판독한 기록 없음) 추측 없이 미수정.

수정: `[WebSquareGenerator] convertLayoutAsTable`이 cell 내부 컴포넌트 변환 시 원래 Div/Layout
basis 대신 그 cell/row 자신의 실제 px 크기(신규 `resolveCellBasisWidth`/`resolveRowBasisHeight`)
를 기준으로 재계산하도록 수정. 결과적으로 cell 내부 컴포넌트는 (구조상 cell == 그 컴포넌트
자신의 geometry이므로) `width:100%;height:100%`가 되며, 하드코딩이 아니라 "자기 자신의 px
값/자기 자신의 px 값" 항등 계산의 결과다. 중첩 컨테이너(cell 안에 다시 컨테이너가 있는 경우)의
손자 컴포넌트에도 기존 재귀 basis 전달 방식이 그대로 올바르게 cascading됨을 실제 corpus 사례로
역산 검증(source px 값과 정확히 일치).

corpus 실측: 149/149 변환 성공, 실제 diff 4개 파일(round6과 동일 대상, Table 판정 경로에만
영향), 나머지 132개 XML byte-identical. `SOURCE_TO_TARGET_ID_MAP_EXPECTED_ONLY`/invariant
class/QName/Phase1 SHA/top-level percentage 전부 무변경 재확인.

최종 `DESIGN_STRUCTURE = FIX_CANDIDATE` / `STATIC_VERIFIED` / `STUDIO_DESIGN_REQUIRED`. 이번
결함은 사용자가 실제로 제공한 화면 스크린샷의 수치와 코드 추적이 정확히 일치해, 지금까지의
라운드 중 가장 직접적인 근거를 가진 수정이다. 상세는
`analysis/freeze-vs-candidate-function-diff.md`의 "후속 라운드 -- 실제 Studio 재실패: Nested
Percentage Double-Scaling + Grid Width 조사" 섹션, raw diff는
`analysis/git-baseline-vs-candidate-production.diff` 참고.

## 후속 라운드 -- Grid 내부 Column Width Ratio Candidate (실험적)

지난 라운드에서 `GRID_COLUMN_WIDTH_MISMATCH = EVIDENCE_INSUFFICIENT`로 미해결이던 Grid
내부 column px 고정폭 문제를, native evidence 부재 상태 그대로 실험적 candidate로 구현해
폐쇄망 Studio 실검증을 가능하게 했다(`GRID_COLUMN_NATIVE_EVIDENCE = EVIDENCE_INSUFFICIENT`,
`GRID_COLUMN_WIDTH_SEMANTIC = EXPERIMENTAL`).

정책: source Grid 자신의 `width` 속성을 분모로 사용(Form width 아님), column width 합계가
Grid width를 초과하지 않을 때만(`columnSum <= gridWidth + 0.5px` tolerance) percentage로
정규화(`NORMALIZED_TO_CONTAINER`); 초과하면 horizontal-scroll semantic 가능성으로 보고 기존
px 그대로 유지(`PIXEL_FALLBACK`); column/Grid width를 읽을 수 없으면 `UNRESOLVED`(px 유지).
header/body/footer는 `convert()`에서 1회만 계산한 동일 `columnPercents` 배열을 공유해
rounding mismatch를 구조적으로 방지.

수정: `[GridFormatConverter] resolveColumnPercents`(신규) + `calculateCellWidth`/
`getSingleColumnWidth`/`appendHeader`/`appendBody`/`appendFooter`/
`appendSynthesizedDatasetBody`/`appendPlaceholderColumn`/`applyCellGeometry`/`convert`(기존
함수, `columnPercents` 파라미터 추가만 -- null이면 기존 px 로직과 완전 동일해 하위 호환
보장). percentage formatter는 `ComponentLayoutConverter.formatPercent` 재사용(중복 없음).

corpus 실측: 149/149 변환 성공, Grid Format을 가진 화면 3개 전부 `NORMALIZED_TO_CONTAINER`
(`grd`@300px/100px=33.3333%, `grdMain`@600px/[100,220,120]px=[16.6667%,36.6667%,20%]).
`PIXEL_FALLBACK`/`UNRESOLVED` corpus 실사례는 0건(로직 경로는 구현). header/body/footer 동일
column 값 일치 확인. 나머지 133개 XML byte-identical. `SOURCE_TO_TARGET_ID_MAP_EXPECTED_ONLY`/
`w2:gridView`/`wq_gvw`/invariant class/QName/Phase1 SHA 전부 무변경 재확인.

최종 `GRID_COLUMN_WIDTH = FIX_CANDIDATE` / `STATIC_VERIFIED` / `STUDIO_DESIGN_REQUIRED`.
native evidence 없이 구현된 실험적 candidate이므로 폐쇄망 실검증 결과에 따라 되돌릴 수 있어야
하며, `columnPercents == null` 분기 하나만 비활성화하면 기존 px 동작으로 완전히 복귀 가능한
낮은 리스크 구조로 설계했다. 상세는 `analysis/freeze-vs-candidate-function-diff.md`의
"후속 라운드 -- Grid 내부 Column Width Ratio Candidate" 섹션, raw diff는
`analysis/git-baseline-vs-candidate-production.diff` 참고.

## 후속 라운드 -- Percentage 0.5% 단위 일괄 정규화

지금까지 소수점 4자리(trailing zero 제거)로 생성되던 모든 percentage geometry 출력을, 가장
가까운 0.5% 단위로 반올림하고 항상 `N.0%`/`N.5%` 한 자리 소수로 통일했다. 계산 기준(parent/
basis)이나 구조는 전혀 재설계하지 않고 formatting 단계만 교체.

전수 조사 결과 `"%"` 리터럴을 직접 붙이는 지점은 `[ComponentLayoutConverter] formatPercent`
단 한 곳뿐이었고, 9개 callsite(Div/Table Row/Table Cell/Grid column 등)가 전부 이 함수를
거친다 -- 신규 함수 없이 이 함수 하나만 generic하게 수정했다(`raw x 2`를 정수로 반올림 후 다시
2로 나누는 BigDecimal 연산, 부동소수 비교 없이 deterministic). 추가로 그동안 구조 상수로 직접
박혀 있던 리터럴 `100%` 7곳(`grp_main`/`grp_resultArea`/Table wrapper/Grid Group 내부
`w2:gridView` fill/Tab `w2:content` 등)도 전부 `formatPercent(100.0)` 호출로 교체해 단일
formatter로 통제했다. 유일한 예외는 `runtime/xplatform-tab-empty.xml`(계산이 아닌 고정
placeholder, 과거 모든 라운드에서 이미 "실제 변환 화면과 무관"으로 확인된 파일) -- 근거를
명시하고 미수정.

사용자가 제시한 14개 예시 + 경계값 4개(6.24/6.25/6.74/6.75), 총 18건을 formatter 단위
테스트로 전부 검증(18/18 PASS). corpus 실측: 149/149 변환 성공, 136개 XML 중 135개에서
percentage precision만 변경(1개는 위 placeholder 예외), percentage 문자열을 정규식으로
제거한 뒤 diff하면 135개 전체 byte-identical(`PERCENT_BASIS_CHANGED = 0` 실증). 전체
1029건의 percentage 값 중 1027건이 `.0%`/`.5%` 규칙 준수(dot_zero=659, dot_five=368),
2건은 위 placeholder 예외. `SOURCE_TO_TARGET_ID_MAP_EXPECTED_ONLY`/invariant class/QName/
Phase1 SHA 전부 무변경 재확인.

`PERCENT_ROUNDING_SUM_DRIFT_COUNT = 1`(`grdMain` Grid: header/body 개별 column 반올림 합
73.0%와 footer의 raw-sum-then-round 73.5%가 0.5% 차이 -- 반올림 비선형성에 의한 것으로
계산 오류 아님, 규칙에 따라 자동 보정하지 않고 기록만 함).

최종 `PERCENT_FORMAT_NORMALIZATION = FIX_CANDIDATE` / `STATIC_VERIFIED`. 이 formatting
변경만으로 기존 `STUDIO_DESIGN_FAILED`가 해결됐다고 선언하지 않는다 -- `STUDIO_DESIGN_
REQUIRED` 유지. 상세는 `analysis/freeze-vs-candidate-function-diff.md`의 "후속 라운드 --
Percentage 0.5% 단위 일괄 정규화" 섹션, raw diff는
`analysis/git-baseline-vs-candidate-production.diff` 참고.

## 후속 라운드 -- XPlatform Visual Parity Quick Fix + Percentage Precision 통일

폐쇄망 재현(STUDIO_DESIGN_FAILED/STUDIO_DESIGN_REPRODUCED)이 계속되어, 이번 라운드는
"native 구조 확대"가 아니라 "XPlatform 원본 좌표/sibling 관계 보존"을 최우선으로 삼았다.

Production 실측 trace 결과 root cause는 B(서로 다른 sibling Div를 Table/Row/Cell로 잘못
재해석)로 확인됐다. `convertLayoutAsTable`의 table topology 판정(`classifyLayoutGeometry`)
이 자식의 겹침 여부만 보고 소스 타입(leaf vs container)을 구분하지 않아, `Div`/`GroupBox`/
`Tab`처럼 그 자체로 독립 좌표계를 가진 container child까지 table cell로 강제 편입되어
원래 left/top을 잃고 있었다(`NestedContainer.xfdl`/`TabExternalRelativePath.xfdl`로 실측
재현). `[WebSquareGenerator] convertLayoutAsTable`에 `hasContainerChild` 판정을 추가해,
container child가 있으면 `TABLE_CONVERSION_SEMANTIC_MISMATCH`로 재분류하고 기존
absolute-pass-through 경로(무수정)로 원래 좌표를 보존하도록 했다. Label/Edit 등 leaf
컴포넌트로만 구성된 실제 검증된 native table(BCI01M0000 evidence)은 그대로 유지된다.

position:absolute는 전역 유지(`ABSOLUTE_POSITIONING = REQUIRED_FOR_VISUAL_FIDELITY`),
`dfbox`/`fl`/`lybox`/`ly_column` 등 신규 native class는 이번 라운드도 미적용
(`LAYOUT_CLASS_EXPANSION = PAUSED`), Grid 내부 column width 로직은 무변경.

동시에 percentage formatter(`[ComponentLayoutConverter] formatPercent`)를 기존
`NEAREST_0.5_PERCENT`에서 `ONE_DECIMAL_PLACE`(소수점 둘째 자리 일반 반올림)로 교체했다.
공통 formatter 하나만 교체했고 9개 계산 callsite + 이전 라운드에 이미 통합된 리터럴
100% 7곳 모두 무수정으로 재사용(`PERCENT_FORMATTER_UNIFIED = PASS`,
`PERCENT_FORMATTER_BYPASS_COUNT = 0`). precision 변경이 basis에 영향을 주지 않았음을
136개 파일 중 구조가 바뀐 2개(위 container-child fix)를 제외한 134개 파일에서 percent
텍스트를 제거한 뒤 diff해 byte-identical로 실증(`PERCENT_BASIS_CHANGED_BY_PRECISION_
UPDATE = 0`).

corpus 실측 최대 basis(900px, `ControlPropertyMatrix.xfdl` Form width) 기준
`PERCENT_ROUNDING_MAX_PIXEL_ERROR_BEFORE`(0.5% step) = 900 x 0.25% = 2.25px,
`PERCENT_ROUNDING_MAX_PIXEL_ERROR_AFTER`(0.1% step) = 900 x 0.05% = 0.45px. 실제
폐쇄망 화면은 이보다 넓을 수 있어 절대 오차가 커질 수 있으나, precision 개선만으로 현재
큰 layout failure가 모두 해결됐다고 선언하지 않는다.

corpus에는 실제로 겹치는 sibling Div 사례가 없어 `OVERLAPPING_SIBLING_DIV_COUNT = 0`
(측정값 그대로 보고, 폐쇄망 실제 화면의 overlap 여부는 이 corpus로 확인 불가).

`INVALID_PERCENT_PRECISION_COUNT = 2`(전부 `runtime/xplatform-tab-empty.xml` placeholder,
과거 라운드와 동일 문서화된 예외), `NaN%=0`, `Infinity%=0`. 149/149 변환 성공, XML
well-formed 136/136, id-map(source->target) diff 0, `btn_cm=12`/`wq_gvw=3` invariant
무변경, Phase1 SHA 2/2 PASS.

최종 `XPLATFORM_VISUAL_PARITY = FIX_CANDIDATE`, `PERCENT_ROUNDING_POLICY = ONE_DECIMAL_
PLACE`, `STATIC_VERIFIED`. `STUDIO_DESIGN_FAILED`/`STUDIO_DESIGN_REPRODUCED`가 이 수정만
으로 해결됐다고 선언하지 않는다 -- `STUDIO_DESIGN_REQUIRED` 유지. 상세는
`analysis/freeze-vs-candidate-function-diff.md`의 "후속 라운드 -- XPlatform Visual
Parity Quick Fix + Percentage Precision 통일" 섹션, raw diff는
`analysis/git-baseline-vs-candidate-production.diff` 참고.

## 후속 라운드 -- Visual Parity Quick Fix (GENERAL_LAYOUT_TABLE_HEURISTIC_PAUSED)

이전 라운드(container child만 table 변환 제외) 이후에도 폐쇄망 재현에서 Button 5:5
강제 분할, 서로 다른 Div의 우측 Button 겹침, Calendar/Combo 비노출, Grid 2개+중간
Span만 정상 노출이 계속 보고됐다. Production 재추적 결과 leaf-only Layout(Button
2개, 단일 Edit/Button 등)은 여전히 `TABLE_LAYOUT_HIGH_CONFIDENCE`로 판정되어 table
row/cell 구조로 변환되고 있었다 -- container child 여부와 무관하게, table cell로
편입되면 `includePosition=false`로 원래 left/top이 사라지고 cell의 flow 위치+100%
채움으로 대체되는 동일한 매커니즘이 leaf 컴포넌트에도 그대로 적용되고 있었다.

`[WebSquareGenerator] convertLayoutAsTable`에 `GENERAL_LAYOUT_TABLE_HEURISTIC_PAUSED`
상수를 추가해, root가 아닌 모든 Layout을 일괄적으로 table 미변환(절대좌표 pass-through)
대상으로 뒀다. 기존 table 생성 코드와 container-only 예외 로직은 삭제하지 않고 `else`
분기로 보존해(재활성화 시 상수만 되돌리면 됨), Grid 자체 구조(`w2:gridView`)와
percentage formatter는 이번 라운드에서 전혀 건드리지 않았다.

corpus 실측 결과 이전에 `TABLE_LAYOUT_HIGH_CONFIDENCE`였던 3건(`Form/TabContainer.xfdl`
의 `tabMain.pageA.edtA`/`tabMain.pageB.btnB`, `Form/TabInlineContent.xfdl`의
`tabMain.pageInline.btnInline`)이 모두 원래 source left/top/width/height를 보존한
절대좌표(`position:absolute`)로 전환됐다(예: `btnInline`이 table cell의 flow
위치(암묵적 0,0)+100% 채움에서 `left:1.9%;top:3.4%;width:14.8%;height:8.3%;`로
source geometry와 정확히 일치하도록 복원). 136개 corpus 파일 중 이 2개 파일만
구조 변경, 나머지 134개는 byte-identical(percent formatter 무변경이므로 percent
텍스트도 전혀 바뀌지 않음, `UNEXPECTED_GENERATED_DIFF = 0`).

Calendar/Combo는 이 corpus의 실제 사례(`ControlPropertyMatrix.xfdl`)에서 이미 root
Layout(leaf-only, table 대상 아님) 소속이라 이번 변경으로 영향받지 않았으나, 생성된
`<w2:inputCalendar>`/`<xf:select1>` 모두 `display:none` 등 숨김 스타일 없이
`position:absolute`와 양수 width/height로 정상 생성됨을 재확인했다
(`CALENDAR_GENERATED_ELEMENT_EXISTS = PASS`, `COMBO_GENERATED_ELEMENT_EXISTS = PASS`,
`CALENDAR_VISIBILITY_STATIC_GATE = PASS`, `COMBO_VISIBILITY_STATIC_GATE = PASS`).
실제 폐쇄망 화면에서 Calendar/Combo가 검색조건 Layout(leaf-only, 이번 라운드 이전에는
table 변환 대상) 안에 있었다면 이번 fix로 함께 절대좌표로 복원됐을 것으로 추정되나,
그 화면 자체를 이 corpus로 재현할 수는 없어 Studio 재확인이 필요하다.

percentage formatter(`formatPercent`)는 이번 라운드 무변경(`PERCENT_FORMATTER_BYPASS_
COUNT = 0`, `PERCENT_BASIS_CHANGED_BY_PRECISION_UPDATE = 0` -- 애초에 건드리지 않았으므로
자명). Grid 구조(`GridFormatConverter.java`)도 이번 라운드 `git diff` 0줄로 무변경
확인(`GRID_IMPLEMENTATION_CHANGE = 0`).

149/149 변환 성공, XML well-formed 136/136, PAGE_JS 136/136 PASS, standalone JS 15/15
PASS, id-map(source->target) diff 0, `btn_cm=12`/`wq_gvw=3` invariant 무변경.

최종 `XPLATFORM_VISUAL_PARITY = FIX_CANDIDATE`, `STATIC_VERIFIED`. `STUDIO_DESIGN_
FAILED`/`STUDIO_DESIGN_REPRODUCED`가 이 수정만으로 해결됐다고 선언하지 않는다 --
`STUDIO_DESIGN_REQUIRED` 유지. 상세는 `analysis/freeze-vs-candidate-function-diff.md`
의 "후속 라운드 -- Visual Parity Quick Fix (GENERAL_LAYOUT_TABLE_HEURISTIC_PAUSED)"
섹션, raw diff는 `analysis/git-baseline-vs-candidate-production.diff` 참고.

## 후속 라운드 -- Absolute Component Clipping Quick Fix

직전 라운드(Layout->Table heuristic pause)로 위치는 개선됐으나, 폐쇄망에서 Button/Combo/
Calendar가 잘려 보이는 새 증상이 보고됐다. Production 실측 trace 결과
`COMPONENT_CLIPPING_ROOT_CAUSE = WRONG_PERCENT_BASIS`로 확정됐다: Div는 보통 자식을
자기 내부 `<Layouts><Layout>`으로 다시 감싸므로 `convertLayoutAsTable`이 그 내부 Layout
자신의 geometry로 basis를 정확히 재계산하지만, GroupBox/PopupDiv처럼 자식을 직접
갖는(내부 Layout 래핑이 없는) container는 이 재계산 경로를 타지 않아 바깥 Layout의(더 큰)
basis를 그대로 물려받고 있었다. 실측 결과 GroupBox 자식 Edit(`divA_grpA_edt`)은
33.3%/16.0%(잘못된 basis)로 계산돼야 할 값이 실제로는 40.0%/24.0%(올바른 basis)여야 했고,
PopupDiv 자식 Static(`pop_popSta`)도 동일 패턴으로 13.3%/3.7% -> 54.5%/20.0%로 축소돼
있었다. 이 축소가 Calendar/Combo처럼 내부 최소 렌더링 크기가 필요한 native 위젯에서
clipping으로 나타난 것으로 판단된다. 이 버그는 직전까지 GroupBox/PopupDiv가 1x1 table
cell로 감싸질 때 cell 자신의 실제 px 크기가 우연히 정확한 basis로 재계산돼(이전 라운드의
NESTED_PERCENT_DOUBLE_SCALING fix) 가려져 있었으나, Table heuristic을 pause하면서 원래
있던 버그가 그대로 드러났다.

`[WebSquareGenerator] convertChildren`의 container 재귀 분기에서, 기존 범용 함수
`resolveLayoutBasis`(Element의 width/height를 읽는 generic 함수, "Layout" 태그 전용
아님)를 재사용해 container 자신의 width/height를 자식의 basis로 재계산하도록 최소
수정했다(container에 자기 geometry가 없으면 기존처럼 물려받은 basis 유지, fallback
보존). Tab/Tabpage는 이 분기 이전에 별도 `convertTab`으로 처리돼 영향받지 않는다.

corpus 실측 결과 이 fix로 2개 파일(`Form/NestedContainer.xml`,
`Form/ControlPropertyMatrix.xml`)만 변경, 나머지 134개는 byte-identical
(`UNEXPECTED_GENERATED_DIFF = 0`). 두 사례 모두 AFTER 값이 source geometry와 정확히
일치함을 손계산으로 재확인(`CHILD_GEOMETRY_ROUNDTRIP = PASS`). container 자신의 geometry
(`PARENT_GEOMETRY_ROUNDTRIP`)는 이번 라운드에서 건드리지 않았으며 기존 값 그대로 PASS.
container 자신에 이미 `position:absolute`가 emit되고 있어(기존 `buildComponentStyle`
로직 무변경) 자식의 containing block이 실제로 그 container와 일치함을 확인했다
(`ABSOLUTE_CONTAINING_BLOCK_MATCH = PASS`). Production 전체에서 `overflow` 관련 CSS는
어디에서도 emit되지 않음을 재확인(`CLIPPING_BY_OVERFLOW_COUNT = 0`, 이번 라운드도
overflow 관련 스타일 추가 없음 -- 전역 `overflow:visible` 적용 금지 규칙 준수). basis
체인이 항상 "container 자신의 실제 px 크기"를 다음 단계 기준으로 넘기는 단일 방향
구조라 이중 스케일링 위험이 없다(`NESTED_PERCENT_DOUBLE_SCALING_COUNT = 0`).

Grid(`GridFormatConverter.java`)와 percentage formatter(`ComponentLayoutConverter.java`)
는 이번 라운드 `git diff` 0줄로 완전히 무변경 확인(`GRID_IMPLEMENTATION_CHANGE = 0`,
`PERCENT_PRECISION_CHANGE = 0`). 149/149 변환 성공, XML well-formed 136/136, PAGE_JS
136/136 PASS, standalone JS 15/15 PASS, id-map diff 0, `btn_cm=12`/`wq_gvw=3` invariant
무변경.

최종 `XPLATFORM_VISUAL_PARITY = FIX_CANDIDATE`, `STATIC_VERIFIED`. `STUDIO_DESIGN_
FAILED`/`STUDIO_DESIGN_REPRODUCED`가 이 수정만으로 해결됐다고 선언하지 않는다 --
`STUDIO_DESIGN_REQUIRED` 유지. 상세는 `analysis/freeze-vs-candidate-function-diff.md`의
"후속 라운드 -- Absolute Component Clipping Quick Fix" 섹션, raw diff는
`analysis/git-baseline-vs-candidate-production.diff` 참고.

## 후속 라운드 -- Nested Height Basis / Clipping Quick Fix

Button/Calendar/Combo가 여전히 잘리거나 안 보이는 증상에 대해 `NESTED_VERTICAL_PERCENT_
DOUBLE_SCALING = CONFIRMED`로 판정하고 두 개의 독립적인 basis 문제를 고쳤다.

**문제 1**: `grp_resultArea`(vertical design canvas의 최상단 chain)에 height가 전혀
emit되지 않고 있었다(`width:100%`만 존재). percentage height 체인이 실제로 resolve
되려면 chain 최상단부터 확정 height(auto 아님)가 필요하므로, `grp_main`과 동일하게
source Form의 선언 design height를 재사용해 `grp_resultArea`에도 height를 명시했다
(`[WebSquareGenerator] appendBody`, 기존 `buildMainAreaStyle` 함수 재사용, 신규 함수
없음). 135/136 파일(Form geometry가 있는 거의 전 corpus)에 이 height가 추가됐다(예:
`ControlPropertyMatrix.xfdl`, Form height=650 -> `grp_resultArea height:650px`, `grp_main`
과 동일).

**문제 2**: `convertLayoutAsTable`이 처리하는 nested `Layout` 자신에게 width/height가
없는 경우(Div가 자식을 감싸는 내부 Layout에 크기를 따로 선언하지 않는 실제 XFDL 패턴),
기존 코드는 그 Layout을 감싸는 Div를 건너뛰고 곧바로 Form 전체 크기로 fallback하고
있었다 -- Div 자신은 부모 대비 올바른 비율(예: 5.3%)로 배치돼 있는데 그 안의 자식은
Div가 아니라 Form 전체를 기준으로 다시 계산되어(예: 3.8%) 실제 렌더링에서 두 비율이
곱해진 것처럼 극단적으로 축소되는 매커니즘이다. `convertChildren`이 이미 올바르게
계산해 둔 basis(그 Layout을 실제로 감싸는 가장 가까운 container의 크기)를 파라미터로
전달받아 우선 사용하도록 고쳤다(`inheritedBasisWidth`/`inheritedBasisHeight`, 기존
`resolveLayoutBasis`/`resolveFormBasis` 함수 재사용, 신규 함수 없음). 이 fixture
corpus에는 해당 패턴(Div 내부 Layout이 width/height 없는 경우)이 실제로 존재하지
않아(전수 조사 0건) 실측 BEFORE/AFTER로 시연할 수는 없었으나, 코드 trace로 논리적
정합성을 확인했고 기존 정상 케이스(corpus 100%)에는 이 fallback 분기가 실행되지
않아 전혀 영향이 없다.

136개 corpus 파일 전체 대조 결과 이번 라운드 변경은 오직 `grp_resultArea` height
추가로만 나타났고(135개 파일), 다른 예기치 않은 차이는 없다(`UNEXPECTED_GENERATED_
DIFF = 0`). Button/Calendar/Combo(전부 이 corpus에서 root Layout 직계)는 이번 두
fix의 직접 영향권 밖이라 이전 라운드와 동일한 값을 유지함을 재확인했다(회귀 없음,
`BUTTON_GEOMETRY_ROUNDTRIP`/`CALENDAR_GEOMETRY_ROUNDTRIP`/`COMBO_GEOMETRY_ROUNDTRIP`
= PASS).

Grid(`GridFormatConverter.java`)와 percentage formatter(`ComponentLayoutConverter.java`)
는 이번 라운드 `git diff` 0줄로 완전히 무변경 확인(`GRID_IMPLEMENTATION_CHANGE = 0`,
`PERCENT_PRECISION_CHANGE = 0`). 149/149 변환 성공, XML well-formed 136/136, PAGE_JS
136/136 PASS, standalone JS 15/15 PASS, id-map diff 0, `btn_cm=12`/`wq_gvw=3` invariant
무변경.

최종 `XPLATFORM_VISUAL_PARITY = FIX_CANDIDATE`, `STATIC_VERIFIED`. `STUDIO_DESIGN_
FAILED`/`STUDIO_DESIGN_REPRODUCED`가 이 수정만으로 해결됐다고 선언하지 않는다 --
`STUDIO_DESIGN_REQUIRED` 유지. 상세는 `analysis/freeze-vs-candidate-function-diff.md`
의 "후속 라운드 -- Nested Height Basis / Clipping Quick Fix" 섹션, raw diff는
`analysis/git-baseline-vs-candidate-production.diff` 참고.

## 후속 라운드 -- Actual 760px Height Hierarchy Fix (Quick)

사용자가 실제 폐쇄망 Studio에서 직접 확인한 "760px를 벗어나는 generated Group / 상단
컴포넌트 미표시" 현상을 corpus 비재현을 근거로 부정하지 않고(`USER_CONFIRMED_STUDIO_
EVIDENCE = ACCEPTED`), `NESTED_PERCENT_HEIGHT_REINTERPRETATION = CONFIRMED`로 시작해
`grp_main`의 height 정책을 재점검했다.

`grp_main`은 converter가 만드는 Type B wrapper(실제 XPlatform source 요소 아님)인데,
지금까지 Form 선언 height를 그대로 물려받고 있었다 -- 이는 percentage 분모가 "실제
authored content 범위"가 아니라 "Form 설계 캔버스 명목값"이라는 뜻이다.
`VERTICAL_CONTAINER_PERCENT_NESTING = DISALLOWED` 원칙에 따라 `grp_main`을 실제
content extent(source 최상위 Layout 자식들의 `max(top+height)`, 신규
`[ComponentLayoutConverter] resolveContentExtentHeight`)로 전환했다. `grp_resultArea`
는 기존대로 Form 선언 height를 유지(section 2 요구사항, 무변경).

가장 중요한 점: `grp_main`이 emit하는 실제 px height와 그 아래 root-level 자식들의
percentage 계산 basis를 반드시 **같은 값**으로 맞췄다(`[WebSquareGenerator]
convertLayoutAsTable`의 root basisHeight도 동일한 `resolveContentExtentHeight`
결과를 공유). 하나만 바꾸면 CSS 렌더링 height와 percentage 분모가 어긋나 새로운
double-scaling을 만들기 때문이다.

corpus 실측: `ControlPropertyMatrix.xfdl`(Form height=650, content extent=490) 기준
`grp_main`이 650px -> 490px로 축소됐고, 그 아래 Label/Button/Combo/Calendar 등 모든
root-level component의 percentage가 자동으로 재계산됐다(예: Calendar top 33.8% ->
44.9%, height 3.7% -> 4.9%). px roundtrip은 전부 그대로 유지된다(44.9%*490=220.01px
= source top 220px, 4.9%*490=24.01px = source height 24px). 88/136 파일에서 content
extent가 Form 선언 height와 달라 이 변경이 나타났고, 나머지 48개는 content extent가
Form height와 정확히 같아 byte-identical이다.

**ancestor-chain-aware 재검증(EFFECTIVE_GEOMETRY_AUDIT)**: `grp_main`의 **새로 축소된
실제 px height**를 기준으로(Form 선언 height가 아니라) 124개 percent-geometry Group
전체를 다시 역산했다 -- `GROUP_BOTTOM_OVER_FORM_HEIGHT_COUNT = 0`,
`GROUP_TOP_UNDER_0_COUNT = 0`(위반 없음, basis 일치 설계가 실제로 정합함을 증명).

nested Div/GroupBox/PopupDiv(`divA`/`grpA`/`edt`, `pop`/`popSta`)의 자체 percentage는
이번 변경의 영향을 받지 않고 무변경 유지(그들의 basis는 자기 자신을 감싸는 container의
own geometry이지 grp_main이 아니므로, `UNINTENDED_WRAPPER_PERCENT_BASIS_COUNT = 0`
그대로 유지).

Grid(`GridFormatConverter.java`)는 이번 라운드 `git diff` 0줄로 완전히 무변경
(`GRID_IMPLEMENTATION_CHANGE = 0`). percentage formatter(`formatPercent`)도 무변경
(`PERCENT_PRECISION_CHANGE = 0`, 1012/1012 XFDL-derived one-decimal 준수 유지).
149/149 변환 성공, XML well-formed 136/136, PAGE_JS 136/136 PASS, standalone JS
15/15 PASS, id-map diff 0, `btn_cm=12`/`wq_gvw=3` invariant 무변경.

최종 `XPLATFORM_VISUAL_PARITY = FIX_CANDIDATE`, `STATIC_VERIFIED`. `STUDIO_DESIGN_
FAILED`/`STUDIO_DESIGN_REPRODUCED`가 이 수정만으로 해결됐다고 선언하지 않는다 --
`STUDIO_DESIGN_REQUIRED` 유지. 상세는 `analysis/freeze-vs-candidate-function-diff.md`
의 "후속 라운드 -- Actual 760px Height Hierarchy Fix (Quick)" 섹션, raw diff는
`analysis/git-baseline-vs-candidate-production.diff` 참고.
