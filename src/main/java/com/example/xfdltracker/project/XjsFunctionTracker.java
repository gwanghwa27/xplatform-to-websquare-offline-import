package com.example.xfdltracker.project;

import com.example.xfdltracker.analyzer.FunctionCallAnalyzer;
import com.example.xfdltracker.io.TextFileUtil;
import com.example.xfdltracker.model.XfdlAnalysisResult;
import com.example.xfdltracker.parser.XfdlFunctionParser;
import java.io.File;

public class XjsFunctionTracker {
    public XfdlAnalysisResult analyze(File xjsFile, String encoding) throws Exception {
        String script = TextFileUtil.read(xjsFile, encoding);
        XfdlAnalysisResult result = new XfdlAnalysisResult();
        new XfdlFunctionParser().parse(script, result);
        new FunctionCallAnalyzer().analyze(result);
        return result;
    }
}
