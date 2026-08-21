package com.example.xfdltracker.converter;

import com.example.xfdltracker.model.EventBinding;
import com.example.xfdltracker.model.XfdlAnalysisResult;
import com.example.xfdltracker.util.JavaScriptCleaner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XPlatform 이벤트 함수 헤더를 변환하면서 기존 함수 본문의
 * 줄 배치와 공백을 가능한 한 그대로 유지한다.
 */
public class EventFunctionConverter {

    private static final Pattern ASSIGNMENT_FUNCTION = Pattern.compile(
            "(?<![A-Za-z0-9_$.])"
          + "(?:this\\s*\\.\\s*)?"
          + "([A-Za-z_$][A-Za-z0-9_$]*)"
          + "\\s*=\\s*function\\s*"
          + "\\(([^)]*)\\)"
          + "(\\s*)\\{");

    private static final Pattern DECLARED_FUNCTION = Pattern.compile(
            "(?<![A-Za-z0-9_$.])"
          + "function\\s+"
          + "([A-Za-z_$][A-Za-z0-9_$]*)"
          + "\\s*\\(([^)]*)\\)"
          + "(\\s*)\\{");

    public String convert(String source, XfdlAnalysisResult analysis) {
        return convert(source, analysis, Collections.<String, String>emptyMap());
    }

    public String convert(
            String source,
            XfdlAnalysisResult analysis,
            Map<String, String> componentIdMap) {

        if (source == null || source.length() == 0) {
            return source == null ? "" : source;
        }

        String converted = convertByPattern(
                source,
                ASSIGNMENT_FUNCTION,
                analysis,
                componentIdMap);

        converted = convertByPattern(
                converted,
                DECLARED_FUNCTION,
                analysis,
                componentIdMap);

        return converted;
    }

    private String convertByPattern(
            String source,
            Pattern pattern,
            XfdlAnalysisResult analysis,
            Map<String, String> componentIdMap) {

        String cleaned = new JavaScriptCleaner().clean(source);
        Matcher matcher = pattern.matcher(cleaned);
        StringBuilder result = new StringBuilder(source.length() + 256);

        int cursor = 0;
        int searchFrom = 0;

        while (matcher.find(searchFrom)) {
            String functionName = matcher.group(1);
            String parameterText = matcher.group(2);
            String whitespaceBeforeBrace = matcher.group(3);

            if (!isEventFunction(functionName, parameterText, analysis)) {
                searchFrom = matcher.end();
                continue;
            }

            int openBrace = matcher.end() - 1;
            int closeBrace = findClosingBrace(cleaned, openBrace);
            if (closeBrace < 0) {
                searchFrom = matcher.end();
                continue;
            }

            result.append(source.substring(cursor, matcher.start()));

            String body = source.substring(openBrace + 1, closeBrace);
            String convertedBody = convertEventBody(
                    functionName,
                    parameterText,
                    body,
                    analysis,
                    componentIdMap);

            result.append("scwin.")
                  .append(functionName)
                  .append(" = function(e)")
                  .append(whitespaceBeforeBrace)
                  .append('{')
                  .append(convertedBody)
                  .append('}');

            cursor = closeBrace + 1;
            searchFrom = closeBrace + 1;
        }

        result.append(source.substring(cursor));
        return result.toString();
    }

    private String convertEventBody(
            String functionName,
            String parameterText,
            String body,
            XfdlAnalysisResult analysis,
            Map<String, String> componentIdMap) {

        ParameterInfo params = parseParameters(parameterText);
        boolean usesObj = params.objName != null && containsIdentifier(body, params.objName);
        boolean usesEvent = params.eventName != null && containsIdentifier(body, params.eventName);

        String convertedBody = body;
        if (usesObj) {
            convertedBody = convertObjectProperties(convertedBody, params.objName);
        }

        List<String> preludeLines = new ArrayList<String>();

        if (usesObj) {
            List<String> targetIds = findBoundTargetIds(functionName, analysis, componentIdMap);
            if (targetIds.size() == 1) {
                preludeLines.add("var " + params.objName
                        + " = $p.getComponentById(\""
                        + escapeJavaScriptString(targetIds.get(0))
                        + "\");");
            } else {
                preludeLines.add("var " + params.objName
                        + " = null; // TODO EVENT_OBJ: resolve event source component");
            }
        }

        if (usesEvent && params.eventName != null && !"e".equals(params.eventName)) {
            preludeLines.add("var " + params.eventName + " = e;");
        }

        if (preludeLines.isEmpty()) {
            return convertedBody;
        }

        return insertPrelude(convertedBody, preludeLines);
    }

