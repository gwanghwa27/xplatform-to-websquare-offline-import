package com.example.xfdltracker.converter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Phase 2 UI 레이아웃 변환기.
 *
 * XPlatform 컴포넌트의 위치/크기를 WebSquare용 CSS로 변환한다.
 * 확정적으로 계산 가능한 좌표만 변환하고, 상대 참조식처럼 임의 계산이
 * 위험한 값은 변환하지 않고 생성기의 TODO 로그로 확인하도록 한다.
 */
public class ComponentLayoutConverter {

    private static final Pattern NUMBER = Pattern.compile("[-+]?\\d+(?:\\.\\d+)?");
    private static final Pattern NUMBER_WITH_UNIT = Pattern.compile(
            "([-+]?\\d+(?:\\.\\d+)?)(px|%)?",
            Pattern.CASE_INSENSITIVE);

    /** source가 left/top/width/height 등 위치/크기 속성을 하나라도 가지는지 여부. */
    public boolean hasGeometry(Element source) {
        return resolveGeometry(source).hasAnyPositionOrSize();
    }

    /** 일반 UI 컴포넌트의 CSS style 문자열을 생성한다(px, position 포함). */
    public String buildComponentStyle(Element source) {
        return buildComponentStyle(source, true);
    }

    /**
     * 일반 UI 컴포넌트의 CSS style(px) 문자열을 생성한다. {@code includePosition=false}면
     * Table 셀처럼 structural placement가 이미 위치를 결정하는 경우 불필요한
     * {@code position:absolute}/{@code left}/{@code top}을 생성하지 않는다(percentage 변환이
     * unresolved라서 px로 fallback하는 경우에도 20번 규칙을 동일하게 지킨다).
     */
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

    /**
     * 루트 group의 style을 생성한다. Form/Layout 크기를 반영하여
     * XPlatform 절대좌표 기준으로 자식 컴포넌트를 배치할 수 있게 한다.
     */
    public String buildRootStyle(Document source) {
        StringBuilder style = new StringBuilder();
        // position:relative/overflow:hidden 미emit -- Studio 실측 확정(ISSUE-20260818-001).
        // 현재 이 메서드의 호출부는 없음(v6 Design Structure 라운드에서 grp_content 제거로 dead
        // code화, 정리는 NEXT_ROUND_CANDIDATE).

        Geometry geometry = findFormGeometry(source);
        if (geometry != null) {
            appendCssLength(style, "width", geometry.width);
            appendCssLength(style, "height", geometry.height);
        }

        if (style.indexOf("width:") < 0) {
            style.append("width:").append(formatPercent(100.0)).append(";");
        }
        if (style.indexOf("height:") < 0) {
            style.append("height:").append(formatPercent(100.0)).append(";");
        }
        return style.toString();
    }

    /**
     * percentage geometry 값을 소수점 둘째 자리에서 일반 반올림해 소수점 첫째 자리까지
     * "N.N%" 형태로 포맷한다(XPLATFORM_VISUAL_PARITY 라운드, PERCENT_ROUNDING =
     * ONE_DECIMAL_PLACE -- 기존 NEAREST_0.5_PERCENT 규칙 폐기). fixture별 예외 없이 모든
     * Production percentage output에 동일 규칙을 적용한다.
     * 예: 4.2105% -&gt; 4.2%, 6.27% -&gt; 6.3%, 98.7069% -&gt; 98.7%.
     */
    public String formatPercent(double value) {
        return java.math.BigDecimal.valueOf(value)
                .setScale(1, java.math.RoundingMode.HALF_UP)
                .toPlainString() + "%";
    }

