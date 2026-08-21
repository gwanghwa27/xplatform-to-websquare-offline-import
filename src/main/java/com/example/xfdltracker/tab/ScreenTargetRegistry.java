package com.example.xfdltracker.tab;

import com.example.xfdltracker.parser.XfdlReader;
import org.w3c.dom.Document;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Project-wide source XFDL -> generated WebSquare XML mapping.
 * Static Tab content and runtime Tab operations intentionally share this resolver.
 */
public class ScreenTargetRegistry {
    public static class Resolution {
        private final String rawReference;
        private final String resolvedSource;
        private final String generatedTarget;
        private final String webSquareSrc;
        private final String method;
        private final String message;
        private final boolean resolved;

        private Resolution(String rawReference, String resolvedSource, String generatedTarget,
                           String webSquareSrc, String method, String message, boolean resolved) {
            this.rawReference = safe(rawReference);
            this.resolvedSource = safe(resolvedSource);
            this.generatedTarget = safe(generatedTarget);
            this.webSquareSrc = safe(webSquareSrc);
            this.method = safe(method);
            this.message = safe(message);
            this.resolved = resolved;
        }
        public String getRawReference() { return rawReference; }
        public String getResolvedSource() { return resolvedSource; }
        public String getGeneratedTarget() { return generatedTarget; }
        public String getWebSquareSrc() { return webSquareSrc; }
        public String getMethod() { return method; }
        public String getMessage() { return message; }
        public boolean isResolved() { return resolved; }
    }

    private final File sourceRoot;
    private final Map<String, File> byRelative = new LinkedHashMap<String, File>();
    private final Map<String, String> targetByRelative = new LinkedHashMap<String, String>();

    public ScreenTargetRegistry(File sourceRoot, List<File> projectFiles) throws Exception {
        this.sourceRoot = sourceRoot.getCanonicalFile();
        if (projectFiles != null) {
            for (File file : projectFiles) {
                if (file == null || !file.isFile() || !file.getName().toLowerCase().endsWith(".xfdl")) continue;
                File canonical = file.getCanonicalFile();
                if (!insideProject(canonical)) continue;
                String rel = relative(this.sourceRoot, canonical);
                String key = canonicalKey(rel);
                if (!byRelative.containsKey(key)) {
                    byRelative.put(key, canonical);
                    targetByRelative.put(key, replaceExtension(rel, ".xml"));
                }
            }
        }
    }

    public Resolution resolve(File ownerScreen, String ownerRelative, String rawReference) throws Exception {
        String normalized = normalizeReference(rawReference);
        if (normalized.length() == 0) return unresolved(rawReference, "EMPTY", "빈 화면 경로");
        if (isRemote(normalized)) return unresolved(rawReference, "REMOTE_URL", "원격 URL은 프로젝트 XFDL target으로 확정하지 않음");
        String filePart = stripQueryAndFragment(normalized);
        if (!filePart.toLowerCase().endsWith(".xfdl")) {
            return unresolved(rawReference, "UNSUPPORTED_REFERENCE", "XFDL 화면 경로가 아님");
        }

        Document doc = new XfdlReader().read(ownerScreen);
        XPlatformServiceRegistry services = XPlatformServiceRegistry.load(sourceRoot, ownerScreen, doc);
        File resolved = null;
        String method = "";
        String message = "";

        int serviceMark = filePart.indexOf("::");
        if (serviceMark > 0) {
            String prefix = filePart.substring(0, serviceMark);
            String tail = filePart.substring(serviceMark + 2);
            XPlatformServiceRegistry.Service service = services.find(prefix);
            if (service == null) return unresolved(rawReference, "SERVICE_PREFIX", "TypeDefinition Service 미정의: " + prefix);
            if (service.getType().length() > 0 && !"form".equalsIgnoreCase(service.getType())) {
                return unresolved(rawReference, "SERVICE_PREFIX", "Service type이 form이 아님: " + service.getType());
            }
            String serviceUrl = normalizeFs(service.getUrl());
            if (isRemote(serviceUrl)) return unresolved(rawReference, "SERVICE_PREFIX_REMOTE", "Service URL이 원격 경로임");
            File base = service.getTypeDefinitionFile() == null ? sourceRoot : service.getTypeDefinitionFile().getParentFile();
            resolved = caseAwareResolve(base, join(serviceUrl, tail));
            if (isRegistered(resolved)) { method = "SERVICE_PREFIX"; message = "TypeDefinition Service url 기준"; }
            if (!isRegistered(resolved)) {
                resolved = caseAwareResolve(sourceRoot, join(serviceUrl, tail));
                if (isRegistered(resolved)) { method = "SERVICE_PREFIX_ROOT"; message = "프로젝트 루트 + Service url 기준"; }
            }
            if (!isRegistered(resolved)) {
                resolved = caseAwareResolve(sourceRoot, join(prefix, tail));
                if (isRegistered(resolved)) { method = "SERVICE_PREFIX_DIRECTORY"; message = "프로젝트 prefix 디렉터리 기준 fallback"; }
            }
        } else {
            resolved = caseAwareResolve(ownerScreen.getParentFile(), filePart);
            if (isRegistered(resolved)) { method = "RELATIVE"; message = "현재 XFDL 기준 상대 경로"; }
            if (!isRegistered(resolved)) {
                resolved = caseAwareResolve(sourceRoot, trimLeadingSlash(filePart));
                if (isRegistered(resolved)) { method = "PROJECT_RELATIVE"; message = "프로젝트 루트 기준 경로"; }
            }
        }

        if (!isRegistered(resolved)) {
            return unresolved(rawReference, serviceMark > 0 ? "SERVICE_PREFIX" : "PATH", "프로젝트 source tree에서 대상 XFDL을 찾지 못함: " + rawReference);
        }
        String sourceRel = relative(sourceRoot, resolved);
        String target = targetForSource(sourceRel);
        if (target.length() == 0) return unresolved(rawReference, "REGISTRY", "대상 XFDL이 ScreenTargetRegistry에 등록되지 않음: " + sourceRel);
        String ownerTarget = replaceExtension(ownerRelative, ".xml");
        return new Resolution(rawReference, sourceRel, target, relativeTarget(ownerTarget, target), method, message, true);
    }