    private String insertPrelude(String body, List<String> preludeLines) {
        String lineSeparator = detectLineSeparator(body);
        String indent = detectBodyIndent(body);
        int leadingBreakLength = leadingLineBreakLength(body);

        StringBuilder out = new StringBuilder(body.length() + 160);

        if (leadingBreakLength > 0) {
            out.append(body.substring(0, leadingBreakLength));
        } else {
            out.append(lineSeparator);
        }

        for (String line : preludeLines) {
            out.append(indent).append(line).append(lineSeparator);
        }

        if (leadingBreakLength > 0) {
            out.append(body.substring(leadingBreakLength));
        } else {
            out.append(body);
        }

        return out.toString();
    }

    private int leadingLineBreakLength(String body) {
        if (body == null || body.length() == 0) {
            return 0;
        }
        if (body.startsWith("\r\n")) {
            return 2;
        }
        if (body.charAt(0) == '\r' || body.charAt(0) == '\n') {
            return 1;
        }
        return 0;
    }

    private String detectLineSeparator(String body) {
        if (body != null) {
            int crlf = body.indexOf("\r\n");
            if (crlf >= 0) {
                return "\r\n";
            }
            if (body.indexOf('\n') >= 0) {
                return "\n";
            }
            if (body.indexOf('\r') >= 0) {
                return "\r";
            }
        }
        return "\n";
    }

    private String detectBodyIndent(String body) {
        if (body == null || body.length() == 0) {
            return "    ";
        }

        int i = leadingLineBreakLength(body);
        StringBuilder indent = new StringBuilder();
        while (i < body.length()) {
            char c = body.charAt(i);
            if (c == ' ' || c == '\t') {
                indent.append(c);
                i++;
            } else {
                break;
            }
        }
        return indent.length() == 0 ? "    " : indent.toString();
    }

    private boolean isEventFunction(
            String functionName,
            String parameterText,
            XfdlAnalysisResult analysis) {

        if (analysis != null) {
            for (EventBinding binding : analysis.getEvents()) {
                if (functionName.equals(binding.getFunctionName())) {
                    return true;
                }
            }
        }

        // 실제 binding을 찾지 못한 오래된/불완전 XFDL도 typed EventInfo가 있으면
        // 이벤트 함수로 판단한다. 단, 단순히 함수명에 "_on"이 있다는 이유만으로
        // 일반 사용자 함수를 이벤트로 바꾸면 원래 파라미터가 소실될 수 있다.
        if (parameterText != null && parameterText.indexOf("EventInfo") >= 0) {
            return true;
        }

        // analysis 없이 이 변환기를 단독 사용하는 레거시 호출에만 기존 이름 휴리스틱을 유지한다.
        return analysis == null
                && functionName != null
                && functionName.indexOf("_on") > 0;
    }

    private List<String> findBoundTargetIds(
            String functionName,
            XfdlAnalysisResult analysis,
            Map<String, String> componentIdMap) {

        Set<String> ids = new LinkedHashSet<String>();
        if (analysis == null) {
            return new ArrayList<String>(ids);
        }

        for (EventBinding binding : analysis.getEvents()) {
            if (!functionName.equals(binding.getFunctionName())) {
                continue;
            }
            String sourceId = binding.getComponentId();
            if (sourceId == null || sourceId.length() == 0) {
                continue;
            }

            String targetId = resolveComponentId(sourceId, componentIdMap);
            if (targetId != null) {
                ids.add(targetId);
            } else if (componentIdMap == null || componentIdMap.isEmpty()) {
                ids.add(sourceId);
            }
        }

        return new ArrayList<String>(ids);
    }

