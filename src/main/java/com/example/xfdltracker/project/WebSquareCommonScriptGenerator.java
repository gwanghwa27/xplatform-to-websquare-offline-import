package com.example.xfdltracker.project;

import com.example.xfdltracker.converter.WebSquareScriptConverter;
import com.example.xfdltracker.io.TextFileUtil;
import com.example.xfdltracker.model.XfdlAnalysisResult;
import java.io.File;

/** XPlatform .xjs 공통 스크립트를 WebSquare용 .js 형태로 1차 변환한다. */
public class WebSquareCommonScriptGenerator {
    public void generate(File xjsFile, File outputFile, XfdlAnalysisResult analysis, String encoding) throws Exception {
        String original = TextFileUtil.read(xjsFile, encoding);
        String converted = new WebSquareScriptConverter().convert(original, analysis);
        TextFileUtil.writeUtf8(outputFile, converted);
    }
}
