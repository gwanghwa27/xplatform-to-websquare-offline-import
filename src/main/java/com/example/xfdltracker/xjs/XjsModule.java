package com.example.xfdltracker.xjs;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XjsModule {
    private final File file;
    private final String relativePath;
    private final String source;
    private final List<String> includes = new ArrayList<String>();
    private final Map<String, XjsSymbol> functions = new LinkedHashMap<String, XjsSymbol>();
    private final Map<String, XjsSymbol> globals = new LinkedHashMap<String, XjsSymbol>();
    private final List<XjsSymbol> duplicateDefinitions = new ArrayList<XjsSymbol>();
    private final List<String> topLevelExecutableStatements = new ArrayList<String>();

    public XjsModule(File file, String relativePath, String source) {
        this.file = file;
        this.relativePath = relativePath;
        this.source = source;
    }
    public File getFile() { return file; }
    public String getRelativePath() { return relativePath; }
    public String getSource() { return source; }
    public List<String> getIncludes() { return includes; }
    public Map<String, XjsSymbol> getFunctions() { return functions; }
    public Map<String, XjsSymbol> getGlobals() { return globals; }
    public List<XjsSymbol> getDuplicateDefinitions() { return duplicateDefinitions; }
    public List<String> getTopLevelExecutableStatements() { return topLevelExecutableStatements; }
}