    public String targetForSource(String sourceRelative) {
        String value = targetByRelative.get(canonicalKey(sourceRelative));
        return value == null ? "" : value;
    }

    public Map<String, String> getSourceToTargetMap() {
        Map<String, String> out = new LinkedHashMap<String, String>();
        for (Map.Entry<String, File> e : byRelative.entrySet()) {
            try {
                String rel = relative(sourceRoot, e.getValue());
                out.put(rel, targetByRelative.get(e.getKey()));
            } catch (Exception ignored) { }
        }
        return Collections.unmodifiableMap(out);
    }

    public List<String> allSources() {
        return new ArrayList<String>(getSourceToTargetMap().keySet());
    }

    /** Runtime translation table for one owner screen. No extension-only fallback is used. */
    public Map<String, String> buildRuntimePathMap(File ownerScreen, String ownerRelative) throws Exception {
        Map<String, String> out = new LinkedHashMap<String, String>();
        String ownerTarget = replaceExtension(ownerRelative, ".xml");
        String ownerDir = parentPath(ownerRelative);
        for (Map.Entry<String, File> e : byRelative.entrySet()) {
            String sourceRel = relative(sourceRoot, e.getValue());
            String target = targetByRelative.get(e.getKey());
            String targetFromOwner = relativeTarget(ownerTarget, target);
            putAlias(out, sourceRel, targetFromOwner);
            String sourceFromOwner = relativePath(ownerDir, sourceRel);
            putAlias(out, sourceFromOwner, targetFromOwner);
            if (sourceFromOwner.startsWith("./")) putAlias(out, sourceFromOwner.substring(2), targetFromOwner);
        }

        Document doc = new XfdlReader().read(ownerScreen);
        XPlatformServiceRegistry services = XPlatformServiceRegistry.load(sourceRoot, ownerScreen, doc);
        for (XPlatformServiceRegistry.Service service : services.getServices()) {
            String base = normalizeFs(service.getUrl());
            if (isRemote(base)) continue;
            for (Map.Entry<String, File> e : byRelative.entrySet()) {
                String sourceRel = relative(sourceRoot, e.getValue());
                String tail = tailUnderBase(sourceRel, base);
                if (tail.length() == 0) {
                    tail = tailUnderBase(sourceRel, service.getPrefixId());
                }
                if (tail.length() > 0) {
                    putAlias(out, service.getPrefixId() + "::" + tail, relativeTarget(ownerTarget, targetByRelative.get(e.getKey())));
                }
            }
        }
        return out;
    }

    private static void putAlias(Map<String,String> out, String source, String target) {
        String key = normalizeRuntimeKey(source);
        if (key.length() == 0 || target == null || target.length() == 0) return;
        String old = out.get(key);
        if (old == null) out.put(key, target);
        else if (!old.equals(target)) out.remove(key); // ambiguous alias must not select an arbitrary screen
    }
    private static String normalizeRuntimeKey(String value) {
        String v = normalizeFs(value).trim();
        while (v.startsWith("./")) v = v.substring(2);
        return v;
    }
    private static String parentPath(String value) {
        String v=normalizeFs(value); int slash=v.lastIndexOf('/'); return slash<0?"":v.substring(0,slash);
    }
    private static String relativePath(String fromDir, String toFile) {
        try {
            java.nio.file.Path from=java.nio.file.Paths.get(fromDir.length()==0?".":fromDir);
            java.nio.file.Path to=java.nio.file.Paths.get(normalizeFs(toFile));
            return from.relativize(to).toString().replace('\\','/');
        } catch(Exception e) { return normalizeFs(toFile); }
    }
    private static String tailUnderBase(String sourceRel, String base) {
        String s=normalizeFs(sourceRel); String b=normalizeFs(base);
        while(b.startsWith("./"))b=b.substring(2); while(b.startsWith("/"))b=b.substring(1);
        if(b.length()==0)return "";
        if(s.toLowerCase().startsWith((b+"/").toLowerCase()))return s.substring(b.length()+1);
        return "";
    }

