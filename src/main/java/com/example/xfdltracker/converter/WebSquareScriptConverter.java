package com.example.xfdltracker.converter;

import com.example.xfdltracker.model.XfdlAnalysisResult;
import com.example.xfdltracker.script.ScopeAwareComponentPropertyConverter;
import com.example.xfdltracker.util.JavaScriptCleaner;
import com.example.xfdltracker.tab.TabRuntimePlan;
import com.example.xfdltracker.tab.TabRuntimeScriptConverter;
import com.example.xfdltracker.tab.CrossScreenScriptConverter;
import com.example.xfdltracker.tab.ScopeBridgeScriptConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Compilable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * JDK 1.8 호환 XPlatform -> WebSquare JavaScript 1차 변환기.
 * 정규식 기반 변환 전에 주석과 문자열 리터럴을 보호한다.
 */
public class WebSquareScriptConverter {

    private static final Pattern THIS_FUNCTION = Pattern.compile(
            "this\\s*\\.\\s*([A-Za-z_$][A-Za-z0-9_$]*)"
          + "\\s*=\\s*function\\s*\\(([^)]*)\\)");

    private static final Pattern DECLARED_FUNCTION = Pattern.compile(
            "(?<![A-Za-z0-9_$.])function\\s+"
          + "([A-Za-z_$][A-Za-z0-9_$]*)"
          + "\\s*\\(([^)]*)\\)");

    public String convert(String originalScript, XfdlAnalysisResult analysis) {
        return convert(originalScript, analysis,
                Collections.<String, String>emptyMap(),
                Collections.<String>emptySet(),
                Collections.<String, String>emptyMap());
    }

    public String convert(
            String originalScript,
            XfdlAnalysisResult analysis,
            Map<String, String> componentIdMap) {
        return convert(originalScript, analysis, componentIdMap, Collections.<String>emptySet(),
                Collections.<String, String>emptyMap());
    }

    public String convert(
            String originalScript,
            XfdlAnalysisResult analysis,
            Map<String, String> componentIdMap,
            Set<String> datasetIds) {
        return convert(originalScript, analysis, componentIdMap, datasetIds,
                Collections.<String, String>emptyMap());
    }

    public String convert(
            String originalScript,
            XfdlAnalysisResult analysis,
            Map<String, String> componentIdMap,
            Set<String> datasetIds,
            Map<String, String> targetComponentTypeMap) {
        return convert(originalScript, analysis, componentIdMap, datasetIds, targetComponentTypeMap, null);
    }

