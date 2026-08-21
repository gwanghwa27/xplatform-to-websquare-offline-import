package com.example.xfdltracker.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 두 가지 용도로 사용하는 JavaScript 보조 클래스:
 * 1) clean(): 주석/문자열을 가리고 길이는 유지한 검색용 복사본 생성
 * 2) protectCommentsAndStrings(): 정규식 변환 전에 원문 주석/문자열 보호
 *
 * JDK 1.8 호환, 외부 라이브러리 미사용.
 */
public class JavaScriptCleaner {

    private enum State {
        NORMAL,
        LINE_COMMENT,
        BLOCK_COMMENT,
        SINGLE_QUOTE,
        DOUBLE_QUOTE,
        TEMPLATE
    }

    public static class ProtectionResult {
        private final String source;
        private final Map<String, String> originals;

        public ProtectionResult(String source, Map<String, String> originals) {
            this.source = source;
            this.originals = originals;
        }

        public String getSource() {
            return source;
        }

        public Map<String, String> getOriginals() {
            return originals;
        }
    }

    /**
     * 원본과 동일한 길이의 검색용 복사본을 반환한다. 주석과 문자열 내용은
     * 공백으로 바꾸되 CR/LF 위치는 유지한다. 이 결과는 절대로
     * 최종 변환 출력으로 사용하지 않는다.
     */
    public String clean(String source) {
        if (source == null || source.length() == 0) {
            return source == null ? "" : source;
        }

        StringBuilder result = new StringBuilder(source.length());
        State state = State.NORMAL;
        boolean escaped = false;

        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';

            switch (state) {
                case NORMAL:
                    if (current == '/' && next == '/') {
                        result.append(' ').append(' ');
                        i++;
                        state = State.LINE_COMMENT;
                    } else if (current == '/' && next == '*') {
                        result.append(' ').append(' ');
                        i++;
                        state = State.BLOCK_COMMENT;
                    } else if (current == '/' && isRegexLiteralStart(source, i)) {
                        int regexEnd = findRegexLiteralEnd(source, i);
                        if (regexEnd > i) {
                            for (int r = i; r < regexEnd; r++) {
                                preserveLine(result, source.charAt(r));
                            }
                            i = regexEnd - 1;
                        } else {
                            result.append(current);
                        }
                    } else if (current == '\'') {
                        result.append(' ');
                        state = State.SINGLE_QUOTE;
                        escaped = false;
                    } else if (current == '"') {
                        result.append(' ');
                        state = State.DOUBLE_QUOTE;
                        escaped = false;
                    } else if (current == '`') {
                        result.append(' ');
                        state = State.TEMPLATE;
                        escaped = false;
                    } else {
                        result.append(current);
                    }
                    break;

                case LINE_COMMENT:
                    if (current == '\r' || current == '\n') {
                        result.append(current);
                        state = State.NORMAL;
                    } else {
                        result.append(' ');
                    }
                    break;

                case BLOCK_COMMENT:
                    if (current == '*' && next == '/') {
                        result.append(' ').append(' ');
                        i++;
                        state = State.NORMAL;
                    } else {
                        preserveLine(result, current);
                    }
                    break;

                case SINGLE_QUOTE:
                    preserveLine(result, current);
                    if (current == '\'' && !escaped) {
                        state = State.NORMAL;
                    }
                    escaped = updateEscape(current, escaped);
                    break;

                case DOUBLE_QUOTE:
                    preserveLine(result, current);
                    if (current == '"' && !escaped) {
                        state = State.NORMAL;
                    }
                    escaped = updateEscape(current, escaped);
                    break;

                case TEMPLATE:
                    preserveLine(result, current);
                    if (current == '`' && !escaped) {
                        state = State.NORMAL;
                    }
                    escaped = updateEscape(current, escaped);
                    break;

                default:
                    result.append(current);
                    break;
            }
        }

