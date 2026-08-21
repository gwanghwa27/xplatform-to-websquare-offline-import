# Freeze -> Candidate Function-level Diff (v6-design-structure-alignment)

Base Freeze: `XPWS-OFFLINE-FREEZE-20260820-02`
Candidate: `work/closed-network-support/candidates/v6-design-structure-alignment/working-copy`

이 문서는 **v6 Design Structure + Table + Grid Group + Percentage Geometry Alignment** 라운드
기준으로 전면 갱신되었다(이전 라운드의 `classifyLayoutGeometry`/`buildTableRows`도 이번 라운드에
다시 수정되었으므로, 이전 버전 문서 내용은 이 문서로 대체된다). Raw diff는 항상 Base Freeze `-02`
→ 현재 candidate 기준(`analysis/freeze-vs-candidate-production.diff`, 773줄, 4개 파일).

`PRODUCTION_DIFF_BEFORE_EDIT = 0`(라운드 시작 시 재확인). `EXPECTED_PRODUCTION_DIFF` = 아래
4개 파일(`ComponentLayoutConverter.java`, `WebSquareGenerator.java`,
`XPlatformProjectConverter.java`, `TabRuntimeScriptGenerator.java`). `UNEXPECTED_PRODUCTION_DIFF
= 0`(그 외 72개 파일 `diff -rq` 무변경 확인).

---

## [ComponentLayoutConverter] hasGeometry — 신규 함수

- CHANGE_TYPE: `NEW_FUNCTION`
- 목적: source가 left/top/width/height 등 위치/크기 속성을 하나라도 가지는지 여부를 외부(
  `WebSquareGenerator`)에 노출한다. `includePosition=false`(Table 셀 내부)일 때도 px/percent
  구분 없이 "이 컴포넌트가 애초에 geometry 변환 대상인지"를 판단하는 데 필요.

AFTER:
```java
    public boolean hasGeometry(Element source) {
        return resolveGeometry(source).hasAnyPositionOrSize();
    }
```

Caller: `WebSquareGenerator.copyBasicProperties`, `WebSquareGenerator.convertChildren`(Grid Group
분기). Callee: 기존 `resolveGeometry`(무수정).

---

## [ComponentLayoutConverter] buildComponentStyle(Element, boolean) — 신규 오버로드

- CHANGE_TYPE: `NEW_OVERLOAD`(기존 `buildComponentStyle(Element)`는 무수정으로 이 오버로드에
  `includePosition=true`로 위임)
- 목적: `includePosition=false`면 `position:absolute`/`left`/`top`을 생성하지 않는다 — Table 셀
  내부처럼 structural placement가 이미 위치를 결정하는 경우, percentage 변환이 unresolved라서
  px로 fallback할 때도 20번 규칙(불필요한 좌표 생성 금지)을 동일하게 지키기 위함.

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

Caller: `WebSquareGenerator.copyBasicProperties`(percent unresolved 시 px fallback),
`WebSquareGenerator.convertChildren`(Grid Group wrapper px fallback).

---

## [ComponentLayoutConverter] formatPercent — 신규 함수

- CHANGE_TYPE: `NEW_FUNCTION`
- 목적: percentage 값을 deterministic하게 포맷(소수점 4자리 반올림, trailing zero 제거).
  fixture별 precision을 두지 않고 모든 Production output에 동일 규칙 적용(24번 규칙).

AFTER:
```java
    public String formatPercent(double value) {
        java.math.BigDecimal bd = java.math.BigDecimal.valueOf(value)
                .setScale(4, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros();
        if (bd.scale() < 0) {
            bd = bd.setScale(0);
        }
        return bd.toPlainString() + "%";
    }
```

실측 예: `25.0` → `25%`, `12.5` → `12.5%`, `9.02255639...` → `9.0226%`, `98.51851...` → `98.5185%`
(corpus 실행 로그에서 그대로 관찰됨).

---

## [ComponentLayoutConverter] resolveLayoutBasis — 신규 함수

- CHANGE_TYPE: `NEW_FUNCTION`
- 목적: `PERCENT_GEOMETRY_PARENT = IMMEDIATE_SOURCE_CONTAINER` 원칙의 핵심 구현 — 어떤 XPlatform
  `Layout` 엘리먼트든, **그 Layout 자신의** width/height(px)가 그 Layout 직계 자식 전체의 percent
  기준(basis)이 된다. Form root Layout이든 Div 내부 Layout이든 이 함수 하나로 통일 처리.

AFTER:
```java
    public double[] resolveLayoutBasis(Element layout) {
        Geometry g = resolveGeometry(layout);
        if (isEmpty(g.width) || isEmpty(g.height)) {
            return null;
        }
        ParsedLength w = parseLength(g.width);
        ParsedLength h = parseLength(g.height);
        if (w == null || h == null || w.value <= 0.0 || h.value <= 0.0) {
            return null;
        }
        return new double[] {w.value, h.value};
    }
```

