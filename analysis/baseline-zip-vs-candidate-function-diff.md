# Baseline ZIP -> Candidate Function-level Diff Audit

Baseline: `work/offline-import/xplatform-to-websquare-offline-import.zip`(read-only, scratch에
extract, ZIP 원본 무수정 — `BASELINE_ZIP_IMMUTABLE = PASS`)
Candidate: `work/closed-network-support/candidates/v6-design-structure-alignment/working-copy`

**중요 발견**: 이 baseline ZIP은 이 candidate가 파생된 `XPWS-OFFLINE-FREEZE-20260820-02`보다
**더 이전 시점**의 스냅샷이다(예: `appendBody`가 아직 `grp_resultArea`/`grp_main` wrapper 없이
`w2:group id="grp_content"` 하나만 만들던 시점, Calendar가 아직 `w2:inputCalendar`가 아닌
`w2:calendar`로 매핑되던 시점). 따라서 이 문서의 diff에는 **이번 세션의 이번 라운드(Table/
Grid Group/Percentage Geometry) 작업**뿐 아니라, 이미 Base Freeze `-02`에 확정되어 있던
**이전 라운드의 historical 작업**(Root/Body wrapper, Calendar QName 수정, btn_cm/wq_gvw video
evidence class, Combo disabledClass)까지 함께 나타난다. 각 함수 항목에 `ROUND` 필드로 구분한다:
`THIS_ROUND`(v6 Design Structure + Table + Grid Group + Percentage Geometry Alignment, 이번
세션 직전 응답에서 수행) vs `HISTORICAL`(이전 라운드, Base Freeze `-02`에 이미 포함되어 있던
확정 fix — 이번 세션에서 재수정하지 않음).

## 1. 분류 요약

- `PRODUCTION_DIFF`: `src/main/java` 5개 파일(아래), 894줄 raw diff.
- `COMMENT_ONLY_DIFF`: 없음(모든 변경 파일이 최소 1개 이상의 functional hunk를 가짐 — 순수
  주석만 변경된 Production 파일은 baseline-ZIP 비교에서는 0건. 단, 이번 라운드에서 별도로 수행한
  "영어→한국어 주석 정리" 자체는 이 문서 4절/`COMMENT_NORMALIZATION_FUNCTIONAL_DIFF` 참고 —
  그건 candidate 내부의 before/after 비교이지 baseline-ZIP 비교가 아니다).
- `ANALYSIS_ONLY_DIFF`: `analysis/`, `docs/`, `audit/`(내용) 등은 이번 비교 범위에서 제외
  (Production 아님).
- `GENERATED_OUTPUT_DIFF`: `build/`, `sample-phase3-output`(reference, 무변경) 등은 별도 —
  본 문서는 소스만 다룬다.

**Production 파일 통계**

| 항목 | 값 |
|---|---|
| Baseline Production file count(`src/main/java/**/*.java`) | 76 |
| Candidate Production file count | 76 |
| Changed Production files | 5 |
| Added Production files | 0 |
| Deleted Production files | 0 |
| 비-Java Production 파일(`.xml`/`.xjs`/`.properties`/`.bat`/`.sh`) 변경 | 0(전부 diff -q 무변경 확인) |

변경 5개 파일: `ComponentMappingRegistry.java`, `ComponentLayoutConverter.java`,
`WebSquareGenerator.java`, `XPlatformProjectConverter.java`, `TabRuntimeScriptGenerator.java`.

`src/main/java/com/example/xfdltracker/report/`는 candidate에만 존재하는 **빈 디렉터리**(파일
없음, `find -name "*.java"` 카운트 76/76 동일에 영향 없음) — 코드 변경 아님, 언급만.

## 2. Functional vs Comment 분리 방법

Java 토큰 인식 comment stripper(문자열/char literal 내부의 `//`, `/* */`는 보존, 실제 comment만
제거 — naive regex 아님, 직접 구현: `strip_java_comments.py`)로 baseline/candidate 5개 파일을
각각 정규화한 뒤 diff. 결과:

- `FUNCTIONAL_CHANGED_FILES = 5`(5개 파일 전부 comment 제거 후에도 diff 존재)
- `COMMENT_ONLY_CHANGED_FILES = 0`(baseline-ZIP 비교 기준 — 5개 파일 모두 최소 1개 functional
  hunk를 포함하므로 "comment만 변경된 파일"은 0건)
- `FUNCTIONAL_CHANGED_FUNCTIONS = 27`(아래 3절 목록, class-level 2건 포함)
- `COMMENT_ONLY_CHANGED_LOCATIONS`: 이 baseline-ZIP 비교에서는 별도 집계하지 않음(모든 hunk가
  functional). 대신 4절의 "이번 세션 주석 정리"에서 candidate 내부 before/after 기준으로 집계.

Raw diff: `analysis/baseline-zip-vs-candidate-production.diff`(894줄).
Functional diff: `analysis/baseline-zip-vs-candidate-functional.diff`(643줄, comment-stripped).

## 3. 함수별 상세

### [ComponentMappingRegistry] class-level (static initializer)

- 파일: `src/main/java/com/example/xfdltracker/mapping/ComponentMappingRegistry.java`
- ROUND: `HISTORICAL`(Base Freeze `-02`에 이미 포함, 이번 세션 무수정)
- 변경 분류: `COMPONENT_CONVERSION`
- 변경 목적: `Calendar` source 매핑을 `w2:calendar`(picker-only)에서 `w2:inputCalendar`(edit box +
  picker)로 수정 — `V6_COMPONENT_MAPPING_MISMATCH` fix.
- Baseline 역할: `add("Calendar", "w2:calendar", ...)` — 실제 XPlatform Calendar(edit+picker
  복합 위젯)와 의미가 다른 QName.
- Candidate 역할: `add("Calendar", "w2:inputCalendar", ...)` — 정확한 QName.
- Functional change: YES
- Caller: `ComponentMappingRegistry` static initializer 자체(`WebSquareGenerator.
  componentMappings.get("Calendar")`가 이 항목을 조회).
