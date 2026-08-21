package com.example.xfdltracker.diagnostic;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Java 8, dependency-free offline diagnostic. NOT part of the Production converter -- takes a
 * real (source XFDL, generated WebSquare XML) pair and recomputes actual ancestor-chain effective
 * pixel geometry for every absolute-positioned generated element, using the real percentage
 * denominator chain (not a flat "% of Form" assumption). Built for real closed-network screens
 * where the sample corpus cannot reproduce the reported symptom.
 *
 * Usage:
 *   java -cp build/diagnostic-classes com.example.xfdltracker.diagnostic.GeometryDiagnostic \
 *       path/to/source.xfdl path/to/generated.xml
 *
 * Output metrics (stdout):
 *   FORM_HEIGHT, FORM_WIDTH
 *   GRP_MAIN_CONTAINING_BLOCK_VALID (PASS if grp_main style declares its own width/height)
 *   OVER_FORM_HEIGHT_GROUP_COUNT (+ top 20 offending elements: id, effective rect)
 *   TARGET_OVERLAP_COUNT (+ top 20 sibling pairs with overlapping effective rect)
 *   TINY_COMPONENT_COUNT (+ top 20, effective width or height &lt; 2px)
 *   NESTED_PERCENT_BASIS_ERROR_COUNT (+ top 20: generated effective px vs source declared px,
 *       matched by id -- generated id is expected to equal the dotted source path with '.'
 *       replaced by '_', mirroring WebSquareGenerator.createTargetId)
 */
public class GeometryDiagnostic {