Caller: `WebSquareGenerator.convertLayoutAsTable`(모든 percent 계산의 basis 소스).

---

## [ComponentLayoutConverter] buildPercentComponentStyle — 신규 함수

- CHANGE_TYPE: `NEW_FUNCTION`
- 목적: 일반 component의 left/top/width/height를 basis 기준 percentage style로 변환. basis
  없음/무효, 또는 width/height(및 includePosition인 경우 left/top)를 확정적으로 읽을 수 없으면
  `null`(PERCENT_GEOMETRY_UNRESOLVED — 호출부가 px fallback 결정).

AFTER:
```java
    public String buildPercentComponentStyle(
            Element source, double basisWidth, double basisHeight, boolean includePosition) {
        Geometry geometry = resolveGeometry(source);
        if (!geometry.hasAnyPositionOrSize()) {
            StringBuilder style = new StringBuilder();
            appendVisualStyle(source, style);
            return style.toString();
        }
        if (basisWidth <= 0.0 || basisHeight <= 0.0) {
            return null;
        }

        ParsedLength left = isEmpty(geometry.left) ? null : parseLength(geometry.left);
        ParsedLength top = isEmpty(geometry.top) ? null : parseLength(geometry.top);
        ParsedLength width = isEmpty(geometry.width) ? null : parseLength(geometry.width);
        ParsedLength height = isEmpty(geometry.height) ? null : parseLength(geometry.height);
        if (width == null || height == null) {
            return null;
        }
        if (includePosition && (left == null || top == null)) {
            return null;
        }

        StringBuilder style = new StringBuilder();
        if (includePosition) {
            style.append("position:absolute;");
            style.append("left:").append(formatPercent(left.value / basisWidth * 100.0)).append(";");
            style.append("top:").append(formatPercent(top.value / basisHeight * 100.0)).append(";");
        }
        style.append("width:").append(formatPercent(width.value / basisWidth * 100.0)).append(";");
        style.append("height:").append(formatPercent(height.value / basisHeight * 100.0)).append(";");
        appendVisualStyle(source, style);
        return style.toString();
    }
```

Caller: `WebSquareGenerator.copyBasicProperties`, `convertChildren`(Grid Group wrapper).

Generated XML 실측(BusinessDivLayoutGridProxy, basisWidth=1330, basisHeight=120,
child left=100/top=10/width=120/height=24):
BEFORE(px, 이전 라운드까지): `style="position:absolute;left:100px;top:10px;width:120px;height:24px;"`
AFTER(percent, 이번 라운드): `style="width:9.0226%;height:20%;"`(이 컴포넌트는 Table 셀 내부라
`includePosition=false`이므로 left/top 자체가 없음 — 20번 규칙).

---

## [ComponentLayoutConverter] buildTableRowStyle / buildTableCellStyle — 신규 함수

- CHANGE_TYPE: `NEW_FUNCTION` (2개)
- 목적: Table row/cell wrapper의 percentage 크기를 **source 실비율**로 계산한다(19번 규칙, 균등
  분할 금지). row는 그 row에 속한 셀들의 실제 top~bottom footprint를 basisHeight 대비 비율로,
  cell은 자신의 width를 basisWidth 대비 비율로 계산. 좌표(left/top)는 만들지 않는다(20번 규칙 —
  structural placement로 대체).

AFTER:
```java
    public String buildTableRowStyle(List<Element> row, double basisHeight) {
        if (basisHeight <= 0.0 || row == null || row.isEmpty()) {
            return null;
        }
        double minTop = Double.MAX_VALUE;
        double maxBottom = -Double.MAX_VALUE;
        for (Element cell : row) {
            Geometry g = resolveGeometry(cell);
            ParsedLength top = isEmpty(g.top) ? null : parseLength(g.top);
            ParsedLength height = isEmpty(g.height) ? null : parseLength(g.height);
            if (top == null || height == null) {
                return null;
            }
            minTop = Math.min(minTop, top.value);
            maxBottom = Math.max(maxBottom, top.value + height.value);
        }
        double rowHeight = maxBottom - minTop;
        if (rowHeight <= 0.0) {
            return null;
        }
        return "width:100%;height:" + formatPercent(rowHeight / basisHeight * 100.0) + ";";
    }

    public String buildTableCellStyle(Element cell, double basisWidth) {
        if (basisWidth <= 0.0 || cell == null) {
            return null;
        }
        Geometry g = resolveGeometry(cell);
        ParsedLength width = isEmpty(g.width) ? null : parseLength(g.width);
        if (width == null) {
            return null;
        }
        return "width:" + formatPercent(width.value / basisWidth * 100.0) + ";height:100%;";
    }
```

