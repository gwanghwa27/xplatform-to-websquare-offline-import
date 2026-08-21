package com.example.xfdltracker.converter;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XPlatform 컴포넌트 속성/API를 WebSquare 형태로 1차 변환한다.
 *
 * WebSquareGenerator의 컴포넌트 맵으로 해석 가능한 경로만
 * 변환하며, 알 수 없거나 중복되어 모호한 경로는 원문 그대로 둔다.
 */
public class ComponentPropertyConverter {

    private static final String PATH =
            "((?:this\\s*\\.\\s*)?[A-Za-z_$][A-Za-z0-9_$]*"
          + "(?:\\s*\\.\\s*[A-Za-z_$][A-Za-z0-9_$]*)*)";

    private static final Pattern ASSIGNMENT = Pattern.compile(
            PATH
          + "\\s*\\.\\s*(value|text|readonly|enable)"
          + "\\s*=(?!=)\\s*([^;\\r\\n]*)(;?)");

    private static final Pattern READ_PROPERTY = Pattern.compile(
            PATH
          + "\\s*\\.\\s*(value|text|id|readonly|enable)\\b");

    public String convert(String source, Map<String, String> componentIdMap) {
        if (source == null || source.length() == 0) {
            return source == null ? "" : source;
        }
        if (componentIdMap == null || componentIdMap.isEmpty()) {
            return source;
        }

        String converted = source;

        converted = convertMethod(converted, componentIdMap, "set_value", "setValue");
        converted = convertMethod(converted, componentIdMap, "get_value", "getValue");
        converted = convertMethod(converted, componentIdMap, "set_text", "setValue");
        converted = convertMethod(converted, componentIdMap, "get_text", "getValue");
        converted = convertMethod(converted, componentIdMap, "set_readonly", "setReadOnly");
        converted = convertMethod(converted, componentIdMap, "get_readonly", "getReadOnly");
        converted = convertMethod(converted, componentIdMap, "get_id", "getID");
        converted = convertEnableMethod(converted, componentIdMap);

        converted = convertAssignments(converted, componentIdMap);
        converted = convertReads(converted, componentIdMap);

        return converted;
    }

    private String convertMethod(
            String source,
            Map<String, String> componentIdMap,
            String xplatformMethod,
            String webSquareMethod) {

        Pattern pattern = Pattern.compile(
                PATH
              + "\\s*\\.\\s*"
              + Pattern.quote(xplatformMethod)
              + "\\s*\\(");

        Matcher matcher = pattern.matcher(source);
        StringBuffer out = new StringBuffer();

        while (matcher.find()) {
            String targetId = resolveComponentId(matcher.group(1), componentIdMap);
            if (targetId == null) {
                matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            String replacement = targetId + "." + webSquareMethod + "(";
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(out);
        return out.toString();
    }

    private String convertEnableMethod(String source, Map<String, String> componentIdMap) {
        Pattern pattern = Pattern.compile(
                PATH
              + "\\s*\\.\\s*set_enable\\s*\\(([^;\\r\\n)]*)\\)");

        Matcher matcher = pattern.matcher(source);
        StringBuffer out = new StringBuffer();

        while (matcher.find()) {
            String targetId = resolveComponentId(matcher.group(1), componentIdMap);
            if (targetId == null) {
                matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            String expression = matcher.group(2).trim();
            String disabledExpression = invertBooleanExpression(expression);
            String replacement = targetId + ".setDisabled(" + disabledExpression + ")";
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(out);
        return out.toString();
    }

    private String convertAssignments(String source, Map<String, String> componentIdMap) {
        Matcher matcher = ASSIGNMENT.matcher(source);
        StringBuffer out = new StringBuffer();

        while (matcher.find()) {
            String targetId = resolveComponentId(matcher.group(1), componentIdMap);
            if (targetId == null) {
                matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            String property = matcher.group(2);
            String expression = matcher.group(3);
            String semicolon = matcher.group(4);
            String replacement;

            if ("value".equals(property) || "text".equals(property)) {
                replacement = targetId + ".setValue(" + expression.trim() + ")" + semicolon;
            } else if ("readonly".equals(property)) {
                replacement = targetId + ".setReadOnly(" + expression.trim() + ")" + semicolon;
            } else {
                replacement = targetId + ".setDisabled("
                        + invertBooleanExpression(expression.trim())
                        + ")" + semicolon;
            }

            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(out);
        return out.toString();
    }

    private String convertReads(String source, Map<String, String> componentIdMap) {
        Matcher matcher = READ_PROPERTY.matcher(source);
        StringBuffer out = new StringBuffer();

        while (matcher.find()) {
            String targetId = resolveComponentId(matcher.group(1), componentIdMap);
            if (targetId == null) {
                matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            String property = matcher.group(2);
            String replacement;

            if ("value".equals(property) || "text".equals(property)) {
                replacement = targetId + ".getValue()";
            } else if ("id".equals(property)) {
                replacement = targetId + ".getID()";
            } else if ("readonly".equals(property)) {
                replacement = targetId + ".getReadOnly()";
            } else {
                replacement = "(!" + targetId + ".getDisabled())";
            }

            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(out);
        return out.toString();
    }

    private String invertBooleanExpression(String expression) {
        if ("true".equals(expression)) {
            return "false";
        }
        if ("false".equals(expression)) {
            return "true";
        }
        return "!(" + expression + ")";
    }

    private String resolveComponentId(String rawPath, Map<String, String> componentIdMap) {
        if (rawPath == null || componentIdMap == null || componentIdMap.isEmpty()) {
            return null;
        }

        String canonical = canonicalizePath(rawPath);
        String exact = componentIdMap.get(canonical);
        if (exact != null) {
            return exact;
        }

        int dot = canonical.lastIndexOf('.');
        String localId = dot >= 0 ? canonical.substring(dot + 1) : canonical;
        if (localId.length() == 0) {
            return null;
        }

        Set<String> matches = new LinkedHashSet<String>();
        for (Map.Entry<String, String> entry : componentIdMap.entrySet()) {
            String key = entry.getKey();
            int keyDot = key.lastIndexOf('.');
            String keyLocal = keyDot >= 0 ? key.substring(keyDot + 1) : key;
            if (localId.equals(keyLocal)) {
                matches.add(entry.getValue());
            }
        }

        if (matches.size() == 1) {
            return matches.iterator().next();
        }
        return null;
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
}
