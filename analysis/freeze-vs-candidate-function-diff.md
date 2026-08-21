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

---

## 후속 라운드 — Root Percentage Containing Block Width Fix

### 배경/증거

사용자가 실제 폐쇄망 Studio 스크린샷 5장을 제공: (1) 원본 XPlatform Design(전체 폭 ~1200px+
정상 레이아웃), (2)(3) 변환된 WebSquare Design/Preview 둘 다 좌측 좁은 영역에 압축, (4)(5) 실제
생성된 WebSquare Source XML에서 `grp_resultArea style=""`, `grp_main style="height:760px;"`이고
그 자식들은 이미 `left:...%;top:...%;width:98...%;height:...%;` 형태로 percentage가 정확히
계산되어 있음을 확인. 즉 percentage 산술 자체는 맞는데, 그 percentage가 기준으로 삼는 containing
block(`grp_resultArea`/`grp_main`)에 명시적 `width`가 없어 CSS 상 containing block width가
사실상 0에 가까운 값(또는 브라우저/WebSquare 렌더러의 fallback 값)이 되어 화면이 압축된 것으로
진단(`ROOT_PERCENT_CONTAINING_BLOCK_WIDTH_DEFECT`).

사용자는 이전 라운드에서 세운 "`grp_resultArea`/`grp_main`은 width를 갖지 않는다"는 불변식이
`grp_content`(명시적 px width/height를 가진 호환 wrapper)가 아직 존재하던 구조를 관찰해 세운
것이며, `grp_content` 제거 이후에는 재검토 대상이라고 명시적으로 지적. 이번 라운드는 그 지적에
따라 root containing block에만 `width:100%`를 추가하고, 자식 percentage 계산 로직은 전혀
건드리지 않는다.

### 변경 함수

**[ComponentLayoutConverter] buildMainAreaStyle(Document source)**

BEFORE:
```java
public String buildMainAreaStyle(Document source) {
    Geometry geometry = findFormGeometry(source);
    if (geometry == null || isEmpty(geometry.height)) {
        return "";
    }
    ParsedLength parsed = parseLength(geometry.height);
    if (parsed == null || parsed.value <= 0.0) {
        return "";
    }
    StringBuilder style = new StringBuilder();
    appendCssLength(style, "height", geometry.height);
    return style.toString();
}
```

AFTER:
```java
public String buildMainAreaStyle(Document source) {
    StringBuilder style = new StringBuilder();
    style.append("width:100%;");

    Geometry geometry = findFormGeometry(source);
    if (geometry == null || isEmpty(geometry.height)) {
        return style.toString();
    }
    ParsedLength parsed = parseLength(geometry.height);
    if (parsed == null || parsed.value <= 0.0) {
        return style.toString();
    }

    appendCssLength(style, "height", geometry.height);
    return style.toString();
}
```

Caller: `WebSquareGenerator.appendBody` — `main.setAttribute("style", layoutConverter.buildMainAreaStyle(source));`
(호출부 자체는 무변경, 반환값만 항상 `width:100%;` 접두를 포함하도록 바뀜).

**[WebSquareGenerator] appendBody(...)** — `grp_resultArea` style 리터럴

BEFORE: `resultArea.setAttribute("style", "");`
AFTER: `resultArea.setAttribute("style", "width:100%;");`

### Full Unified Diff

전체 unified diff: [analysis/git-baseline-vs-candidate-production.diff](git-baseline-vs-candidate-production.diff)
(GIT-BASELINE-XPWS-OFFLINE-FREEZE-20260820-02, commit `549a998` 대비 누적. 이번 라운드분은 그
diff 파일의 마지막 두 hunk — `buildMainAreaStyle`, `grp_resultArea` style 리터럴).

### Generated XML BEFORE/AFTER (실제 corpus 값, `Form/ComponentMethodConversion.xml`)

BEFORE (라운드5 이전 출력, `corpus-output-round4`):
```xml
<xf:group id="grp_resultArea" style="">
    <xf:group id="grp_main" style="height:300px;">
        <xf:select1 ... id="cbo" style="position:absolute;left:1.6667%;top:3.3333%;width:16.6667%;height:8%;"/>
```

AFTER (이번 라운드 출력, `corpus-output-round5`):
```xml
<xf:group id="grp_resultArea" style="width:100%;">
    <xf:group id="grp_main" style="width:100%;height:300px;">
        <xf:select1 ... id="cbo" style="position:absolute;left:1.6667%;top:3.3333%;width:16.6667%;height:8%;"/>
        <xf:group id="grd_gridGroup" style="position:absolute;left:1.6667%;top:16.6667%;width:50%;height:40%;">
```

`cbo`/`grd_gridGroup`의 percentage 값은 BEFORE=AFTER 완전 동일 — 이번 라운드가 root wrapper의
`width`만 추가하고 자식 percentage 계산에는 손대지 않았음을 실측으로 확인.

### 영향 범위

전체 corpus 149개 화면 변환(성공 149/149), XML 출력 136개 중:
- `grp_resultArea style="width:100%;"` : 135/136 (제외된 1건은 변환 화면이 아닌 사전 존재
  placeholder `runtime/xplatform-tab-empty.xml` — 이번 라운드 미접촉, grep으로 확인)
- `grp_main` style에 `width:100%` 포함 : 135/136(동일 예외)
- `grp_content` 잔존 : 0건
- 자식 percentage geometry 값 변경 : 0건(전수 diff 확인)

### 대표 3건 percentage 산술 검증

**1) 단상위(Form-direct-child) 컴포넌트 — `cbo`, `Form/ComponentMethodConversion.xfdl`**

소스: `<Form ... width="600" height="300">` (Layout 없이 Form 직계 자식, basis=Form 전체),
`<Combo id="cbo" left="10" top="10" width="100" height="24"/>`

계산: left=10/600=1.6667%, top=10/300=3.3333%, width=100/600=16.6667%, height=24/300=8%
생성 결과: `left:1.6667%;top:3.3333%;width:16.6667%;height:8%;` — 일치.

**2) Grid Group — `grd`/`grd_gridGroup`, `Form/ComponentMethodConversion.xfdl`**

소스: 동일 Form(basis 600x300), `<Grid id="grd" ... left="10" top="50" width="300" height="120">`

계산: left=10/600=1.6667%, top=50/300=16.6667%, width=300/600=50%, height=120/300=40%
생성 결과: `grd`와 `grd_gridGroup` 둘 다 `left:1.6667%;top:16.6667%;width:50%;height:40%;` — 일치
(Grid Group wrapper와 내부 `w2:gridView`가 같은 basis를 공유하는 기존 동작도 무변경 확인).

**3) Table(TABLE_LAYOUT_HIGH_CONFIDENCE) — `divWrap_layoutTableRow0Col0`, `Form/Main/TabExternalRelativePath.xfdl`**

소스: `<Div id="divWrap" left="0" top="0" width="580" height="380"><Layouts><Layout width="580" height="380">`
(basis=580x380), `<Tab id="tabNested" left="10" top="10" width="550" height="340">`

계산: row height=340/380=89.4737%, col width=550/580=94.8276%
생성 결과: `divWrap_layoutTableRow0` style=`width:100%;height:89.4737%;`,
`divWrap_layoutTableRow0Col0` style=`width:94.8276%;height:100%;` — 일치
(Table row는 항상 width:100%, 자기 자신의 height%만 계산하는 기존 규칙도 무변경 확인).

### 회귀 결과

| 항목 | 결과 |
|---|---|
| 컴파일 | 0 errors |
| 전체 corpus 변환 | 149/149 성공 |
| XML parse | 136/136 well-formed |
| standalone JS | 15/15(무변경) |
| Phase1 SHA | 2/2 PASS(Sample, CommentProtection — `<script>` CDATA만 hash하므로 root/body style 변경과 무관, 무변경 재확인) |
| `SOURCE_TO_TARGET_ID_MAP_EXPECTED_ONLY` | PASS(403/403 key, `<` 135건=`grp_content` 전용, `>` 0건 unexpected, id 생성 로직 이번 라운드 미접촉) |
| invariant class/QName | `btn_cm=12`, `wq_gvw=3` 전부 무변경 |
| `grp_resultArea width:100%` | 135/136(placeholder 1건 제외) |
| `grp_main width:100%` | 135/136(동일 예외) |
| `grp_content` 잔존 | 0건 |
| `position:relative`(root wrapper) | 0건(전체 1 match는 무관 placeholder 파일) |
| `overflow:hidden`(root wrapper) | 0건 |
| `INVALID_PERCENT_STYLE_COUNT` | 0(NaN%/Infinity%/음수% 없음) |
| 하드코딩 px width | 0건(root wrapper는 전부 `width:100%` — 구조 상수, source 계산값 아님) |

### Completion Gate

`ROOT_PERCENT_CONTAINING_BLOCK_AUDIT = PASS`(root cause 확인: `grp_resultArea`/`grp_main`
containing block에 명시적 width 부재). `ROOT_PERCENT_WIDTH_CHAIN = PASS`(body -> grp_resultArea
(`width:100%`) -> grp_main(`width:100%`) -> child(`%`) 체인 전부 명시적 width 보유, 실측
135/136). `STRUCTURE_TOPOLOGY_PRESERVED = PASS`(Div/Table/Grid Group 계층 자체 무변경).
`PERCENT_ARITHMETIC = PASS`(대표 3건 역산 일치). `DIV_PERCENT_GEOMETRY = PASS`,
`TABLE_PERCENT_GEOMETRY = PASS`, `GRID_GROUP_PERCENT_GEOMETRY = PASS`(전부 자식 값 무변경
실측). `COMPONENT_QNAME_PRESERVED = PASS`, `EXISTING_CLASS_PRESERVED = PASS`,
`BODY_LIFECYCLE_ATTRIBUTES_PRESERVED = PASS`(이번 라운드 미접촉 영역). `INVALID_PERCENT_STYLE_
COUNT = 0`, `NaN% = 0`, `Infinity% = 0`, `UNEXPECTED_GENERATED_DIFF = 0`(root wrapper style
2곳 외 XML 구조 diff 없음, 전수 확인).

## Status

`ComponentLayoutConverter.buildMainAreaStyle`, `WebSquareGenerator.appendBody`(grp_resultArea
style literal) 모두 `STATIC_VERIFIED`(compile/corpus 변환/canonical map/invariant/percentage
역산 실측 완료). `STUDIO_DESIGN_VERIFIED`는 선언하지 않음 — 사용자의 실제 폐쇄망 Studio
재확인 필요(`STUDIO_DESIGN_REQUIRED`). 최종 `ROOT_PERCENT_CONTAINING_BLOCK = FIX_CANDIDATE`.

---

## 후속 라운드 — Native v6 Layout Structure Gap Root-Cause Audit + Minimal Alignment

### 배경/증거