Generated XML 실측(div_summary, basisWidth=1330, basisHeight=60, row0 children height=24 top=10
→ rowHeight=24, cell width=120):
`<xf:group id="div_summary_layoutTableRow0" style="width:100%;height:40%;">`
`<xf:group id="div_summary_layoutTableRow0Col0" style="width:9.0226%;height:100%;">`
(24/60=40%, 120/1330=9.0226% — 균등분할이 아닌 실비율).

---

## [ComponentLayoutConverter] classifyLayoutGeometry — 기존 함수 수정(판정 기준 완화)

- CHANGE_TYPE: `MODIFIED_FUNCTION`(지난 라운드 신규 함수, 이번 라운드 재수정)
- 목적: 14/15번 규칙 — row/column 수가 2 미만(1-row/1-column)이거나 row마다 column 수가 달라도
  (완전한 사각 grid가 아니어도) 더 이상 fallback 사유로 쓰지 않는다. fallback은 오직 geometry
  확정 불가(`UNRESOLVED_LAYOUT`)와 실제 겹침(`ABSOLUTE_LAYOUT_FALLBACK`)뿐이다.

BEFORE(이전 라운드):
```java
    public String classifyLayoutGeometry(List<Element> children) {
        List<CellGeometry> cells = resolveCellGeometries(children);
        if (cells == null) {
            return "UNRESOLVED_LAYOUT";
        }
        Map<Double, List<CellGeometry>> byTop = groupByTop(cells);
        if (byTop.size() < 2) {
            return "ABSOLUTE_LAYOUT_FALLBACK";
        }
        TreeSet<Double> lefts = new TreeSet<Double>();
        for (CellGeometry c : cells) { lefts.add(c.left); }
        if (lefts.size() < 2) {
            return "ABSOLUTE_LAYOUT_FALLBACK";
        }
        boolean rectangular = true;
        for (List<CellGeometry> row : byTop.values()) {
            if (row.size() != lefts.size()) { rectangular = false; break; }
            TreeSet<Double> rowLefts = new TreeSet<Double>();
            for (CellGeometry c : row) { rowLefts.add(c.left); }
            if (!rowLefts.equals(lefts)) { rectangular = false; break; }
        }
        if (rectangular && !hasOverlap(cells)) {
            return "TABLE_LAYOUT_HIGH_CONFIDENCE";
        }
        return "TABLE_LAYOUT_HEURISTIC";
    }
```

AFTER(이번 라운드):
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

영향: `TABLE_LAYOUT_HEURISTIC` 반환값 자체가 없어짐(더 이상 필요 없음 — 비사각형도
HIGH_CONFIDENCE). corpus 실측: 이전 라운드 `HIGH_CONFIDENCE=0/135`, 이번 라운드
`HIGH_CONFIDENCE=5`(실제 corpus, root Layout 제외 — 아래 표 참고).

---

## [ComponentLayoutConverter] resolveCellGeometries — 기존 함수 수정(child 수 제한 제거)

- CHANGE_TYPE: `MODIFIED_FUNCTION`
- 목적: 14번 규칙 — child 수 자체를 fallback 사유로 쓰지 않는다.

BEFORE: `if (children == null || children.size() < 4) { return null; }`
AFTER: `if (children == null || children.isEmpty()) { return null; }`

---

## [WebSquareGenerator] appendBody — 기존 함수 수정(global grp_content 제거)

- CHANGE_TYPE: `MODIFIED_FUNCTION`
- 목적: `GLOBAL_GRP_CONTENT_XFDL_COUNT = 0` — 이전까지 모든 content를 감싸던
  `xf:group id="grp_content"` wrapper를 제거하고, 변환된 구조가 `grp_main` 바로 아래 나타나도록
  한다.

BEFORE:
```java
        Element main = out.createElementNS(NS_XF, "xf:group");
        main.setAttribute("id", "grp_main");
        main.setAttribute("style", layoutConverter.buildMainAreaStyle(source));
        resultArea.appendChild(main);

        Element root = out.createElementNS(NS_XF, "xf:group");
        root.setAttribute("id", "grp_content");
        root.setAttribute("style", layoutConverter.buildRootStyle(source));
        main.appendChild(root);
        registerFormRootMapping(source);

        Element sourceRoot = source.getDocumentElement();
        convertChildren(out, sourceRoot, root, "", analysis, 0);
```

AFTER:
```java
        Element main = out.createElementNS(NS_XF, "xf:group");
        main.setAttribute("id", "grp_main");
        main.setAttribute("style", layoutConverter.buildMainAreaStyle(source));
        resultArea.appendChild(main);
        registerFormRootMapping(source);

        Element sourceRoot = source.getDocumentElement();
        convertChildren(out, sourceRoot, main, "", analysis, 0, null, -1.0, -1.0, true);
```

