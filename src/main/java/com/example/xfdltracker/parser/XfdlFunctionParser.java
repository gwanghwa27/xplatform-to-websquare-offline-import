package com.example.xfdltracker.parser;

import com.example.xfdltracker.model.FunctionInfo;
import com.example.xfdltracker.model.XfdlAnalysisResult;
import com.example.xfdltracker.util.JavaScriptCleaner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XfdlFunctionParser {
    private static final Pattern ASSIGNMENT_FUNCTION = Pattern.compile(
        "(?:this\\s*\\.\\s*)?([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*function\\s*\\([^)]*\\)\\s*\\{");
    private static final Pattern DECLARED_FUNCTION = Pattern.compile(
        "function\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\([^)]*\\)\\s*\\{");

    public void parse(String originalScript, XfdlAnalysisResult result) {
        JavaScriptCleaner cleaner = new JavaScriptCleaner();
        String cleaned = cleaner.clean(originalScript);
        extractByPattern(originalScript, cleaned, ASSIGNMENT_FUNCTION, result);
        extractByPattern(originalScript, cleaned, DECLARED_FUNCTION, result);
    }

    private void extractByPattern(String original, String cleaned, Pattern pattern, XfdlAnalysisResult result) {
        Matcher m = pattern.matcher(cleaned);
        while (m.find()) {
            String name = m.group(1);
            int open = m.end() - 1;
            int close = findClosingBrace(cleaned, open);
            if (close < 0) continue;
            String body = original.substring(open + 1, close);
            int line = calculateLine(original, m.start());
            if (!result.getFunctions().containsKey(name)) {
                result.getFunctions().put(name, new FunctionInfo(name, body, line));
            }
        }
    }

    private int findClosingBrace(String source, int openingBrace) {
        int depth = 0;
        for (int i = openingBrace; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private int calculateLine(String source, int position) {
        int line = 1;
        for (int i = 0; i < position; i++) if (source.charAt(i) == '\n') line++;
        return line;
    }
}
