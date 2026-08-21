package com.example.xfdltracker.xjs;

import java.util.LinkedHashSet;
import java.util.Set;

public class XjsSymbol {
    private final XjsSymbolType type;
    private final String name;
    private final String relativePath;
    private final String source;
    private final String body;
    private final int line;
    private final Set<String> calledFunctions = new LinkedHashSet<String>();
    private final Set<String> referencedIdentifiers = new LinkedHashSet<String>();

    public XjsSymbol(XjsSymbolType type, String name, String relativePath,
                     String source, String body, int line) {
        this.type = type;
        this.name = name;
        this.relativePath = relativePath;
        this.source = source;
        this.body = body == null ? "" : body;
        this.line = line;
    }

    public XjsSymbolType getType() { return type; }
    public String getName() { return name; }
    public String getRelativePath() { return relativePath; }
    public String getSource() { return source; }
    public String getBody() { return body; }
    public int getLine() { return line; }
    public Set<String> getCalledFunctions() { return calledFunctions; }
    public Set<String> getReferencedIdentifiers() { return referencedIdentifiers; }
    public String key() { return relativePath + "#" + type + "#" + name; }
}