Generated XML BEFORE(corpus 어느 화면이든 공통 골격, 이전 라운드까지):
```xml
<xf:group id="grp_resultArea" style="">
  <xf:group id="grp_main" style="height:800px;">
    <xf:group id="grp_content" style="width:1350px;height:800px;">
      <!-- flat px 절대좌표 components -->
    </xf:group>
  </xf:group>
</xf:group>
```

Generated XML AFTER(이번 라운드, BusinessDivLayoutGridProxy 실측):
```xml
<xf:group id="grp_resultArea" style="">
  <xf:group id="grp_main" style="height:800px;">
    <w2:group id="div_search" style="position:absolute;left:0.7407%;top:1.25%;width:98.5185%;height:15%;">...</w2:group>
    <w2:group id="div_summary" style="position:absolute;left:0.7407%;top:17.5%;width:98.5185%;height:7.5%;">...</w2:group>
    <xf:group id="grd_list_gridGroup" style="position:absolute;left:0.7407%;top:26.25%;width:98.5185%;height:62.5%;">
      <w2:gridView class="wq_gvw" id="grd_list" style="width:100%;height:100%;"/>
    </xf:group>
  </xf:group>
</xf:group>
```

영향 output 수: 136/136(전체 corpus, `grp_content` 잔존 0건 실측 확인).

---

## [WebSquareGenerator] convertChildren — 기존 함수 수정(basis 파라미터 + Grid Group)

- CHANGE_TYPE: `MODIFIED_FUNCTION`(7-인자 → 10-인자, 이전 라운드부터 존재하던 onlyChild 필터는
  그대로 재사용)
- 목적: (1) `basisWidth`/`basisHeight`/`includePosition`을 재귀 전체에 threading해 어디서든
  percent 계산이 가능하게 함. (2) XPlatform `Grid`(container 아님)를 자체적으로 `xf:group`
  wrapper로 감싼다(`GRID_GROUP_STRUCTURE`, 21번 규칙) — Grid 자신의 id/class/binding은 무변경,
  wrapper는 synthetic id(componentIdMap 미등록, usedTargetIds 충돌 방지만).

BEFORE/AFTER 전체 Unified Diff: `analysis/freeze-vs-candidate-production.diff` L371-507 참고
(요약 아님 — 실제 173줄 diff, 이 문서에는 신규 Grid Group 분기만 발췌).

AFTER(신규 Grid Group 분기, 기존 `targetParent.appendChild(target);` 한 줄을 대체):
```java
                if ("w2:gridView".equals(targetTag)) {
                    Element gridWrapper = out.createElementNS(NS_XF, "xf:group");
                    String wrapperId = createUniqueTargetId(buildSourcePath(sourcePath, "gridGroup"));
                    gridWrapper.setAttribute("id", wrapperId);
                    String wrapperStyle = layoutConverter.hasGeometry(src)
                            ? ((basisWidth > 0.0 && basisHeight > 0.0)
                                    ? layoutConverter.buildPercentComponentStyle(src, basisWidth, basisHeight, true)
                                    : null)
                            : "";
                    if (wrapperStyle == null) {
                        wrapperStyle = layoutConverter.buildComponentStyle(src, true);
                    }
                    gridWrapper.setAttribute("style", sanitizeXml10(wrapperStyle));
                    target.setAttribute("style", "width:100%;height:100%;");
                    gridWrapper.appendChild(target);
                    targetParent.appendChild(gridWrapper);
                } else {
                    targetParent.appendChild(target);
                }
```

Generated XML BEFORE(이전 라운드까지, Grid는 wrapper 없이 직접 배치):
```xml
<w2:gridView class="wq_gvw" id="grd" style="position:absolute;left:10px;top:210px;width:1330px;height:500px;"/>
```

Generated XML AFTER(이번 라운드, corpus 실측 `grd`):
```xml
<xf:group id="grd_gridGroup" style="...">
  <w2:gridView class="wq_gvw" id="grd" style="width:100%;height:100%;"/>
</xf:group>
```

영향 output 수: corpus 실측 `[UI GRID GROUP]` 3건(`grd_gridGroup`, `grdMain_gridGroup`,
`grd_gridGroup` — 서로 다른 화면), `wq_gvw`/`w2:gridView` class/QName/id/binding 전부 무변경 확인.

---

## [WebSquareGenerator] convertLayoutAsTable — 기존 함수 수정(basis 계산 + root Layout 예외)

- CHANGE_TYPE: `MODIFIED_FUNCTION`
- 목적: (1) 이 Layout 자신의 width/height로 basis를 계산해 하위 전체(테이블/fallback 양쪽)에
  전달. (2) 12번 규칙 — `parentPath`가 비어 있으면(아직 어떤 Div도 거치지 않은 Form/Tabpage
  최상위 Layout) classification과 무관하게 강제로 flat pass-through
  (`ROOT_FORM_LAYOUT_NOT_A_TABLE_TARGET`) — 목표 hierarchy(`grp_main` 바로 아래 Div
  Group/Grid Group)가 불필요한 wrapper 계층 없이 나타나도록 함. (3) row/cell wrapper에 percent
  style 부여, cell 내부 실제 component는 `includePosition=false`로 재귀.

