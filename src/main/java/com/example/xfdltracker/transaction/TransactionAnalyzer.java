package com.example.xfdltracker.transaction;

import com.example.xfdltracker.util.JavaScriptCleaner;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lightweight balanced-parenthesis parser for XPlatform transaction calls. */
public class TransactionAnalyzer {
    private static final Pattern START = Pattern.compile(
            "(?<![A-Za-z0-9_$.])(?:this\\s*\\.\\s*)?transaction\\s*\\(");

    public List<TransactionCall> analyze(String source) {
        List<TransactionCall> result = new ArrayList<TransactionCall>();
        if (source == null || source.length() == 0) return result;
        String cleaned = new JavaScriptCleaner().clean(source);
        Matcher matcher = START.matcher(cleaned);
        while (matcher.find()) {
            if (isFunctionDeclaration(cleaned, matcher.start())) continue;
            int open = cleaned.indexOf('(', matcher.start());
            int close = findClosingParen(cleaned, open);
            if (open < 0 || close < 0) continue;
            String argCleaned = cleaned.substring(open + 1, close);
            List<int[]> ranges = splitArgumentRanges(argCleaned);
            List<String> args = new ArrayList<String>();
            for (int[] range : ranges) {
                int from = open + 1 + range[0];
                int to = open + 1 + range[1];
                args.add(source.substring(from, to).trim());
            }
            result.add(new TransactionCall(
                    lineOf(source, matcher.start()),
                    args,
                    source.substring(matcher.start(), close + 1)));
        }
        return result;
    }

    private boolean isFunctionDeclaration(String source, int start) {
        int p = start - 1;
        while (p >= 0 && Character.isWhitespace(source.charAt(p))) p--;
        int end = p + 1;
        while (p >= 0 && (Character.isLetterOrDigit(source.charAt(p))
                || source.charAt(p) == '_' || source.charAt(p) == '$')) p--;
        return "function".equals(source.substring(p + 1, end));
    }

    private int findClosingParen(String source, int opening) {
        if (opening < 0) return -1;
        int depth = 0;
        for (int i = opening; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private List<int[]> splitArgumentRanges(String text) {
        List<int[]> result = new ArrayList<int[]>();
        if (text.trim().length() == 0) return result;
        int start = 0, paren = 0, bracket = 0, brace = 0;
        for (int i = 0; i <= text.length(); i++) {
            boolean end = i == text.length();
            char c = end ? ',' : text.charAt(i);
            if (!end) {
                if (c == '(') paren++;
                else if (c == ')' && paren > 0) paren--;
                else if (c == '[') bracket++;
                else if (c == ']' && bracket > 0) bracket--;
                else if (c == '{') brace++;
                else if (c == '}' && brace > 0) brace--;
            }
            if ((end || c == ',') && paren == 0 && bracket == 0 && brace == 0) {
                result.add(new int[] { start, i });
                start = i + 1;
            }
        }
        return result;
    }

    private int lineOf(String source, int pos) {
        int line = 1;
        for (int i = 0; i < pos && i < source.length(); i++) if (source.charAt(i) == '\n') line++;
        return line;
    }
}
