package com.example.xfdltracker.audit;

import com.example.xfdltracker.converter.WebSquareGenerator;
import com.example.xfdltracker.mapping.ComponentMapping;
import com.example.xfdltracker.mapping.ComponentMappingRegistry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * READ-ONLY, out-of-Production evidence-closure tool for SOURCE_TO_TARGET_ID_MAP_IDENTICAL.
 *
 * Does not reimplement createUniqueTargetId's uniqueness/suffix algorithm: it invokes the real,
 * unmodified WebSquareGenerator.createUniqueTargetId (and buildSourcePath/canonicalizePath/
 * sanitizeXml10/getSourceTagName/shouldTraverseUnknownElement/isFirstDirectLayout) via reflection
 * on a fresh WebSquareGenerator instance per file -- mirroring generate()'s own per-file reset
 * semantics (componentIdMap.clear()/usedTargetIds.clear() + wrapper-id reservation) exactly, so
 * usedTargetIds accumulates state through a file's traversal exactly as Production does.
 *
 * The one piece of Production logic this tool DOES mirror itself (not via reflection, since it is
 * a single trivial `if (!map.containsKey(k)) map.put(k, v);` first-wins guard, not part of the ID
 * generation algorithm) is the componentIdMap first-wins dedup at each of the 4 real put() sites in
 * WebSquareGenerator: convertChildren regular-component branch, convertTab Tab-element branch,
 * convertTab per-Tabpage branch, and registerFormRootMapping. This tool additionally reports
 * whether it ever actually observed a duplicate-key collision (first-wins triggered), as a direct
 * cross-check against the "[UI TODO] 중복 source path" console-log count from the real run (both
 * were 0 in this corpus).
 *
 * Usage: ComponentIdMapAudit <sample-phase3-project dir> <reserve_grp_resultArea_and_main: true|false> <output snapshot file> [formRootTargetId]
 *
 * v6 Design Structure + Table + Grid Group + Percentage Geometry Alignment 라운드: candidate의
 * registerFormRootMapping이 Form root를 grp_content 대신 grp_main으로 매핑하므로(다른 mapping은
 * 무변경), 이 리터럴을 4번째 CLI 인자로 뺐다 -- Base 스냅샷은 "grp_content"(기본값, 인자 생략),
 * candidate 스냅샷은 "grp_main"으로 실행한다. EXPECTED_SOURCE_TO_TARGET_MAP_DIFF는 이 값 하나뿐이다.
 */
public class ComponentIdMapAudit {

    private static Method mBuildSourcePath;
    private static Method mCanonicalizePath;
    private static Method mCreateUniqueTargetId;
    private static Method mSanitizeXml10;
    private static Method mGetSourceTagName;
    private static Method mShouldTraverseUnknownElement;
    private static Method mIsFirstDirectLayout;
    private static Field fUsedTargetIds;