- Callee: 없음(데이터 등록).
- 영향 component: XPlatform `Calendar` 컴포넌트 전체.
- 영향 output: corpus 실측 `w2:inputCalendar` 1건(`w2:calendar` 0건).

BEFORE:
```java
add("Calendar", "w2:calendar", SupportLevel.PARTIAL, false, "date/edit format partially mapped");
```
AFTER:
```java
add("Calendar", "w2:inputCalendar", SupportLevel.PARTIAL, false, "date/edit format partially mapped; uiplugin.inputCalendar (edit box + picker), not bare uiplugin.calendar (picker-only) -- see V6_COMPONENT_MAPPING_MISMATCH fix");
```
Unified Diff:
```diff
-        add("Calendar", "w2:calendar", SupportLevel.PARTIAL, false, "date/edit format partially mapped");
+        add("Calendar", "w2:inputCalendar", SupportLevel.PARTIAL, false, "date/edit format partially mapped; uiplugin.inputCalendar (edit box + picker), not bare uiplugin.calendar (picker-only) -- see V6_COMPONENT_MAPPING_MISMATCH fix");
```
Generated XML impact: `ROOT_STRUCTURE=NO`, `COMPONENT_CONVERSION=YES` — Calendar QName 변경.
Regression: PASS(corpus `w2:inputCalendar=1`, Base Freeze `-02`와 동일 — 이번 세션에서 확인만).
Status: `STATIC_VERIFIED`(historical, 재검증 완료).

---

### [ComponentLayoutConverter] class-level (imports)

- ROUND: `THIS_ROUND`
- 변경 분류: `TABLE_LAYOUT` / `PERCENT_GEOMETRY`(지원 import)
- `import java.util.ArrayList;`, `import java.util.List;` 추가 — `classifyLayoutGeometry`/
  `buildTableRows`/`resolveCellGeometries` 등이 사용하는 `List<Element>`/`List<CellGeometry>`용.
- Functional change: YES(컴파일에 필요하므로 functional, 단 실행 동작에는 그 자체로 영향 없음).
- Generated XML impact: `NONE`(import 자체는 output에 영향 없음).

---

### [ComponentLayoutConverter] hasGeometry — 신규 함수

- ROUND: `THIS_ROUND`
- 변경 분류: `PERCENT_GEOMETRY`
- 목적: source가 위치/크기 속성을 하나라도 갖는지 판별 — percent 변환 대상 여부 판정에 사용.
- Caller: `[WebSquareGenerator] copyBasicProperties`, `[WebSquareGenerator] convertChildren`
  (Grid Group 분기).
- Callee: `resolveGeometry`(기존, 무수정).
- Functional change: YES(신규).

BEFORE: 없음(baseline ZIP에 이 클래스 자체에 이 메서드 없음).
AFTER:
```java
public boolean hasGeometry(Element source) {
    return resolveGeometry(source).hasAnyPositionOrSize();
}
```
Generated XML impact: `NO_GENERATED_OUTPUT_IMPACT`(직접적으로는 없음 — 판정 helper).
Status: `STATIC_VERIFIED`.

---

### [ComponentLayoutConverter] buildComponentStyle — 기존 함수 확장(오버로드 추가)

- ROUND: `THIS_ROUND`
- 변경 분류: `PERCENT_GEOMETRY`
- 목적: `includePosition=false`(Table 셀 내부)일 때 `position:absolute`/`left`/`top`을 생성하지
  않도록 오버로드 추가. 기존 1-인자 시그니처는 그대로 유지(2-인자로 위임, 하위 호환).
- Baseline 역할: 항상 `position:absolute` + left/top/width/height(px) emit.
- Candidate 역할: `includePosition` 플래그로 좌표 emit 여부 제어, width/height는 항상 emit.
- Functional change: YES
- Caller: `[WebSquareGenerator] copyBasicProperties`(px fallback 경로), `convertChildren`
  (Grid Group wrapper px fallback).
- Callee: `resolveGeometry`, `appendCssLength`, `appendVisualStyle`(전부 기존, 무수정).

BEFORE:
```java
public String buildComponentStyle(Element source) {
    Geometry geometry = resolveGeometry(source);
    StringBuilder style = new StringBuilder();
    if (geometry.hasAnyPositionOrSize()) {
        style.append("position:absolute;");
        appendCssLength(style, "left", geometry.left);
        appendCssLength(style, "top", geometry.top);
        appendCssLength(style, "width", geometry.width);
        appendCssLength(style, "height", geometry.height);
    }
    appendVisualStyle(source, style);
    return style.toString();
}
```
AFTER:
```java
public String buildComponentStyle(Element source) {
    return buildComponentStyle(source, true);
}

public String buildComponentStyle(Element source, boolean includePosition) {
    Geometry geometry = resolveGeometry(source);
    StringBuilder style = new StringBuilder();
    if (geometry.hasAnyPositionOrSize()) {
        if (includePosition) {
            style.append("position:absolute;");
            appendCssLength(style, "left", geometry.left);
            appendCssLength(style, "top", geometry.top);
        }
        appendCssLength(style, "width", geometry.width);
        appendCssLength(style, "height", geometry.height);
    }
    appendVisualStyle(source, style);
    return style.toString();
}
```
Generated XML impact: `PERCENT_GEOMETRY`(px fallback 경로에서 Table 셀 내부 컴포넌트는 좌표 없이
width/height만 emit). Affected outputs = 13/136(px fallback 발생 건수, corpus 실측).
Status: `STATIC_VERIFIED`.

---

### [ComponentLayoutConverter] buildRootStyle — 기존 함수 수정(historical) + 현재 미사용

- ROUND: `HISTORICAL`(position:relative/overflow:hidden 제거는 ISSUE-20260818-001 Studio 빈
  화면 fix, Base Freeze `-02`에 이미 포함) — **단, 이 함수의 유일한 호출부(`appendBody`의
  `grp_content` 엘리먼트 생성)가 이번 라운드(THIS_ROUND)에 제거되면서 이 함수 자체가 현재
  unused 상태가 되었다.**
