package com.example.xfdltracker.tab;

import com.example.xfdltracker.parser.XfdlReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reads XPlatform TypeDefinition Services used by logical paths such as Base::Search.xfdl. */
public class XPlatformServiceRegistry {
    public static class Service {
        private final String prefixId;
        private final String type;
        private final String url;
        private final File typeDefinitionFile;

        private Service(String prefixId, String type, String url, File typeDefinitionFile) {
            this.prefixId = prefixId == null ? "" : prefixId;
            this.type = type == null ? "" : type;
            this.url = url == null ? "" : url;
            this.typeDefinitionFile = typeDefinitionFile;
        }
        public String getPrefixId() { return prefixId; }
        public String getType() { return type; }
        public String getUrl() { return url; }
        public File getTypeDefinitionFile() { return typeDefinitionFile; }
    }

    private final Map<String, Service> services = new LinkedHashMap<String, Service>();
    private File typeDefinitionFile;
    private String warning = "";

    public static XPlatformServiceRegistry load(File sourceRoot, File xfdlFile, Document xfdl) {
        XPlatformServiceRegistry out = new XPlatformServiceRegistry();
        try {
            File typeDef = out.resolveTypeDefinition(sourceRoot, xfdlFile, xfdl);
            if (typeDef == null || !typeDef.isFile()) return out;
            out.typeDefinitionFile = typeDef.getCanonicalFile();
            Document doc = new XfdlReader().read(typeDef);
            NodeList all = doc.getElementsByTagName("*");
            for (int i = 0; i < all.getLength(); i++) {
                Node node = all.item(i);
                if (!(node instanceof Element)) continue;
                Element e = (Element) node;
                if (!"Service".equals(localName(e))) continue;
                String prefix = e.getAttribute("prefixid").trim();
                if (prefix.length() == 0) prefix = e.getAttribute("id").trim();
                if (prefix.length() == 0) continue;
                Service service = new Service(prefix, e.getAttribute("type").trim(),
                        e.getAttribute("url").trim(), out.typeDefinitionFile);
                if (!out.services.containsKey(prefix.toLowerCase())) out.services.put(prefix.toLowerCase(), service);
            }
        } catch (Exception e) {
            out.warning = "TypeDefinition Service 분석 실패: " + safe(e.getMessage(), e.getClass().getName());
        }
        return out;
    }

    public Service find(String prefix) {
        return services.get(prefix == null ? "" : prefix.toLowerCase());
    }
    public File getTypeDefinitionFile() { return typeDefinitionFile; }
    public String getWarning() { return warning; }
    public java.util.List<Service> getServices() {
        return java.util.Collections.unmodifiableList(new java.util.ArrayList<Service>(services.values()));
    }

    private File resolveTypeDefinition(File sourceRoot, File xfdlFile, Document xfdl) throws Exception {
        NodeList all = xfdl.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            if (!(all.item(i) instanceof Element)) continue;
            Element e = (Element) all.item(i);
            if (!"TypeDefinition".equals(localName(e))) continue;
            String url = e.getAttribute("url").trim();
            if (url.length() == 0 || isRemote(url)) continue;
            File candidate = new File(xfdlFile.getParentFile(), url.replace('/', File.separatorChar).replace('\\', File.separatorChar));
            if (candidate.isFile()) return candidate.getCanonicalFile();
            candidate = new File(sourceRoot, normalizeRelative(url));
            if (candidate.isFile()) return candidate.getCanonicalFile();
        }
        File fallback = new File(sourceRoot, "default_typedef.xml");
        return fallback.isFile() ? fallback.getCanonicalFile() : null;
    }

    private static String localName(Element e) {
        String v = e.getLocalName();
        if (v != null && v.length() > 0) return v;
        v = e.getTagName();
        int colon = v.indexOf(':');
        return colon >= 0 ? v.substring(colon + 1) : v;
    }
    private static boolean isRemote(String v) {
        String low = v == null ? "" : v.trim().toLowerCase();
        return low.startsWith("http://") || low.startsWith("https://") || low.startsWith("ftp://");
    }
    private static String normalizeRelative(String v) {
        String s = v == null ? "" : v.replace('\\', '/');
        while (s.startsWith("./")) s = s.substring(2);
        while (s.startsWith("/")) s = s.substring(1);
        return s.replace('/', File.separatorChar);
    }
    private static String safe(String value, String fallback) {
        return value == null || value.length() == 0 ? fallback : value.replace('\r', ' ').replace('\n', ' ');
    }
}