BEFORE(이전 라운드):
```java
    private void convertLayoutAsTable(
            Document out, Element layout, Element targetParent, String parentPath,
            XfdlAnalysisResult analysis, int depth) {
        List<Element> children = directElementChildren(layout);
        String classification = layoutConverter.classifyLayoutGeometry(children);
        if (!"TABLE_LAYOUT_HIGH_CONFIDENCE".equals(classification)) {
            convertChildren(out, layout, targetParent, parentPath, analysis, depth);
            return;
        }
        List<List<Element>> rows = layoutConverter.buildTableRows(children);
        int rowIndex = 0;
        for (List<Element> row : rows) {
            Element rowGroup = out.createElementNS(NS_XF, "xf:group");
            String rowTargetId = createUniqueTargetId(buildSourcePath(parentPath, "layoutTableRow" + rowIndex));
            rowGroup.setAttribute("id", rowTargetId);
            targetParent.appendChild(rowGroup);
            int colIndex = 0;
            for (Element cell : row) {
                Element cellGroup = out.createElementNS(NS_XF, "xf:group");
                String cellTargetId = createUniqueTargetId(buildSourcePath(parentPath, "layoutTableRow" + rowIndex + "Col" + colIndex));
                cellGroup.setAttribute("id", cellTargetId);
                rowGroup.appendChild(cellGroup);
                convertChildren(out, layout, cellGroup, parentPath, analysis, depth, cell);
                colIndex++;
            }
            rowIndex++;
        }
    }
```

AFTER: 전체 코드는 `src/main/java/com/example/xfdltracker/converter/WebSquareGenerator.java`의
동명 메서드 참고(basis 계산 2줄, `isRootFormLayout` guard 4줄, row/cell style 각 6줄 추가). 핵심
변경 라인:
```java
        boolean isRootFormLayout = parentPath.length() == 0;
        String classification = isRootFormLayout
                ? "ROOT_FORM_LAYOUT_NOT_A_TABLE_TARGET"
                : layoutConverter.classifyLayoutGeometry(children);
        double[] basis = layoutConverter.resolveLayoutBasis(layout);
        double basisWidth = basis == null ? -1.0 : basis[0];
        double basisHeight = basis == null ? -1.0 : basis[1];
```

Generated XML BEFORE/AFTER: 위 `appendBody` 항목의 예시가 그대로 이 함수의 실제 산출물이다(root
Layout은 table화되지 않고, `div_search`/`div_summary` 내부 Layout만 table화됨).

영향 output 수: real corpus `Layout -> table` 5건(아래 표), root Layout 강제 fallback 121건.

---

## [WebSquareGenerator] convertTab / copyBasicProperties / registerFormRootMapping — 파라미터/리터럴 수정

- CHANGE_TYPE: `MODIFIED_FUNCTION`(3개)
- `convertTab`: `basisWidth`/`basisHeight`/`includePosition` 파라미터 추가, Tabpage content
  진입 시 독립 scope로 취급해 basis를 fresh(`-1.0, -1.0, true`)하게 리셋(자신의 Layout을 만나면
  다시 계산됨).
- `copyBasicProperties`: 기존 2-인자 오버로드는 유지(내부적으로 basis 없음으로 위임), 신규
  5-인자 오버로드가 percent 우선/px fallback 로직을 수행.
- `registerFormRootMapping`: `componentIdMap.put(formId, "grp_content")` →
  `componentIdMap.put(formId, "grp_main")`(`EXPECTED_SOURCE_TO_TARGET_MAP_DIFF`, 유일한 예상
  변화).

전체 코드/diff: `analysis/freeze-vs-candidate-production.diff` L630-737 참고.

---

## [XPlatformProjectConverter] Tab runtime placeholder 생성부 — 리터럴 수정

- CHANGE_TYPE: `MODIFIED_FUNCTION`(함수명 무변경, 문자열 리터럴만)
- BEFORE: `"  <body><w2:group id=\"grp_content\" style=\"...\"/></body>\n"`
- AFTER: `"  <body><w2:group id=\"grp_main\" style=\"...\"/></body>\n"`
- 목적: Tab runtime의 빈 placeholder 페이지(`runtime/xplatform-tab-empty.xml`)도 실제
  WebSquareGenerator가 더 이상 만들지 않는 `grp_content` 대신 `grp_main`을 참조하도록 일치.
- 영향 output: `runtime/xplatform-tab-empty.xml`(corpus 전체 conversion에서 1회 생성) 실측 확인.

---

## [TabRuntimeScriptGenerator] currentFrame / parentWindow — 리터럴 수정

