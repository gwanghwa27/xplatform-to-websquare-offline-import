package com.example.xfdltracker.converter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.LinkedHashMap;
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

    /** 일반 UI 컴포넌트의 CSS style 문자열을 생성한다. */
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

    /**
     * 루트 group의 style을 생성한다. Form/Layout 크기를 반영하여
     * XPlatform 절대좌표 기준으로 자식 컴포넌트를 배치할 수 있게 한다.
     */
    public String buildRootStyle(Document source) {
        StringBuilder style = new StringBuilder();
        // position:relative and overflow:hidden intentionally NOT emitted here: root style is
        // STUDIO_DESIGN_VERIFIED / GEOMETRY_RUNTIME_VERIFIED / REGRESSION_VERIFIED /
        // PATCH_READY (user-confirmed on real closed-network Studio, including absolute-child
        // rendering position). Full chronology/evidence:
        // work/closed-network-support/issues/ISSUE-20260818-001-studio-design-blank/ISSUE.md.

        Geometry geometry = findFormGeometry(source);
        if (geometry != null) {
            appendCssLength(style, "width", geometry.width);
            appendCssLength(style, "height", geometry.height);
        }

        if (style.indexOf("width:") < 0) {
            style.append("width:100%;");
        }
        if (style.indexOf("height:") < 0) {
            style.append("height:100%;");
        }
        return style.toString();
    }

    /**
     * WebSquare AI v6 grp_main wrapper(V6_STRUCTURE_PARTIAL_ALIGNMENT)의 style을 생성한다.
     * buildRootStyle과 동일한 geometry resolution(findFormGeometry)을 재사용하며, 유효한 양수
     * height를 얻은 경우에만 height만 반환한다(width/position/overflow는 절대 emit하지 않음).
     * 유효한 height를 얻지 못하면 height:0px; 같은 placeholder 없이 빈 문자열을 반환한다.
     * height-only/no-width convention은 단일 real v6 화면 관찰(video evidence) 기반이며 아직
     * universal rule로 검증된 것은 아니다.
     */
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