    private String resolveComponentId(String sourceId, Map<String, String> componentIdMap) {
        if (componentIdMap == null || componentIdMap.isEmpty()) {
            return null;
        }

        String canonical = canonicalizePath(sourceId);
        String exact = componentIdMap.get(canonical);
        if (exact != null) {
            return exact;
        }

        int dot = canonical.lastIndexOf('.');
        String localId = dot >= 0 ? canonical.substring(dot + 1) : canonical;
        String match = null;

        for (Map.Entry<String, String> entry : componentIdMap.entrySet()) {
            String key = entry.getKey();
            int keyDot = key.lastIndexOf('.');
            String keyLocal = keyDot >= 0 ? key.substring(keyDot + 1) : key;
            if (!localId.equals(keyLocal)) {
                continue;
            }
            if (match != null && !match.equals(entry.getValue())) {
                return null;
            }
            match = entry.getValue();
        }
        return match;
    }

    private String canonicalizePath(String rawPath) {
        String value = rawPath.replaceAll("\\s+", "");
        if (value.startsWith("this.")) {
            value = value.substring(5);
        }
        value = value.replace(".form.", ".");
        while (value.startsWith("form.")) {
            value = value.substring(5);
        }
        return value;
    }

    private ParameterInfo parseParameters(String parameterText) {
        ParameterInfo info = new ParameterInfo();
        if (parameterText == null || parameterText.trim().length() == 0) {
            return info;
        }

        String[] params = parameterText.split(",");
        if (params.length >= 1) {
            String first = removeType(params[0]);
            if (first.length() > 0) {
                info.objName = first;
            }
        }
        if (params.length >= 2) {
            String second = removeType(params[1]);
            if (second.length() > 0) {
                info.eventName = second;
            }
        }
        return info;
    }

    private String removeType(String parameter) {
        if (parameter == null) {
            return "";
        }
        String value = parameter.trim();
        int colon = value.indexOf(':');
        if (colon >= 0) {
            value = value.substring(0, colon);
        }
        return value.trim();
    }

    private boolean containsIdentifier(String source, String identifier) {
        if (source == null || identifier == null || identifier.length() == 0) {
            return false;
        }
        Pattern p = Pattern.compile(
                "(?<![A-Za-z0-9_$])"
              + Pattern.quote(identifier)
              + "(?![A-Za-z0-9_$])");
        return p.matcher(new JavaScriptCleaner().clean(source)).find();
    }

    private String convertObjectProperties(String body, String objName) {
        String obj = Pattern.quote(objName);
        String converted = body;

        converted = converted.replaceAll(
                "\\b" + obj + "\\s*\\.\\s*get_id\\s*\\(\\s*\\)",
                Matcher.quoteReplacement(objName + ".getID()"));
        converted = converted.replaceAll(
                "\\b" + obj + "\\s*\\.\\s*get_value\\s*\\(\\s*\\)",
                Matcher.quoteReplacement(objName + ".getValue()"));
        converted = converted.replaceAll(
                "\\b" + obj + "\\s*\\.\\s*get_text\\s*\\(\\s*\\)",
                Matcher.quoteReplacement(objName + ".getValue()"));
        converted = converted.replaceAll(
                "\\b" + obj + "\\s*\\.\\s*id\\b",
                Matcher.quoteReplacement(objName + ".getID()"));
        converted = converted.replaceAll(
                "\\b" + obj + "\\s*\\.\\s*(?:text|value)\\b",
                Matcher.quoteReplacement(objName + ".getValue()"));
        converted = converted.replaceAll(
                "\\b" + obj + "\\s*\\.\\s*readonly\\b",
                Matcher.quoteReplacement(objName + ".getReadOnly()"));
        converted = converted.replaceAll(
                "\\b" + obj + "\\s*\\.\\s*enable\\b",
                Matcher.quoteReplacement("(!" + objName + ".getDisabled())"));

        return converted;
    }

    private int findClosingBrace(String cleanedSource, int openBrace) {
        int depth = 0;
        for (int i = openBrace; i < cleanedSource.length(); i++) {
            char c = cleanedSource.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String escapeJavaScriptString(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static class ParameterInfo {
        private String objName;
        private String eventName;
    }
}