- 변경 분류: `ROOT_STRUCTURE`
- Functional change: YES(historical 기준), 그러나 이번 라운드는 이 함수의 **호출 여부**만
  바꿨을 뿐 함수 본문 자체는 무수정.
- Caller(baseline): `appendBody`. Caller(candidate, 현재): **없음**(dead code).

BEFORE(baseline ZIP):
```java
public String buildRootStyle(Document source) {
    StringBuilder style = new StringBuilder();
    style.append("position:relative;");
    style.append("overflow:hidden;");
    Geometry geometry = findFormGeometry(source);
    if (geometry != null) {
        appendCssLength(style, "width", geometry.width);
        appendCssLength(style, "height", geometry.height);
    }
    if (style.indexOf("width:") < 0) { style.append("width:100%;"); }
    if (style.indexOf("height:") < 0) { style.append("height:100%;"); }
    return style.toString();
}
```
AFTER(candidate, 함수 본문은 historical round 이후 무변경, `position:relative`/`overflow:hidden`
이미 제거된 상태):
```java
public String buildRootStyle(Document source) {
    StringBuilder style = new StringBuilder();
    Geometry geometry = findFormGeometry(source);
    if (geometry != null) {
        appendCssLength(style, "width", geometry.width);
        appendCssLength(style, "height", geometry.height);
    }
    if (style.indexOf("width:") < 0) { style.append("width:100%;"); }
    if (style.indexOf("height:") < 0) { style.append("height:100%;"); }
    return style.toString();
}
```
Generated XML impact: `NONE`(현재 호출되지 않으므로 output에 영향 없음).
Status: `UNEXPECTED_CHANGE` 후보 아님(정당한 historical fix) 이지만, **dead code 정리 필요 —
`NEXT_ROUND_CANDIDATE`로 이미 기록됨**(analysis 문서 참고).

---

### [ComponentLayoutConverter] formatPercent / resolveLayoutBasis / buildPercentComponentStyle / buildTableRowStyle / buildTableCellStyle — 신규 함수 5개

- ROUND: `THIS_ROUND`
- 변경 분류: `PERCENT_GEOMETRY`
- 각 함수의 코드/목적/caller-callee는 `analysis/freeze-vs-candidate-function-diff.md`(이전
  응답에서 작성)에 이미 BEFORE(없음)/AFTER 전체 코드로 기록되어 있음 — 여기서는 baseline-ZIP
  기준으로 "전부 신규(baseline에 존재하지 않음)"임을 재확인만 한다(중복 방지, 동일 문서 재작성
  생략).
- Caller 요약: `buildPercentComponentStyle`은 `copyBasicProperties`/`convertChildren`(Grid
  Group), `buildTableRowStyle`/`buildTableCellStyle`은 `convertLayoutAsTable`, `resolveLayoutBasis`
  는 `convertLayoutAsTable`, `formatPercent`는 위 4개 함수 내부에서 공통 사용.
- Functional change: YES(전부 신규).
- Generated XML impact: `PERCENT_GEOMETRY`. Affected outputs = 124/136(percent 적용), 13/136
  (unresolved, px fallback).
- Status: `STATIC_VERIFIED`.

---

### [ComponentLayoutConverter] classifyLayoutGeometry — 신규(이전 라운드) + 기준 완화(이번 라운드)

- ROUND: `THIS_ROUND`(baseline ZIP 기준으로는 통째로 신규 — 이 함수 자체가 baseline에 없음.
  단, 이번 세션 내에서도 "1차 신규 도입(이전 라운드)" → "판정 기준 완화(이번 라운드)" 두 단계를
  거쳤음을 참고로 명시)
- 변경 분류: `TABLE_LAYOUT`
- Caller: `[WebSquareGenerator] convertLayoutAsTable`.
- Callee: `resolveCellGeometries`, `hasOverlap`.
- Functional change: YES

BEFORE: 없음(baseline ZIP에 함수 자체가 없음 — Table 개념 자체가 baseline에 존재하지 않음,
당시는 모든 `Layout`이 flat pass-through).
AFTER:
```java
public String classifyLayoutGeometry(List<Element> children) {
    List<CellGeometry> cells = resolveCellGeometries(children);
    if (cells == null || cells.isEmpty()) {
        return "UNRESOLVED_LAYOUT";
    }
    if (hasOverlap(cells)) {
        return "ABSOLUTE_LAYOUT_FALLBACK";
    }
    return "TABLE_LAYOUT_HIGH_CONFIDENCE";
}
```
Generated XML impact: `TABLE_STRUCTURE`. Affected outputs: real corpus HIGH_CONFIDENCE=5/136,
UNRESOLVED=2/136(children=0), root-Layout 강제 fallback=121/136(별도 guard, `convertLayoutAsTable`
쪽 책임).
Status: `STATIC_VERIFIED`.

---

### [ComponentLayoutConverter] buildTableRows / resolveCellGeometries / groupByTop / hasOverlap / CellGeometry — 신규(이전 라운드), resolveCellGeometries만 이번 라운드 완화

- ROUND: `buildTableRows`/`groupByTop`/`hasOverlap`/`CellGeometry`(신규 클래스) = 이전 라운드
  신규(baseline 기준으로는 전부 신규), 이번 라운드 무수정. `resolveCellGeometries`만 이번 라운드
  `children.size() < 4` 조건 제거(`children.isEmpty()`만 검사).
- 변경 분류: `TABLE_LAYOUT`
- `resolveCellGeometries` BEFORE(이전 라운드 버전, baseline에는 없음):
  `if (children == null || children.size() < 4) { return null; }`
- `resolveCellGeometries` AFTER(이번 라운드): `if (children == null || children.isEmpty()) { return null; }`
- Generated XML impact: `TABLE_STRUCTURE`(1-row/소수 child Layout도 table 대상이 됨).
- Status: `STATIC_VERIFIED`.

---

### [WebSquareGenerator] generate(6-인자 overload) — 기존 함수 수정(historical)