    /**
     * percentage 기준(basis)은 항상 {@code PERCENT_GEOMETRY_PARENT = IMMEDIATE_SOURCE_CONTAINER}
     * 원칙에 따라, 자식을 담고 있는 XPlatform {@code Layout} 엘리먼트 자신의 width/height(px)다
     * (Form root Layout이든 Div 내부 Layout이든 동일 방식 -- Div의 Group, Div/Layout 내부
     * component, Layout-as-Table의 row/column 크기 모두 이 basis 하나로 계산한다). width/height가
     * 없거나 파싱 불가/0 이하이면 null(호출부가 {@code PERCENT_GEOMETRY_UNRESOLVED}로 처리).
     */
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

    /**
     * STT00030 계열 실제 업무 화면 Studio 실패(STUDIO_DESIGN_FAILED) root cause fix: XPlatform
     * source는 component가 {@code Form} 바로 아래(Layouts/Layout wrapper 없이) 있거나, 최상위
     * {@code Layout} 자신에 width/height가 없는 경우가 실존한다 -- 이 경우 {@link
     * #resolveLayoutBasis}가 첫 {@code Layout}을 만날 때까지(또는 영원히) basis를 못 얻어
     * 전체 화면이 PIXEL_GEOMETRY_FALLBACK으로 떨어지며, 그 px 좌표가 grp_content(이번 라운드
     * 이전까지 존재하던, 폭 자체를 px로 고정해주던 wrapper) 없이 렌더링돼 Design/Preview에서
     * 좌측 상단 좁은 영역으로 collapse한다({@code SOURCE_PIXEL_GEOMETRY_REMAINS_IN_GENERATED_
     * STRUCTURE}). {@code findFormGeometry}(기존, {@link #buildMainAreaStyle}이 재사용 중인
     * Form-우선/Layout-차선 fallback)를 재사용해 Form 전체를 초기/최후 basis로 제공한다 --
     * 특정 화면의 width/height를 하드코딩하지 않고, Form 자신의 실제 선언값만 사용한다.
     */
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

    /**
     * source의 left/top/width/height를 basis(immediate source Layout의 width/height) 기준
     * percentage style로 변환한다. {@code includePosition=false}면 left/top을 emit하지 않는다
     * (Table 셀 내부처럼 structural placement가 이미 위치를 결정하는 경우 -- 20번 규칙).
     * source 자체가 geometry를 전혀 갖지 않으면(hasAnyPositionOrSize=false) 그냥 visual style만
     * 반환한다(변환 대상이 아님, unresolved 아님). basis가 없거나(<=0) width/height(그리고
     * includePosition인 경우 left/top)를 확정적으로 읽을 수 없으면 null을 반환한다
     * (PERCENT_GEOMETRY_UNRESOLVED -- 호출부가 px fallback 여부를 결정).
     */
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

    /**
     * Table row wrapper(xf:group)의 style. row 안 셀들의 실제 top/height 분포(min top ~ max
     * bottom)로 row 자체의 세로 footprint를 구해 basisHeight 대비 비율로 변환한다(19번 규칙 --
     * source geometry 실비율 사용, 균등분할 금지). 좌우 offset은 structural placement(문서 순서
     * stacking)로 대체하므로 emit하지 않는다(20번 규칙). 계산 불가면 null.
     */
    public String buildTableRowStyle(List<Element> row, double basisHeight) {
        if (basisHeight <= 0.0) {
            return null;
        }
        double rowHeight = resolveRowBasisHeight(row);
        if (rowHeight <= 0.0) {
            return null;
        }
        return "width:" + formatPercent(100.0) + ";height:"
                + formatPercent(rowHeight / basisHeight * 100.0) + ";";
    }

    /**
     * Table cell wrapper(xf:group)의 style. 셀 자신의 width를 basisWidth 대비 비율로 변환한다(19번
     * 규칙). height는 row를 100% 채운다(structural placement). 계산 불가면 null.
     */
    public String buildTableCellStyle(Element cell, double basisWidth) {
        if (basisWidth <= 0.0) {
            return null;
        }
        double cellWidth = resolveCellBasisWidth(cell);
        if (cellWidth <= 0.0) {
            return null;
        }
        return "width:" + formatPercent(cellWidth / basisWidth * 100.0) + ";height:"
                + formatPercent(100.0) + ";";
    }

