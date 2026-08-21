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