사용자가 폐쇄망에서 이전 라운드(Root Percentage Containing Block Width Fix) 적용 결과를 재확인했으나
Design/Preview가 여전히 원본 XPlatform과 다른 layout으로 표시됨(`STUDIO_DESIGN_FAILED`,
`STUDIO_DESIGN_REPRODUCED` 유지)을 보고. 사용자는 이번에는 root width를 더 손대지 말고, 정상 native
v6 화면과 현재 generated source의 layout-container 구조 차이가 실제 원인인지 먼저 규명하라고 요청.

제공된 native evidence(`KakaoTalk_20260819_172319366.jpg` Design 스크린샷,
`KakaoTalk_20260819_172339844.mp4` 43.25초 Source 탭 스크롤 영상)는 이번 세션에서 처음 본
자료가 아니다 — 동일 evidence가 이전에 이미 별도 candidate(`v6-class-mapping`)에서 OpenCV
0.5초 간격 87프레임 추출 + vision 직접 판독(OCR 미사용)으로 전체 문서 구조를 처음부터 끝까지
철저히 조사되어 있었다(`analysis/v6-video-source-analysis.md`,
`analysis/evidence/v6-video-source-components.csv`, `v6-class-mapping` candidate). 이번 라운드는
이 기존 문서를 재분석 근거로 그대로 재사용했다(영상 파일 자체는 이번 세션에서도 재확인 시도했으나
ffmpeg/ImageMagick video delegate가 이 환경에 여전히 없어 직접 재판독은 불가 --
`VideoDelegateFailed`, 추측 없이 기존의 검증된 판독 결과만 사용). 해당 문서를
`analysis/evidence-snapshots/native-layout-container-semantic/`에 복사해 이 candidate에도
보존했다.

### 1. ROOT_WIDTH_DEFECT 재판정

기존 evidence(`v6-video-source-components.csv` 43행)에 native table root 자체가
`xf:group tagname="table" class="w2tb_tb" style="width:100%"`로 관측되며, 상위
`grp_main`도 `style="height:760px;"`(width 없음)로 정상 렌더링됨이 이미 문서화돼 있었다.
즉 native v6 화면에서도 `grp_main`이 명시적 width 없이 정상 동작한다 -- 지난 라운드의
"width 부재가 collapse의 원인"이라는 진단을 뒷받침하는 반증도 지지도 이 evidence만으로는
결정할 수 없다. 지난 라운드의 fix(`grp_resultArea`/`grp_main`에 `width:100%;` 추가)를 적용한
뒤에도 사용자가 동일 증상을 재현했다는 사실은, width 부재가 유일한 원인이 아니었음을
시사한다.

판정: `ROOT_WIDTH_DEFECT = NOT_CONFIRMED`. 지난 라운드의 `width:100%;` 추가 자체는
native evidence와 모순되지 않으므로(native가 width를 금지하는 것이 아니라 단지 명시하지 않을
뿐 -- 브라우저 기본 block box 동작과 동일값) 되돌리지 않는다. 다만 이 fix가 사용자가 보고한
"업무 영역이 좁게 압축" 증상의 완전한 해결책은 아니었다는 것을 이번 라운드에서 인정하고
기록한다.

### 2. NATIVE_LAYOUT_CONTAINER_SEMANTIC 판정

기존 evidence는 native v6에서 다음 구조를 100%/n=1~3 신뢰도로 문서화했다:

| 역할 | 구조 |
|---|---|
| 섹션 제목 | `xf:group class="dfbox"` > `xf:group class="fl"` > `w2:textbox class="df_tit"` |
| 다열 입력 표 | `xf:group class="lybox"` > `xf:group class="ly_column col_N"` > `xf:group tagname="table" class="w2tb_tb"` > (`xf:group tagname="tr"` > `xf:group tagname="th"/"td" class="w2tb_th"/"w2tb_td"`)* |
| 버튼 행 | `xf:group class="fr"` |

핵심: `tagname` 속성은 CSS skin class가 아니라 WebSquare 렌더러가 실제로 어떤 HTML 요소
(`<table>`/`<tr>`/`<th>`/`<td>`)를 생성하는지를 결정하는 구조적 신호다(`w2tb_th`/`w2tb_td`는
`tagname=th`/`tagname=td`인 모든 인스턴스에서 예외 0건 100% 대응 -- evidence 4절).

판정: `NATIVE_LAYOUT_CONTAINER_SEMANTIC = REQUIRED`(구조적 신호가 명확히 확인됨, 단순
decoration이 아님).

### 3. ABSOLUTE_PERCENT_LAYOUT_STRATEGY 평가

현재 candidate의 `[WebSquareGenerator] convertChildren`은 Table 미판정 컴포넌트 전부를
`position:absolute;left:%;top:%;width:%;height:%`로 생성한다(코드 확인, 이번 라운드
무변경). Table 판정 경로(`convertLayoutAsTable`)만 row/column 구조를 만들지만, 지금까지는
`tagname`/`class` 없이 순수 `xf:group` nesting뿐이었다(4절 CURRENT_GROUP_ONLY_TABLE_MODEL
참고).

corpus 실측: `TABLE_LAYOUT_HIGH_CONFIDENCE` 실적용은 5/135(`divWrap`, `divA`,
`tabMain.pageA/pageB/pageInline`) -- 나머지 130/135는 여전히 절대좌표 % 전략을 그대로 사용한다.
사용자가 원래 제공한 Studio 실패 스크린샷(검색조건/버튼/Grid 영역)이 이 5건의 Table-판정
경로에 해당한다는 직접 증거는 없다 -- 즉 이번 evidence만으로 "전체 화면 압축" 증상이
`ABSOLUTE_PERCENT_LAYOUT_STRATEGY` 자체 때문인지 확정할 pairing이 없다.

판정: Table-판정 경로에 한해서는 `ROOT_CAUSE_CANDIDATE`(native가 `tagname=table/tr/td`
구조를 쓰는데 현재는 순수 group nesting뿐이었다는 명확한 gap이 있었음 -- 이번 라운드에서 최소
수정). 나머지 절대좌표 % 경로(Div/Grid Group 등 130/135) 전체에 대해서는
`EVIDENCE_INSUFFICIENT`로 유지 -- 사용자가 보고한 원래 실패 화면의 실제 generated XML 구조(Table
판정 경로인지 여부)가 확인되지 않았다.

### 4. CURRENT_GROUP_ONLY_TABLE_MODEL 판정

판정: `INCOMPLETE`. 기존 row/column `xf:group` 구조는 위치/크기(percentage geometry)는
정확히 계산하고 있었으나, native v6가 실제로 사용하는 `tagname="table"/"tr"/"td"` +
`class="w2tb_tb"/"w2tb_td"` 구조적 속성이 전혀 없었다 -- WebSquare 렌더러 관점에서는 일반
`<div>` 계열 group일 뿐 실제 HTML table이 아니었을 가능성이 있다.

th(header) vs td(data) 세분화는 이번 라운드 corpus에서 안전하게 일반화할 근거가 없다(예:
`divWrap`/`tabMain.pageA` 등 실제 TABLE_LAYOUT_HIGH_CONFIDENCE 5건 전부가 label+input 쌍이
아니라 단일 컴포넌트(Tab/Input/Button)를 담은 cell이다 -- "이 cell은 header"라고 판별할 source
신호가 없다). 따라서 모든 cell을 `td`/`w2tb_td`로만 표시하고 th는 미적용으로 유지
(`UNRESOLVED`).

### 5. dfbox/fl/lybox/ly_column/fr 적용 여부

