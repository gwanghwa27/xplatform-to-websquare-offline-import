package com.example.xfdltracker.xjs;

import com.example.xfdltracker.io.TextFileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XjsRepository {
    private final File sourceRoot;
    private final Map<String, XjsModule> modules = new LinkedHashMap<String, XjsModule>();
    private final Map<String, List<XjsSymbol>> functionIndex = new LinkedHashMap<String, List<XjsSymbol>>();
    private final Map<String, List<XjsSymbol>> globalIndex = new LinkedHashMap<String, List<XjsSymbol>>();

    public XjsRepository(File sourceRoot) {
        this.sourceRoot = sourceRoot;
    }

    public void add(File file, String encoding) throws Exception {
        String rel = relative(sourceRoot, file);
        String source = TextFileUtil.read(file, encoding);
        XjsModule module = new XjsModule(file, rel, source);
        new JavaScriptSymbolScanner().parseModule(module);
        modules.put(normalizePath(rel), module);
        index(functionIndex, module.getFunctions());
        index(globalIndex, module.getGlobals());
        for (XjsSymbol duplicate : module.getDuplicateDefinitions()) {
            Map<String, List<XjsSymbol>> target = duplicate.getType() == XjsSymbolType.FUNCTION
                    ? functionIndex : globalIndex;
            List<XjsSymbol> list = target.get(duplicate.getName());
            if (list == null) { list = new ArrayList<XjsSymbol>(); target.put(duplicate.getName(), list); }
            list.add(duplicate);
        }
    }

    private void index(Map<String, List<XjsSymbol>> target, Map<String, XjsSymbol> symbols) {
        for (XjsSymbol symbol : symbols.values()) {
            List<XjsSymbol> list = target.get(symbol.getName());
            if (list == null) {
                list = new ArrayList<XjsSymbol>();
                target.put(symbol.getName(), list);
            }
            list.add(symbol);
        }
    }

    public List<XjsSymbol> findFunctions(String name) { return ordered(functionIndex.get(name)); }
    public List<XjsSymbol> findGlobals(String name) { return ordered(globalIndex.get(name)); }
    public List<XjsModule> allModules() { return new ArrayList<XjsModule>(modules.values()); }

    public List<XjsModule> resolveInclude(String includePath) {
        String normalized = normalizeInclude(includePath);
        List<XjsModule> exact = new ArrayList<XjsModule>();
        List<XjsModule> byName = new ArrayList<XjsModule>();
        String fileName = normalized;
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0) fileName = normalized.substring(slash + 1);

        for (Map.Entry<String, XjsModule> e : modules.entrySet()) {
            String path = e.getKey();
            if (path.equals(normalized) || path.endsWith("/" + normalized)) exact.add(e.getValue());
            else if (path.equals(fileName) || path.endsWith("/" + fileName)) byName.add(e.getValue());
        }
        if (!exact.isEmpty()) return sortModules(exact);
        return sortModules(byName);
    }

    public XjsModule getModule(String relativePath) { return modules.get(normalizePath(relativePath)); }

    private List<XjsSymbol> ordered(List<XjsSymbol> source) {
        if (source == null) return Collections.emptyList();
        List<XjsSymbol> copy = new ArrayList<XjsSymbol>(source);
        Collections.sort(copy, new Comparator<XjsSymbol>() {
            public int compare(XjsSymbol a, XjsSymbol b) {
                int c = a.getRelativePath().compareToIgnoreCase(b.getRelativePath());
                if (c != 0) return c;
                return a.getLine() - b.getLine();
            }
        });
        return copy;
    }

    private List<XjsModule> sortModules(List<XjsModule> source) {
        Collections.sort(source, new Comparator<XjsModule>() {
            public int compare(XjsModule a, XjsModule b) {
                return a.getRelativePath().compareToIgnoreCase(b.getRelativePath());
            }
        });
        return source;
    }

    public static String normalizeInclude(String value) {
        String v = value == null ? "" : value.trim().replace('\\', '/');
        int service = v.indexOf("::");
        if (service >= 0) v = v.substring(service + 2);
        while (v.startsWith("./")) v = v.substring(2);
        while (v.startsWith("/")) v = v.substring(1);
        return normalizePath(v);
    }

    public static String normalizePath(String value) {
        return value == null ? "" : value.replace('\\', '/').toLowerCase();
    }

    private static String relative(File root, File file) throws IOException {
        Path rootPath = root.getCanonicalFile().toPath();
        Path filePath = file.getCanonicalFile().toPath();
        return rootPath.relativize(filePath).toString().replace('\\', '/');
    }
}
