package com.example.xfdltracker.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XfdlAnalysisResult {
    private final Map<String, FunctionInfo> functions = new LinkedHashMap<String, FunctionInfo>();
    private final List<EventBinding> events = new ArrayList<EventBinding>();

    public Map<String, FunctionInfo> getFunctions() { return functions; }
    public List<EventBinding> getEvents() { return events; }
}