미적용, `EVIDENCE_INSUFFICIENT` 유지. 이 class들은 "이 Div가 섹션 제목을 가진 wrapper"
또는 "이 Div가 N-column 레이아웃 박스"라는 설계 의도를 나타내며, XPlatform source에는 이런
개념(section title 여부, column 수 의도)을 신뢰성 있게 판별할 대응 속성이 없다(기존
`v6-video-source-analysis.md` 6절 결론과 동일 -- "XPlatform source에 이런 표 헤더/셀/섹션
wrapper 개념 자체가 없다"). 근거 없는 class를 붙이지 않는다는 원칙(`component-class-
implementation-decision.md`)을 유지, 이번 라운드도 적용하지 않는다.

### 6. Production 변경

[WebSquareGenerator] convertLayoutAsTable(...) -- 기존 함수 수정(신규 함수 아님)

변경 범위: `TABLE_LAYOUT_HIGH_CONFIDENCE`로 판정된 기존 row/column 생성 로직에 3곳만 추가:
1. row/column 그룹을 감싸는 새 `xf:group`(`tagname="table" class="w2tb_tb" style="width:100%;"`)을
   생성해 `targetParent`에 붙이고, 기존에 `targetParent`에 직접 붙던 row group들을 이 wrapper
   자식으로 옮김.
2. row group에 `tagname="tr"` 추가(class는 evidence대로 미부여).
3. cell group에 `tagname="td"` + `class="w2tb_td"` 추가.

percentage geometry 계산(`buildTableRowStyle`/`buildTableCellStyle`), row/cell 판정 알고리즘
(`classifyLayoutGeometry`/`buildTableRows`), `basisWidth`/`basisHeight` 산출 로직은 전혀
변경하지 않았다.

Full Unified Diff: analysis/git-baseline-vs-candidate-production.diff
(누적, 이번 라운드분은 파일 마지막 hunk).

Caller: `convertChildren`(Layout을 만나면 `convertLayoutAsTable` 호출, 무변경).
Callee: `layoutConverter.buildTableRowStyle`/`buildTableCellStyle`(무변경), `createUniqueTargetId`/
`buildSourcePath`(기존 helper 재사용, 신규 helper 없음).

### Generated XML BEFORE/AFTER(실제 corpus, Form/Main/TabExternalRelativePath.xml)

BEFORE(corpus-output-round5):
```
<xf:group id="divWrap_layoutTableRow0" style="width:100%;height:89.4737%;">
    <xf:group id="divWrap_layoutTableRow0Col0" style="width:94.8276%;height:100%;">
        <w2:tabControl ... id="divWrap_tabNested" style="width:94.8276%;height:89.4737%;">
```

AFTER(corpus-output-round6):
```
<xf:group class="w2tb_tb" id="divWrap_layoutTable" style="width:100%;" tagname="table">
    <xf:group id="divWrap_layoutTableRow0" style="width:100%;height:89.4737%;" tagname="tr">
        <xf:group class="w2tb_td" id="divWrap_layoutTableRow0Col0" style="width:94.8276%;height:100%;" tagname="td">
            <w2:tabControl ... id="divWrap_tabNested" style="width:94.8276%;height:89.4737%;">
```

row/cell의 percentage 값(89.4737%/94.8276%)은 BEFORE=AFTER 완전 동일 -- 구조적 속성
(tagname/class/신규 wrapper)만 추가됐음을 실측 확인.

### 영향 범위

corpus 149개 화면 변환 성공 149/149, 136개 XML 중 이번 변경으로 실제 diff 발생 4개 파일
(TabExternalRelativePath.xml, NestedContainer.xml, TabContainer.xml,
TabInlineContent.xml -- TABLE_LAYOUT_HIGH_CONFIDENCE 5건이 이 4개 파일에 분포, 한 파일에
Tab 페이지 2개가 각각 판정된 경우 포함). 나머지 132개 XML은 byte-identical(diff -rq 확인).
4개 파일의 diff 내용은 전부 tagname/class/layoutTable wrapper 추가만이며, 다른 어떤
속성/구조도 변경되지 않았음을 각 파일 line-by-line diff로 확인.

### 회귀 결과

| 항목 | 결과 |
|---|---|
| 컴파일 | 0 errors |
| 전체 corpus 변환 | 149/149 성공 |
| XML parse | 136/136 well-formed |
| standalone JS | 15/15(무변경) |
| Phase1 SHA | 2/2 PASS(무변경) |
| SOURCE_TO_TARGET_ID_MAP_EXPECTED_ONLY | PASS(403/403 key, diff 0 -- layoutTable/tr/td wrapper는 synthetic id라 componentIdMap에 애초에 없음) |
| invariant class/QName | btn_cm=12, wq_gvw=3 전부 무변경 |
| grp_resultArea/grp_main width | 135/136(무변경, 이번 라운드 미접촉) |
| 실제 diff 발생 XML | 4/136(TABLE_LAYOUT_HIGH_CONFIDENCE 5건이 속한 파일), 나머지 132개 byte-identical |
| diff 내용 | 전부 tagname/class/layoutTable wrapper 추가만(row/cell percentage 값 불변, line-by-line 확인) |

### Completion Gate

DIV_NATIVE_LAYOUT_STRUCTURE -- 이번 라운드는 Div 자체(dfbox/fl/lybox/ly_column)에 적용하지
않았으므로 NOT_APPLICABLE_THIS_ROUND(EVIDENCE_INSUFFICIENT). TABLE_NATIVE_LAYOUT_STRUCTURE =
PASS(TABLE_LAYOUT_HIGH_CONFIDENCE 경로에 tagname=table/tr/td + class=w2tb_tb/w2tb_td 적용,
실측 5/5). GRID_PARENT_STRUCTURE = UNCHANGED(이번 라운드 미접촉, 기존 UNRESOLVED 유지).
PERCENT_GEOMETRY_PARENT_SEMANTIC = PASS(row/cell percentage 계산 완전 무변경). COMPONENT_
QNAME_PRESERVED = PASS, EXISTING_CLASS_PRESERVED = PASS(btn_cm/wq_gvw 무변경).
BODY_LIFECYCLE_ATTRIBUTES_PRESERVED = PASS(미접촉). UNEXPECTED_GENERATED_DIFF = 0(4개
파일의 diff 전부 의도한 tagname/class/wrapper 추가로 설명됨, 그 외 132개 파일은 완전
byte-identical).

## Status

[WebSquareGenerator] convertLayoutAsTable(기존 함수 수정) -- STATIC_VERIFIED(compile/corpus
변환/canonical map/invariant 실측 완료, 4개 대상 파일 line-by-line diff 확인). STUDIO_DESIGN_
VERIFIED는 선언하지 않음 -- 사용자의 실제 폐쇄망 Studio 확인 필요(STUDIO_DESIGN_REQUIRED).

중요한 한계 고지: 이번 fix는 corpus 5/135(TABLE_LAYOUT_HIGH_CONFIDENCE 경로)에만 영향을
준다. 사용자가 원래 보고한 "검색조건/버튼/Grid 영역이 전체적으로 좁게 압축"되는 증상이 정확히
이 5건에 해당하는 화면에서 발생한 것인지는 이번 evidence로 확인할 수 없었다 -- 만약 실패 화면이
Table-판정 경로를 타지 않는 화면이라면, 이번 fix는 그 화면의 시각적 증상을 바꾸지 않을 수 있다.
ABSOLUTE_PERCENT_LAYOUT_STRATEGY가 절대좌표 % 경로 전체(130/135)에 대해서도 근본 원인인지는
EVIDENCE_INSUFFICIENT로 남으며, 최종 DESIGN_STRUCTURE = FIX_CANDIDATE / STATIC_VERIFIED /
STUDIO_DESIGN_REQUIRED(전체 문제의 완전한 해결이 아닌, 확인된 구조적 gap 하나에 대한 최소
수정).

---

## 후속 라운드 -- 실제 Studio 재실패: Nested Percentage Double-Scaling + Grid Width 조사

### 배경/증거

사용자가 지난 라운드(Native v6 Layout Structure Gap 최소 정렬) 적용 이후 폐쇄망에서 실제 업무
화면(STT00030, BCI01M0000)을 재변환/확인했으나 여전히 `STUDIO_DESIGN_FAILED`/
`STUDIO_DESIGN_REPRODUCED`. 이번에는 사용자가 직접 촬영한 실제 generated Source 코드 스크린샷
(WebSquare Studio 소스 탭)을 근거로, 다음 두 구체적 패턴을 지목했다:

```xml
<xf:group class="w2tb_td" id="Div00_layoutTableRow0Col0" style="width:6.0345%;height:100%;" tagname="td">
    <xf:trigger class="btn_cm" ev:onclick="scwin.btn_excel_onclick" id="Div00_btn_excel"
                style="width:6.0345%;height:2.6316%;" tabIndex="6" value="엑셀"/>
</xf:group>
```

Cell 자신의 width(`6.0345%`)와 그 안의 실제 컴포넌트(trigger)의 width(`6.0345%`)가 **동일한
값**으로 중복 계산되어 있음을 확인.

### 1. NESTED_PERCENT_DOUBLE_SCALING 원인 추적

`[WebSquareGenerator] convertLayoutAsTable`(지난 라운드까지 수정된 함수)에서 cell 내부
컴포넌트 변환 호출부:

```java
convertChildren(
        out, layout, cellGroup, parentPath, analysis, depth, cell,
        basisWidth, basisHeight, false);
```

`basisWidth`/`basisHeight`는 이 Layout(Div 내부) **전체**의 basis(예: `ComponentMethodConversion`
Form 기준 600x300 같은 원래 Div/Layout 크기)다. 이 값을 그대로 cell 내부 컴포넌트 변환에도
전달하면, `[WebSquareGenerator] copyBasicProperties`(1176줄) -> `[ComponentLayoutConverter]
buildPercentComponentStyle`이 **컴포넌트 자신의 px width/height를 같은 basisWidth/basisHeight로
다시 나눠** percentage를 계산한다.

그런데 `[ComponentLayoutConverter] buildTableCellStyle`은 이미 "cell 자신의 width = 그
컴포넌트 자신의 source width / 같은 basisWidth"로 cell의 폭을 정확히 그 컴포넌트 크기에 맞춰
계산해 놓은 상태다(cell에는 정확히 1개의 XPlatform 컴포넌트만 들어간다 -- `buildTableRows`가
Layout 직계 자식 1개당 정확히 1개의 cell을 만드는 구조, 재확인). 즉:

- cell width% = `component_px_width / basisWidth * 100`
- (수정 전) child width% = `component_px_width / basisWidth * 100` (cell width%와 **동일**)

CSS에서 자식의 `width:N%`는 **자신의 실제 렌더링된 부모(cell)의 폭**을 기준으로 계산되므로,
실제 렌더링 폭 = `cell_render_width * (child_percent/100)` = `(basis 대비 cell%) x (basis 대비
child%, 동일값)` = 원래 의도한 폭의 제곱 비율로 축소된다(예: 6.0345% -> 실효 약 0.364%).
height도 동일 패턴(row height% x child height% 이중 곱).

**판정**: `NESTED_PERCENT_DOUBLE_SCALING = CONFIRMED`(코드 추적 + 사용자 제공 실제 generated
XML 스크린샷 수치 일치로 실증).

### 2. TABLE_PERCENT_BASIS_TRACE

| source component path | generated Table | Row | Cell | Child | basis(각 단계) | generated % |
|---|---|---|---|---|---|---|
| `Div00.btn_excel`(XPlatform Button) | `Div00_layoutTable` | `Div00_layoutTableRow0`(row) | `Div00_layoutTableRow0Col0` | `Div00_btn_excel`(xf:trigger) | Row/Cell: Div00 자신의 Layout basisWidth/basisHeight(수정 전과 후 동일, 무변경) / **Child(수정 전)**: 동일 Div00 basisWidth/basisHeight(cell과 동일 basis 재사용 -- 결함) / **Child(수정 후)**: cell 자신의 px 크기(`resolveCellBasisWidth`)와 row 자신의 px 크기(`resolveRowBasisHeight`) | 수정 전: cell=6.0345%, child=6.0345%(중복) / 수정 후: cell=6.0345%(무변경), child=100%/100% |

(사용자가 제공한 실제 화면의 정확한 source px 값은 화면 자체가 폐쇄망 로컬 파일이라 이 문서에
직접 반입하지 않았다 -- 아래 "Generated XML BEFORE/AFTER"는 동일 구조를 재현하는 로컬 corpus
실제 사례(`Form/TabContainer.xml`)로 대체 검증했다. 구조/수치 패턴은 사용자가 제공한 스크린샷과
동일함을 코드 경로 추적으로 확인.)

### 3. GRID_COLUMN_WIDTH_MISMATCH 조사 결과

`[GridFormatConverter]`(`getSingleColumnWidth`/`calculateCellWidth` 등)는 XPlatform Grid
Format의 `Column size="N"`을 `w2:column width="N"`(고정 px)으로 그대로 매핑한다. 이는
WebSquare `w2:gridView`의 표준 column-width API(px 기준)이며, 이번 세션의 native v6 evidence
(`v6-video-source-analysis.md`, `v6-video-source-components.csv`)에는 grid **내부 column
width**에 대한 직접 판독 기록이 전혀 없다(영상에서 column 개별 폭까지 확대/판독한 적 없음 --
grid 전체 wrapper의 `width:100%`만 기록됨). 즉:

- w2:gridView가 column width 합계를 client 폭에 맞춰 자동 scale하는지
- percentage column width를 지원하는지
- 아니면 합계가 초과하면 정상적으로 가로 스크롤되는(의도된) 동작인지

를 판별할 local/native evidence가 전혀 없다. 이 세 시나리오 중 어느 것이 맞는지 추측하면
`w2:gridView`의 실제 렌더링 계약(다른 corpus 화면 -- 특히 의도적으로 넓은 grid를 가진 화면 --
을 깨뜨릴 위험)을 무근거로 바꾸는 것이 된다.

**판정**: `GRID_COLUMN_WIDTH_MISMATCH = EVIDENCE_INSUFFICIENT`. Production 미수정
(`GridFormatConverter` 무변경 -- diff 0 확인). 사용자가 실제 폐쇄망 Studio에서 grid 폭이
column 합계보다 큰 경우 실제로 어떻게 렌더링되는지(가로 스크롤 정상 동작인지, 아니면 확대/축소가
필요한지) 직접 관찰해 알려주면 다음 라운드에서 재조사 가능.

### 4. Production 변경

**[ComponentLayoutConverter] buildTableRowStyle/buildTableCellStyle(기존 함수, 리팩터링)**
+ **resolveCellBasisWidth/resolveRowBasisHeight(신규 함수, private 계산 로직을 public 재사용
가능한 형태로 추출)**

BEFORE(`buildTableRowStyle`/`buildTableCellStyle`, 라운드 내부에 계산 로직 인라인):
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

AFTER(계산 로직을 `resolveRowBasisHeight`/`resolveCellBasisWidth`로 추출, 두 곳에서 재사용):
```java
public String buildTableRowStyle(List<Element> row, double basisHeight) {
    if (basisHeight <= 0.0) {
        return null;
    }
    double rowHeight = resolveRowBasisHeight(row);
    if (rowHeight <= 0.0) {
        return null;
    }
    return "width:100%;height:" + formatPercent(rowHeight / basisHeight * 100.0) + ";";
}

public String buildTableCellStyle(Element cell, double basisWidth) {
    if (basisWidth <= 0.0) {
        return null;
    }
    double cellWidth = resolveCellBasisWidth(cell);
    if (cellWidth <= 0.0) {
        return null;
    }
    return "width:" + formatPercent(cellWidth / basisWidth * 100.0) + ";height:100%;";
}

public double resolveCellBasisWidth(Element cell) { /* cell 자신의 px width, 계산 불가시 -1 */ }
public double resolveRowBasisHeight(List<Element> row) { /* row 자신의 px height, 계산 불가시 -1 */ }
```

`buildTableRowStyle`/`buildTableCellStyle`의 실제 반환값(퍼센트 문자열)은 이 리팩터링으로
전혀 바뀌지 않는다(동일 계산을 함수 추출만 한 것) -- corpus diff로 실측 확인(row/cell 자신의
style은 132/136 파일에서 byte-identical, 4개 대상 파일에서도 row/cell style 값 자체는
무변경).

**[WebSquareGenerator] convertLayoutAsTable(기존 함수 수정)** -- cell 내부 컴포넌트 변환 시
basis 교체

BEFORE:
```java
convertChildren(
        out, layout, cellGroup, parentPath, analysis, depth, cell,
        basisWidth, basisHeight, false);
```

AFTER:
```java
double rowBasisHeightPx = layoutConverter.resolveRowBasisHeight(row);
...
double cellBasisWidthPx = layoutConverter.resolveCellBasisWidth(cell);
double childBasisWidth = cellBasisWidthPx > 0.0 ? cellBasisWidthPx : basisWidth;
double childBasisHeight = rowBasisHeightPx > 0.0 ? rowBasisHeightPx : basisHeight;
convertChildren(
        out, layout, cellGroup, parentPath, analysis, depth, cell,
        childBasisWidth, childBasisHeight, false);
```

계산 불가(`-1`) 시 기존 `basisWidth`/`basisHeight`로 fallback -- 기존 UNRESOLVED 처리 경로와
동일하게 동작(퇴행 없음).

Full Unified Diff: [analysis/git-baseline-vs-candidate-production.diff](git-baseline-vs-candidate-production.diff)
(누적, 이번 라운드분은 파일 마지막 hunk).

Caller: `convertLayoutAsTable`(row/cell 루프, 무변경 호출 구조). Callee: `resolveCellBasisWidth`/
`resolveRowBasisHeight`(신규, `resolveGeometry`/`parseLength` 등 기존 private helper 재사용,
새 클래스 없음).

### Generated XML BEFORE/AFTER(실제 corpus, `Form/TabContainer.xml`)

BEFORE(`corpus-output-round6`):
```xml
<xf:group class="w2tb_td" id="tabMain_pageB_layoutTableRow0Col0" style="width:14.8148%;height:100%;" tagname="td">
    <xf:trigger class="btn_cm" id="tabMain_pageB_btnB" style="width:14.8148%;height:8.2759%;" value="B"/>
</xf:group>
```

AFTER(`corpus-output-round7`):
```xml
<xf:group class="w2tb_td" id="tabMain_pageB_layoutTableRow0Col0" style="width:14.8148%;height:100%;" tagname="td">
    <xf:trigger class="btn_cm" id="tabMain_pageB_btnB" style="width:100%;height:100%;" value="B"/>
</xf:group>
```

cell 자신의 style(`width:14.8148%;height:100%;`)은 완전 동일 -- 오직 그 내부 child의 style만
`width:14.8148%;height:8.2759%;` -> `width:100%;height:100%;`로 수정됨을 실측 확인. 4개 대상
파일 중 `NestedContainer.xml`은 cell 내부가 컨테이너(`w2:group`)라 그 자식(`xf:input`)까지
연쇄적으로 재계산됐다(구조가 아니라 값만 -- 아래 "중첩 컨테이너 케이스" 참고).

**중첩 컨테이너 케이스**(`Form/NestedContainer.xml`, `divA_grpA`가 cell의 유일한 내용이면서
그 자신도 컨테이너):

BEFORE:
```xml
<w2:group id="divA_grpA" style="width:83.3333%;height:66.6667%;" value="Group">
    <xf:input id="divA_grpA_edt" style="position:absolute;left:1.6667%;top:3.3333%;width:33.3333%;height:16%;"/>
</w2:group>
```

AFTER:
```xml
<w2:group id="divA_grpA" style="width:100%;height:100%;" value="Group">
    <xf:input id="divA_grpA_edt" style="position:absolute;left:2%;top:5%;width:40%;height:24%;"/>
</w2:group>
```

`divA_grpA`(cell의 직접 컴포넌트) 자신은 예상대로 100%/100%가 됐고, 그 자식 `divA_grpA_edt`도
연쇄적으로 재계산됐다 -- `divA_grpA`의 기존(수정 전) basis(px 300x150 Div 전체)가 아니라
`divA_grpA` 자신의 실제 px 크기(250x100, cell/row basis와 동일)를 기준으로 다시 계산되어
`left:1.6667%->2%`(5px/250=2%), `top:3.3333%->5%`(5px/100=5%), `width:33.3333%->40%`
(100px/250=40%), `height:16%->24%`(24px/100=24%). 이는 `PERCENT_GEOMETRY_PARENT =
IMMEDIATE_SOURCE_CONTAINER` 원칙(자식은 자신을 직접 담는 컨테이너 자신의 geometry를 기준으로
계산)이 cell 내부 재귀에도 올바르게 전파된 결과이며, 별도 코드 추가 없이 기존 `convertChildren`의
재귀 basis 전달 방식만으로 자동으로 correct하게 cascading됨을 확인했다(의도한 부작용, 결함
아님 -- 수치를 XPlatform source 원본(`left=5,top=5,width=100,height=24px`, `divA_grpA=
250x100px`)과 직접 역산해 검증 완료).

### 영향 범위

corpus 149개 화면 변환 성공 149/149, 136개 XML 중 실제 diff 발생 4개 파일(round6과 동일 -- 이번
fix도 `TABLE_LAYOUT_HIGH_CONFIDENCE` 5건이 속한 파일에만 영향), 나머지 132개 XML은
byte-identical. 4개 파일의 diff는 전부 cell 내부 child(및 그 자손)의 percentage 값 재계산뿐이며,
cell/row/table wrapper 자신의 style, id, tagname, class는 전혀 변경되지 않았다(line-by-line
diff 확인).

### 회귀 결과

| 항목 | 결과 |
|---|---|
| 컴파일(clean build, `build/classes` 재생성 후) | 0 errors |
| 전체 corpus 변환 | 149/149 성공 |
| XML parse | 136/136 well-formed |
| standalone JS | 15/15(무변경) |
| Phase1 SHA | 2/2 PASS(무변경) |
| `SOURCE_TO_TARGET_ID_MAP_EXPECTED_ONLY` | PASS(403/403 key, diff 0) |
| invariant class/QName | `btn_cm=12`, `wq_gvw=3` 전부 무변경 |
| `grp_resultArea`/`grp_main` width | 135/136(무변경, 이번 라운드 미접촉) |
| top-level(non-table) percentage(`cbo`/`grd_gridGroup` 등) | 무변경 재확인(`TOP_LEVEL_PERCENT_ARITHMETIC = PASS`, 재설계 없음) |
| 실제 diff 발생 XML | 4/136(round6과 동일 대상), 나머지 132개 byte-identical |
| diff 내용 | 전부 cell 내부 child(및 컨테이너 자손) percentage 값 변경만(cell/row/table 자신은 무변경) |

### Completion Gate

`TABLE_CELL_CHILD_PARENT_BASIS = PASS`(cell 내부 child가 이제 cell/row 자신의 px 크기를
기준으로 계산됨, 실측 4/4 대상 파일). `NESTED_PERCENT_DOUBLE_SCALING = 0`(수정 후 cell
width%와 child width%가 더 이상 동일 값으로 중복되지 않음, 전부 100%/100%로 정규화 확인).
`STRUCTURE_TOPOLOGY_PRESERVED = PASS`(id/tagname/class/구조 전부 무변경, style 값만 변경).
`ROOT_STRUCTURE_UNCHANGED_THIS_ROUND = PASS`(`grp_resultArea`/`grp_main` 완전 무변경).
`COMPONENT_QNAME_PRESERVED = PASS`, `EXISTING_CLASS_PRESERVED = PASS`(`btn_cm`/`wq_gvw`
무변경). `BODY_LIFECYCLE_ATTRIBUTES_PRESERVED = PASS`(미접촉). `SOURCE_TO_TARGET_ID_MAP_
EXPECTED_ONLY = PASS`. `UNEXPECTED_GENERATED_DIFF = 0`(4개 파일의 diff 전부 의도한 cell
child basis 재계산으로 설명됨, 그 외 132개 파일 byte-identical).

## Status

`[ComponentLayoutConverter] buildTableRowStyle/buildTableCellStyle`(기존 함수, 리팩터링),
`resolveCellBasisWidth`/`resolveRowBasisHeight`(신규 함수), `[WebSquareGenerator]
convertLayoutAsTable`(기존 함수 수정) -- `STATIC_VERIFIED`(compile/corpus 변환/canonical
map/invariant/4개 대상 파일 line-by-line diff + 역산 검증 완료). `GridFormatConverter`는
`GRID_COLUMN_WIDTH_MISMATCH = EVIDENCE_INSUFFICIENT`로 미수정(diff 0). `STUDIO_DESIGN_
VERIFIED`는 선언하지 않음 -- 사용자의 실제 폐쇄망 Studio 확인 필요(`STUDIO_DESIGN_REQUIRED`).

최종 `DESIGN_STRUCTURE = FIX_CANDIDATE` / `STATIC_VERIFIED` / `STUDIO_DESIGN_REQUIRED`. 이번
fix는 corpus의 Table 판정 경로(5/135)에 존재하던 명확한 이중 축소 결함을 제거했다 -- 사용자가
제공한 실제 화면 스크린샷의 수치 패턴과 정확히 일치하는 결함이었으므로, 이번에는 이전 라운드들과
달리 사용자가 보고한 실패 증상과 직접 대응되는 근거가 있다. 다만 Grid 자체의 column width 문제는
evidence 부족으로 이번에도 미해결로 남는다.

---

## 후속 라운드 -- Grid 내부 Column Width Ratio Candidate (실험적, native evidence 없음)

### 배경

지난 라운드(Nested Percentage Double-Scaling 수정)에서 `GRID_COLUMN_WIDTH_MISMATCH =
EVIDENCE_INSUFFICIENT`로 미해결로 남겨둔 항목. 이번 라운드는 native v6 evidence가 여전히 없는
상태에서(`GRID_COLUMN_NATIVE_EVIDENCE = EVIDENCE_INSUFFICIENT`), 폐쇄망 Studio에서 실제
렌더링을 비교 검증할 수 있도록 실험적 candidate를 구현한다(`GRID_COLUMN_WIDTH_SEMANTIC =
EXPERIMENTAL`). 이번 라운드 시작 시점의 참고 commit(`4b99695`)과 현재 HEAD는 완전히 동일(git
diff 0) -- 이전 라운드의 작업과 겹치거나 충돌하는 부분 없음.

### width attribute 지원 범위 조사 결과

local repository/기존 generated reference 어디에도 `w2:column width="N%"` 형태의 실제 native
evidence가 없다(`GRID_COLUMN_NATIVE_EVIDENCE = EVIDENCE_INSUFFICIENT`, 5번 규칙의 "D. local
evidence로 판단 불가"). 이번 candidate는 XML 표준(percentage 문자열은 단순 attribute value
문자열이라 XML 1.0 syntax 자체는 항상 허용 -- `w2:column`은 domain-defined attribute이므로
parser/schema 레벨에서 값 형식을 제한하지 않음, 이 프로젝트의 XML 생성/파싱 경로에 값 형식
검증이 없음을 코드로 확인)만 확인한 뒤, WebSquare semantic 확정 여부는 `STUDIO_DESIGN_REQUIRED`로
유지한다.

### Grid 기준 폭 계산 정책

사용자 지시대로 분모를 Form width가 아니라 **source Grid 자신의 declared `width` 속성**으로
우선 사용한다(`sourceGrid.getAttribute("width")`, `[GridFormatConverter] convert`의 파라미터로
이미 전달받고 있던 실제 XPlatform `Grid` 엘리먼트). border/scrollbar 등 임의 보정 상수는
사용하지 않았다(`GRID_COLUMN_SUM_TOLERANCE_PX = 0.5`는 정수 px 입력 간 부동소수 오차만 흡수하는
rounding tolerance이며, 시각적 보정이 아니다).

### column 합계 대 Grid width 불일치 처리

3가지로 분류(전부 corpus 실측):

| 분류 | 조건 | 처리 |
|---|---|---|
| `NORMALIZED_TO_CONTAINER` | column 정의 전부 숫자로 읽히고, source Grid width 유효, `columnSum <= gridWidth + 0.5px` | 각 column을 `columnWidth/gridWidth*100`로 percentage 변환 |
| `PIXEL_FALLBACK` | `columnSum > gridWidth + 0.5px`(horizontal-scroll semantic 가능성) | 기존 px 값 그대로 유지(무변경) |
| `UNRESOLVED` | column 정의 없음/숫자 아님, 또는 source Grid width 없음/유효하지 않음 | 기존 px 값 그대로 유지(무변경) |

`column sum < Grid width`인 경우(이번 corpus의 3건 전부 이 케이스) 남는 영역을 마지막 column에
임의로 몰아주지 않고, 각 column을 있는 그대로 `자기 px / Grid px`로 계산한다 -- 합계가 100%
미만이 되는 것 자체가 "Grid 폭 중 일부가 비어있다"는 source semantic을 percentage로도 그대로
보존한 결과다(왜곡 없음).

### 대표 Grid 실제 trace (corpus 3건 전부)

| source file | source Grid id | source Grid width | column count | source column widths | column sum | normalized 결과 | fallback |
|---|---|---|---|---|---|---|---|
| `Form/ComponentMethodConversion.xfdl` | `grd` | 300 | 1 | `[100]` | 100 | `[33.3333%]` | 아니오 |
| `Form/GridAdvancedPhase3.xfdl` | `grdMain` | 600 | 3 | `[100, 220, 120]` | 440 | `[16.6667%, 36.6667%, 20%]` | 아니오 |
| `Form/UnsupportedFeatures.xfdl` | `grd` | 300 | 1 | `[100]` | 100 | `[33.3333%]` | 아니오 |

corpus 149개 화면 전체에서 `TABLE_LAYOUT_HIGH_CONFIDENCE`와 달리 실제 Grid Format을 가진
화면은 이 3개뿐이며, 전부 `NORMALIZED_TO_CONTAINER`로 분류됐다(`PIXEL_FALLBACK`/`UNRESOLVED`
corpus 실사례 0건 -- 로직 경로는 구현/코드리뷰로 존재하나 이번 corpus에서 트리거된 적 없음).

### Header/Body/Footer 일관성 검증

`grdMain`(3-column) 실측: header `[16.6667%, 36.6667%, 20%]`, body(2 visible column, 3번째는
select) `[16.6667%, 36.6667%, 20%]`(동일 컬럼 인덱스와 정확히 일치), footer(colspan=3)
`73.3333%`(`16.6667+36.6667+20`의 합과 정확히 일치, 별도 재계산 없이 동일
`columnPercents`(`double[]`, `convert()`에서 1회만 계산해 header/body/footer 3곳에 공통 전달)
배열을 그대로 사용하므로 rounding mismatch 발생 불가능(구조적으로 보장).

### Production 변경

**[GridFormatConverter] resolveColumnPercents(Element, List&lt;String&gt;, String)** -- 신규
함수

```java
private double[] resolveColumnPercents(Element sourceGrid, List<String> widths, String gridId) {
    // widths 전부 숫자로 읽을 수 있고, sourceGrid.getAttribute("width")가 유효하고,
    // columnSum <= gridWidth + tolerance일 때만 percentage 배열 반환, 그 외 전부 null
    // (기존 px 경로 완전 무변경)
}
```

**[GridFormatConverter] convert(...)** -- 기존 함수 수정(호출부 1곳 추가)

BEFORE:
```java
appendHeader(out, targetGridView, gridId, format);
appendBody(out, targetGridView, gridId, format, datasetColumns);
appendFooter(out, targetGridView, gridId, format);
```

AFTER:
```java
double[] columnPercents = resolveColumnPercents(sourceGrid, format.getColumnWidths(), gridId);

appendHeader(out, targetGridView, gridId, format, columnPercents);
appendBody(out, targetGridView, gridId, format, datasetColumns, columnPercents);
appendFooter(out, targetGridView, gridId, format, columnPercents);
```

**[GridFormatConverter] calculateCellWidth/getSingleColumnWidth** -- 기존 함수 수정(파라미터
`double[] columnPercents` 추가, null이면 기존 px 로직 완전 동일 -- 하위 호환)

BEFORE(`getSingleColumnWidth` 예시):
```java
private String getSingleColumnWidth(List<String> widths, int index, String gridId) {
    if (index < 0 || index >= widths.size()) {
        return "";
    }
    String normalized = normalizeSize(widths.get(index));
    ...
    return normalized;
}
```

AFTER:
```java
private String getSingleColumnWidth(
        List<String> widths, int index, String gridId, double[] columnPercents) {
    if (index < 0 || index >= widths.size()) {
        return "";
    }
    if (columnPercents != null) {
        return index < columnPercents.length
                ? percentFormatter.formatPercent(columnPercents[index])
                : "";
    }
    String normalized = normalizeSize(widths.get(index));
    ...
    return normalized;
}
```

`calculateCellWidth`도 동일 원칙(colspan 구간의 percent 합을 반환, `columnPercents==null`이면
기존 px 합산 로직 완전 그대로).

**[GridFormatConverter] appendHeader/appendBody/appendFooter/appendSynthesizedDatasetBody/
appendPlaceholderColumn/applyCellGeometry** -- 기존 함수 수정(파라미터로 `columnPercents`를
전달만 함, 각 함수 내부 로직/구조는 무변경).

percentage formatter는 새로 만들지 않고 `[ComponentLayoutConverter] formatPercent`를
`percentFormatter`(private final 필드)로 재사용했다(중복 formatter 생성 금지 원칙 준수).

Full Unified Diff: [analysis/git-baseline-vs-candidate-production.diff](git-baseline-vs-candidate-production.diff)
(누적, 이번 라운드분은 `GridFormatConverter.java` hunk 전체).

Caller: `[WebSquareGenerator] convertChildren`(`gridFormatConverter.convert(out, src, target)`
호출, 무변경). Callee: 신규 `resolveColumnPercents`는 기존 `normalizeSize`/`formatNumber`
(px 파싱/포맷)와 `ComponentLayoutConverter.formatPercent`(percentage 포맷)만 재사용, 새 helper
클래스 없음.

### Generated XML BEFORE/AFTER(실제 corpus, `Form/GridAdvancedPhase3.xml`, `grdMain`)

BEFORE(`corpus-output-round7`):
```xml
<w2:column id="grdMain_head_r0_c0" inputType="text" value="코드" width="100"/>
<w2:column id="grdMain_head_r0_c1" inputType="text" value="이름" width="220"/>
<w2:column id="grdMain_head_r0_c2" inputType="text" value="유형" width="120"/>
...
<w2:column id="CODE" inputType="text" width="100"/>
<w2:column id="grdMain_body_r0_c1" inputType="text" readOnly="true" width="220"/>
<w2:column id="TYPE" inputType="select" width="120">
...
<w2:column colSpan="3" displayMode="label" id="grdMain_summ_r0_c0" inputType="text" value="summary" width="440"/>
```

AFTER(`corpus-output-round9`):
```xml
<w2:column id="grdMain_head_r0_c0" inputType="text" value="코드" width="16.6667%"/>
<w2:column id="grdMain_head_r0_c1" inputType="text" value="이름" width="36.6667%"/>
<w2:column id="grdMain_head_r0_c2" inputType="text" value="유형" width="20%"/>
...
<w2:column id="CODE" inputType="text" width="16.6667%"/>
<w2:column id="grdMain_body_r0_c1" inputType="text" readOnly="true" width="36.6667%"/>
<w2:column id="TYPE" inputType="select" width="20%">
...
<w2:column colSpan="3" displayMode="label" id="grdMain_summ_r0_c0" inputType="text" value="summary" width="73.3333%"/>
```

header(`grdMain_head_r0_c0/c1/c2`)와 body(`CODE`/`grdMain_body_r0_c1`/`TYPE`)의 동일 logical
column이 항상 같은 percent 값을 가짐을 실측 확인(`GRID_HEADER_BODY_FOOTER_WIDTH_CONSISTENCY =
PASS`). footer(colspan=3)의 `73.3333%`는 `16.6667+36.6667+20`의 합과 정확히 일치.

### 영향 범위

corpus 149개 화면 전체 변환 성공 149/149, 실제 diff 발생 3개 파일(Grid Format을 가진 화면
전체와 일치 -- `ComponentMethodConversion.xml`, `GridAdvancedPhase3.xml`,
`UnsupportedFeatures.xml`), 나머지 133개 XML byte-identical. 3개 파일의 diff는 전부
`w2:column width` attribute 값 변경뿐이며, id/QName/class/colSpan/readOnly/inputType 등 다른
속성은 전혀 변경되지 않았다(line-by-line diff 확인).

### 회귀 결과

| 항목 | 결과 |
|---|---|
| 컴파일(clean build) | 0 errors |
| 전체 corpus 변환 | 149/149 성공 |
| XML parse | 136/136 well-formed |
| standalone JS | 15/15(무변경) |
| Phase1 SHA | 2/2 PASS(무변경) |
| `SOURCE_TO_TARGET_ID_MAP_EXPECTED_ONLY` | PASS(403/403 key, diff 0) |
| `w2:gridView`/`wq_gvw` | 3/3 무변경 |
| invariant class/QName(`btn_cm`/`wq_gvw`) | 전부 무변경 |
| `grp_resultArea`/`grp_main` width | 135/136(무변경, 이번 라운드 미접촉) |
| `GRID_COLUMN_INVALID_WIDTH_COUNT` | 0(NaN/Infinity/음수 없음) |
| 실제 diff 발생 XML | 3/136(Grid Format 보유 화면 전체와 일치), 나머지 133개 byte-identical |
| diff 내용 | 전부 `w2:column width` 값 변경만 |

### Completion Gates

`GRID_COLUMN_NATIVE_EVIDENCE = EVIDENCE_INSUFFICIENT`. `GRID_COLUMN_WIDTH_SEMANTIC =
EXPERIMENTAL`. `GRID_COLUMN_RATIO_CALCULATION = PASS`(3건 전부 역산 일치). `GRID_HEADER_BODY_
FOOTER_WIDTH_CONSISTENCY = PASS`(공통 `columnPercents` 배열 재사용으로 구조적 보장).
`GRID_COLUMN_NORMALIZED_COUNT = 3`. `GRID_COLUMN_PIXEL_FALLBACK_COUNT = 0`(corpus 실사례
없음, 로직 경로는 구현됨). `GRID_COLUMN_UNRESOLVED_COUNT = 0`(corpus 실사례 없음, 로직 경로는
구현됨). `GRID_COLUMN_INVALID_WIDTH_COUNT = 0`. `GRID_QNAME_PRESERVED = PASS`(`w2:gridView`
무변경). `GRID_CLASS_PRESERVED = PASS`(`wq_gvw` 무변경). `STRUCTURE_TOPOLOGY_PRESERVED = PASS`
(header/body/footer/row/column 구조 자체 무변경, width 값만 변경). `UNEXPECTED_GENERATED_DIFF
= 0`(3개 파일 diff 전부 의도한 width 값 변경으로 설명됨).

## Status

`[GridFormatConverter] resolveColumnPercents`(신규 함수), `convert`/`calculateCellWidth`/
`getSingleColumnWidth`/`appendHeader`/`appendBody`/`appendFooter`/
`appendSynthesizedDatasetBody`/`appendPlaceholderColumn`/`applyCellGeometry`(기존 함수 수정,
파라미터 추가만) -- `STATIC_VERIFIED`(compile/corpus 변환/canonical map/invariant/3개 대상
파일 line-by-line diff + 역산 검증 완료). `STUDIO_DESIGN_VERIFIED`는 선언하지 않음 -- 사용자의
실제 폐쇄망 Studio 확인 필요(`STUDIO_DESIGN_REQUIRED`, native evidence가 없는 실험적 candidate
이므로 특히 중요).

최종 `GRID_COLUMN_WIDTH = FIX_CANDIDATE` / `STATIC_VERIFIED` / `STUDIO_DESIGN_REQUIRED`. 이번
candidate는 native evidence 없이 구현된 실험적 시도이므로, 사용자가 폐쇄망에서 실제 렌더링을
확인한 결과에 따라 되돌릴 수 있어야 한다(모든 변경이 `columnPercents == null`이면 기존 코드
경로와 완전히 동일하게 동작하도록 설계되어, 되돌림도 이 조건 분기 하나만 제거하면 되는 낮은
리스크 구조).

---

## 후속 라운드 -- Percentage 0.5% 단위 일괄 정규화

### 배경

지금까지 여러 라운드에 걸쳐 percentage geometry가 소수점 4자리(`formatPercent`, trailing
zero 제거) precision으로 생성되고 있었다(예: `6.0345%`, `98.7069%`). 이번 라운드는 사용자
요청에 따라 모든 percentage 출력을 가장 가까운 0.5% 단위로 반올림하고, 최종 문자열을 항상
`N.0%` 또는 `N.5%` 형태로 통일한다. 계산 기준(parent/basis)이나 구조는 전혀 재설계하지
않는다 -- formatting/precision 단계만 교체한다.

### 1. 기존 percentage 생성 위치 전수 조사

`src/main/java` 전체에서 `"%"` 리터럴을 직접 붙이는 지점과 percentage formatter 호출 지점을
전수 검색했다:

- `"%"` 문자열 리터럴을 직접 붙이는 곳: `[ComponentLayoutConverter] formatPercent`의
  `return bd.toPlainString() + "%";` **단 한 곳**뿐(`grep -rn '"%"' src/main/java` 확인).
- 모든 percentage 계산은 `[ComponentLayoutConverter] formatPercent(double)`를 거친다 -- 별도
  `Math.round`/`DecimalFormat`/개별 rounding 로직은 어디에도 없음(`grep -rn "Math.round|
  DecimalFormat|BigDecimal" src/main/java` 확인, `BigDecimal` 사용처는 `formatPercent` 내부
  1곳뿐).