- ROUND: `HISTORICAL`(`grp_resultArea`/`grp_main` id 예약은 Root/Body 정렬 라운드, Base Freeze
  `-02`에 이미 포함, 이번 세션 무수정)
- 변경 분류: `ROOT_STRUCTURE`
- Caller: `generate`(1/4/5-인자 오버로드들이 이 6-인자로 위임).
- Callee: `usedTargetIds.add(...)`, `XfdlReader.read`, `BindingAnalyzer.analyze`,
  `appendBody`, `appendScript`, `appendStyle`.

BEFORE:
```java
usedTargetIds.add("grp_content");
```
AFTER:
```java
usedTargetIds.add("grp_content");
usedTargetIds.add("grp_resultArea");
usedTargetIds.add("grp_main");
```
Generated XML impact: `ROOT_STRUCTURE`(id 충돌 방지용 사전 예약, 직접적 출력 아님).
Status: `STATIC_VERIFIED`(historical, 재검증 완료).

---

### [WebSquareGenerator] appendBody — 기존 함수 수정(historical + 이번 라운드)

- ROUND: **양쪽 다** — historical(단일 `w2:group id="grp_content"` → `grp_resultArea`/`grp_main`
  2단 wrapper 도입)과 THIS_ROUND(그 안쪽의 3번째 wrapper `grp_content` 자체를 제거하고
  `convertChildren`을 `grp_main`에 직접 연결, basis 파라미터 초기값 threading)가 누적됨.
- 변경 분류: `ROOT_STRUCTURE`
- Caller: `generate`(6-인자 overload).
- Callee: `bindFormLifecycle`, `buildMainAreaStyle`, `registerFormRootMapping`, `convertChildren`,
  `finalizePageLoadBinding`, `logUnmappedEventBindings`.

BEFORE(baseline ZIP, historical 이전):
```java
Element root = out.createElementNS(NS_W2, "w2:group");
root.setAttribute("id", "grp_content");
root.setAttribute("style", layoutConverter.buildRootStyle(source));
body.appendChild(root);
registerFormRootMapping(source);
Element sourceRoot = source.getDocumentElement();
convertChildren(out, sourceRoot, root, "", analysis, 0);
```
AFTER(candidate, 누적):
```java
Element resultArea = out.createElementNS(NS_XF, "xf:group");
resultArea.setAttribute("id", "grp_resultArea");
resultArea.setAttribute("style", "");
body.appendChild(resultArea);

Element main = out.createElementNS(NS_XF, "xf:group");
main.setAttribute("id", "grp_main");
main.setAttribute("style", layoutConverter.buildMainAreaStyle(source));
resultArea.appendChild(main);
registerFormRootMapping(source);

Element sourceRoot = source.getDocumentElement();
convertChildren(out, sourceRoot, main, "", analysis, 0, null, -1.0, -1.0, true);
```
Generated XML BEFORE(baseline): `<body><w2:group id="grp_content" style="position:relative;overflow:hidden;width:...px;height:...px;">...content...</w2:group></body>`
Generated XML AFTER(candidate): `<body><xf:group id="grp_resultArea" style=""><xf:group id="grp_main" style="height:...px;">...content(직접, wrapper 없음)...</xf:group></xf:group></body>`
Affected outputs = 136/136(corpus 전체 — 모든 화면의 root wrapper 구조에 영향).
Status: `STATIC_VERIFIED`.

---

### [WebSquareGenerator] convertChildren — 기존 함수 수정(이전 라운드 + 이번 라운드 누적)

- ROUND: 이전 라운드(`onlyChild` 필터 추가, 5-인자→7-인자) + THIS_ROUND(`basisWidth`/
  `basisHeight`/`includePosition` 추가 10-인자, Grid Group wrapper 분기 추가)
- 변경 분류: `TABLE_LAYOUT` / `PERCENT_GEOMETRY` / `GRID_GROUP_STRUCTURE`
- Caller: `appendBody`, `convertLayoutAsTable`(fallback 경로 + cell 배치), 자기 자신(container
  재귀, pass-through 재귀), `convertTab`(Tabpage content).
- Callee: `convertTab`, `copyBasicProperties`, `applyComponentSpecificProperties`,
  `applyBindings`, `bindEvents`, `gridFormatConverter.convert`, `layoutConverter.hasGeometry`/
  `buildPercentComponentStyle`/`buildComponentStyle`, `convertLayoutAsTable`, 재귀 자기 자신.
- baseline 대비 신규 overload인 10-인자 시그니처를 호출하는 경로: 전부(baseline에는 이 파라미터
  자체가 없었으므로 모든 호출 경로가 사실상 "신규 시그니처" 사용).

BEFORE(baseline, 5-인자):
```java
private void convertChildren(
        Document out, Element sourceParent, Element targetParent,
        String parentPath, XfdlAnalysisResult analysis, int depth) {
    ...
    Element src = (Element) node;
    String sourceTag = getSourceTagName(src);
    ...
    if ("Tab".equals(sourceTag) && componentMapping != null && targetTag != null) {
        convertTab(out, src, targetParent, parentPath, analysis, depth, componentMapping);
        continue;
    }
    if (targetTag != null) {
        ...
        copyBasicProperties(src, target);
        ...
        targetParent.appendChild(target);
        ...
        if (isContainerComponent(sourceTag)) {
            convertChildren(out, src, target, sourcePath, analysis, depth + 1);
        }
        continue;
    }
    if (shouldTraverseUnknownElement(sourceTag)) {
        ...
        convertChildren(out, src, targetParent, parentPath, analysis, depth + 1);
    }
}
```
AFTER(candidate, 10-인자, Grid Group 분기 포함 — 전체 코드는
`src/main/java/com/example/xfdltracker/converter/WebSquareGenerator.java`의 동명 메서드,
발췌는 이전 응답의 `analysis/freeze-vs-candidate-production.diff` L371-507 참고): `onlyChild`
필터, basis 4-파라미터 threading, `if ("w2:gridView".equals(targetTag))` Grid Group wrapper
분기가 추가됐다(6절 "Generated XML BEFORE/AFTER"에 실제 XML 첨부).

