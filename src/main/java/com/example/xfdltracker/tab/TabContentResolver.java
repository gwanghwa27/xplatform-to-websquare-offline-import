package com.example.xfdltracker.tab;

import com.example.xfdltracker.mapping.ComponentMappingRegistry;
import com.example.xfdltracker.parser.XfdlReader;
import com.example.xfdltracker.util.JavaScriptCleaner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves static Tabpage.url references without flattening the referenced XFDL into the parent page.
 * Resolution is path-aware: relative path -> project path -> TypeDefinition Service prefix.
 */
public class TabContentResolver {
    private static final String[] EXTERNAL_ATTRS = {
            "url", "formurl", "contenturl", "src", "source"
    };
    private static final Set<String> STRUCTURAL = new HashSet<String>(Arrays.asList(
            "Layouts", "Layout", "Tabpages", "Script", "Dataset", "DataSet", "Bind", "BindEvent",
            "Formats", "Format", "Columns", "Rows", "Band", "Cell", "Contents", "Content", "Source", "Url"));

    private static final Pattern DYNAMIC_SET_URL = Pattern.compile(
            "(?m)(this\\s*\\.\\s*)?([A-Za-z_$][A-Za-z0-9_$]*(?:\\s*\\.\\s*(?:[A-Za-z_$][A-Za-z0-9_$]*|tabpages\\s*\\[[^\\]]+\\]))+)\\s*\\.\\s*set_url\\s*\\(([^)]*)\\)");
    private static final Pattern DYNAMIC_URL_ASSIGN = Pattern.compile(
            "(?m)(this\\s*\\.\\s*)?([A-Za-z_$][A-Za-z0-9_$]*(?:\\s*\\.\\s*(?:[A-Za-z_$][A-Za-z0-9_$]*|tabpages\\s*\\[[^\\]]+\\]))+)\\s*\\.\\s*url\\s*=\\s*([^;\\r\\n]+)");
    private static final Pattern TAB_MUTATION = Pattern.compile(
            "(?<![A-Za-z0-9_$])(addTabpage|insertTabpage|removeTabpage|getTabpageCount)\\s*\\(");
    private static final Pattern PARENT_CHILD = Pattern.compile(
            "(?<![A-Za-z0-9_$])(parent|opener|getOwnerFrame|arguments)\\b");

    private final File sourceRoot;
    private final ScreenTargetRegistry screenTargets;
    private final ComponentMappingRegistry componentMappings = new ComponentMappingRegistry();

    public TabContentResolver(File sourceRoot) throws IOException {
        this.sourceRoot = sourceRoot.getCanonicalFile();
        this.screenTargets = null;
    }

    public TabContentResolver(File sourceRoot, ScreenTargetRegistry screenTargets) throws IOException {
        this.sourceRoot = sourceRoot.getCanonicalFile();
        this.screenTargets = screenTargets;
    }

    public TabContentPlan analyze(File xfdlFile, String relativePath) throws Exception {
        Document document = new XfdlReader().read(xfdlFile);
        TabContentPlan plan = new TabContentPlan(relativePath);
        XPlatformServiceRegistry services = XPlatformServiceRegistry.load(sourceRoot, xfdlFile, document);
        if (services.getWarning().length() > 0) plan.addWarning(services.getWarning());
        walk(document.getDocumentElement(), "", xfdlFile, relativePath, services, plan, 0);
        inspectScript(new XfdlReader().extractScript(document), plan);
        return plan;
    }