    public String convert(
            String originalScript,
            XfdlAnalysisResult analysis,
            Map<String, String> componentIdMap,
            Set<String> datasetIds,
            Map<String, String> targetComponentTypeMap,
            TabRuntimePlan tabRuntimePlan) {

        String originalSource = originalScript == null ? "" : originalScript;
        String source = originalSource;
        if (tabRuntimePlan != null && tabRuntimePlan.isRuntimeRequired()) {
            source = new TabRuntimeScriptConverter().convert(source, tabRuntimePlan, componentIdMap);
            source = new CrossScreenScriptConverter().convert(source, tabRuntimePlan, componentIdMap);
            source = new ScopeBridgeScriptConverter().convert(source, tabRuntimePlan);
        }

        debug("STEP 0 : 원본", source);

        // XPlatform include 문법은 일반 JavaScript 문법이 아니므로 실행 가능한 include 라인을 먼저 제거한다.
        // 주석 안의 include는 실행 include로 처리하지 않는다.
        source = convertIncludeStatements(source);

        JavaScriptCleaner.ProtectionResult protection =
                JavaScriptCleaner.protectCommentsAndStrings(source);
        String protectedSource = protection.getSource();

        // XPlatform 타입 변수 선언은 일반 JavaScript 문법이 아니므로 제거한다.
        // 예: var obj:ExtCommon -> var obj
        protectedSource = removeTypedIdentifiersInDeclarations(protectedSource);

        debug("STEP 1 : 주석/문자열 보호 후", protectedSource);

        String converted = new EventFunctionConverter().convert(
                protectedSource,
                analysis,
                componentIdMap);

        debug("STEP 2 : 이벤트 함수 변환 후", converted);

        converted = convertThisAssignedFunctions(converted);
        converted = convertDeclaredFunctions(converted);

        // Phase 3: local/parameter/global symbol이 component ID를 가리는 경우를 먼저 보호한다.
        converted = new ScopeAwareComponentPropertyConverter().convert(
                converted,
                componentIdMap);

        // Phase 3 selected component APIs use component type metadata and the same scope rules.
        converted = new ComponentApiConverter().convert(
                converted, componentIdMap, targetComponentTypeMap, protection.getOriginals());

        // XFDL/XJS 통합 Script에 동일한 Dataset API 규칙을 적용한다.
        converted = new DatasetApiConverter().convert(converted, datasetIds);

        // 명시적인 this.transaction()은 일반 this.fn() 처리보다 먼저 변환해야 한다.
        converted = converted.replaceAll(
                "\\bthis\\s*\\.\\s*transaction\\s*\\(",
                "scwin.xpTransaction(");

        converted = converted.replaceAll(
                "\\bthis\\s*\\.\\s*([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(",
                "scwin.$1(");

        converted = prefixKnownFunctionCalls(converted, analysis);

        converted = converted.replaceAll(
                "(?<![A-Za-z0-9_$])trace\\s*\\(",
                "console.log(");
        converted = converted.replaceAll(
                "\\bapplication\\s*\\.\\s*alert\\s*\\(",
                "\\$p.alert(");
        converted = converted.replaceAll(
                "\\bthis\\s*\\.\\s*alert\\s*\\(",
                "\\$p.alert(");
        converted = converted.replaceAll(
                "(?<![A-Za-z0-9_$.])alert\\s*\\(",
                "\\$p.alert(");

        converted = converted.replaceAll(
                "\\bscwin\\s*\\.\\s*transaction\\s*\\(",
                "scwin.xpTransaction(");
        converted = converted.replaceAll(
                "(?<![A-Za-z0-9_$.])transaction\\s*\\(",
                "scwin.xpTransaction(");

        // 다른 변환 단계가 함수 본문을 다시 조립했더라도 XPlatform 타입 문법이
        // 남지 않도록 최종 정리를 한 번 더 수행한다.
        converted = removeTypedIdentifiersInDeclarations(converted);
        converted = convertTypedFunctionParameters(converted);

        // 마지막으로 include를 다시 정리해 이후 변환 단계에서 재유입된 include까지 제거한다.
        converted = convertIncludeStatements(converted);
        validateNoXPlatformInclude(converted);

        converted = JavaScriptCleaner.restoreProtectedText(converted, protection);

        debug("STEP 3 : 주석/문자열 복원 후", converted);
        validateJavaScriptSyntax(converted);

        StringBuilder out = new StringBuilder(originalSource.length() + converted.length() + 1024);

        out.append("// ============================================================\n");
        out.append("// [XPlatform 원본 스크립트 - 주석으로 보존]\n");
        out.append("// ============================================================\n");
        appendOriginalAsLineComments(out, originalSource);
        out.append("// ============================================================\n\n");

        out.append("// ============================================================\n");
        out.append("// [WebSquare 변환 스크립트 - 1차 변환]\n");
        out.append("// 운영 적용 전에 TODO 표시 항목을 확인하세요.\n");
        out.append("// ============================================================\n");
        out.append("var scwin = (typeof scwin === \"undefined\") ? {} : scwin;\n\n");
        out.append("// TODO: WebSquare submission을 구성한 뒤 이 호환 함수를 교체하세요.\n");
        out.append("scwin.xpTransaction = function() {\n");
        out.append("    console.warn(\"[마이그레이션 TODO] XPlatform transaction 호출을 WebSquare submission으로 변환해야 합니다.\", arguments);\n");
        out.append("};\n\n");
        if (converted.indexOf("scwin.xpAddDataListRow(") >= 0) {
            out.append("// XPlatform Dataset.addRow() 반환 index 의미를 보존하기 위한 DataList 호환 helper.\n");
            out.append("scwin.xpAddDataListRow = function(dataList) {\n");
            out.append("    var row = dataList.getRowCount();\n");
            out.append("    dataList.insertRow(row);\n");
            out.append("    return row;\n");
            out.append("};\n\n");
        }
        out.append(converted);
        if (converted.length() > 0 && !endsWithLineBreak(converted)) {
            out.append('\n');
        }

        return out.toString();
    }