Generated XML impact: `ROOT_STRUCTURE` / `TABLE_STRUCTURE` / `GRID_GROUP` / `PERCENT_GEOMETRY`
(사실상 corpus 전체 output의 body 내부 전체 구조에 관여하는 핵심 함수). Affected outputs =
136/136.
Status: `STATIC_VERIFIED`.

---

### [WebSquareGenerator] convertLayoutAsTable — 신규(이전 라운드) + 대폭 수정(이번 라운드)

- ROUND: THIS_ROUND(baseline에는 함수 자체가 없음 — Table 개념 자체가 이전 라운드 신규 도입,
  이번 라운드에 basis 계산/root-layout guard/percent style 부여로 대폭 확장)
- 변경 분류: `TABLE_LAYOUT` / `PERCENT_GEOMETRY`
- Caller: `[WebSquareGenerator] convertChildren`(pass-through 분기, `"Layout".equals(sourceTag)`).
- Callee: `directElementChildren`, `layoutConverter.classifyLayoutGeometry`/`resolveLayoutBasis`/
  `buildTableRows`/`buildTableRowStyle`/`buildTableCellStyle`, `createUniqueTargetId`,
  `buildSourcePath`, `convertChildren`(fallback 경로 + cell 배치, 2가지 호출 패턴).

BEFORE: 없음(baseline ZIP에는 `Layout`이 항상 flat pass-through — 이 함수 자체가 존재하지 않음,
당시 `convertChildren`의 pass-through 분기는 `Layout`/`Layouts`/`FDL`/`Form`/기타 unknown 태그를
전부 동일하게 재귀만 했다).
AFTER: 이전 응답의 `analysis/freeze-vs-candidate-production.diff`(Base Freeze `-02` 기준 diff)
L509-627에 전체 코드 기록됨(중복 생략). 핵심: `isRootFormLayout = parentPath.length() == 0`일
때 classification과 무관하게 `ROOT_FORM_LAYOUT_NOT_A_TABLE_TARGET`으로 강제 fallback.
Generated XML impact: `TABLE_STRUCTURE` / `PERCENT_GEOMETRY`. Affected outputs: HIGH_CONFIDENCE
5/136, root-layout guard 121/136(강제 fallback), UNRESOLVED 2/136.
Status: `STATIC_VERIFIED`.

---

### [WebSquareGenerator] directElementChildren — 신규 함수(이전 라운드, 이번 라운드 무수정)

- ROUND: baseline 기준 신규(THIS_ROUND 범주에 포함, 함수 자체는 무수정).
- 변경 분류: `TABLE_LAYOUT`
- Caller: `convertLayoutAsTable`.
- Callee: 없음(NodeList 순회).
- Generated XML impact: `NONE`(직접 output 생성 안함, helper).

---

### [WebSquareGenerator] convertTab — 기존 함수 수정(이번 라운드)

- ROUND: THIS_ROUND
- 변경 분류: `PERCENT_GEOMETRY`
- 목적: `basisWidth`/`basisHeight`/`includePosition` 파라미터 추가, Tabpage content 진입 시
  독립 scope로 취급해 basis를 fresh하게 리셋.
- Caller: `[WebSquareGenerator] convertChildren`("Tab".equals(sourceTag) 분기).
- Callee: `copyBasicProperties`, `applyComponentSpecificProperties`, `applyBindings`,
  `bindEvents`, `directTabpages`, `applyExternalTabContent`, `convertChildren`(Tabpage content).

BEFORE(baseline 시그니처):
```java
private void convertTab(
        Document out, Element src, Element targetParent, String parentPath,
        XfdlAnalysisResult analysis, int depth, ComponentMapping componentMapping) {
    ...
    copyBasicProperties(src, tabControl);
    ...
    convertChildren(out, page, content, pagePath, analysis, depth + 1);
}
```
AFTER:
```java
private void convertTab(
        Document out, Element src, Element targetParent, String parentPath,
        XfdlAnalysisResult analysis, int depth, ComponentMapping componentMapping,
        double basisWidth, double basisHeight, boolean includePosition) {
    ...
    copyBasicProperties(src, tabControl, basisWidth, basisHeight, includePosition);
    ...
    convertChildren(out, page, content, pagePath, analysis, depth + 1, null, -1.0, -1.0, true);
}
```
Generated XML impact: `PERCENT_GEOMETRY`(Tab 컨트롤 자체의 style, Tabpage content의 fresh basis
리셋). Affected outputs: corpus 내 `Tab` 컴포넌트를 포함한 화면 다수(정확한 개수는 `[UI 변환] Tab`
로그 카운트로 측정 가능 — corpus는 Tab 중심 fixture 비중이 매우 높음, 97개 화면이 단일 Tab
컴포넌트 구조).
Status: `STATIC_VERIFIED`.

---

### [WebSquareGenerator] copyBasicProperties — 기존 함수 수정(이번 라운드)

- ROUND: THIS_ROUND
- 변경 분류: `PERCENT_GEOMETRY`
- Caller: `convertChildren`(mapped-component 분기), `convertTab`.
- Callee: `layoutConverter.hasGeometry`/`buildPercentComponentStyle`/`buildComponentStyle`,
  `sanitizeXml10`, `copyAttributeIfPresent`, `resolveVideoEvidenceBaseClass`,
  `appendClassTokenIfAbsent`.