- CHANGE_TYPE: `MODIFIED_FUNCTION`(함수명/로직 무변경, 문자열 리터럴만)
- BEFORE: `component('grp_content')`, `w.grp_content`(2곳)
- AFTER: `component('grp_main')`, `w.grp_main`(2곳)
- 목적: `WebSquareGenerator.registerFormRootMapping`이 `grp_main`으로 Form root를 등록하므로,
  동일 id-string 기반 lookup(`V5_RUNTIME_REGRESSION_REQUIRED`는 이 라운드에서 새로 발생/해소되지
  않음, 기존 gap 그대로) 대상 literal도 함께 변경.
- 영향 output: `runtime/xplatform-tab-runtime.js`(standalone 참조본) 및 Tab runtime이 필요한
  각 화면의 인라인 `<script>` — corpus 실측 `xplatform-tab-runtime.js` 내 `grp_main` 4회,
  `grp_content` 0회.

---

## Regression 결과 요약(상세는 최종 보고 참고)

| 항목 | 결과 |
|---|---|
| clean compile | PASS(0 errors, 76 source files) |
| project conversion | 149/149 성공, 0 실패 |
| XML parse | 136/136 well-formed |
| standalone JS | 15/15(무변경) |
| Phase1 SHA | 2/2 PASS(정적 reference fixture, 이번 변경과 무관 확인) |
| `SOURCE_TO_TARGET_ID_MAP_EXPECTED_ONLY` | PASS(403/403 key, 135건 `grp_content->grp_main`만 변경, 그 외 0건) |
| invariant class/QName | `btn_cm=12`, `wq_gvw=3`, disabledClass=4, Calendar=1, `xf:trigger=12`, `w2:gridView=3`, `w2:span=9` 전부 Base와 동일 |
| `INVALID_PERCENT_STYLE_COUNT` | 0(NaN%/Infinity%/음수% 없음) |

## 후속 라운드 — 실제 Studio 실패 기반 Percentage Geometry Root Cause Fix

Baseline: `GIT-BASELINE-XPWS-OFFLINE-FREEZE-20260820-02`(commit `549a998`). 상세 raw diff:
`analysis/git-baseline-vs-candidate-production.diff`(910줄, 이 라운드 이전 전체 누적 변경 포함).

### Evidence

사용자가 실제 폐쇄망 WebSquare Studio에서 변환 결과를 확인한 결과
(`USER_CONFIRMED_CLOSED_NETWORK_STUDIO`): Design/Preview 양쪽 모두 업무 화면 전체가 좌측 상단의
좁은 영역으로 압축되어 표시됨(`STUDIO_DESIGN_FAILED`, `STUDIO_DESIGN_REPRODUCED` — Design
전용 버그가 아니라 실제 generated geometry 문제로 재확인). 별첨 영상(현재 변환된 Design Source
화면 녹화)은 `.mp4` 바이너리이며 이 환경에 설치된 도구(ImageMagick, ffmpeg 부재)로 프레임 추출이
불가능해 **판독하지 못했다** — 24번 규칙("흐린 값 추측 금지")에 따라 영상 내용은 이번 root
cause 판단에 사용하지 않았고, 대신 로컬에서 직접 재현/재생성한 generated XML로 cross-check했다
(정상적인 해석 방법, 24번 규칙이 요구하는 local output 재확인과 일치). 비교 참고용 정상 화면
스크린샷(이미지 3, `BCI01M0000`)은 시각적 convention 참고로만 사용했고 class/style을 그대로
복제하지 않았다(`NO_PAIRED_LEGACY_TO_V6_REFERENCE_AVAILABLE_BY_PROJECT_NATURE`).

### Root cause

사용자가 제공한 예시(`style="position:absolute;left:0px;top:...px;width:1145px;height:...px;"`)
는 Div Group/Table Row/Cell/일반 component/Grid Group 전 범주에서 percentage 변환이 전혀
적용되지 않고 원본 px가 그대로 남아있음을 보여준다 — 이는 개별 계산식 오류가 아니라 **basis
자체가 전혀 확보되지 않았음**을 시사했다.

기존 코드(`[WebSquareGenerator] convertLayoutAsTable`)는 basis를 오직 현재 순회 중인 XPlatform
`Layout` 엘리먼트 **자신의** width/height 속성에서만 얻었고, 최초 진입 시(`appendBody`)에는
basis를 항상 `-1.0`(unresolved)로 고정했다. 실제 corpus를 재조사한 결과, 다음 두 가지 실존
패턴이 이 가정을 깬다:

1. 일부 XPlatform 화면은 component가 `Layouts`/`Layout` wrapper 없이 **`Form` 바로 아래**
   존재한다(예: corpus `sample-phase3-project/Form/ComponentMethodConversion.xfdl`처럼 `Combo`/
   `Grid`가 `Form`의 직계 자식) — 이 경우 `"Layout"` 태그를 절대 만나지 못하므로 basis가
   전체 화면에서 영원히 `-1`로 남는다.