    /**
     * NESTED_PERCENT_DOUBLE_SCALING fix: table cell 내부의 실제 XPlatform 컴포넌트는
     * cell/row 자신을 채우는 것이지, 원래 Div/Layout 전체 basis를 다시 기준으로 삼지 않는다
     * (PERCENT_GEOMETRY_PARENT = IMMEDIATE_GENERATED_CONTAINER -- 4번 규칙). {@link
     * #buildTableCellStyle}이 cell의 width를 "cell 자신의 source width / Layout basisWidth"로
     * 계산하는 것과 동일한 px 값을 여기서 반환해, 그 cell에 들어가는 컴포넌트의 percent 계산
     * 기준(basis)으로 재사용할 수 있게 한다. 현재 구조상 한 cell에는 정확히 1개의 XPlatform
     * component만 들어가며 cell의 width는 그 컴포넌트 자신의 width와 같으므로, 이 값을 basis로
     * 쓰면 컴포넌트의 width%는 자동으로 100%가 된다(하드코딩이 아니라 "컴포넌트 자신의 px 값 /
     * 컴포넌트 자신의 px 값"이라는 항등 계산의 결과 -- 여러 컴포넌트를 담는 cell로 확장되어도
     * 동일한 나눗셈 공식이 그대로 유효하다). 계산 불가면 -1.
     */
    public double resolveCellBasisWidth(Element cell) {
        if (cell == null) {
            return -1.0;
        }
        Geometry g = resolveGeometry(cell);
        ParsedLength width = isEmpty(g.width) ? null : parseLength(g.width);
        if (width == null || width.value <= 0.0) {
            return -1.0;
        }
        return width.value;
    }

    /**
     * NESTED_PERCENT_DOUBLE_SCALING fix: {@link #resolveCellBasisWidth}와 동일한 목적으로, row
     * 안 셀들의 실제 top/height 분포(min top ~ max bottom)로 row 자신의 세로 footprint(px)를
     * 구한다. {@link #buildTableRowStyle}의 rowHeight 계산과 동일 로직을 공유한다(중복 계산
     * 방지). 계산 불가면 -1.
     */
    public double resolveRowBasisHeight(List<Element> row) {
        if (row == null || row.isEmpty()) {
            return -1.0;
        }
        double minTop = Double.MAX_VALUE;
        double maxBottom = -Double.MAX_VALUE;
        for (Element cell : row) {
            Geometry g = resolveGeometry(cell);
            ParsedLength top = isEmpty(g.top) ? null : parseLength(g.top);
            ParsedLength height = isEmpty(g.height) ? null : parseLength(g.height);
            if (top == null || height == null) {
                return -1.0;
            }
            minTop = Math.min(minTop, top.value);
            maxBottom = Math.max(maxBottom, top.value + height.value);
        }
        double rowHeight = maxBottom - minTop;
        return rowHeight > 0.0 ? rowHeight : -1.0;
    }

    /**
     * XPlatform {@code Layout} 직계 자식들의 geometry(left/top/width/height)만으로 table topology
     * 변환 가능 여부를 generic하게 판정한다. Magic pixel tolerance는 사용하지 않는다 -- 정확히
     * 동일한 top 좌표값을 가진 자식만 같은 row로 묶는다(exact numeric equality).
     *
     * <p>v6 Design Structure + Table + Grid Group + Percentage Geometry Alignment 라운드에서
     * 판정 기준을 완화했다: row 수/column 수가 2 미만이거나(1-row/1-column) row마다 column 수가
     * 다르더라도(완전한 사각 grid가 아니더라도) 그 자체를 fallback 사유로 쓰지 않는다(row/column
     * clustering이 곧 table topology이며, {@link #buildTableRows}가 row별로 실제 존재하는 셀만
     * 배치하므로 임의 span을 만들지 않는다). fallback은 오직: 자식 geometry를 확정적으로 읽을 수
     * 없을 때({@code UNRESOLVED_LAYOUT}), 자식이 없을 때({@code UNRESOLVED_LAYOUT}), 자식끼리
     * 실제로 겹칠 때({@code ABSOLUTE_LAYOUT_FALLBACK} -- topology 계산 자체가 불가능/불안전)만
     * 발생한다.
     *
     * <p>반환값은 다음 3개 문자열 중 하나: {@code TABLE_LAYOUT_HIGH_CONFIDENCE},
     * {@code ABSOLUTE_LAYOUT_FALLBACK}, {@code UNRESOLVED_LAYOUT}.
     */
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

