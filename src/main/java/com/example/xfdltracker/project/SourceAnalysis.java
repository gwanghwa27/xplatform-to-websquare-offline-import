package com.example.xfdltracker.project;

import com.example.xfdltracker.model.XfdlAnalysisResult;
import java.io.File;

public class SourceAnalysis {
    private final File sourceFile;
    private final String relativePath;
    private final String sourceType;
    private final XfdlAnalysisResult analysis;

    public SourceAnalysis(File sourceFile, String relativePath, String sourceType, XfdlAnalysisResult analysis) {
        this.sourceFile = sourceFile;
        this.relativePath = relativePath;
        this.sourceType = sourceType;
        this.analysis = analysis;
    }

    public File getSourceFile() { return sourceFile; }
    public String getRelativePath() { return relativePath; }
    public String getSourceType() { return sourceType; }
    public XfdlAnalysisResult getAnalysis() { return analysis; }
}