BEFORE:
```java
private void copyBasicProperties(Element src, Element target) {
    ...
    String style = sanitizeXml10(layoutConverter.buildComponentStyle(src));
    if (style.length() > 0) { target.setAttribute("style", style); }
    ...
}
```
AFTER:
```java
private void copyBasicProperties(Element src, Element target) {
    copyBasicProperties(src, target, -1.0, -1.0, true);
}

private void copyBasicProperties(
        Element src, Element target, double basisWidth, double basisHeight, boolean includePosition) {
    ...
    String style;
    if (layoutConverter.hasGeometry(src)) {
        String percentStyle = (basisWidth > 0.0 && basisHeight > 0.0)
                ? layoutConverter.buildPercentComponentStyle(src, basisWidth, basisHeight, includePosition)
                : null;
        style = percentStyle != null ? percentStyle : layoutConverter.buildComponentStyle(src, includePosition);
    } else {
        style = layoutConverter.buildComponentStyle(src, includePosition);
    }
    style = sanitizeXml10(style);
    if (style.length() > 0) { target.setAttribute("style", style); }
    ...
}
```
Generated XML impact: `PERCENT_GEOMETRY`(모든 일반 component의 style 속성). Affected outputs =
124/136(percent 적용) + 13/136(px fallback) = 137건 개별 컴포넌트 단위 측정(화면 수 아님).
Status: `STATIC_VERIFIED`.

---

### [WebSquareGenerator] resolveVideoEvidenceBaseClass / appendClassTokenIfAbsent — 신규 함수(historical)

- ROUND: `HISTORICAL`(v6 실제 화면 영상 판독 evidence 기반 base class 라운드, Base Freeze `-02`에
  이미 포함, 이번 세션 무수정)
- 변경 분류: `COMPONENT_CONVERSION`
- 목적: `xf:trigger`→`btn_cm`, `w2:gridView`→`wq_gvw` base class 자동 부여.
- Caller: `copyBasicProperties`.
- Generated XML impact: `NO_GENERATED_OUTPUT_IMPACT`(이번 라운드 관점에서는 무변경 — class 값은
  이전과 동일). Affected outputs = 12(`btn_cm`) + 3(`wq_gvw`) = 15건, corpus 실측, Base와 동일.
- Status: `STATIC_VERIFIED`(historical, 재검증만).

---

### [WebSquareGenerator] applyComponentSpecificProperties — 기존 함수 수정(historical, Combo disabledClass)

- ROUND: `HISTORICAL`(Combo `disabledClass="w2selectbox_disabled"` 보호 fix, Base Freeze `-02`에
  이미 포함, 이번 세션 무수정)
- 변경 분류: `COMPONENT_CONVERSION`
- BEFORE: `target.setAttribute("appearance", "minimal");`(Combo 분기, disabledClass 없음)
- AFTER: `target.setAttribute("appearance", "minimal"); target.setAttribute("disabledClass", "w2selectbox_disabled");`
- Generated XML impact: `NO_GENERATED_OUTPUT_IMPACT`(이번 라운드 기준 무변경, historical).
  Affected outputs = 4건(corpus 실측, Base와 동일).
- Status: `STATIC_VERIFIED`(historical, 재검증만).

---

### [WebSquareGenerator] registerFormRootMapping — 기존 함수 수정(이번 라운드)

- ROUND: THIS_ROUND
- 변경 분류: `SOURCE_TARGET_MAPPING` / `ROOT_STRUCTURE`
- Caller: `appendBody`.
- Callee: `findDescendants`, `canonicalizePath`, `sanitizeXml10`.

BEFORE:
```java
private void registerFormRootMapping(Document source) {
    List<Element> forms = findDescendants(source.getDocumentElement(), "Form");
    if (forms.isEmpty()) return;
    String formId = canonicalizePath(sanitizeXml10(forms.get(0).getAttribute("id")));
    if (formId.length() > 0 && !componentIdMap.containsKey(formId)) {
        componentIdMap.put(formId, "grp_content");
        targetComponentTypeMap.put("grp_content", "Form");
        System.out.println("[UI 매핑] Form " + formId + " -> grp_content (lifecycle obj 호환)");
    }
}
```
AFTER:
```java
private void registerFormRootMapping(Document source) {
    List<Element> forms = findDescendants(source.getDocumentElement(), "Form");
    if (forms.isEmpty()) return;
    String formId = canonicalizePath(sanitizeXml10(forms.get(0).getAttribute("id")));
    if (formId.length() > 0 && !componentIdMap.containsKey(formId)) {
        componentIdMap.put(formId, "grp_main");
        targetComponentTypeMap.put("grp_main", "Form");
        System.out.println("[UI 매핑] Form " + formId + " -> grp_main (lifecycle obj 호환)");
    }
}
```
Generated XML impact: `NONE`(componentIdMap 등록 — 생성된 body XML 자체가 아니라 script 변환 시
사용되는 매핑 테이블). `SOURCE_TO_TARGET_ID_MAP_EXPECTED_ONLY` 영향(135건 예상 변경, PASS 확인
완료).
Status: `STATIC_VERIFIED`.

---

### [XPlatformProjectConverter] writeTabRuntimeResources — 기존 함수 수정(이번 라운드)

- ROUND: THIS_ROUND
- 변경 분류: `LIFECYCLE_COMPATIBILITY`
- Caller: 프로젝트 conversion 메인 흐름(`hasRuntimeTabs(tabRuntimePlans)`일 때 호출, L224).
- Callee: `TabRuntimeScriptGenerator.generateStandaloneReference`, `TextFileUtil.writeUtf8`.

BEFORE:
```java
+ "  <body><w2:group id=\"grp_content\" style=\"position:relative;width:100%;height:100%;\"/></body>\n"
```
AFTER:
```java
+ "  <body><w2:group id=\"grp_main\" style=\"position:relative;width:100%;height:100%;\"/></body>\n"
```
Generated XML impact: `NONE`(body 구조 아님, `runtime/xplatform-tab-empty.xml` placeholder
전용). Affected outputs = corpus conversion 1회당 1개 파일(`runtime/xplatform-tab-empty.xml`).
Status: `STATIC_VERIFIED`.

---

### [TabRuntimeScriptGenerator] generate — 기존 함수 수정(이번 라운드, literal만)

- ROUND: THIS_ROUND
- 변경 분류: `LIFECYCLE_COMPATIBILITY`
- Caller: `[WebSquareGenerator] appendScript`(runtime 필요 시 `new TabRuntimeScriptGenerator().generate(...)`).
- Callee: 없음(문자열 조립).