    private void walk(Element parent, String parentPath, File screenFile, String screenRel,
                      XPlatformServiceRegistry services, TabContentPlan plan, int depth) throws Exception {
        if (parent == null || depth > 200) return;
        NodeList children = parent.getChildNodes();
        boolean firstLayoutSeen = false;
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element)) continue;
            Element child = (Element) node;
            String tag = localName(child);
            if ("Layouts".equals(localName(parent)) && "Layout".equals(tag)) {
                if (firstLayoutSeen) continue;
                firstLayoutSeen = true;
            }
            if ("Tab".equals(tag)) {
                analyzeTab(child, parentPath, screenFile, screenRel, services, plan, depth + 1);
                continue;
            }
            if (shouldTraverse(tag)) {
                String childParentPath = parentPath;
                if (!"Form".equals(tag) && componentMappings.isContainer(tag)) {
                    String id = clean(child.getAttribute("id"));
                    if (id.length() > 0) childParentPath = path(parentPath, id);
                }
                walk(child, childParentPath, screenFile, screenRel, services, plan, depth + 1);
            }
        }
    }

    private void analyzeTab(Element tab, String parentPath, File screenFile, String screenRel,
                            XPlatformServiceRegistry services, TabContentPlan plan, int depth) throws Exception {
        String tabId = clean(tab.getAttribute("id"));
        if (tabId.length() == 0) tabId = "Tab";
        String tabPath = path(parentPath, tabId);
        boolean preload = "true".equalsIgnoreCase(clean(tab.getAttribute("preload")));
        TabContentReference.LoadingMode loadingMode = preload
                ? TabContentReference.LoadingMode.EAGER : TabContentReference.LoadingMode.LAZY;
        List<Element> pages = directTabpages(tab);
        for (int i = 0; i < pages.size(); i++) {
            Element page = pages.get(i);
            String pageId = clean(page.getAttribute("id"));
            if (pageId.length() == 0) pageId = "tabpage" + i;
            String pagePath = path(tabPath, pageId);
            ReferenceValue raw = externalReference(page);
            if (raw != null && raw.value.length() > 0) {
                boolean mixed = hasInlineUi(page);
                TabContentReference ref = resolveReference(screenFile, screenRel, tabId, tabPath,
                        pageId, pagePath, i, raw, services, loadingMode, mixed);
                plan.addReference(ref);
                if (mixed) plan.addWarning("mixed inline/external Tabpage: " + pagePath
                        + " external=" + raw.value + " (external content kept; inline child conversion suppressed)");
                // External content wins. The generator does not flatten inline children, so dependency
                // analysis must not invent nested inline Tab relationships that will never be generated.
                continue;
            }
            // Nested inline Tab still belongs to this XFDL and must be analyzed recursively.
            walk(page, pagePath, screenFile, screenRel, services, plan, depth + 1);
        }
    }

    private TabContentReference resolveReference(
            File screenFile, String screenRel, String tabId, String tabPath,
            String pageId, String pagePath, int index, ReferenceValue raw,
            XPlatformServiceRegistry services, TabContentReference.LoadingMode loadingMode,
            boolean mixed) throws Exception {

        String normalized = normalizeReference(raw.value);
        if (isRemote(normalized)) {
            return ref(screenRel, tabPath, pagePath, tabId, pageId, index, raw, normalized,
                    "", "", "", "REMOTE_URL", TabContentReference.Status.EXTERNAL_URL,
                    loadingMode, mixed, "원격 URL은 로컬 XFDL 변환 대상으로 확정할 수 없음");
        }
        String filePart = stripQueryAndFragment(normalized);
        if (!filePart.toLowerCase().endsWith(".xfdl")) {
            return ref(screenRel, tabPath, pagePath, tabId, pageId, index, raw, normalized,
                    "", "", "", "UNSUPPORTED_REFERENCE", TabContentReference.Status.UNRESOLVED,
                    loadingMode, mixed, "XFDL 외부 화면 참조로 확정할 수 없는 값");
        }

        Resolution resolved;
        if (screenTargets != null) {
            ScreenTargetRegistry.Resolution shared = screenTargets.resolve(screenFile, screenRel, normalized);
            if (shared.isResolved()) {
                return ref(screenRel, tabPath, pagePath, tabId, pageId, index, raw, normalized,
                        shared.getResolvedSource(), shared.getGeneratedTarget(), shared.getWebSquareSrc(),
                        shared.getMethod(), TabContentReference.Status.RESOLVED, loadingMode, mixed, shared.getMessage());
            }
            resolved = new Resolution(null, shared.getMethod(), shared.getMessage());
        } else {
            int serviceMark = filePart.indexOf("::");
            if (serviceMark > 0) {
                String prefix = filePart.substring(0, serviceMark);
                String tail = filePart.substring(serviceMark + 2);
                resolved = resolveService(prefix, tail, services);
            } else {
                resolved = resolvePath(screenFile, filePart);
            }
        }

        if (resolved.file == null) {
            return ref(screenRel, tabPath, pagePath, tabId, pageId, index, raw, normalized,
                    "", "", "", resolved.method, TabContentReference.Status.UNRESOLVED,
                    loadingMode, mixed, resolved.message);
        }
        String childRel = relative(sourceRoot, resolved.file);
        String generatedTarget = replaceExtension(childRel, ".xml");
        String parentTarget = replaceExtension(screenRel, ".xml");
        String webSquareSrc = relativeTarget(parentTarget, generatedTarget);
        return ref(screenRel, tabPath, pagePath, tabId, pageId, index, raw, normalized,
                childRel, generatedTarget, webSquareSrc, resolved.method,
                TabContentReference.Status.RESOLVED, loadingMode, mixed, resolved.message);
    }

    private Resolution resolvePath(File screenFile, String value) throws Exception {
        String normalized = normalizeFsPath(value);
        File relative = caseAwareResolve(screenFile.getParentFile(), normalized);
        if (isProjectFile(relative)) return new Resolution(relative, "RELATIVE", "현재 XFDL 기준 상대 경로");
        File rootRelative = caseAwareResolve(sourceRoot, trimLeadingSlash(normalized));
        if (isProjectFile(rootRelative)) return new Resolution(rootRelative, "PROJECT_RELATIVE", "프로젝트 루트 기준 경로");
        return new Resolution(null, "PATH", "상대/프로젝트 경로에서 대상 XFDL을 찾지 못함: " + value);
    }

    private Resolution resolveService(String prefix, String tail, XPlatformServiceRegistry services) throws Exception {
        XPlatformServiceRegistry.Service service = services.find(prefix);
        if (service == null) return new Resolution(null, "SERVICE_PREFIX", "TypeDefinition Service 미정의: " + prefix);
        if (service.getType().length() > 0 && !"form".equalsIgnoreCase(service.getType())) {
            return new Resolution(null, "SERVICE_PREFIX", "Service type이 form이 아님: " + prefix + " type=" + service.getType());
        }
        String serviceUrl = service.getUrl().replace('\\', '/');
        if (isRemote(serviceUrl)) {
            return new Resolution(null, "SERVICE_PREFIX_REMOTE", "Service URL이 원격 경로이므로 source tree와 자동 대응하지 않음: " + serviceUrl);
        }
        File base = service.getTypeDefinitionFile() == null
                ? sourceRoot : service.getTypeDefinitionFile().getParentFile();
        String joined = joinPath(serviceUrl, tail);
        File candidate = caseAwareResolve(base, normalizeFsPath(joined));
        if (isProjectFile(candidate)) return new Resolution(candidate, "SERVICE_PREFIX", "TypeDefinition Service url 기준");
        candidate = caseAwareResolve(sourceRoot, normalizeFsPath(joined));
        if (isProjectFile(candidate)) return new Resolution(candidate, "SERVICE_PREFIX_ROOT", "프로젝트 루트 + Service url 기준");
        // Source projects commonly store the development form tree under the prefix-id directory while
        // the runtime Service url points to a compiled Win32/HTML5 subtree. This is still path-based;
        // no filename-only search is performed.
        candidate = caseAwareResolve(sourceRoot, normalizeFsPath(joinPath(prefix, tail)));
        if (isProjectFile(candidate)) return new Resolution(candidate, "SERVICE_PREFIX_DIRECTORY", "프로젝트 prefix 디렉터리 기준 fallback");
        return new Resolution(null, "SERVICE_PREFIX", "Service 경로에서 대상 XFDL을 찾지 못함: " + prefix + "::" + tail
                + " serviceUrl=" + serviceUrl);
    }

    private File caseAwareResolve(File base, String relative) throws Exception {
        if (base == null) return null;
        File current = base.getCanonicalFile();
        String normalized = relative == null ? "" : relative.replace('\\', '/');
        String[] parts = normalized.split("/");
        for (String part : parts) {
            if (part.length() == 0 || ".".equals(part)) continue;
            if ("..".equals(part)) {
                current = current.getParentFile();
                if (current == null) return null;
                continue;
            }
            File exact = new File(current, part);
            if (exact.exists()) { current = exact; continue; }
            File[] files = current.listFiles();
            if (files == null) return null;
            File match = null;
            for (File file : files) {
                if (!file.getName().equalsIgnoreCase(part)) continue;
                if (match != null) return null; // ambiguous on case-sensitive file systems
                match = file;
            }
            if (match == null) return null;
            current = match;
        }
        return current.getCanonicalFile();
    }

    private boolean isProjectFile(File file) throws Exception {
        if (file == null || !file.isFile()) return false;
        String root = sourceRoot.getCanonicalPath();
        String child = file.getCanonicalPath();
        return child.equals(root) || child.startsWith(root + File.separator);
    }

    private void inspectScript(String script, TabContentPlan plan) {
        if (script == null || script.length() == 0) return;
        String cleaned = new JavaScriptCleaner().clean(script);
        findDynamic(DYNAMIC_SET_URL, cleaned, script, "set_url", plan);
        findDynamic(DYNAMIC_URL_ASSIGN, cleaned, script, "url assignment", plan);
        Matcher mutation = TAB_MUTATION.matcher(cleaned);
        while (mutation.find()) plan.addDynamicUsage("TAB STRUCTURE API " + mutation.group(1) + " line " + lineOf(script, mutation.start()));
        Matcher parent = PARENT_CHILD.matcher(cleaned);
        while (parent.find()) plan.addParentChildUsage(parent.group(1) + " line " + lineOf(script, parent.start()));
        inspectParentToExternalChildAccess(cleaned, script, plan);
    }

    private void inspectParentToExternalChildAccess(String cleaned, String original, TabContentPlan plan) {
        for (TabContentReference ref : plan.getReferences()) {
            String tab = Pattern.quote(ref.getTabId());
            String page = Pattern.quote(ref.getTabPageId());
            Pattern direct = Pattern.compile("(?<![A-Za-z0-9_$])(?:this\\s*\\.\\s*)?" + tab
                    + "\\s*\\.\\s*" + page + "\\s*\\.");
            Matcher m = direct.matcher(cleaned);
            while (m.find()) {
                plan.addParentChildUsage("PARENT_TO_TAB_CHILD line " + lineOf(original, m.start())
                        + " tabPage=" + ref.getTabPagePath()
                        + " source=" + sourceLine(original, m.start()));
            }
            Pattern indexed = Pattern.compile("(?<![A-Za-z0-9_$])(?:this\\s*\\.\\s*)?" + tab
                    + "\\s*\\.\\s*tabpages\\s*\\[[^\\]]+\\]\\s*\\.");
            Matcher im = indexed.matcher(cleaned);
            while (im.find()) {
                plan.addParentChildUsage("PARENT_TO_TAB_CHILD_INDEXED line " + lineOf(original, im.start())
                        + " tab=" + ref.getTabPath()
                        + " source=" + sourceLine(original, im.start()));
            }
        }
    }

    private static String sourceLine(String source, int offset) {
        int start = source.lastIndexOf('\n', Math.max(0, offset - 1));
        start = start < 0 ? 0 : start + 1;
        int end = source.indexOf('\n', offset);
        if (end < 0) end = source.length();
        return compact(source.substring(start, end));
    }

    private void findDynamic(Pattern pattern, String cleaned, String original, String kind, TabContentPlan plan) {
        Matcher m = pattern.matcher(cleaned);
        while (m.find()) {
            String target = compact(original.substring(m.start(2), m.end(2)));
            String sourceLine = sourceLine(original, m.start());
            plan.addDynamicUsage(kind + " line " + lineOf(original, m.start()) + " target=" + target + " source=" + sourceLine);
        }
    }

    private ReferenceValue externalReference(Element page) {
        for (String attr : EXTERNAL_ATTRS) {
            String value = clean(page.getAttribute(attr));
            if (value.length() > 0 && looksLikeScreenReference(value)) return new ReferenceValue(attr, value);
        }
        NodeList children = page.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element)) continue;
            Element child = (Element) children.item(i);
            String tag = localName(child);
            if (!("Content".equals(tag) || "Contents".equals(tag) || "Source".equals(tag) || "Url".equals(tag))) continue;
            for (String attr : EXTERNAL_ATTRS) {
                String value = clean(child.getAttribute(attr));
                if (value.length() > 0 && looksLikeScreenReference(value)) return new ReferenceValue(tag + "." + attr, value);
            }
            String text = clean(child.getTextContent());
            if (looksLikeScreenReference(text)) return new ReferenceValue(tag + ".text", text);
        }
        return null;
    }

    private boolean hasInlineUi(Element page) {
        NodeList all = page.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            if (!(all.item(i) instanceof Element)) continue;
            String tag = localName((Element) all.item(i));
            if (STRUCTURAL.contains(tag) || "Tabpage".equals(tag)) continue;
            return true;
        }
        return false;
    }

    private List<Element> directTabpages(Element tab) {
        List<Element> result = new ArrayList<Element>();
        NodeList children = tab.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element)) continue;
            Element child = (Element) children.item(i);
            String tag = localName(child);
            if ("Tabpage".equals(tag)) result.add(child);
            else if ("Tabpages".equals(tag)) {
                NodeList pages = child.getChildNodes();
                for (int p = 0; p < pages.getLength(); p++) {
                    if (pages.item(p) instanceof Element && "Tabpage".equals(localName((Element) pages.item(p))))
                        result.add((Element) pages.item(p));
                }
            }
        }
        return result;
    }

    private boolean shouldTraverse(String tag) {
        return !("Script".equals(tag) || "Dataset".equals(tag) || "DataSet".equals(tag)
                || "Formats".equals(tag) || "Format".equals(tag) || "Band".equals(tag) || "Cell".equals(tag));
    }

    private static String normalizeReference(String value) {
        String v = clean(value).replace('\\', '/');
        while (v.startsWith("././")) v = v.substring(2);
        return v;
    }
    private static String stripQueryAndFragment(String value) {
        int q = value.indexOf('?');
        int h = value.indexOf('#');
        int end = value.length();
        if (q >= 0 && q < end) end = q;
        if (h >= 0 && h < end) end = h;
        return value.substring(0, end);
    }
    private static String normalizeFsPath(String value) { return value == null ? "" : value.replace('\\', '/'); }
    private static String trimLeadingSlash(String value) { String v=value; while(v.startsWith("/"))v=v.substring(1); return v; }
    private static String joinPath(String left, String right) {
        String l = left == null ? "" : left.replace('\\', '/');
        String r = right == null ? "" : right.replace('\\', '/');
        if (l.length() == 0) return r;
        if (!l.endsWith("/")) l += "/";
        while (r.startsWith("/")) r = r.substring(1);
        return l + r;
    }
    private static boolean looksLikeScreenReference(String value) {
        String low = value == null ? "" : value.trim().toLowerCase();
        return low.indexOf(".xfdl") >= 0 || low.indexOf("::") > 0;
    }
    private static boolean isRemote(String value) {
        String low = value == null ? "" : value.trim().toLowerCase();
        return low.startsWith("http://") || low.startsWith("https://") || low.startsWith("ftp://");
    }
    private static String replaceExtension(String path, String extension) {
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        int dot = path.lastIndexOf('.');
        if (dot <= slash) return path + extension;
        return path.substring(0, dot) + extension;
    }
    private static String relativeTarget(String parentTarget, String childTarget) {
        Path parent = new File(parentTarget).toPath().getParent();
        Path child = new File(childTarget).toPath();
        String rel = parent == null ? child.toString() : parent.relativize(child).toString();
        rel = rel.replace('\\', '/');
        if (!rel.startsWith(".") && rel.indexOf('/') < 0) rel = "./" + rel;
        return rel;
    }
    private static String relative(File root, File file) throws IOException {
        return root.getCanonicalFile().toPath().relativize(file.getCanonicalFile().toPath()).toString().replace('\\', '/');
    }
    private static String path(String parent, String id) { return parent == null || parent.length() == 0 ? id : parent + "." + id; }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String compact(String value) { return value == null ? "" : value.replaceAll("\\s+", " ").trim(); }
    private static int lineOf(String source, int offset) { int line=1; for(int i=0;i<offset&&i<source.length();i++) if(source.charAt(i)=='\n') line++; return line; }
    private static String localName(Element e) {
        String v=e.getLocalName(); if(v!=null&&v.length()>0)return v;
        v=e.getTagName(); int c=v.indexOf(':'); return c>=0?v.substring(c+1):v;
    }
    private static TabContentReference ref(String screenRel, String tabPath, String pagePath,
            String tabId, String pageId, int index, ReferenceValue raw, String normalized,
            String resolvedSource, String generatedTarget, String webSquareSrc, String method,
            TabContentReference.Status status, TabContentReference.LoadingMode loadingMode,
            boolean mixed, String message) {
        return new TabContentReference(screenRel, tabPath, pagePath, tabId, pageId, index,
                raw.attribute, raw.value, normalized, resolvedSource, generatedTarget, webSquareSrc,
                method, status, loadingMode, mixed, message);
    }

    private static class ReferenceValue {
        private final String attribute, value;
        private ReferenceValue(String attribute, String value) { this.attribute=attribute; this.value=value; }
    }
    private static class Resolution {
        private final File file; private final String method, message;
        private Resolution(File file, String method, String message) { this.file=file; this.method=method; this.message=message; }
    }
}