- `formatPercent` 호출부(`PERCENT_FORMATTER_CALLSITE_COUNT = 9`):
  1. `[ComponentLayoutConverter] buildPercentComponentStyle` -- left/top/width/height(일반
     component, Div Group/Grid Group 등)
  2. `[ComponentLayoutConverter] buildTableRowStyle` -- Table row height
  3. `[ComponentLayoutConverter] buildTableCellStyle` -- Table cell width
  4. `[GridFormatConverter] calculateCellWidth`(percentFormatter 경유) -- Grid column colspan
  5. `[GridFormatConverter] getSingleColumnWidth`(percentFormatter 경유) -- Grid column 단일
  6. `[GridFormatConverter] resolveColumnPercents`(percentFormatter 경유, 로그 메시지만)

- 리터럴 `100%`(계산이 아니라 구조 상수로 직접 박혀 있던 곳, `PERCENT_FORMATTER_BYPASS_COUNT`
  기준 이번 라운드 전 상태로는 우회 지점): `[ComponentLayoutConverter] buildRootStyle`(dead
  code, 호출부 없음), `buildTableRowStyle`/`buildTableCellStyle`(구조적 100% 부분), `[ComponentLayoutConverter] buildMainAreaStyle`(`grp_main` width),
  `[WebSquareGenerator] appendBody`(`grp_resultArea` width), `convertChildren`(Grid Group
  내부 `w2:gridView` fill), `convertLayoutAsTable`(table wrapper width), `convertTab`
  (`w2:content` fill) -- 총 7곳. 전부 `formatPercent(100.0)` 호출로 교체해 단일 formatter로
  통제한다.