    /**
     * {@link #classifyLayoutGeometry}가 {@code TABLE_LAYOUT_HIGH_CONFIDENCE}를 반환한 경우에만
     * 호출한다. top 오름차순으로 정렬된 row 목록을, 각 row는 left 오름차순으로 정렬된 셀 목록으로
     * 반환한다.
     */
    public List<List<Element>> buildTableRows(List<Element> children) {
        List<CellGeometry> cells = resolveCellGeometries(children);
        if (cells == null) {
            return new ArrayList<List<Element>>();
        }
        Map<Double, List<CellGeometry>> byTop = groupByTop(cells);
        List<Double> tops = new ArrayList<Double>(byTop.keySet());
        java.util.Collections.sort(tops);

        List<List<Element>> rows = new ArrayList<List<Element>>();
        for (Double top : tops) {
            List<CellGeometry> row = byTop.get(top);
            java.util.Collections.sort(row, new java.util.Comparator<CellGeometry>() {
                public int compare(CellGeometry a, CellGeometry b) {
                    return Double.compare(a.left, b.left);
                }
            });
            List<Element> rowElements = new ArrayList<Element>();
            for (CellGeometry c : row) {
                rowElements.add(c.element);
            }
            rows.add(rowElements);
        }
        return rows;
    }

    /**
     * children이 비어있거나 left/top/width/height를 확정적으로 읽을 수 없으면 null. child 수
     * 자체는 fallback 사유로 쓰지 않는다(1-row/소수 child Div도 table 변환 대상, 14번 규칙).
     */
    private List<CellGeometry> resolveCellGeometries(List<Element> children) {
        if (children == null || children.isEmpty()) {
            return null;
        }
        List<CellGeometry> cells = new ArrayList<CellGeometry>();
        for (Element child : children) {
            Geometry g = resolveGeometry(child);
            ParsedLength left = isEmpty(g.left) ? null : parseLength(g.left);
            ParsedLength top = isEmpty(g.top) ? null : parseLength(g.top);
            ParsedLength width = isEmpty(g.width) ? null : parseLength(g.width);
            ParsedLength height = isEmpty(g.height) ? null : parseLength(g.height);
            if (left == null || top == null || width == null || height == null) {
                return null;
            }
            cells.add(new CellGeometry(child, left.value, top.value, width.value, height.value));
        }
        return cells;
    }

    private Map<Double, List<CellGeometry>> groupByTop(List<CellGeometry> cells) {
        Map<Double, List<CellGeometry>> map = new LinkedHashMap<Double, List<CellGeometry>>();
        for (CellGeometry c : cells) {
            List<CellGeometry> row = map.get(c.top);
            if (row == null) {
                row = new ArrayList<CellGeometry>();
                map.put(c.top, row);
            }
            row.add(c);
        }
        return map;
    }