BEFORE(2개 literal):
```java
s.append("    function currentFrame(){var root=component('grp_content'); ... }\n");
s.append("    function parentWindow(depth) { ... if(!w||!w.grp_content||!w.grp_content.getScope) ... frame=w.grp_content.getScope(); ... }\n");
```
AFTER:
```java
s.append("    function currentFrame(){var root=component('grp_main'); ... }\n");
s.append("    function parentWindow(depth) { ... if(!w||!w.grp_main||!w.grp_main.getScope) ... frame=w.grp_main.getScope(); ... }\n");
```
Generated XML impact: `NONE`(body 구조 아님, `<script>` CDATA 내부 JS literal). `Phase1 SHA`는
이 literal 변경과 무관한 정적 reference fixture만 검사하므로 영향 없음(재확인 완료, PASS).
Affected outputs: Tab runtime이 필요한 corpus 화면(런타임 JS 삽입 대상) + standalone
`runtime/xplatform-tab-runtime.js` 1건.
Status: `STATIC_VERIFIED`.

---

## 4. Summary Matrix

| Class | Function | Change Type | Functional? | Round | Caller | Generated Impact | Affected Outputs | Status |
|---|---|---|---|---|---|---:|---:|---|
| ComponentMappingRegistry | class-level(static init, Calendar) | COMPONENT_CONVERSION | YES | HISTORICAL | (static registry) | COMPONENT_CONVERSION | 1/136 | STATIC_VERIFIED |
| ComponentLayoutConverter | class-level(imports) | REFACTOR_ONLY | YES | THIS_ROUND | — | NONE | — | STATIC_VERIFIED |
| ComponentLayoutConverter | hasGeometry — 신규 | PERCENT_GEOMETRY | YES | THIS_ROUND | copyBasicProperties, convertChildren | NO_GENERATED_OUTPUT_IMPACT | — | STATIC_VERIFIED |
| ComponentLayoutConverter | buildComponentStyle(오버로드 추가) | PERCENT_GEOMETRY | YES | THIS_ROUND | copyBasicProperties, convertChildren | PERCENT_GEOMETRY | 13/136 | STATIC_VERIFIED |
| ComponentLayoutConverter | buildRootStyle | ROOT_STRUCTURE | YES | HISTORICAL(호출 제거는 THIS_ROUND) | (없음, dead code) | NONE | 0 | STATIC_VERIFIED |
| ComponentLayoutConverter | formatPercent — 신규 | PERCENT_GEOMETRY | YES | THIS_ROUND | (내부 helper) | PERCENT_GEOMETRY | 124/136 | STATIC_VERIFIED |
| ComponentLayoutConverter | resolveLayoutBasis — 신규 | PERCENT_GEOMETRY | YES | THIS_ROUND | convertLayoutAsTable | PERCENT_GEOMETRY | 128/136(Layout 판정) | STATIC_VERIFIED |
| ComponentLayoutConverter | buildPercentComponentStyle — 신규 | PERCENT_GEOMETRY | YES | THIS_ROUND | copyBasicProperties, convertChildren | PERCENT_GEOMETRY | 124/136 | STATIC_VERIFIED |
| ComponentLayoutConverter | buildTableRowStyle — 신규 | TABLE_LAYOUT/PERCENT_GEOMETRY | YES | THIS_ROUND | convertLayoutAsTable | TABLE_STRUCTURE | 5/136 | STATIC_VERIFIED |
| ComponentLayoutConverter | buildTableCellStyle — 신규 | TABLE_LAYOUT/PERCENT_GEOMETRY | YES | THIS_ROUND | convertLayoutAsTable | TABLE_STRUCTURE | 5/136 | STATIC_VERIFIED |
| ComponentLayoutConverter | classifyLayoutGeometry | TABLE_LAYOUT | YES | THIS_ROUND(신규+완화) | convertLayoutAsTable | TABLE_STRUCTURE | 128/136 | STATIC_VERIFIED |
| ComponentLayoutConverter | buildTableRows — 신규 | TABLE_LAYOUT | YES | THIS_ROUND | convertLayoutAsTable | TABLE_STRUCTURE | 5/136 | STATIC_VERIFIED |
| ComponentLayoutConverter | resolveCellGeometries | TABLE_LAYOUT | YES | THIS_ROUND(신규+완화) | classifyLayoutGeometry, buildTableRows | TABLE_STRUCTURE | 128/136 | STATIC_VERIFIED |
| ComponentLayoutConverter | groupByTop — 신규 | TABLE_LAYOUT | YES | THIS_ROUND | classifyLayoutGeometry, buildTableRows | TABLE_STRUCTURE | 128/136 | STATIC_VERIFIED |
| ComponentLayoutConverter | hasOverlap — 신규 | TABLE_LAYOUT | YES | THIS_ROUND | classifyLayoutGeometry | TABLE_STRUCTURE | 128/136 | STATIC_VERIFIED |
| ComponentLayoutConverter | CellGeometry — 신규 클래스 | TABLE_LAYOUT | YES | THIS_ROUND | (내부 데이터 클래스) | NONE | — | STATIC_VERIFIED |
| WebSquareGenerator | generate(6-인자) | ROOT_STRUCTURE | YES | HISTORICAL | generate(1/4/5-인자) | ROOT_STRUCTURE | 136/136 | STATIC_VERIFIED |
| WebSquareGenerator | appendBody | ROOT_STRUCTURE | YES | HISTORICAL+THIS_ROUND | generate(6-인자) | ROOT_STRUCTURE | 136/136 | STATIC_VERIFIED |
| WebSquareGenerator | convertChildren | TABLE_LAYOUT/GRID_GROUP_STRUCTURE/PERCENT_GEOMETRY | YES | HISTORICAL+THIS_ROUND | appendBody, convertLayoutAsTable, convertTab, self | 위 3종 전부 | 136/136 | STATIC_VERIFIED |
| WebSquareGenerator | convertLayoutAsTable — 신규 | TABLE_LAYOUT/PERCENT_GEOMETRY | YES | THIS_ROUND | convertChildren | TABLE_STRUCTURE | 128/136 | STATIC_VERIFIED |
| WebSquareGenerator | directElementChildren — 신규 | TABLE_LAYOUT | YES | THIS_ROUND | convertLayoutAsTable | NONE | — | STATIC_VERIFIED |
| WebSquareGenerator | convertTab | PERCENT_GEOMETRY | YES | THIS_ROUND | convertChildren | PERCENT_GEOMETRY | Tab 포함 화면 다수 | STATIC_VERIFIED |
| WebSquareGenerator | copyBasicProperties | PERCENT_GEOMETRY | YES | THIS_ROUND | convertChildren, convertTab | PERCENT_GEOMETRY | 137건(컴포넌트 단위) | STATIC_VERIFIED |
| WebSquareGenerator | resolveVideoEvidenceBaseClass — 신규 | COMPONENT_CONVERSION | YES | HISTORICAL | copyBasicProperties | NO_GENERATED_OUTPUT_IMPACT | 15 | STATIC_VERIFIED |
| WebSquareGenerator | appendClassTokenIfAbsent — 신규 | COMPONENT_CONVERSION | YES | HISTORICAL | copyBasicProperties | NO_GENERATED_OUTPUT_IMPACT | 15 | STATIC_VERIFIED |
| WebSquareGenerator | applyComponentSpecificProperties | COMPONENT_CONVERSION | YES | HISTORICAL | convertChildren, convertTab | NO_GENERATED_OUTPUT_IMPACT | 4 | STATIC_VERIFIED |
| WebSquareGenerator | registerFormRootMapping | SOURCE_TARGET_MAPPING/ROOT_STRUCTURE | YES | THIS_ROUND | appendBody | NONE(mapping table) | 135 | STATIC_VERIFIED |
| XPlatformProjectConverter | writeTabRuntimeResources | LIFECYCLE_COMPATIBILITY | YES | THIS_ROUND | project conversion 메인 흐름 | NONE | 1(placeholder 파일) | STATIC_VERIFIED |
| TabRuntimeScriptGenerator | generate | LIFECYCLE_COMPATIBILITY | YES | THIS_ROUND | WebSquareGenerator.appendScript | NONE(script literal) | Tab runtime 화면 + standalone 1건 | STATIC_VERIFIED |

