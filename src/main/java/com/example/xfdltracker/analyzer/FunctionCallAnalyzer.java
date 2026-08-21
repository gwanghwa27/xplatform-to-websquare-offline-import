package com.example.xfdltracker.analyzer;

import com.example.xfdltracker.model.FunctionInfo;
import com.example.xfdltracker.model.XfdlAnalysisResult;
import com.example.xfdltracker.util.JavaScriptCleaner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FunctionCallAnalyzer {
    /* fn(...), this.fn(...), ds.addRow(...), application.fn(...) 호출을 찾는다. 보고되는 이름은 마지막 메서드명이다. */
    private static final Pattern FUNCTION_CALL = Pattern.compile(
        "(?<![A-Za-z0-9_$])(?:(?:[A-Za-z_$][A-Za-z0-9_$]*)\\s*\\.\\s*)?([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(");
    private static final Set<String> EXCLUDED = new HashSet<String>(Arrays.asList(
        "if","for","while","switch","catch","function","return","typeof","delete","new","throw","with"));

    public void analyze(XfdlAnalysisResult result) {
        JavaScriptCleaner cleaner = new JavaScriptCleaner();
        for (FunctionInfo fn : result.getFunctions().values()) {
            Matcher m = FUNCTION_CALL.matcher(cleaner.clean(fn.getBody()));
            while (m.find()) {
                String called = m.group(1);
                if (!EXCLUDED.contains(called)) fn.getCalledFunctions().add(called);
            }
        }
    }
}