        return result.toString();
    }

    /**
     * 일반 JavaScript를 포맷하지 않고 주석과 문자열 리터럴만 보호한다.
     * 일반 문자, space, tab, CR, LF는 원문 그대로 복사한다.
     */
    public static ProtectionResult protectCommentsAndStrings(String source) {
        if (source == null || source.length() == 0) {
            return new ProtectionResult(
                    source == null ? "" : source,
                    new LinkedHashMap<String, String>());
        }

        StringBuilder result = new StringBuilder(source.length());
        Map<String, String> originals = new LinkedHashMap<String, String>();
        int tokenIndex = 0;
        int i = 0;

        while (i < source.length()) {
            char ch = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';

            // 줄 주석. 바로 앞의 가로 공백(space/tab)도 함께 보존한다.
            if (ch == '/' && next == '/') {
                int commentStart = i;
                int protectStart = commentStart;

                while (protectStart > 0) {
                    char prev = source.charAt(protectStart - 1);
                    if (prev == ' ' || prev == '\t') {
                        protectStart--;
                    } else {
                        break;
                    }
                }

                int removeCount = commentStart - protectStart;
                if (removeCount > 0 && result.length() >= removeCount) {
                    result.setLength(result.length() - removeCount);
                }

                i += 2;
                while (i < source.length()) {
                    char c = source.charAt(i);
                    if (c == '\r' || c == '\n') {
                        break;
                    }
                    i++;
                }

                String original = source.substring(protectStart, i);
                String token = createCommentToken(tokenIndex++);
                originals.put(token, original);
                result.append(token);
                continue;
            }

            // 블록 주석. 각 논리 줄을 따로 보호하고 CR/LF는 유지한다.
            if (ch == '/' && next == '*') {
                int start = i;
                i += 2;

                while (i < source.length()) {
                    if (i + 1 < source.length()
                            && source.charAt(i) == '*'
                            && source.charAt(i + 1) == '/') {
                        i += 2;
                        break;
                    }
                    i++;
                }

                String original = source.substring(start, i);
                appendBlockCommentPlaceholders(
                        result,
                        originals,
                        original,
                        tokenIndex++);
                continue;
            }

            // JavaScript 정규식 리터럴. 함수 중괄호 탐색과 속성 정규식 변환에서
            // 정규식 내부 문자를 코드로 오인하지 않도록 문자열과 동일하게 보호한다.
            if (ch == '/' && isRegexLiteralStart(source, i)) {
                int end = findRegexLiteralEnd(source, i);
                if (end > i) {
                    String original = source.substring(i, end);
                    String token = createStringToken(tokenIndex++);
                    originals.put(token, original);
                    result.append(token);
                    i = end;
                    continue;
                }
            }

            // 작은따옴표 문자열.
            if (ch == '\'') {
                int start = i;
                i++;
                boolean escaped = false;

                while (i < source.length()) {
                    char c = source.charAt(i);
                    if (escaped) {
                        escaped = false;
                        i++;
                        continue;
                    }
                    if (c == '\\') {
                        escaped = true;
                        i++;
                        continue;
                    }
                    if (c == '\'') {
                        i++;
                        break;
                    }
                    i++;
                }

                String original = source.substring(start, i);
                String token = createStringToken(tokenIndex++);
                originals.put(token, original);
                result.append(token);
                continue;
            }

            // 큰따옴표 문자열.
            if (ch == '"') {
                int start = i;
                i++;
                boolean escaped = false;

                while (i < source.length()) {
                    char c = source.charAt(i);
                    if (escaped) {
                        escaped = false;
                        i++;
                        continue;
                    }
                    if (c == '\\') {
                        escaped = true;
                        i++;
                        continue;
                    }
                    if (c == '"') {
                        i++;
                        break;
                    }
                    i++;
                }

                String original = source.substring(start, i);
                String token = createStringToken(tokenIndex++);
                originals.put(token, original);
                result.append(token);
                continue;
            }

            // Template literal. XPlatform에서는 드물지만 안전하게 보호한다.
            if (ch == '`') {
                int start = i;
                i++;
                boolean escaped = false;

                while (i < source.length()) {
                    char c = source.charAt(i);
                    if (escaped) {
                        escaped = false;
                        i++;
                        continue;
                    }
                    if (c == '\\') {
                        escaped = true;
                        i++;
                        continue;
                    }
                    if (c == '`') {
                        i++;
                        break;
                    }
                    i++;
                }

                String original = source.substring(start, i);
                String token = createStringToken(tokenIndex++);
                originals.put(token, original);
                result.append(token);
                continue;
            }

            // 일반 JavaScript는 원문 그대로 복사한다. 여기서 공백을 trim하거나 정규화하지 않는다.
            result.append(ch);
            i++;
        }

        return new ProtectionResult(result.toString(), originals);
    }

    /**
     * 현재 '/'가 나눗셈 연산자가 아니라 정규식 리터럴 시작일 가능성이 높은지 판단한다.
     * 완전한 JavaScript parser를 구현하지 않고, 정규식이 올 수 없는 "표현식 종료 토큰"
     * 뒤에서는 false를 반환하는 보수적인 방식으로 동작한다.
     */
    private static boolean isRegexLiteralStart(String source, int slashIndex) {
        if (source == null || slashIndex < 0 || slashIndex >= source.length()
                || source.charAt(slashIndex) != '/') {
            return false;
        }

        char next = slashIndex + 1 < source.length()
                ? source.charAt(slashIndex + 1) : '\0';
        if (next == '/' || next == '*' || next == '\0' || next == '\r' || next == '\n') {
            return false;
        }

        int p = slashIndex - 1;
        while (p >= 0 && Character.isWhitespace(source.charAt(p))) {
            p--;
        }
        if (p < 0) {
            return true;
        }

        char prev = source.charAt(p);
        if (prev == '(' || prev == '[' || prev == '{'
                || prev == '=' || prev == ':' || prev == ',' || prev == ';'
                || prev == '!' || prev == '?' || prev == '&' || prev == '|'
                || prev == '+' || prev == '-' || prev == '*' || prev == '%'
                || prev == '^' || prev == '~' || prev == '<' || prev == '>') {
            return true;
        }

        if (prev == ')' && followsControlCondition(source, p)) {
            return true;
        }

        if (Character.isJavaIdentifierPart(prev) || prev == '$') {
            int end = p + 1;
            int start = p;
            while (start >= 0) {
                char c = source.charAt(start);
                if (!Character.isJavaIdentifierPart(c) && c != '$') {
                    break;
                }
                start--;
            }
            String word = source.substring(start + 1, end);
            return isRegexPrefixKeyword(word);
        }

        return false;
    }

    private static boolean isRegexPrefixKeyword(String word) {
        return "return".equals(word)
                || "throw".equals(word)
                || "case".equals(word)
                || "delete".equals(word)
                || "void".equals(word)
                || "typeof".equals(word)
                || "instanceof".equals(word)
                || "in".equals(word)
                || "of".equals(word)
                || "new".equals(word)
                || "yield".equals(word)
                || "await".equals(word)
                || "else".equals(word)
                || "do".equals(word);
    }

    private static boolean followsControlCondition(String source, int closeParen) {
        int depth = 0;
        for (int i = closeParen; i >= 0; i--) {
            char c = source.charAt(i);
            if (c == ')') {
                depth++;
            } else if (c == '(') {
                depth--;
                if (depth == 0) {
                    int p = i - 1;
                    while (p >= 0 && Character.isWhitespace(source.charAt(p))) {
                        p--;
                    }
                    int end = p + 1;
                    while (p >= 0
                            && (Character.isJavaIdentifierPart(source.charAt(p))
                            || source.charAt(p) == '$')) {
                        p--;
                    }
                    String word = source.substring(p + 1, end);
                    return "if".equals(word)
                            || "while".equals(word)
                            || "for".equals(word)
                            || "with".equals(word);
                }
            }
        }
        return false;
    }

    /** @return 정규식 flags까지 포함한 종료 위치(exclusive), 찾지 못하면 -1. */
    private static int findRegexLiteralEnd(String source, int slashIndex) {
        boolean escaped = false;
        boolean inClass = false;

        for (int i = slashIndex + 1; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '\r' || c == '\n') {
                return -1;
            }
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '[') {
                inClass = true;
                continue;
            }
            if (c == ']' && inClass) {
                inClass = false;
                continue;
            }
            if (c == '/' && !inClass) {
                int end = i + 1;
                while (end < source.length()
                        && Character.isJavaIdentifierPart(source.charAt(end))) {
                    end++;
                }
                return end;
            }
        }
        return -1;
    }

    public static String restoreProtectedText(
            String source,
            ProtectionResult protection) {

        if (source == null || protection == null) {
            return source;
        }

        String result = source;
        for (Map.Entry<String, String> entry : protection.getOriginals().entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static String createCommentToken(int index) {
        return "__XPWS_COMMENT_" + index + "__";
    }

    private static String createStringToken(int index) {
        // 보호된 소스도 JavaScript 표현식 문법이 유지되도록 문자열 토큰 형태를 사용한다.
        return "\"__XPWS_STRING_" + index + "__\"";
    }

    private static String createBlockCommentLineToken(int blockIndex, int lineIndex) {
        return "__XPWS_BLOCK_COMMENT_" + blockIndex + "_" + lineIndex + "__";
    }

    private static void appendBlockCommentPlaceholders(
            StringBuilder result,
            Map<String, String> originals,
            String original,
            int blockIndex) {

        int lineIndex = 0;
        int start = 0;

        for (int i = 0; i <= original.length(); i++) {
            boolean end = i == original.length();
            boolean lineBreak = !end
                    && (original.charAt(i) == '\r' || original.charAt(i) == '\n');

            if (!end && !lineBreak) {
                continue;
            }

            String line = original.substring(start, i);
            String token = createBlockCommentLineToken(blockIndex, lineIndex++);
            originals.put(token, line);
            result.append(token);

            if (end) {
                break;
            }

            if (original.charAt(i) == '\r'
                    && i + 1 < original.length()
                    && original.charAt(i + 1) == '\n') {
                result.append('\r').append('\n');
                i++;
            } else {
                result.append(original.charAt(i));
            }

            start = i + 1;
        }
    }

    private void preserveLine(StringBuilder result, char c) {
        if (c == '\r' || c == '\n') {
            result.append(c);
        } else {
            result.append(' ');
        }
    }

    private boolean updateEscape(char c, boolean escaped) {
        if (c == '\\') {
            return !escaped;
        }
        return false;
    }
}
