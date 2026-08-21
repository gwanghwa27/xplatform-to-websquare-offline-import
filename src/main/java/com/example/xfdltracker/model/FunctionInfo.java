package com.example.xfdltracker.model;

import java.util.LinkedHashSet;
import java.util.Set;

public class FunctionInfo {
    private final String name;
    private final String body;
    private final int startLine;
    private final Set<String> calledFunctions = new LinkedHashSet<String>();

    public FunctionInfo(String name, String body, int startLine) {
        this.name = name;
        this.body = body;
        this.startLine = startLine;
    }

    public String getName() { return name; }
    public String getBody() { return body; }
    public int getStartLine() { return startLine; }
    public Set<String> getCalledFunctions() { return calledFunctions; }
}