- **예외 1곳**: `[XPlatformProjectConverter] writeTabRuntimeResources`의 정적 placeholder
  `runtime/xplatform-tab-empty.xml`(`<w2:group id="grp_main" style="position:relative;
  width:100%;height:100%;">`) -- 이전 여러 라운드에서 이미 "실제 변환 화면이 아닌 무관
  hardcoded placeholder"로 확인된 파일이며, 이 문자열은 percentage **계산**의 결과가 아니라
  Java 소스에 직접 박힌 별도 XML 문서 literal이다. formatter 관리 대상(계산된 percentage
  geometry)이 아니라고 판단해 이번 라운드에서 건드리지 않았다 -- 근거: (a) 이 파일은 실제
  변환된 XPlatform 화면과 무관, (b) 과거 모든 라운드의 root/table/grid 감사에서 일관되게
  제외 대상으로 취급됨, (c) 건드리면 오히려 "percentage geometry 정규화"라는 이번 범위를
  넘어 무관 파일까지 diff를 만들게 됨.

### 2. 공통 formatter 수정(신규 함수 없음)

기존 `[ComponentLayoutConverter] formatPercent` 하나만 generic하게 수정했다. 신규 함수는
추가하지 않았다(9개 callsite가 이미 전부 이 함수를 거치고 있어 그대로 재사용 가능).

