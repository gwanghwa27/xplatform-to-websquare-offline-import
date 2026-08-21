package com.example.xfdltracker.analyzer;

import com.example.xfdltracker.model.EventBinding;
import com.example.xfdltracker.model.FunctionInfo;
import com.example.xfdltracker.model.XfdlAnalysisResult;

import java.util.HashSet;
import java.util.Set;

public class CallTreePrinter {
    public void print(XfdlAnalysisResult result) {
        for (EventBinding event : result.getEvents()) {
            System.out.println(event.getComponentId() + "." + event.getEventName());
            printFunction(event.getFunctionName(), result, "  ", new HashSet<String>());
            System.out.println();
        }
    }

    private void printFunction(String functionName, XfdlAnalysisResult result, String indent, Set<String> path) {
        System.out.println(indent + "-> " + functionName);
        if (!path.add(functionName)) {
            System.out.println(indent + "  [재귀 호출]");
            return;
        }
        FunctionInfo function = result.getFunctions().get(functionName);
        if (function == null) {
            System.out.println(indent + "  [외부 함수 또는 미정의]");
            path.remove(functionName);
            return;
        }
        for (String called : function.getCalledFunctions()) {
            printFunction(called, result, indent + "  ", path);
        }
        path.remove(functionName);
    }
}