    private boolean isRegistered(File file) throws Exception {
        if (file == null || !file.isFile() || !insideProject(file)) return false;
        return byRelative.containsKey(canonicalKey(relative(sourceRoot, file.getCanonicalFile())));
    }

    private boolean insideProject(File file) throws Exception {
        if (file == null) return false;
        String root = sourceRoot.getCanonicalPath();
        String child = file.getCanonicalPath();
        return child.equals(root) || child.startsWith(root + File.separator);
    }

    private File caseAwareResolve(File base, String relative) throws Exception {
        if (base == null) return null;
        File current = base.getCanonicalFile();
        String[] parts = normalizeFs(relative).split("/");
        for (String part : parts) {
            if (part.length() == 0 || ".".equals(part)) continue;
            if ("..".equals(part)) {
                current = current.getParentFile();
                if (current == null || !insideOrAncestor(current)) return null;
                continue;
            }
            File exact = new File(current, part);
            if (exact.exists()) { current = exact; continue; }
            File[] children = current.listFiles();
            if (children == null) return null;
            File match = null;
            for (File child : children) {
                if (!child.getName().equalsIgnoreCase(part)) continue;
                if (match != null) return null;
                match = child;
            }
            if (match == null) return null;
            current = match;
        }
        return current.getCanonicalFile();
    }

    private boolean insideOrAncestor(File file) throws Exception {
        String root = sourceRoot.getCanonicalPath();
        String p = file.getCanonicalPath();
        return p.equals(root) || p.startsWith(root + File.separator) || root.startsWith(p + File.separator);
    }

    private static Resolution unresolved(String raw, String method, String message) {
        return new Resolution(raw, "", "", "", method, message, false);
    }
    private static String normalizeReference(String value) {
        String v = safe(value).trim().replace('\\', '/');
        while (v.startsWith("././")) v = v.substring(2);
        return v;
    }
    private static String stripQueryAndFragment(String value) {
        int end = value.length();
        int q = value.indexOf('?'); if (q >= 0 && q < end) end = q;
        int h = value.indexOf('#'); if (h >= 0 && h < end) end = h;
        return value.substring(0, end);
    }
    private static boolean isRemote(String value) {
        String v = safe(value).trim().toLowerCase();
        return v.startsWith("http://") || v.startsWith("https://") || v.startsWith("ftp://");
    }
    private static String trimLeadingSlash(String value) {
        String v = normalizeFs(value); while (v.startsWith("/")) v = v.substring(1); return v;
    }
    private static String normalizeFs(String value) { return safe(value).replace('\\', '/'); }
    private static String join(String left, String right) {
        String l = normalizeFs(left), r = normalizeFs(right);
        if (l.length() == 0) return r;
        return l.endsWith("/") ? l + r : l + "/" + r;
    }
    private static String canonicalKey(String value) { return normalizeFs(value).toLowerCase(); }
    private static String replaceExtension(String path, String extension) {
        String v = normalizeFs(path); int dot = v.lastIndexOf('.'); int slash = v.lastIndexOf('/');
        return dot > slash ? v.substring(0, dot) + extension : v + extension;
    }
    private static String relative(File root, File file) throws Exception {
        Path r = root.getCanonicalFile().toPath(); Path f = file.getCanonicalFile().toPath();
        return r.relativize(f).toString().replace('\\', '/');
    }
    private static String relativeTarget(String parentTarget, String childTarget) {
        try {
            String parent = normalizeFs(parentTarget); int slash = parent.lastIndexOf('/');
            String parentDir = slash >= 0 ? parent.substring(0, slash) : "";
            java.nio.file.Path p = java.nio.file.Paths.get(parentDir.length() == 0 ? "." : parentDir);
            java.nio.file.Path c = java.nio.file.Paths.get(normalizeFs(childTarget));
            String rel = p.relativize(c).toString().replace('\\', '/');
            // Preserve the Phase 3 static-Tab baseline for same-directory child pages.
            if (!rel.startsWith(".") && rel.indexOf('/') < 0) rel = "./" + rel;
            return rel;
        } catch (Exception e) { return normalizeFs(childTarget); }
    }
    private static String safe(String value) { return value == null ? "" : value; }
}