BEFORE:
```java
/**
 * percentage geometry 값을 deterministic하게 포맷한다(소수점 4자리에서 반올림, trailing zero
 * 제거). fixture별 precision을 두지 않고 모든 Production output에 동일 규칙을 적용한다.
 * 예: 25.0000% -> 25%, 12.5000% -> 12.5%.
 */
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

AFTER:
```java
/**
 * percentage geometry 값을 가장 가까운 0.5% 단위로 반올림하고, 항상 소수점 첫째 자리까지
 * "N.0%" 또는 "N.5%" 형태로 포맷한다(PERCENT_FORMAT_NORMALIZATION 라운드). fixture별
 * 예외 없이 모든 Production percentage output에 동일 규칙을 적용한다.
 * 예: 6.0345% -> 6.0%, 12.76% -> 13.0%, 98.7069% -> 98.5%.
 */
public String formatPercent(double value) {
    java.math.BigDecimal doubled = java.math.BigDecimal.valueOf(value)
            .multiply(java.math.BigDecimal.valueOf(2));
    java.math.BigDecimal roundedDoubled =
            doubled.setScale(0, java.math.RoundingMode.HALF_UP);
    java.math.BigDecimal rounded = roundedDoubled.divide(java.math.BigDecimal.valueOf(2));
    return rounded.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString() + "%";
}
```

원리: `raw x 2`를 정수로 반올림(HALF_UP)한 뒤 다시 2로 나누면 정확히 가장 가까운 0.5 배수가
되고(부동소수 비교 없이 BigDecimal 정수 반올림만 사용해 deterministic), 마지막에 소수 1자리로
`setScale`해 항상 `N.0`/`N.5` 형태를 보장한다. 별도 unit 검증(18개 케이스, 사용자 제시 예시
전부 + 경계값 4개 `6.24/6.25/6.74/6.75`)에서 전부 `PASS`(상세: 5번 섹션).

**리터럴 100% 우회 지점 7곳**을 전부 `formatPercent(100.0)` 호출로 교체(`buildRootStyle`,
`buildTableRowStyle`, `buildTableCellStyle`, `buildMainAreaStyle`, `appendBody`
(`grp_resultArea`), `convertChildren`(Grid Group 내부 fill), `convertLayoutAsTable`
(table wrapper), `convertTab`(`w2:content`)) -- `formatPercent(100.0)`는 항상 `"100.0%"`를
반환하므로 이 교체 자체는 순수 텍스트 치환이며 값이 바뀌지 않는 경우는 없다(전부 `100%` ->
`100.0%`로 정확히 1건씩 변경).

### 3. formatPercent 단위 검증(18건)

| 입력 | 기대값(사용자 제시) | 실제 결과 | 판정 |
|---|---|---|---|
| 0 | 0.0% | 0.0% | PASS |
| 0.24 | 0.0% | 0.0% | PASS |
| 0.25 | 0.5% | 0.5% | PASS |
| 0.49 | 0.5% | 0.5% | PASS |
| 4.2105 | 4.0% | 4.0% | PASS |
| 4.26 | 4.5% | 4.5% | PASS |
| 6.0345 | 6.0% | 6.0% | PASS |
| 6.27 | 6.5% | 6.5% | PASS |
| 12.74 | 12.5% | 12.5% | PASS |
| 12.76 | 13.0% | 13.0% | PASS |
| 25 | 25.0% | 25.0% | PASS |
| 98.7069 | 98.5% | 98.5% | PASS |
| 99.76 | 100.0% | 100.0% | PASS |
| 100 | 100.0% | 100.0% | PASS |
| 6.24(경계) | 6.0% | 6.0% | PASS |
| 6.25(경계) | 6.5% | 6.5% | PASS |
| 6.74(경계) | 6.5% | 6.5% | PASS |
| 6.75(경계) | 7.0% | 7.0% | PASS |

18/18 PASS(스크래치 디렉토리 독립 실행 `FormatPercentTest.java`, Production
`ComponentLayoutConverter.formatPercent`를 직접 호출).

### Full Unified Diff

[analysis/git-baseline-vs-candidate-production.diff](git-baseline-vs-candidate-production.diff)
(누적, 이번 라운드분은 `ComponentLayoutConverter.java`/`WebSquareGenerator.java` 마지막
hunk들 -- `formatPercent` 본체 교체 + 리터럴 `100%` 7곳 치환).

Caller: 9개 callsite(위 1번 섹션) 전부 무변경(파라미터/호출 방식 동일, 반환 문자열 precision만
달라짐). Callee: `BigDecimal.multiply`/`setScale`/`divide`(JDK 표준 API, 신규 helper 없음).

### Generated XML BEFORE/AFTER(대표 4건, 전부 실제 corpus 값)

**A. Div Group**(`Form/ControlPropertyMatrix.xml`, 실제 corpus 값):
```
BEFORE: style="position:absolute;left:1.1111%;top:1.5385%;width:11.1111%;height:3.6923%;color:#112233;background:#eeeeee;"
AFTER:  style="position:absolute;left:1.0%;top:1.5%;width:11.0%;height:3.5%;color:#112233;background:#eeeeee;"
```

**B. Table Row/Cell**(`Form/Main/TabExternalRelativePath.xml`, `divWrap`):
```
BEFORE: <xf:group id="divWrap_layoutTableRow0" style="width:100%;height:89.4737%;" tagname="tr">
            <xf:group class="w2tb_td" id="divWrap_layoutTableRow0Col0" style="width:94.8276%;height:100%;" tagname="td">