2. `Layout` 태그가 존재하더라도 그 자신에게 width/height가 없는 경우(실제 업무 화면에서 확인,
   corpus 자체 예시는 이번 조사로 재구성함) — 첫 Layout 진입 시점에 basis 확보가 실패해 그
   이하 전체가 unresolved로 떨어진다.

두 경우 모두 `[WebSquareGenerator] copyBasicProperties`가 `basisWidth<=0`이므로 percent를
시도조차 하지 않고 무조건 기존 px(`buildComponentStyle`)로 fallback한다 — 이 자체는 안전한
fallback이지만, 이전 라운드까지 존재하던 `grp_content`(폭을 px로 고정해주던 wrapper)가 이번
percent-geometry 라운드에서 제거됐기 때문에, 그 px 절대좌표가 실제 폭이 정의되지 않은 컨테이너
체인 위에서 렌더링되며 화면이 좁게 collapse하는 것으로 판단된다
(`SOURCE_PIXEL_GEOMETRY_REMAINS_IN_GENERATED_STRUCTURE` 확정).

corpus 재실측: 이전 라운드 기준 `PIXEL_GEOMETRY_FALLBACK_COUNT=13`이었던 항목(`grd`, `cbo`,
`btn` 등, 로그상 전부 `basisWidth=-1.0`)이 정확히 이 두 패턴에 해당함을 확인했다(root cause와
실측 fallback 목록이 일치).

### [ComponentLayoutConverter] resolveFormBasis — 신규 함수

- 변경 분류: `PERCENT_GEOMETRY`
- 목적: 기존 `findFormGeometry`(Form 우선, 없으면 첫 `Layout` 차선 — `buildMainAreaStyle`이
  이미 재사용 중인 private helper)를 재사용해 Form 전체를 초기/최후 basis로 제공한다. 신규
  Production class 없음, 기존 helper 재연결(21번 규칙 준수).
- Caller: `[WebSquareGenerator] appendBody`(초기 basis), `[WebSquareGenerator]
  convertLayoutAsTable`(Layout 자신에 geometry 없을 때 fallback).
- Callee: 기존 `findFormGeometry`(무수정), `parseLength`(무수정).

BEFORE: 없음(신규).
AFTER:
```java
public double[] resolveFormBasis(Document source) {
    Geometry g = findFormGeometry(source);
    if (g == null || isEmpty(g.width) || isEmpty(g.height)) {
        return null;
    }
    ParsedLength w = parseLength(g.width);
    ParsedLength h = parseLength(g.height);
    if (w == null || h == null || w.value <= 0.0 || h.value <= 0.0) {
        return null;
    }
    return new double[] {w.value, h.value};
}
```

### [WebSquareGenerator] appendBody — 초기 basis를 Form 기준으로 수정

BEFORE:
```java
        Element sourceRoot = source.getDocumentElement();
        convertChildren(
                out, sourceRoot, main, "", analysis, 0, null,
                -1.0,
                -1.0,
                true);
```
AFTER:
```java
        double[] formBasis = layoutConverter.resolveFormBasis(source);
        double initialBasisWidth = formBasis == null ? -1.0 : formBasis[0];
        double initialBasisHeight = formBasis == null ? -1.0 : formBasis[1];

        Element sourceRoot = source.getDocumentElement();
        convertChildren(
                out, sourceRoot, main, "", analysis, 0, null,
                initialBasisWidth,
                initialBasisHeight,
                true);
```
Generated XML BEFORE(`Form` 직계 자식 component, corpus 실측, 이전 라운드):
`<w2:gridView class="wq_gvw" id="grd" style="position:absolute;left:10px;top:60px;width:300px;height:120px;"/>`
Generated XML AFTER(동일 컴포넌트, 이번 수정, corpus 실측 basisWidth=600.0/basisHeight=400.0
— Form 자신의 선언값):
`style="position:absolute;left:1.6667%;top:16.6667%;width:50%;height:40%;"`(Grid Group wrapper
경유, 아래 참고).
영향 output 수: corpus 실측 fallback 13건 전부 해소(`PIXEL_GEOMETRY_FALLBACK_COUNT: 13 -> 0`).

### [WebSquareGenerator] convertLayoutAsTable — Layout 자신에 geometry 없을 때 Form fallback 추가