    private static final Pattern STYLE_NUM = Pattern.compile(
            "(top|left|width|height)\\s*:\\s*(-?\\d+(?:\\.\\d+)?)(%|px)");

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: GeometryDiagnostic <source.xfdl> <generated.xml>");
            System.exit(2);
            return;
        }
        File sourceFile = new File(args[0]);
        File generatedFile = new File(args[1]);

        Document sourceDoc = parse(sourceFile);
        Document generatedDoc = parse(generatedFile);

        Element form = findFirstByLocalName(sourceDoc, "Form");
        double formWidth = form == null ? -1.0 : parsePx(form.getAttribute("width"));
        double formHeight = form == null ? -1.0 : parsePx(form.getAttribute("height"));

        System.out.println("FORM_WIDTH=" + formWidth);
        System.out.println("FORM_HEIGHT=" + formHeight);

        Map<String, Rect> sourceRects = new LinkedHashMap<String, Rect>();
        Element sourceLayout = findFirstByLocalName(sourceDoc, "Layout");
        if (sourceLayout != null) {
            walkSource(sourceLayout, "", sourceRects);
        }
        System.out.println("SOURCE_ID_RECT_COUNT=" + sourceRects.size());

        Element grpMain = findById(generatedDoc, "grp_main");
        Element grpResultArea = findById(generatedDoc, "grp_resultArea");

        boolean grpMainContainingBlockValid = false;
        if (grpMain != null) {
            Map<String, double[]> mainStyle = parseStyle(grpMain.getAttribute("style"));
            grpMainContainingBlockValid = mainStyle.containsKey("width") && mainStyle.containsKey("height");
        }
        System.out.println("GRP_MAIN_CONTAINING_BLOCK_VALID=" + (grpMainContainingBlockValid ? "PASS" : "FAIL"));
        System.out.println("GRP_MAIN_STYLE_RAW=" + (grpMain == null ? "(not found)" : "\"" + grpMain.getAttribute("style") + "\""));
        System.out.println("GRP_RESULT_AREA_STYLE_RAW=" + (grpResultArea == null ? "(not found)" : "\"" + grpResultArea.getAttribute("style") + "\""));

        List<GenRect> genRects = new ArrayList<GenRect>();
        // grp_resultArea/grp_main width is always "100.0%" by construction, but that chains up
        // to the ROOT LAYOUT's own declared width when the root Layout declares one explicitly
        // (PERCENT_GEOMETRY_PARENT = SOURCE_IMMEDIATE_CONTAINER -- mirrors
        // ComponentLayoutConverter.resolveLayoutBasis), which is not always equal to the Form's
        // own width. Fall back to FORM_WIDTH only when the root Layout has no declared width.
        double rootLayoutWidth = sourceLayout == null ? -1.0 : parsePx(sourceLayout.getAttribute("width"));
        double rootEffW = rootLayoutWidth > 0.0 ? rootLayoutWidth : formWidth;
        // grp_main's height, when present, is an explicit px value (content-extent based, or
        // Form/Layout declared height as fallback) and can be read directly.
        Map<String, double[]> mainStyleParsed = grpMain == null
                ? new LinkedHashMap<String, double[]>() : parseStyle(grpMain.getAttribute("style"));
        double rootEffH = mainStyleParsed.containsKey("height")
                ? mainStyleParsed.get("height")[0] // px unit already (unitFlag irrelevant here, value is the px number)
                : formHeight; // fallback assumption when grp_main has no declared box -- flagged above
        if (grpMain != null) {
            walkGenerated(grpMain, 0.0, 0.0, rootEffW, rootEffH, "grp_main", genRects);
        }
        System.out.println("GENERATED_ABSOLUTE_ELEMENT_COUNT=" + genRects.size());

        // OVER_FORM_HEIGHT_GROUP_COUNT
        List<GenRect> overHeight = new ArrayList<GenRect>();
        if (formHeight > 0) {
            for (GenRect r : genRects) {
                if (r.bottom() > formHeight + 0.5) {
                    overHeight.add(r);
                }
            }
        }
        System.out.println("OVER_FORM_HEIGHT_GROUP_COUNT=" + overHeight.size());
        printTop("OVER_FORM_HEIGHT", overHeight, 20);

        // TARGET_OVERLAP_COUNT (siblings only)
        List<String> overlaps = new ArrayList<String>();
        Map<String, List<GenRect>> byParent = new LinkedHashMap<String, List<GenRect>>();
        for (GenRect r : genRects) {
            List<GenRect> l = byParent.get(r.parentPath);
            if (l == null) {
                l = new ArrayList<GenRect>();
                byParent.put(r.parentPath, l);
            }
            l.add(r);
        }
        for (List<GenRect> siblings : byParent.values()) {
            for (int i = 0; i < siblings.size(); i++) {
                for (int j = i + 1; j < siblings.size(); j++) {
                    GenRect a = siblings.get(i);
                    GenRect b = siblings.get(j);
                    if (overlapsRect(a, b)) {
                        overlaps.add(a.id + " <-> " + b.id
                                + " a=[" + fmt(a.left) + "," + fmt(a.top) + "," + fmt(a.width) + "," + fmt(a.height) + "]"
                                + " b=[" + fmt(b.left) + "," + fmt(b.top) + "," + fmt(b.width) + "," + fmt(b.height) + "]");
                    }
                }
            }
        }
        System.out.println("TARGET_OVERLAP_COUNT=" + overlaps.size());
        for (int i = 0; i < Math.min(20, overlaps.size()); i++) {
            System.out.println("  OVERLAP " + overlaps.get(i));
        }

        // TINY_COMPONENT_COUNT
        List<GenRect> tiny = new ArrayList<GenRect>();
        for (GenRect r : genRects) {
            if ((r.width >= 0 && r.width < 2.0) || (r.height >= 0 && r.height < 2.0)) {
                tiny.add(r);
            }
        }
        System.out.println("TINY_COMPONENT_COUNT=" + tiny.size());
        printTop("TINY", tiny, 20);

        // NESTED_PERCENT_BASIS_ERROR_COUNT (matched by id == dotted-source-path with '.' -> '_')
        List<String> basisErrors = new ArrayList<String>();
        for (GenRect r : genRects) {
            Rect src = sourceRects.get(r.id);
            if (src == null) {
                continue;
            }
            double dw = Math.abs(r.width - src.width);
            double dh = Math.abs(r.height - src.height);
            if (dw > 1.0 || dh > 1.0) {
                basisErrors.add(r.id + " generated=[" + fmt(r.width) + "x" + fmt(r.height) + "]"
                        + " source=[" + fmt(src.width) + "x" + fmt(src.height) + "]"
                        + " delta=[" + fmt(dw) + "," + fmt(dh) + "]");
            }
        }
        System.out.println("NESTED_PERCENT_BASIS_ERROR_COUNT=" + basisErrors.size());
        for (int i = 0; i < Math.min(20, basisErrors.size()); i++) {
            System.out.println("  BASIS_ERROR " + basisErrors.get(i));
        }
    }

    private static void printTop(String label, List<GenRect> list, int limit) {
        for (int i = 0; i < Math.min(limit, list.size()); i++) {
            GenRect r = list.get(i);
            System.out.println("  " + label + " id=" + r.id
                    + " parent=" + r.parentPath
                    + " effTop=" + fmt(r.top) + " effLeft=" + fmt(r.left)
                    + " effWidth=" + fmt(r.width) + " effHeight=" + fmt(r.height)
                    + " effBottom=" + fmt(r.bottom()));
        }
    }

    private static boolean overlapsRect(GenRect a, GenRect b) {
        boolean xOverlap = a.left < b.left + b.width && b.left < a.left + a.width;
        boolean yOverlap = a.top < b.top + b.height && b.top < a.top + a.height;
        return xOverlap && yOverlap;
    }

    private static String fmt(double v) {
        return String.format("%.1f", v);
    }

    // ---- source-side: cumulative absolute px rect per id (dotted path, '.' -> '_') ----

    private static void walkSource(Element el, String path, Map<String, Rect> out) {
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (!(n instanceof Element)) {
                continue;
            }
            Element child = (Element) n;
            String tag = localName(child);
            String id = child.getAttribute("id");
            Double top = readPx(child, "top");
            Double left = readPx(child, "left");
            Double width = readPx(child, "width");
            Double height = readPx(child, "height");
            String childPath = id.length() > 0 ? (path.length() > 0 ? path + "." + id : id) : path;

            if (id.length() > 0 && top != null && left != null && width != null && height != null) {
                out.put(childPath.replace('.', '_'), new Rect(left, top, width, height));
            }

            boolean isContainer = "Div".equals(tag) || "GroupBox".equals(tag) || "PopupDiv".equals(tag);
            if (isContainer) {
                // descend into its own Layouts/Layout wrapper if present, else direct children
                Element innerLayout = findChildLayout(child);
                if (innerLayout != null) {
                    walkSource(innerLayout, childPath, out);
                } else {
                    walkSource(child, childPath, out);
                }
            } else {
                // pass-through wrappers (Layouts) etc: keep descending with same path
                if (!id.equals(child.getAttribute("id")) || tag.equals("Layouts")) {
                    walkSource(child, path, out);
                }
            }
        }
    }

    private static Element findChildLayout(Element container) {
        NodeList children = container.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element && "Layouts".equals(localName((Element) n))) {
                NodeList lchildren = n.getChildNodes();
                for (int j = 0; j < lchildren.getLength(); j++) {
                    Node ln = lchildren.item(j);
                    if (ln instanceof Element && "Layout".equals(localName((Element) ln))) {
                        return (Element) ln;
                    }
                }
            }
        }
        return null;
    }

    private static Double readPx(Element el, String attr) {
        String v = el.getAttribute(attr);
        if (v == null || v.trim().length() == 0) {
            return null;
        }
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---- generated-side: ancestor-chain-aware effective px rect ----

    private static final class GenRect {
        String id;
        String parentPath;
        double top, left, width, height;
        GenRect(String id, String parentPath, double top, double left, double width, double height) {
            this.id = id;
            this.parentPath = parentPath;
            this.top = top;
            this.left = left;
            this.width = width;
            this.height = height;
        }
        double bottom() { return top + height; }
    }

    private static void walkGenerated(
            Element el, double parentTop, double parentLeft, double parentEffW, double parentEffH,
            String parentPath, List<GenRect> out) {
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (!(n instanceof Element)) {
                continue;
            }
            Element child = (Element) n;
            String id = child.getAttribute("id");
            Map<String, double[]> style = parseStyle(child.getAttribute("style"));

            double[] topV = style.get("top");
            double[] leftV = style.get("left");
            double[] widthV = style.get("width");
            double[] heightV = style.get("height");

            if (topV != null && leftV != null && widthV != null && heightV != null) {
                double effTop = parentTop + resolve(topV, parentEffH);
                double effLeft = parentLeft + resolve(leftV, parentEffW);
                double effWidth = resolve(widthV, parentEffW);
                double effHeight = resolve(heightV, parentEffH);
                if (id.length() > 0) {
                    out.add(new GenRect(id, parentPath, effTop, effLeft, effWidth, effHeight));
                }
                walkGenerated(child, effTop, effLeft, effWidth, effHeight,
                        parentPath + "/" + (id.length() > 0 ? id : localName(child)), out);
                continue;
            }
            // no full absolute geometry on this element: keep same effective frame
            walkGenerated(child, parentTop, parentLeft, parentEffW, parentEffH,
                    parentPath + "/" + (id.length() > 0 ? id : localName(child)), out);
        }
    }

    private static double resolve(double[] valueAndUnit, double parentEff) {
        // valueAndUnit = {value, unitFlag} where unitFlag 0=px, 1=%
        if (valueAndUnit[1] == 0.0) {
            return valueAndUnit[0];
        }
        return (valueAndUnit[0] / 100.0) * parentEff;
    }

    private static Map<String, double[]> parseStyle(String style) {
        Map<String, double[]> out = new LinkedHashMap<String, double[]>();
        if (style == null) {
            return out;
        }
        Matcher m = STYLE_NUM.matcher(style);
        while (m.find()) {
            double value = Double.parseDouble(m.group(2));
            double unitFlag = "%".equals(m.group(3)) ? 1.0 : 0.0;
            out.put(m.group(1), new double[] {value, unitFlag});
        }
        return out;
    }

    // ---- shared helpers ----

    private static Document parse(File f) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(f);
    }

    private static String localName(Element el) {
        String ln = el.getLocalName();
        if (ln != null && ln.length() > 0) {
            return ln;
        }
        String tag = el.getTagName();
        int colon = tag.indexOf(':');
        return colon >= 0 ? tag.substring(colon + 1) : tag;
    }

    private static Element findFirstByLocalName(Document doc, String name) {
        NodeList all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Node n = all.item(i);
            if (n instanceof Element && name.equals(localName((Element) n))) {
                return (Element) n;
            }
        }
        return null;
    }

    private static Element findById(Document doc, String id) {
        NodeList all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Node n = all.item(i);
            if (n instanceof Element && id.equals(((Element) n).getAttribute("id"))) {
                return (Element) n;
            }
        }
        return null;
    }

    private static double parsePx(String v) {
        if (v == null || v.trim().length() == 0) {
            return -1.0;
        }
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    private static final class Rect {
        double left, top, width, height;
        Rect(double left, double top, double width, double height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }
    }
}