## 5. Unexpected Scope Audit

`COMPONENT_CLASS_APPLICATION = PAUSED` 원칙 위반 여부 점검: 이번 세션(THIS_ROUND)에서 신규
class mapping 추가 0건, Static QName 변경 0건, unrelated Button/Grid/Combo semantic 변경 0건,
unrelated lifecycle 변경 0건, Historical Defect 작업 0건.

`HISTORICAL` 라벨이 붙은 6개 함수(`ComponentMappingRegistry` class-level, `buildRootStyle`,
`generate(6-인자)`, `resolveVideoEvidenceBaseClass`/`appendClassTokenIfAbsent`,
`applyComponentSpecificProperties`)는 baseline ZIP이 이번 candidate 계보보다 훨씬 이전 시점이라
diff에 나타나는 것일 뿐, 전부 이미 Base Freeze `-02`에 확정되어 있던 정당한 fix이며 이번 세션에서
재수정되지 않았다 — `UNEXPECTED_CHANGE` 후보 아님(class/QName 값 자체를 이번 세션에서 실측
재확인, 6절 참고).

**`UNEXPECTED_FUNCTIONAL_CHANGE_COUNT = 0`**, **`UNEXPECTED_PRODUCTION_CHANGE_COUNT = 0`**.

## 6. 보호 항목 재확인(baseline ZIP 대비, corpus 실측)

| 항목 | baseline ZIP 시점 값 | candidate 값 | 비고 |
|---|---|---|---|
| Static | `w2:span` | `w2:span` | 무변경 |
| Calendar | `w2:calendar`(구) | `w2:inputCalendar`(신) | HISTORICAL fix, 이번 세션 무변경 |
| Button | `xf:trigger` | `xf:trigger` | 무변경 |
| Button class | (baseline엔 없음) | `btn_cm`(12) | HISTORICAL 추가, 이번 세션 무변경 |
| Grid | `w2:gridView` | `w2:gridView` | 무변경 |
| Grid class | (baseline엔 없음) | `wq_gvw`(3) | HISTORICAL 추가, 이번 세션 무변경 |
| Combo disabledClass | (baseline엔 없음) | `w2selectbox_disabled`(4) | HISTORICAL 추가, 이번 세션 무변경 |

## 7. Regression(이번 오디트 라운드)

Production 코드 변경 없음(이번은 순수 audit + comment normalization 라운드) — 8절의 comment
normalization 전/후 비교만 수행, corpus 전체 재변환은 이전 라운드에서 이미 완료된 결과를 그대로
carry-forward.

## 8. 영어 -> 한국어 주석 정리(이 라운드에서 별도 수행)

범위: 이번 baseline-ZIP 비교로 식별된 변경 5개 Production 파일 중, 순수 pre-existing 영어 주석이
남아있는 파일들. 상세 결과는 이 문서가 아니라 comment-normalization 작업 자체의 최종 보고(본
응답의 마지막 절)에서 `COMMENT_NORMALIZATION_FUNCTIONAL_DIFF` 등으로 별도 보고한다(이 문서는
baseline-ZIP 대비 diff 감사 전용이며, comment normalization은 candidate 내부의 before/after
비교이기 때문).

## 9. 최종 판정

`BASELINE_ZIP_DIFF_AUDIT = PASS`
`FUNCTION_LEVEL_DIFF_COVERAGE = PASS`(변경된 모든 hunk가 위 함수 목록 중 하나에 귀속됨 확인)
`CALLER_CALLEE_AUDIT = PASS`(전부 실제 source grep으로 확인, 이름 추측 없음)
`UNEXPECTED_FUNCTIONAL_CHANGE_COUNT = 0`
`UNEXPECTED_PRODUCTION_CHANGE_COUNT = 0`