BEFORE:
```java
        double[] basis = layoutConverter.resolveLayoutBasis(layout);
        double basisWidth = basis == null ? -1.0 : basis[0];
        double basisHeight = basis == null ? -1.0 : basis[1];
```
AFTER:
```java
        double[] basis = layoutConverter.resolveLayoutBasis(layout);
        if (basis == null) {
            basis = layoutConverter.resolveFormBasis(layout.getOwnerDocument());
        }
        double basisWidth = basis == null ? -1.0 : basis[0];
        double basisHeight = basis == null ? -1.0 : basis[1];
```
Caller/Callee: 무변경(기존과 동일 — `convertChildren` pass-through 분기가 유일한 caller).
Generated XML impact: `PERCENT_GEOMETRY`. 영향 output 수: real corpus 13건(위와 동일 모수,
Layout 미존재/geometry 없음 두 패턴 합산).

### Structural proxy 재검증(SYNTHETIC_STRUCTURAL_PROXY_VERIFIED)

기존 `BusinessDivLayoutGridProxy` 재실행(무변경, 여전히 percent 정상). 신규 합성 fixture 2건으로
이번 두 패턴을 직접 재현/검증:

- `NoLayoutWrapperProxy.xfdl`(Div/Grid가 `Layouts`/`Layout` 없이 `Form` 직계 자식) — 수정 전
  가정상 basis 영원히 `-1`이었을 케이스, 수정 후 실측 `div_search style="...left:0.7407%;
  top:1.25%;width:98.5185%;height:15%;"`, `grd_list_gridGroup`도 동일 패턴으로 percent 정상
  적용, Table 구조(1-row) 유지 확인.
- `LayoutMissingSizeProxy.xfdl`(`<Layouts><Layout>` 존재하지만 `Layout` 자신에 width/height
  없음, `Form`에만 존재) — 동일하게 Form fallback으로 정상 percent 적용 확인.

두 fixture 모두 corpus/Production count에 미포함, `SYNTHETIC_STRUCTURAL_PROXY_VERIFIED`까지만
— 사용자의 실제 폐쇄망 Studio 재확인을 대신하지 않는다.

### Regression(이번 라운드)

| 항목 | 결과 |
|---|---|
| clean compile | PASS(0 errors) |
| project conversion | 149/149 성공, 0 실패 |
| XML parse | 136/136 well-formed |
| standalone JS | 15/15(무변경) |
| Phase1 SHA | 2/2 PASS |
| `SOURCE_TO_TARGET_ID_MAP_EXPECTED_ONLY` | PASS(403/403 key, 135건 `grp_content->grp_main`만, 그 외 0건 — 무변경 재확인, id 생성 로직 자체는 이번 라운드에 안 건드림) |
| invariant class/QName | `btn_cm=12`, `wq_gvw=3`, disabledClass=4, Calendar=1 전부 무변경 |
| `UI PERCENT 적용` | 137건(이전 124건 -> 137건) |
| `UI PERCENT UNRESOLVED` | **0건(이전 13건 -> 0건)** |
| `INVALID_PERCENT_STYLE_COUNT` | 0(NaN%/Infinity%/음수% 없음) |

### Completion Gate

`STRUCTURE_TOPOLOGY_PRESERVED = PASS`(Div/Table/Grid Group 계층 구조 자체는 이번 라운드에서
전혀 건드리지 않음 — basis 계산 로직만 수정). `ROOT_WRAPPER_GEOMETRY_UNCHANGED = PASS`
(`grp_resultArea style=""`, `grp_main style="height:Npx;"` 무변경 실측). `PERCENT_GEOMETRY_
CONVERSION = PASS`, `PERCENT_GEOMETRY_PARENT_SEMANTIC = PASS`(basis는 여전히 "가장 가까운
XPlatform Layout 자신의 width/height" 우선, 없을 때만 Form 전체로 fallback — 원칙 자체는
불변). `DIV_PERCENT_GEOMETRY = PASS`, `TABLE_ROW_COLUMN_PERCENT_GEOMETRY = PASS`,
`GRID_GROUP_PERCENT_GEOMETRY = PASS`. `COMPONENT_QNAME_PRESERVED = PASS`,
`EXISTING_CLASS_PRESERVED = PASS`(`btn_cm`/`wq_gvw`/disabledClass/Calendar 전부 무변경).
`BODY_LIFECYCLE_ATTRIBUTES_PRESERVED = PASS`(무변경, 이번 라운드 미접촉 영역).

`ABSOLUTE_PX_GEOMETRY_REMAINING_COUNT = 0`, `PERCENT_GEOMETRY_UNRESOLVED_COUNT = 0`,
`PIXEL_GEOMETRY_FALLBACK_COUNT = 0`, `INVALID_PERCENT_STYLE_COUNT = 0`, `NaN% = 0`,
`Infinity% = 0`.

## Status

모든 변경 함수: `STATIC_VERIFIED`(compile/conversion/canonical map/invariant 실측 완료).
`STUDIO_DESIGN_VERIFIED`는 선언하지 않음 — 사용자의 실제 폐쇄망 Studio 확인 필요
(`STUDIO_DESIGN_REQUIRED`). 최종 `PERCENT_GEOMETRY = FIX_CANDIDATE`.