    private String convertThisAssignedFunctions(String source) {
        Matcher matcher = THIS_FUNCTION.matcher(source);
        StringBuffer out = new StringBuffer();

        while (matcher.find()) {
            String name = matcher.group(1);
            String args = normalizeParameters(matcher.group(2));
            String replacement = "scwin." + name + " = function(" + args + ")";
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private String convertDeclaredFunctions(String source) {
        Matcher matcher = DECLARED_FUNCTION.matcher(source);
        StringBuffer out = new StringBuffer();

        while (matcher.find()) {
            String functionName = matcher.group(1);
            String parameters = normalizeParameters(matcher.group(2));
            String replacement = "scwin." + functionName + " = function(" + parameters + ")";
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(out);
        return out.toString();
    }

    private String removeTypedIdentifiersInDeclarations(String source) {
        if (source == null || source.length() == 0) {
            return source == null ? "" : source;
        }

        Pattern declarationStart = Pattern.compile("\\b(var|let|const)(\\s+)");
        Matcher matcher = declarationStart.matcher(source);
        StringBuilder out = new StringBuilder(source.length());
        int cursor = 0;

        while (matcher.find(cursor)) {
            out.append(source.substring(cursor, matcher.end()));

            int bodyStart = matcher.end();
            int bodyEnd = findDeclarationEnd(source, bodyStart);
            String declarationBody = removeTypedDeclarators(
                    source.substring(bodyStart, bodyEnd));
            out.append(removeTypedIdentifiersInDeclarations(declarationBody));
            cursor = bodyEnd;

            if (cursor >= source.length()) {
                break;
            }
        }

        out.append(source.substring(cursor));
        return out.toString();
    }

    /**
     * var/let/const 선언의 최상위 declarator에 붙은 XPlatform 타입만 제거한다.
     * 객체 리터럴의 key:value, 삼항식 등 선언식 내부의 ':'는 건드리지 않는다.
     */
    private String removeTypedDeclarators(String declarationBody) {
        StringBuilder out = new StringBuilder(declarationBody.length());
        int segmentStart = 0;
        int paren = 0;
        int bracket = 0;
        int brace = 0;

        for (int i = 0; i <= declarationBody.length(); i++) {
            boolean end = i == declarationBody.length();
            char ch = end ? '\0' : declarationBody.charAt(i);

            if (!end) {
                if (ch == '(') paren++;
                else if (ch == ')' && paren > 0) paren--;
                else if (ch == '[') bracket++;
                else if (ch == ']' && bracket > 0) bracket--;
                else if (ch == '{') brace++;
                else if (ch == '}' && brace > 0) brace--;
            }

            if (end || (ch == ',' && paren == 0 && bracket == 0 && brace == 0)) {
                out.append(removeTypeFromDeclarator(declarationBody.substring(segmentStart, i)));
                if (!end) {
                    out.append(ch);
                }
                segmentStart = i + 1;
            }
        }
        return out.toString();
    }

    private String removeTypeFromDeclarator(String declarator) {
        int i = 0;
        while (i < declarator.length() && Character.isWhitespace(declarator.charAt(i))) {
            i++;
        }
        if (i >= declarator.length() || !isIdentifierStart(declarator.charAt(i))) {
            return declarator;
        }

        i++;
        while (i < declarator.length() && isIdentifierPart(declarator.charAt(i))) {
            i++;
        }

        int colon = i;
        while (colon < declarator.length() && Character.isWhitespace(declarator.charAt(colon))) {
            colon++;
        }
        if (colon >= declarator.length() || declarator.charAt(colon) != ':') {
            return declarator;
        }

        int typeStart = colon + 1;
        while (typeStart < declarator.length()
                && Character.isWhitespace(declarator.charAt(typeStart))) {
            typeStart++;
        }
        if (typeStart >= declarator.length()
                || !isIdentifierStart(declarator.charAt(typeStart))) {
            return declarator;
        }

        int typeEnd = typeStart + 1;
        while (typeEnd < declarator.length()) {
            char ch = declarator.charAt(typeEnd);
            if (isIdentifierPart(ch) || ch == '.') {
                typeEnd++;
            } else {
                break;
            }
        }

        int next = typeEnd;
        while (next < declarator.length() && Character.isWhitespace(declarator.charAt(next))) {
            next++;
        }
        if (next < declarator.length() && declarator.charAt(next) != '=') {
            String tail = declarator.substring(next);
            if (!(tail.startsWith("in ") || "in".equals(tail)
                    || tail.startsWith("of ") || "of".equals(tail))) {
                return declarator;
            }
        }

        return declarator.substring(0, colon) + declarator.substring(typeEnd);
    }

    private int findDeclarationEnd(String source, int from) {
        int paren = 0;
        int bracket = 0;
        int brace = 0;

        for (int i = from; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch == '(') paren++;
            else if (ch == ')') {
                if (paren > 0) {
                    paren--;
                } else if (bracket == 0 && brace == 0) {
                    // for (var key:Type in obj) 형태에서 선언 시작 전의 '('는
                    // 스캔 범위 밖이므로 이 ')'를 선언 종료로 처리한다.
                    return i;
                }
            }
            else if (ch == '[') bracket++;
            else if (ch == ']' && bracket > 0) bracket--;
            else if (ch == '{') brace++;
            else if (ch == '}' && brace > 0) brace--;

            if (paren == 0 && bracket == 0 && brace == 0) {
                if (ch == ';' || ch == '\r' || ch == '\n') {
                    return i;
                }
            }
        }
        return source.length();
    }

    private boolean isIdentifierStart(char ch) {
        return (ch >= 'A' && ch <= 'Z')
                || (ch >= 'a' && ch <= 'z')
                || ch == '_'
                || ch == '$';
    }

    private boolean isIdentifierPart(char ch) {
        return isIdentifierStart(ch) || (ch >= '0' && ch <= '9');
    }

    private String convertTypedFunctionParameters(String source) {
        if (source == null || source.length() == 0) {
            return source == null ? "" : source;
        }

        Pattern function = Pattern.compile("function\\s*\\(([^)]*)\\)");
        Matcher matcher = function.matcher(source);
        StringBuffer out = new StringBuffer();

        while (matcher.find()) {
            String parameters = normalizeParameters(matcher.group(1));
            String replacement = "function(" + parameters + ")";
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(out);
        return out.toString();
    }

    private void validateJavaScriptSyntax(String source) {
        if (source == null || source.length() == 0) {
            return;
        }

        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        if (!(engine instanceof Compilable)) {
            System.out.println("[JS 문법검사 건너뜀] Nashorn 엔진을 찾을 수 없습니다.");
            return;
        }

        try {
            ((Compilable) engine).compile(source);
            System.out.println("[JS 문법검사 정상]");
        } catch (ScriptException e) {
            System.err.println(
                    "[JS 문법 오류] line=" + e.getLineNumber()
                            + ", column=" + e.getColumnNumber());
            System.err.println("[JS 문법 오류] " + e.getMessage());
            printJavaScriptErrorContext(source, e.getLineNumber());
            throw new IllegalStateException(
                    "변환 JavaScript 문법 오류: line=" + e.getLineNumber()
                            + ", column=" + e.getColumnNumber(),
                    e);
        }
    }

    private void printJavaScriptErrorContext(String source, int errorLine) {
        if (source == null || errorLine <= 0) {
            return;
        }

        String normalized = source.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        int from = Math.max(1, errorLine - 3);
        int to = Math.min(lines.length, errorLine + 3);

        System.err.println("===== JS 오류 주변 소스 =====");
        for (int line = from; line <= to; line++) {
            String mark = line == errorLine ? ">>> " : "    ";
            System.err.println(mark + line + " : " + lines[line - 1]);
        }
        System.err.println("==========================");
    }

    private String prefixKnownFunctionCalls(String source, XfdlAnalysisResult analysis) {
        if (analysis == null || analysis.getFunctions().isEmpty()) {
            return source;
        }

        List<String> names = new ArrayList<String>(analysis.getFunctions().keySet());
        Collections.sort(names, new Comparator<String>() {
            public int compare(String a, String b) {
                return b.length() - a.length();
            }
        });

        String converted = source;
        for (String name : names) {
            Pattern pattern = Pattern.compile(
                    "(?<![A-Za-z0-9_$.])"
                  + Pattern.quote(name)
                  + "(\\s*)\\(");
            Matcher matcher = pattern.matcher(converted);
            StringBuffer out = new StringBuffer();

            while (matcher.find()) {
                int previous = previousNonWhitespace(converted, matcher.start() - 1);
                if (previous >= 0 && converted.charAt(previous) == '.') {
                    matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
                    continue;
                }

                String replacement = "scwin." + name + matcher.group(1) + "(";
                matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(out);
            converted = out.toString();
        }
        return converted;
    }

    private int previousNonWhitespace(String source, int index) {
        int i = index;
        while (i >= 0) {
            char c = source.charAt(i);
            if (!Character.isWhitespace(c)) {
                return i;
            }
            i--;
        }
        return -1;
    }

    private String normalizeParameters(String args) {
        if (args == null || args.trim().length() == 0) {
            return args == null ? "" : args.trim();
        }

        String[] parts = args.split(",");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            int colon = part.indexOf(':');
            if (colon >= 0) {
                part = part.substring(0, colon).trim();
            }
            if (i > 0) {
                out.append(", ");
            }
            out.append(part);
        }
        return out.toString();
    }

    private String convertIncludeStatements(String source) {
        if (source == null || source.length() == 0) {
            return source == null ? "" : source;
        }

        final int NORMAL = 0;
        final int LINE_COMMENT = 1;
        final int BLOCK_COMMENT = 2;
        final int SINGLE_QUOTE = 3;
        final int DOUBLE_QUOTE = 4;
        final int TEMPLATE = 5;

        int state = NORMAL;
        boolean escaped = false;
        boolean onlyWhitespaceOnLine = true;
        StringBuilder out = new StringBuilder(source.length() + 64);

        int i = 0;
        while (i < source.length()) {
            char ch = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';

            if (state == NORMAL
                    && onlyWhitespaceOnLine
                    && startsIncludeKeyword(source, i)) {

                int lineEnd = i;
                while (lineEnd < source.length()) {
                    char c = source.charAt(lineEnd);
                    if (c == '\r' || c == '\n') {
                        break;
                    }
                    lineEnd++;
                }

                String includeLine = source.substring(i, lineEnd);
                String includePath = extractIncludePath(includeLine);
                out.append("// [XPlatform include 제거]");
                if (includePath.length() > 0) {
                    out.append(' ').append(includePath);
                }
                System.out.println("[정보] XPlatform include 제거, 경로=" + includePath);

                i = lineEnd;
                onlyWhitespaceOnLine = false;
                continue;
            }

            out.append(ch);

            if (state == NORMAL) {
                if (ch == '/' && next == '/') {
                    out.append(next);
                    i += 2;
                    state = LINE_COMMENT;
                    continue;
                }
                if (ch == '/' && next == '*') {
                    out.append(next);
                    i += 2;
                    state = BLOCK_COMMENT;
                    continue;
                }
                if (ch == '\'') {
                    state = SINGLE_QUOTE;
                    escaped = false;
                } else if (ch == '"') {
                    state = DOUBLE_QUOTE;
                    escaped = false;
                } else if (ch == '`') {
                    state = TEMPLATE;
                    escaped = false;
                }
            } else if (state == LINE_COMMENT) {
                if (ch == '\r' || ch == '\n') {
                    state = NORMAL;
                }
            } else if (state == BLOCK_COMMENT) {
                if (ch == '*' && next == '/') {
                    out.append(next);
                    i += 2;
                    state = NORMAL;
                    continue;
                }
            } else if (state == SINGLE_QUOTE) {
                if (ch == '\'' && !escaped) {
                    state = NORMAL;
                }
                escaped = updateEscape(ch, escaped);
            } else if (state == DOUBLE_QUOTE) {
                if (ch == '"' && !escaped) {
                    state = NORMAL;
                }
                escaped = updateEscape(ch, escaped);
            } else if (state == TEMPLATE) {
                if (ch == '`' && !escaped) {
                    state = NORMAL;
                }
                escaped = updateEscape(ch, escaped);
            }

            if (ch == '\r' || ch == '\n') {
                onlyWhitespaceOnLine = true;
            } else if (onlyWhitespaceOnLine && ch != ' ' && ch != '\t') {
                onlyWhitespaceOnLine = false;
            }

            i++;
        }

        return out.toString();
    }

    private boolean startsIncludeKeyword(String source, int index) {
        String keyword = "include";
        if (index + keyword.length() > source.length()) {
            return false;
        }
        if (!source.regionMatches(index, keyword, 0, keyword.length())) {
            return false;
        }
        int after = index + keyword.length();
        if (after >= source.length()) {
            return false;
        }
        char next = source.charAt(after);
        return next == ' ' || next == '\t';
    }

    private String extractIncludePath(String includeLine) {
        if (includeLine == null) {
            return "";
        }

        int firstDoubleQuote = includeLine.indexOf('"');
        int firstSingleQuote = includeLine.indexOf('\'');
        int start = -1;
        char quote = 0;

        if (firstDoubleQuote >= 0 && firstSingleQuote >= 0) {
            if (firstDoubleQuote < firstSingleQuote) {
                start = firstDoubleQuote;
                quote = '"';
            } else {
                start = firstSingleQuote;
                quote = '\'';
            }
        } else if (firstDoubleQuote >= 0) {
            start = firstDoubleQuote;
            quote = '"';
        } else if (firstSingleQuote >= 0) {
            start = firstSingleQuote;
            quote = '\'';
        }

        if (start < 0) {
            return "";
        }

        int end = includeLine.indexOf(quote, start + 1);
        if (end < 0) {
            return "";
        }

        return includeLine.substring(start + 1, end);
    }

    private void validateNoXPlatformInclude(String converted) {
        String checked = convertIncludeStatements(converted);
        if (!checked.equals(converted)) {
            throw new IllegalStateException(
                    "변환 후 실행 가능한 XPlatform include 문장이 남아 있습니다.");
        }
        System.out.println("[확인] 실행 스크립트에서 include가 제거되었습니다");
    }

    private boolean updateEscape(char c, boolean escaped) {
        if (c == '\\') {
            return !escaped;
        }
        return false;
    }

    private void appendOriginalAsLineComments(StringBuilder out, String originalSource) {
        if (originalSource == null || originalSource.length() == 0) {
            return;
        }

        int start = 0;
        for (int i = 0; i <= originalSource.length(); i++) {
            boolean end = i == originalSource.length();
            boolean lineBreak = !end
                    && (originalSource.charAt(i) == '\r' || originalSource.charAt(i) == '\n');

            if (!end && !lineBreak) {
                continue;
            }

            out.append("// ").append(originalSource.substring(start, i)).append('\n');

            if (end) {
                break;
            }
            if (originalSource.charAt(i) == '\r'
                    && i + 1 < originalSource.length()
                    && originalSource.charAt(i + 1) == '\n') {
                i++;
            }
            start = i + 1;
        }
    }

    private boolean endsWithLineBreak(String source) {
        if (source == null || source.length() == 0) {
            return false;
        }
        char c = source.charAt(source.length() - 1);
        return c == '\r' || c == '\n';
    }

    private void debug(String label, String source) {
        if (!Boolean.getBoolean("xpws.debug.script")) {
            return;
        }
        System.out.println("===== " + label + " =====");
        System.out.println(debugVisibleWhitespace(source));
    }

    private String debugVisibleWhitespace(String source) {
        if (source == null) {
            return "null";
        }

        StringBuilder out = new StringBuilder(source.length() + 64);
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch == '\t') {
                out.append("[TAB]");
            } else if (ch == '\r') {
                out.append("[CR]");
            } else if (ch == '\n') {
                out.append("[LF]\n");
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }
}