    private boolean hasOverlap(List<CellGeometry> cells) {
        for (int i = 0; i < cells.size(); i++) {
            for (int j = i + 1; j < cells.size(); j++) {
                CellGeometry a = cells.get(i);
                CellGeometry b = cells.get(j);
                boolean xOverlap = a.left < b.left + b.width && b.left < a.left + a.width;
                boolean yOverlap = a.top < b.top + b.height && b.top < a.top + a.height;
                if (xOverlap && yOverlap) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final class CellGeometry {
        private final Element element;
        private final double left;
        private final double top;
        private final double width;
        private final double height;

        private CellGeometry(Element element, double left, double top, double width, double height) {
            this.element = element;
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * WebSquare AI v6 grp_main wrapper(V6_STRUCTURE_PARTIAL_ALIGNMENT)의 style을 생성한다.
     * buildRootStyle과 동일한 geometry resolution(findFormGeometry)을 재사용하며, 유효한 양수
     * height를 얻은 경우에만 height를 추가로 반환한다(position/overflow는 절대 emit하지 않음).
     *
     * <p>ROOT_PERCENT_CONTAINING_BLOCK_DEFECT fix: 이전까지는 height-only(width 미emit)였다
     * (단일 real v6 화면 관찰 기반, universal rule로 검증된 적 없다고 자체 명시돼 있었음). 그
     * 관찰은 global {@code grp_content}(px width/height를 가진 compatibility wrapper)가 아직
     * 존재하던 시점의 것이다 -- {@code grp_content} 제거 이후에는 {@code grp_main} 직계 자식들이
     * 전부 percentage width로 바뀌었고, 실제 폐쇄망 Studio 재현(STUDIO_DESIGN_FAILED,
     * STUDIO_DESIGN_REPRODUCED -- 업무 영역이 좌측 좁은 영역에 collapse)으로 percentage 자식이
     * 참조할 containing block에 명시적 width가 반드시 필요함이 확인됐다. {@code width:100%;}는
     * source geometry에서 계산한 값이 아니라 구조적 상수이므로 특정 화면 px 하드코딩이 아니다.
     */
    public String buildMainAreaStyle(Document source) {
        StringBuilder style = new StringBuilder();
        style.append("width:").append(formatPercent(100.0)).append(";");

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

    /** 어떤 XPlatform 위치 속성을 사용했는지 진단용 문자열로 반환한다. */
    public String describeLayoutSource(Element source) {
        if (source == null) {
            return "";
        }
        String positionType = trim(source.getAttribute("positiontype"));
        String position2 = trim(source.getAttribute("position2"));
        String position = trim(source.getAttribute("position"));

        if (position2.length() > 0
                && ("position2".equalsIgnoreCase(positionType)
                || position.length() == 0)) {
            return "position2=" + position2;
        }
        if (position.length() > 0) {
            return "position=" + position;
        }
        if (hasExplicitGeometry(source)) {
            return "left/top/width/height";
        }
        return "";
    }

    /** 음수/지원하지 않는 단위/역전 좌표 때문에 크기를 안전하게 계산할 수 없는지 확인한다. */
    public boolean hasInvalidSize(Element source) {
        if (source == null) {
            return false;
        }

        String width = trim(source.getAttribute("width"));
        String height = trim(source.getAttribute("height"));
        if ((width.length() > 0 && normalizeNonNegativeLength(width) == null)
                || (height.length() > 0 && normalizeNonNegativeLength(height) == null)) {
            return true;
        }

        String position = trim(source.getAttribute("position"));
        if (position.length() > 0) {
            Geometry geometry = parsePosition(position);
            if (geometry == null) {
                return true;
            }
            if ((!isEmpty(geometry.left) && !isEmpty(geometry.right) && isEmpty(geometry.width))
                    || (!isEmpty(geometry.top) && !isEmpty(geometry.bottom) && isEmpty(geometry.height))) {
                return true;
            }
        }

        String position2 = trim(source.getAttribute("position2"));
        if (position2.length() > 0) {
            String[] parts = position2.split("\\s+");
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                int colon = part.indexOf(':');
                if (colon <= 0 || colon >= part.length() - 1) {
                    continue;
                }
                String key = part.substring(0, colon).toLowerCase();
                String value = part.substring(colon + 1);
                if (("w".equals(key) || "h".equals(key))
                        && normalizeNonNegativeLength(value) == null) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean hasUnsupportedRelativeLayout(Element source) {
        if (source == null) {
            return false;
        }
        String position2 = trim(source.getAttribute("position2"));
        if (position2.length() == 0) {
            return false;
        }

        String[] parts = position2.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            int colon = part.indexOf(':');
            if (colon <= 0 || colon == part.length() - 1) {
                continue;
            }
            String key = part.substring(0, colon).toLowerCase();
            if (!("l".equals(key) || "t".equals(key) || "r".equals(key)
                    || "b".equals(key) || "w".equals(key) || "h".equals(key))) {
                continue;
            }
            String value = part.substring(colon + 1);
            if (!isSupportedLength(value)) {
                return true;
            }
        }
        return false;
    }

    private Geometry findFormGeometry(Document source) {
        if (source == null) {
            return null;
        }

        Element formElement = findFirstElement(source, "Form");
        if (formElement != null) {
            Geometry form = resolveGeometry(formElement);
            if (!isEmpty(form.width) || !isEmpty(form.height)) {
                return form;
            }
        }

        Element layout = findFirstElement(source, "Layout");
        if (layout != null) {
            Geometry geometry = new Geometry();
            geometry.width = normalizeNonNegativeLength(layout.getAttribute("width"));
            geometry.height = normalizeNonNegativeLength(layout.getAttribute("height"));
            if (!isEmpty(geometry.width) || !isEmpty(geometry.height)) {
                return geometry;
            }
        }

        return null;
    }

    private Element findFirstElement(Document source, String tagName) {
        NodeList elements = source.getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); i++) {
            Node node = elements.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            Element element = (Element) node;
            String localName = element.getLocalName();
            String actual = localName != null && localName.length() > 0
                    ? localName : element.getTagName();
            int colon = actual.indexOf(':');
            if (colon >= 0) {
                actual = actual.substring(colon + 1);
            }
            if (tagName.equals(actual)) {
                return element;
            }
        }
        return null;
    }

    private Geometry resolveGeometry(Element source) {
        Geometry geometry = new Geometry();
        if (source == null) {
            return geometry;
        }

        // 일부 XFDL은 left/top/width/height를 직접 가지고 있으므로 먼저 읽는다.
        geometry.left = normalizeLength(source.getAttribute("left"));
        geometry.top = normalizeLength(source.getAttribute("top"));
        geometry.width = normalizeNonNegativeLength(source.getAttribute("width"));
        geometry.height = normalizeNonNegativeLength(source.getAttribute("height"));
        geometry.right = normalizeLength(source.getAttribute("right"));
        geometry.bottom = normalizeLength(source.getAttribute("bottom"));

        String positionType = trim(source.getAttribute("positiontype"));
        String position2 = trim(source.getAttribute("position2"));
        String position = trim(source.getAttribute("position"));

        Geometry parsed = null;
        if (position2.length() > 0
                && ("position2".equalsIgnoreCase(positionType)
                || position.length() == 0)) {
            parsed = parsePosition2(position2);
        }
        if (parsed == null && position.length() > 0) {
            parsed = parsePosition(position);
        }

        if (parsed != null) {
            // position/position2 값이 있으면 XPlatform 원본 위치 정의를 우선한다.
            geometry.mergeFrom(parsed);
        }

        return geometry;
    }

    private Geometry parsePosition(String value) {
        String[] parts = trim(value).split("\\s+");
        if (parts.length < 5 || !"absolute".equalsIgnoreCase(parts[0])) {
            return null;
        }

        String left = normalizeLength(parts[1]);
        String top = normalizeLength(parts[2]);
        String right = normalizeLength(parts[3]);
        String bottom = normalizeLength(parts[4]);

        Geometry geometry = new Geometry();
        geometry.left = left;
        geometry.top = top;
        geometry.right = right;
        geometry.bottom = bottom;

        geometry.width = subtractLengths(right, left);
        geometry.height = subtractLengths(bottom, top);
        return geometry;
    }

    private Geometry parsePosition2(String value) {
        String[] parts = trim(value).split("\\s+");
        Map<String, String> values = new LinkedHashMap<String, String>();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if ("absolute".equalsIgnoreCase(part)) {
                continue;
            }
            int colon = part.indexOf(':');
            if (colon <= 0 || colon >= part.length() - 1) {
                continue;
            }
            String key = part.substring(0, colon).toLowerCase();
            String raw = part.substring(colon + 1);
            if (("l".equals(key) || "t".equals(key) || "r".equals(key)
                    || "b".equals(key) || "w".equals(key) || "h".equals(key))
                    && isSupportedLength(raw)) {
                values.put(key, normalizeLength(raw));
            }
        }

        if (values.isEmpty()) {
            return null;
        }

        Geometry geometry = new Geometry();
        geometry.left = values.get("l");
        geometry.top = values.get("t");
        geometry.right = values.get("r");
        geometry.bottom = values.get("b");
        geometry.width = normalizeNonNegativeLength(values.get("w"));
        geometry.height = normalizeNonNegativeLength(values.get("h"));

        if (isEmpty(geometry.width)) {
            geometry.width = subtractLengths(geometry.right, geometry.left);
        }
        if (isEmpty(geometry.height)) {
            geometry.height = subtractLengths(geometry.bottom, geometry.top);
        }
        return geometry;
    }

    private void appendVisualStyle(Element source, StringBuilder style) {
        if ("false".equalsIgnoreCase(trim(source.getAttribute("visible")))) {
            style.append("display:none;");
        }

        String color = trim(source.getAttribute("color"));
        if (color.length() > 0) {
            style.append("color:").append(color).append(';');
        }

        String background = trim(source.getAttribute("background"));
        if (background.length() > 0) {
            style.append("background:").append(background).append(';');
        }

        String cursor = trim(source.getAttribute("cursor")).toLowerCase();
        if (isSafeCursor(cursor)) {
            style.append("cursor:").append(cursor).append(';');
        }

        String opacity = trim(source.getAttribute("opacity"));
        if (opacity.length() > 0 && NUMBER.matcher(opacity).matches()) {
            double value = Double.parseDouble(opacity);
            if (value >= 0.0 && value <= 100.0) {
                if (value > 1.0) {
                    value = value / 100.0;
                }
                style.append("opacity:").append(formatNumber(value)).append(';');
            }
        }

        appendAlignment(source.getAttribute("align"), style);
        appendPadding(source.getAttribute("padding"), style);
    }

    private boolean isSafeCursor(String value) {
        return "auto".equals(value) || "default".equals(value) || "pointer".equals(value)
                || "text".equals(value) || "wait".equals(value) || "help".equals(value)
                || "move".equals(value) || "crosshair".equals(value) || "not-allowed".equals(value);
    }

    private void appendAlignment(String align, StringBuilder style) {
        String value = trim(align).toLowerCase();
        if (value.length() == 0) {
            return;
        }
        String[] parts = value.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if ("left".equals(part) || "center".equals(part) || "right".equals(part)
                    || "justify".equals(part)) {
                style.append("text-align:").append(part).append(';');
            } else if ("top".equals(part) || "middle".equals(part) || "bottom".equals(part)) {
                String css = "middle".equals(part) ? "middle" : part;
                style.append("vertical-align:").append(css).append(';');
            }
        }
    }

    private void appendPadding(String padding, StringBuilder style) {
        String value = trim(padding);
        if (value.length() == 0) {
            return;
        }
        String[] parts = value.split("[ ,]+", -1);
        if (parts.length < 1 || parts.length > 4) {
            return;
        }
        StringBuilder css = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = normalizeNonNegativeLength(parts[i]);
            if (p == null || p.length() == 0) {
                return;
            }
            if (i > 0) {
                css.append(' ');
            }
            css.append(toCssLength(p));
        }
        style.append("padding:").append(css).append(';');
    }

    private void appendCssLength(StringBuilder style, String name, String value) {
        if (isEmpty(value)) {
            return;
        }
        style.append(name).append(':').append(toCssLength(value)).append(';');
    }

    private String toCssLength(String value) {
        String v = trim(value);
        if (v.length() == 0) {
            return v;
        }
        if (v.endsWith("%") || v.toLowerCase().endsWith("px")
                || "auto".equalsIgnoreCase(v)) {
            return v;
        }
        if (NUMBER.matcher(v).matches()) {
            return v + "px";
        }
        return v;
    }

    private String normalizeLength(String value) {
        String v = trim(value);
        if (v.length() == 0) {
            return null;
        }
        if (isSupportedLength(v)) {
            return v;
        }
        return null;
    }

    private String normalizeNonNegativeLength(String value) {
        String normalized = normalizeLength(value);
        if (normalized == null || "auto".equalsIgnoreCase(normalized)) {
            return normalized;
        }
        ParsedLength parsed = parseLength(normalized);
        return parsed != null && parsed.value >= 0.0 ? normalized : null;
    }

    private boolean isSupportedLength(String value) {
        String v = trim(value);
        if (v.length() == 0 || "auto".equalsIgnoreCase(v)) {
            return v.length() > 0;
        }
        return NUMBER_WITH_UNIT.matcher(v).matches();
    }

    private String subtractLengths(String end, String start) {
        if (isEmpty(end) || isEmpty(start)) {
            return null;
        }

        ParsedLength endValue = parseLength(end);
        ParsedLength startValue = parseLength(start);
        if (endValue == null || startValue == null) {
            return null;
        }
        if (!endValue.unit.equals(startValue.unit)) {
            return null;
        }

        double difference = endValue.value - startValue.value;
        if (difference < 0.0) {
            return null;
        }
        return formatNumber(difference) + endValue.unit;
    }

    private ParsedLength parseLength(String value) {
        Matcher matcher = NUMBER_WITH_UNIT.matcher(trim(value));
        if (!matcher.matches()) {
            return null;
        }
        String unit = matcher.group(2);
        if (unit == null) {
            unit = "";
        }
        return new ParsedLength(Double.parseDouble(matcher.group(1)), unit);
    }

    private boolean hasExplicitGeometry(Element source) {
        return trim(source.getAttribute("left")).length() > 0
                || trim(source.getAttribute("top")).length() > 0
                || trim(source.getAttribute("width")).length() > 0
                || trim(source.getAttribute("height")).length() > 0;
    }

    private String formatNumber(double value) {
        long integer = (long) value;
        if (value == integer) {
            return String.valueOf(integer);
        }
        String result = String.valueOf(value);
        while (result.indexOf('.') >= 0 && result.endsWith("0")) {
            result = result.substring(0, result.length() - 1);
        }
        if (result.endsWith(".")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private boolean isEmpty(String value) {
        return value == null || value.length() == 0;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class ParsedLength {
        private final double value;
        private final String unit;

        private ParsedLength(double value, String unit) {
            this.value = value;
            this.unit = unit;
        }
    }

    private static final class Geometry {
        private String left;
        private String top;
        private String right;
        private String bottom;
        private String width;
        private String height;

        private boolean hasAnyPositionOrSize() {
            return left != null || top != null || width != null || height != null;
        }

        private void mergeFrom(Geometry other) {
            if (other == null) {
                return;
            }
            if (other.left != null) left = other.left;
            if (other.top != null) top = other.top;
            if (other.right != null) right = other.right;
            if (other.bottom != null) bottom = other.bottom;
            if (other.width != null) width = other.width;
            if (other.height != null) height = other.height;
        }
    }
}