AFTER:  <xf:group id="divWrap_layoutTableRow0" style="width:100.0%;height:89.5%;" tagname="tr">
            <xf:group class="w2tb_td" id="divWrap_layoutTableRow0Col0" style="width:95.0%;height:100.0%;" tagname="td">
```

**C. Grid Group**(`Form/ComponentMethodConversion.xml`, `grd_gridGroup`):
```
BEFORE: <xf:group id="grd_gridGroup" style="position:absolute;left:1.6667%;top:16.6667%;width:50%;height:40%;">
            <w2:gridView class="wq_gvw" ... style="width:100%;height:100%;">
AFTER:  <xf:group id="grd_gridGroup" style="position:absolute;left:1.5%;top:16.5%;width:50.0%;height:40.0%;">
            <w2:gridView class="wq_gvw" ... style="width:100.0%;height:100.0%;">
```

**D. Grid column**(`Form/GridAdvancedPhase3.xml`, `grdMain`):
```
BEFORE: <w2:column id="grdMain_head_r0_c0" ... width="16.6667%"/>
        <w2:column id="grdMain_head_r0_c1" ... width="36.6667%"/>
        <w2:column id="grdMain_head_r0_c2" ... width="20%"/>
AFTER:  <w2:column id="grdMain_head_r0_c0" ... width="16.5%"/>
        <w2:column id="grdMain_head_r0_c1" ... width="36.5%"/>
        <w2:column id="grdMain_head_r0_c2" ... width="20.0%"/>
```

### 영향 범위

corpus 149개 화면 전체 변환 성공 149/149, 136개 XML 중 135개에서 diff 발생(전부 percentage
precision 변경), 1개(`runtime/xplatform-tab-empty.xml`, 계산되지 않은 고정 placeholder)만
무변경. percentage 문자열을 제외한 나머지 내용은 135개 전체에서 byte-identical함을 정규식
치환 후 diff로 확인(`PERCENT_BASIS_CHANGED = 0` 근거).

### 회귀 결과

| 항목 | 결과 |
|---|---|
| 컴파일(clean build) | 0 errors |
| 전체 corpus 변환 | 149/149 성공 |
| XML parse | 136/136 well-formed |
| standalone JS | 15/15(무변경) |
| Phase1 SHA | 2/2 PASS(무변경) |
| `SOURCE_TO_TARGET_ID_MAP_EXPECTED_ONLY` | PASS(403/403 key, diff 0) |
| invariant class/QName(`btn_cm`/`wq_gvw`) | 전부 무변경(12/3) |
| 실제 diff 발생 XML | 135/136(placeholder 1개 제외) |
| percent 제거 후 diff | 0/136(전수 확인, percentage 문자열 외 전부 byte-identical) |
| `formatPercent` unit test | 18/18 PASS |

### Completion Gates(corpus 실측)

`PERCENT_FORMAT_RULE = PASS`. `PERCENT_BASIS_CHANGED = 0`(구조/basis 완전 무변경, 정규식
치환 후 diff 0으로 실증). `INVALID_PERCENT_PRECISION_COUNT = 2`(전부 예외로 확인된
`runtime/xplatform-tab-empty.xml`의 고정 placeholder literal -- 실제 변환 결과 아님, 근거는
위 1번 섹션). `PERCENT_FORMATTER_BYPASS_COUNT = 0`(이번 라운드 이후 -- 이전에 있던 리터럴
`100%` 7곳 전부 formatter 경유로 교체 완료, 위 placeholder 1곳만 남았고 그 이유를 명시).
`NaN% = 0`, `Infinity% = 0`. `PERCENT_DOT_ZERO_COUNT = 659`, `PERCENT_DOT_FIVE_COUNT = 368`
(정규식 `^-?\d+\.(0|5)%$` 기준 전수 스캔, 총 1029건 중 1027건이 이 형식 준수, 2건은 위
placeholder 예외).

`PERCENT_ROUNDING_SUM_DRIFT_COUNT = 1`(corpus 실측): `Form/GridAdvancedPhase3.xfdl`의
`grdMain` Grid에서, header/body의 개별 column을 각각 반올림한 값의 합(`16.5% + 36.5% + 20.0%
= 73.0%`)과, footer(colSpan=3)가 raw percent 합계를 한 번에 반올림한 값(`73.3333% ->
73.5%`)이 0.5% 차이가 난다(원본 raw 값 `73.3334%` 자체는 동일 -- 반올림을 개별로 하는지
합산 후 하는지에 따라 비선형적으로 갈리는 것이며, 계산 오류가 아니다). 규칙 12에 따라 마지막
column을 임의로 보정하지 않았고, 이 문서에 drift로 명시적으로 기록만 한다.

## Status

`[ComponentLayoutConverter] formatPercent`(기존 함수 수정, 신규 함수 없음),
`buildRootStyle`/`buildTableRowStyle`/`buildTableCellStyle`/`buildMainAreaStyle`(리터럴
`100%` 치환만), `[WebSquareGenerator] appendBody`/`convertChildren`/`convertLayoutAsTable`/
`convertTab`(리터럴 `100%` 치환만) -- `STATIC_VERIFIED`(compile/corpus 변환/canonical
map/invariant/percent-strip diff 0/unit test 18건 전부 확인 완료).

최종 `PERCENT_FORMAT_NORMALIZATION = FIX_CANDIDATE` / `STATIC_VERIFIED`. 기존 실제 Studio
실패 상태(`STUDIO_DESIGN_FAILED`/`STUDIO_DESIGN_REPRODUCED`)가 이 rounding/formatting
변경만으로 자동 해결됐다고 선언하지 않는다 -- `STUDIO_DESIGN_REQUIRED` 유지.

## 후속 라운드 -- XPlatform Visual Parity Quick Fix + Percentage Precision 통일

### 배경 / 가설 검증

실제 폐쇄망 재현(STUDIO_DESIGN_FAILED/STUDIO_DESIGN_REPRODUCED)의 우선 원인 후보로 A~E
5가지를 제시받았다. Production 코드(`ComponentLayoutConverter`/`WebSquareGenerator`)를
직접 추적한 결과:

- A(잘못된 parent coordinate basis): `resolveLayoutBasis`/`resolveFormBasis`는 이미
  `PERCENT_GEOMETRY_PARENT = IMMEDIATE_SOURCE_CONTAINER` 원칙대로 동작 중이었다(이전
  라운드에 이미 반영, 이번 라운드 실측 trace로 재확인만 함 -- 아래 trace 참고).
- B(서로 다른 sibling Div를 Table/Row/Cell로 잘못 재해석): **실제 원인으로 확인됨.**
  `convertLayoutAsTable`의 `classifyLayoutGeometry`는 XPlatform `Layout`의 직계 자식을
  "겹치지 않으면 모두 table row/cell"로 분류한다 -- 이 판정은 자식의 **소스 타입을 전혀
  구분하지 않는다.** 그 결과 Label/Edit 같은 leaf 컴포넌트 grid뿐 아니라, 그 자체로 독립
  좌표계를 가진 `Div`/`GroupBox`/`Tab` 같은 container child까지 table cell로 강제
  편입되어 `includePosition=false`로 원래 left/top을 잃고 cell의 structural placement로
  대체됐다(`NestedContainer.xfdl`의 `GroupBox grpA`, `TabExternalRelativePath.xfdl`의
  `Tab tabNested`로 실측 재현, 아래 BEFORE/AFTER 참고).
- C(nested component에 Form 기준 percentage 재사용): 이전 라운드(NESTED_PERCENT_DOUBLE_
  SCALING fix)에서 이미 해결, 이번 라운드 재검증 결과 회귀 없음.
- D(source overlap/stacking 손실): `hasOverlap`이 겹치는 경우 이미 `ABSOLUTE_LAYOUT_
  FALLBACK`으로 절대좌표를 보존 중(회귀 없음). 이번 corpus에는 실제 겹치는 sibling 사례가
  없어(`OVERLAPPING_SIBLING_DIV_COUNT=0`, 아래 참고) 실측 재확인은 못 했다.
- E(container hierarchy flattening): B와 동일 root cause(위 참고).

따라서 이번 라운드는 **B를 직접 수정**했고, position:absolute는 전역 유지
(`ABSOLUTE_POSITIONING = REQUIRED_FOR_VISUAL_FIDELITY`, 관련 코드 무변경).

---

### 변경 1 -- `[WebSquareGenerator] convertLayoutAsTable` / 신규 `hasContainerChild`

**목적**: container 컴포넌트(Div/GroupBox/PopupDiv/Tab/Tabpage)가 Layout의 직계 자식으로
있으면 table row/cell 구조로 병합하지 않고 원래 절대좌표로 보존한다
(`TABLE_CONVERSION_SEMANTIC_MISMATCH`). Label/Edit 등 leaf 컴포넌트만으로 구성된 실제
검증된 native table 사례(BCI01M0000 evidence)는 이 override 대상이 아니므로 무변경.

**BEFORE**:
```java
List<Element> children = directElementChildren(layout);
boolean isRootFormLayout = parentPath.length() == 0;
String classification = isRootFormLayout
        ? "ROOT_FORM_LAYOUT_NOT_A_TABLE_TARGET"
        : layoutConverter.classifyLayoutGeometry(children);
