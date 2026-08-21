package com.example.xfdltracker.analyzer;

import com.example.xfdltracker.model.EventBinding;
import com.example.xfdltracker.model.FunctionInfo;
import com.example.xfdltracker.model.XfdlAnalysisResult;

import java.util.HashSet;
import java.util.Set;

public class UnusedFunctionAnalyzer {
    public Set<String> findUnused(XfdlAnalysisResult result) {
        Set<String> referenced = new HashSet<String>();
        for (EventBinding event : result.getEvents()) referenced.add(event.getFunctionName());
        for (FunctionInfo fn : result.getFunctions().values()) referenced.addAll(fn.getCalledFunctions());
        Set<String> unused = new HashSet<String>(result.getFunctions().keySet());
        unused.removeAll(referenced);
        return unused;
    }
}