    private static boolean reserveWrappers;
    private static String formRootTargetId;
    private static Map<String, String> canonicalMap; // per-file map (mirrors componentIdMap, first-wins)
    private static int duplicateEvents = 0;
    private static WebSquareGenerator gen;

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("usage: ComponentIdMapAudit <sample-phase3-project dir> <reserve_wrappers true|false> <output file>");
            System.exit(2);
        }
        File root = new File(args[0]);
        reserveWrappers = Boolean.parseBoolean(args[1]);
        File outFile = new File(args[2]);
        formRootTargetId = args.length >= 4 ? args[3] : "grp_content";

        Class<WebSquareGenerator> cls = WebSquareGenerator.class;
        mBuildSourcePath = cls.getDeclaredMethod("buildSourcePath", String.class, String.class);
        mBuildSourcePath.setAccessible(true);
        mCanonicalizePath = cls.getDeclaredMethod("canonicalizePath", String.class);
        mCanonicalizePath.setAccessible(true);
        mCreateUniqueTargetId = cls.getDeclaredMethod("createUniqueTargetId", String.class);
        mCreateUniqueTargetId.setAccessible(true);
        mSanitizeXml10 = cls.getDeclaredMethod("sanitizeXml10", String.class);
        mSanitizeXml10.setAccessible(true);
        mGetSourceTagName = cls.getDeclaredMethod("getSourceTagName", Element.class);
        mGetSourceTagName.setAccessible(true);
        mShouldTraverseUnknownElement = cls.getDeclaredMethod("shouldTraverseUnknownElement", String.class);
        mShouldTraverseUnknownElement.setAccessible(true);
        mIsFirstDirectLayout = cls.getDeclaredMethod("isFirstDirectLayout", Element.class, Element.class);
        mIsFirstDirectLayout.setAccessible(true);
        fUsedTargetIds = cls.getDeclaredField("usedTargetIds");
        fUsedTargetIds.setAccessible(true);

        List<File> xfdlFiles = new ArrayList<File>();
        collectXfdl(root, xfdlFiles);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        TreeMap<String, Map<String, String>> perFileSnapshots = new TreeMap<String, Map<String, String>>();

        for (File f : xfdlFiles) {
            String rel = root.toPath().relativize(f.toPath()).toString().replace('\\', '/');

            gen = new WebSquareGenerator();
            canonicalMap = new LinkedHashMap<String, String>();

            @SuppressWarnings("unchecked")
            Set<String> usedTargetIds = (Set<String>) fUsedTargetIds.get(gen);
            usedTargetIds.add("grp_content");
            if (reserveWrappers) {
                usedTargetIds.add("grp_resultArea");
                usedTargetIds.add("grp_main");
            }

            Document doc = dbf.newDocumentBuilder().parse(f);
            Element sourceRoot = doc.getDocumentElement();

            // registerFormRootMapping equivalent: Form -> grp_content (first Form descendant only)
            registerFormRootMappingEquivalent(sourceRoot);

            walkChildren(sourceRoot, "");

            perFileSnapshots.put(rel, new TreeMap<String, String>(canonicalMap));
        }

        PrintWriter w = new PrintWriter(outFile, "UTF-8");
        int totalKeys = 0;
        for (Map.Entry<String, Map<String, String>> fileEntry : perFileSnapshots.entrySet()) {
            for (Map.Entry<String, String> e : fileEntry.getValue().entrySet()) {
                w.println(fileEntry.getKey() + " | " + e.getKey() + " -> " + e.getValue());
                totalKeys++;
            }
        }
        w.close();

        System.out.println("FILES_SCANNED=" + xfdlFiles.size());
        System.out.println("TOTAL_MAP_KEYS=" + totalKeys);
        System.out.println("DUPLICATE_FIRST_WINS_EVENTS=" + duplicateEvents);
        System.out.println("RESERVE_WRAPPERS=" + reserveWrappers);
        System.out.println("SNAPSHOT_WRITTEN=" + outFile.getPath());
    }

    private static void collectXfdl(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File c : children) {
            if (c.isDirectory()) collectXfdl(c, out);
            else if (c.getName().endsWith(".xfdl")) out.add(c);
        }
    }

    private static String sanitize(String v) throws Exception {
        return (String) mSanitizeXml10.invoke(gen, v);
    }

    private static String sourceTag(Element el) throws Exception {
        return (String) mGetSourceTagName.invoke(gen, el);
    }

    private static String buildSourcePath(String parentPath, String localId) throws Exception {
        return (String) mBuildSourcePath.invoke(gen, parentPath, localId);
    }

    private static String canonicalizePath(String p) throws Exception {
        return (String) mCanonicalizePath.invoke(gen, p);
    }

    private static String createUniqueTargetId(String sourcePath) throws Exception {
        return (String) mCreateUniqueTargetId.invoke(gen, sourcePath);
    }

    private static boolean shouldTraverseUnknown(String tag) throws Exception {
        return (Boolean) mShouldTraverseUnknownElement.invoke(gen, tag);
    }

    private static boolean isFirstDirectLayout(Element layouts, Element candidate) throws Exception {
        return (Boolean) mIsFirstDirectLayout.invoke(gen, layouts, candidate);
    }

    private static void putFirstWins(String canonicalPath, String targetId) {
        if (!canonicalMap.containsKey(canonicalPath)) {
            canonicalMap.put(canonicalPath, targetId);
        } else {
            duplicateEvents++;
        }
    }

    /** Mirrors WebSquareGenerator.registerFormRootMapping's componentIdMap.put site (line ~1231-1232). */
    private static void registerFormRootMappingEquivalent(Element sourceRoot) throws Exception {
        Element form = findFirstDescendant(sourceRoot, "Form");
        if (form == null) return;
        String formId = canonicalizePath(sanitize(form.getAttribute("id")));
        if (formId.length() > 0) {
            putFirstWins(formId, formRootTargetId);
        }
    }

    private static Element findFirstDescendant(Element root, String tag) throws Exception {
        NodeList all = root.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Node n = all.item(i);
            if (n instanceof Element && tag.equals(sourceTag((Element) n))) return (Element) n;
        }
        return null;
    }

    /** Mirrors WebSquareGenerator.convertChildren's traversal + componentIdMap.put sites. */
    private static void walkChildren(Element sourceParent, String parentPath) throws Exception {
        NodeList children = sourceParent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            Element src = (Element) node;
            String sourceTag = sourceTag(src);

            if ("Layouts".equals(sourceTag(sourceParent)) && "Layout".equals(sourceTag)
                    && !isFirstDirectLayout(sourceParent, src)) {
                continue;
            }

            ComponentMappingRegistry registry = new ComponentMappingRegistry();
            ComponentMapping componentMapping = registry.get(sourceTag);
            String targetTag = componentMapping == null ? null : componentMapping.getTargetTag();

            if ("Tab".equals(sourceTag) && componentMapping != null && targetTag != null) {
                walkTab(src, parentPath);
                continue;
            }

            if (targetTag != null) {
                String localId = sanitize(src.getAttribute("id"));
                if (localId.length() == 0) continue;

                String sourcePath = buildSourcePath(parentPath, localId);
                String targetId = createUniqueTargetId(sourcePath);
                String canonicalPath = canonicalizePath(sourcePath);
                putFirstWins(canonicalPath, targetId);

                if (registry.isContainer(sourceTag)) {
                    walkChildren(src, sourcePath);
                }
                continue;
            }

            if (shouldTraverseUnknown(sourceTag)) {
                walkChildren(src, parentPath);
            }
        }
    }

    /** Mirrors WebSquareGenerator.convertTab's componentIdMap.put sites (Tab id + per-Tabpage content id). */
    private static void walkTab(Element src, String parentPath) throws Exception {
        String localId = sanitize(src.getAttribute("id"));
        if (localId.length() == 0) return;
        String sourcePath = buildSourcePath(parentPath, localId);
        String tabTargetId = createUniqueTargetId(sourcePath);
        String canonicalPath = canonicalizePath(sourcePath);
        putFirstWins(canonicalPath, tabTargetId);

        List<Element> pages = directTabpages(src);
        for (int i = 0; i < pages.size(); i++) {
            Element page = pages.get(i);
            String pageLocalId = sanitize(page.getAttribute("id"));
            if (pageLocalId.length() == 0) pageLocalId = "tabpage" + i;
            String pagePath = buildSourcePath(sourcePath, pageLocalId);
            createUniqueTargetId(pagePath + ".tab"); // tabHeaderId: not a componentIdMap key, consumed for usedTargetIds state only
            String contentId = createUniqueTargetId(pagePath + ".content");

            String canonicalPagePath = canonicalizePath(pagePath);
            putFirstWins(canonicalPagePath, contentId);

            // Deliberately OVER-INCLUSIVE (see WrapperIdCollisionAudit javadoc for rationale):
            // always recurse into Tabpage content, even where the real generator might route to an
            // independent external Form scope via tabContentPlan and skip recursion. This makes the
            // per-file map snapshot a superset of what the real per-file componentIdMap would hold
            // in the external-content case -- safe direction for detecting any Base/Candidate
            // divergence (a divergence in the superset implies a divergence in the subset only if
            // it falls within the subset; the externally-referenced file is itself scanned in full
            // as its own top-level root elsewhere in this same run, so its own true content is
            // still exactly verified there).
            walkChildren(page, pagePath);
        }
    }

    private static List<Element> directTabpages(Element tab) throws Exception {
        List<Element> result = new ArrayList<Element>();
        NodeList children = tab.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element)) continue;
            Element child = (Element) node;
            String tag = sourceTag(child);
            if ("Tabpage".equals(tag)) {
                result.add(child);
            } else if ("Tabpages".equals(tag)) {
                NodeList pages = child.getChildNodes();
                for (int p = 0; p < pages.getLength(); p++) {
                    Node pn = pages.item(p);
                    if (pn instanceof Element && "Tabpage".equals(sourceTag((Element) pn))) {
                        result.add((Element) pn);
                    }
                }
            }
        }
        return result;
    }
}