double[] basis = layoutConverter.resolveLayoutBasis(layout);
```

**AFTER**:
```java
List<Element> children = directElementChildren(layout);
boolean isRootFormLayout = parentPath.length() == 0;
String classification = isRootFormLayout
        ? "ROOT_FORM_LAYOUT_NOT_A_TABLE_TARGET"
        : layoutConverter.classifyLayoutGeometry(children);
// XPLATFORM_VISUAL_PARITY: Div/GroupBox/PopupDiv/Tab/Tabpage처럼 그 자체로 독립된
// 좌표계를 가진 container child는 table row/cell 구조(structural placement, position
// 제거)로 병합하지 않는다 -- 원래 XPlatform sibling Div의 left/top/width/height와
// overlap 관계를 그대로 보존하기 위해 절대좌표 pass-through로 처리한다
// (TABLE_CONVERSION_SEMANTIC_MISMATCH). label/input 등 leaf component만으로 구성된
// Layout(실제 검증된 native table 사례)은 이 override 대상이 아니다.
if (!isRootFormLayout
        && "TABLE_LAYOUT_HIGH_CONFIDENCE".equals(classification)
        && hasContainerChild(children)) {
    classification = "TABLE_CONVERSION_SEMANTIC_MISMATCH";
}
double[] basis = layoutConverter.resolveLayoutBasis(layout);
```

신규 private 함수:
```java
/** children 중 하나라도 container 컴포넌트(Div/GroupBox/PopupDiv/Tab/Tabpage 등)인지 확인. */
private boolean hasContainerChild(List<Element> children) {
    for (Element child : children) {
        if (isContainerComponent(getSourceTagName(child))) {
            return true;
        }
    }
    return false;
}
```

`classification`이 `TABLE_LAYOUT_HIGH_CONFIDENCE`가 아니면 이미 존재하던 아래 fallback
분기가 그대로 처리한다(무수정, 재사용):
```java
if (!"TABLE_LAYOUT_HIGH_CONFIDENCE".equals(classification)) {
    convertChildren(
            out, layout, targetParent, parentPath, analysis, depth, null,
            basisWidth, basisHeight, true);
    return;
}
```
(`includePosition=true`로 재호출 -- 각 container child가 자신의 실제 left/top/width/
height를 그대로 percentage로 변환, overlap 여부와 무관하게 원래 좌표 유지)

**Caller**: `convertChildren`(Layout 태그를 만나면 호출) -- 무변경.
**Callee**: `isContainerComponent`(기존), `getSourceTagName`(기존), `layoutConverter.
classifyLayoutGeometry`(기존, ComponentLayoutConverter -- 무변경).

**Generated XML BEFORE** (`NestedContainer.xml`, `Form/NestedContainer.xfdl`의 `divA` ->
`Layout(300x150)` -> `GroupBox grpA`, source: `left=10 top=10 width=250 height=100`):
```xml
<w2:group id="divA" style="position:absolute;left:4.0%;top:6.5%;width:60.0%;height:50.0%;">
    <xf:group class="w2tb_tb" id="divA_layoutTable" style="width:100.0%;" tagname="table">
        <xf:group id="divA_layoutTableRow0" style="width:100.0%;height:66.5%;" tagname="tr">
            <xf:group class="w2tb_td" id="divA_layoutTableRow0Col0" style="width:83.5%;height:100.0%;" tagname="td">
                <w2:group id="divA_grpA" style="width:100.0%;height:100.0%;" value="Group">
                    <xf:input id="divA_grpA_edt" style="position:absolute;left:2.0%;top:5.0%;width:40.0%;height:24.0%;"/>
                </w2:group>
            </xf:group>
        </xf:group>
    </xf:group>
```

**Generated XML AFTER**:
```xml
<w2:group id="divA" style="position:absolute;left:4.0%;top:6.7%;width:60.0%;height:50.0%;">
    <w2:group id="divA_grpA" style="position:absolute;left:3.3%;top:6.7%;width:83.3%;height:66.7%;" value="Group">
        <xf:input id="divA_grpA_edt" style="position:absolute;left:1.7%;top:3.3%;width:33.3%;height:16.0%;"/>
    </w2:group>
```
`grpA`의 source(left=10,top=10 / basis 300x150)를 그대로 percent 환산하면
left=10/300*100=3.33%, top=10/150*100=6.67% -- AFTER 값(3.3%/6.7%)과 일치. BEFORE는
table cell structural placement로 이 값이 소실되고 0,0(flow) + 100%/83.5% 강제 채움으로
대체돼 있었다.

**영향 output 수**: 2개 파일(`Form/NestedContainer.xml`, `Form/Main/TabExternalRelativePath.xml`)
-- 136개 corpus 파일 전체를 percent-stripped diff로 대조해 이 2개만 구조 변경, 나머지
134개는 percent 텍스트만 변경(아래 변경 2 참고).

**Regression**: 149/149 변환 성공, XML well-formed 136/136, id-map(source->target 전체
라인) round11 vs round12 diff 0(신규 wrapper 제거로 synthetic id 2개가 줄었을 뿐 실제
컴포넌트 target id는 전부 동일), classification 카운트 TABLE_LAYOUT_HIGH_CONFIDENCE=3,
TABLE_CONVERSION_SEMANTIC_MISMATCH=2(신규), ABSOLUTE_LAYOUT_FALLBACK=0,
ROOT_FORM_LAYOUT_NOT_A_TABLE_TARGET=121, UNRESOLVED_LAYOUT=2.

**Status**: `FIX_CANDIDATE` / `STATIC_VERIFIED`.

---

### 변경 2 -- `[ComponentLayoutConverter] formatPercent`

**목적**: `PERCENT_ROUNDING` 정책을 `NEAREST_0.5_PERCENT`에서 `ONE_DECIMAL_PLACE`로
교체(둘째 자리 일반 반올림, 첫째 자리까지 유지). 기존 공통 formatter 하나만 교체, 신규
함수 추가 없음. basis/parent 계산은 전혀 건드리지 않음(`PERCENT_BASIS_CHANGED_BY_
PRECISION_UPDATE = 0`).

**BEFORE**:
```java
public String formatPercent(double value) {
    java.math.BigDecimal doubled = java.math.BigDecimal.valueOf(value)
            .multiply(java.math.BigDecimal.valueOf(2));
    java.math.BigDecimal roundedDoubled =
            doubled.setScale(0, java.math.RoundingMode.HALF_UP);
    java.math.BigDecimal rounded = roundedDoubled.divide(java.math.BigDecimal.valueOf(2));
    return rounded.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString() + "%";
}
```

**AFTER**:
```java
public String formatPercent(double value) {
    return java.math.BigDecimal.valueOf(value)
            .setScale(1, java.math.RoundingMode.HALF_UP)
            .toPlainString() + "%";
}
```

**Caller**: 9개 계산 callsite(Div/Button/nested child/Grid Group/Table Row/Table Cell/Grid
column) + 리터럴 100%였다가 이전 라운드에 이미 이 함수로 통합된 7개 callsite, 전부
무수정 재사용(이번 라운드에서 formatter의 내부 구현만 교체, callsite는 하나도 건드리지
않음). `PERCENT_FORMATTER_UNIFIED = PASS`, `PERCENT_FORMATTER_BYPASS_COUNT = 0`(9월+7곳
전부 formatPercent 경유 -- grep으로 `%` 리터럴을 직접 만드는 다른 지점 없음을 재확인).

**Generated XML BEFORE/AFTER 예시 (Button, 실제 corpus)**:
`Form/ControlPropertyMatrix.xfdl`의 `Button btn`(source `left=120 top=10 width=100
height=24`, basis = root Form Layout 900x650):
- raw: left=13.3333%, top=1.5385%, width=11.1111%, height=3.6923%
- BEFORE(0.5%): `left:13.5%;top:1.5%;width:11.0%;height:3.5%;`
- AFTER(0.1%): `left:13.3%;top:1.5%;width:11.1%;height:3.7%;`
- px roundtrip(basis 900x650 기준): BEFORE left=121.5px(오차 1.5px)/width=99px(오차1px)/
  height=22.75px(오차1.25px) vs AFTER left=119.7px(오차0.3px)/width=99.9px(오차0.1px)/
  height=24.05px(오차0.05px)

**영향 output 수**: 134/136 파일(percent 텍스트만 변경, 구조 무변경 -- 나머지 2개는 변경
1과 중복이라 이미 포함).

**Regression**: `INVALID_PERCENT_PRECISION_COUNT = 2`(전부 `runtime/xplatform-tab-empty.xml`
placeholder, 과거 모든 라운드와 동일 문서화된 예외), `NaN%=0`, `Infinity%=0`,
`PERCENT_BASIS_CHANGED_BY_PRECISION_UPDATE = 0`(변경 1로 인한 2개 파일을 제외한 134개
파일에서 percent 텍스트 제거 후 byte-identical 실증).

**Status**: `PERCENT_ROUNDING_POLICY = ONE_DECIMAL_PLACE` / `FIX_CANDIDATE` /
`STATIC_VERIFIED`.
