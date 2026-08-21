package com.example.xfdltracker.xjs;

import com.example.xfdltracker.util.JavaScriptCleaner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lightweight JavaScript/XPlatform scanner. It is deliberately not a full JS parser. */
public class JavaScriptSymbolScanner {
    private static final Pattern DECLARED_FUNCTION = Pattern.compile(
            "(?<![A-Za-z0-9_$.])function\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(([^)]*)\\)\\s*\\{");
    private static final Pattern ASSIGNED_FUNCTION = Pattern.compile(
            "(?<![A-Za-z0-9_$.])(?:this\\s*\\.\\s*)?([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*function\\s*\\(([^)]*)\\)\\s*\\{");
    private static final Pattern CALL = Pattern.compile(
            "(?<![A-Za-z0-9_$.])(?:([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\.\\s*)?([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(");
    private static final Pattern IDENTIFIER = Pattern.compile("(?<![A-Za-z0-9_$.])([A-Za-z_$][A-Za-z0-9_$]*)(?![A-Za-z0-9_$])");
    private static final Set<String> KEYWORDS = new HashSet<String>(Arrays.asList(
            "if","for","while","switch","catch","function","return","typeof","delete","new","throw","with",
            "var","let","const","else","do","case","break","continue","default","try","finally","in","instanceof",
            "true","false","null","undefined","this","void","yield","class","extends","static","get","set"));

    public void parseModule(XjsModule module) {
        String source = module.getSource();
        String cleaned = new JavaScriptCleaner().clean(source);
        extractFunctions(source, cleaned, DECLARED_FUNCTION, module);
        extractFunctions(source, cleaned, ASSIGNED_FUNCTION, module);
        extractTopLevelGlobals(source, cleaned, module);
        module.getIncludes().addAll(extractIncludes(source));
        extractTopLevelExecutableStatements(source, cleaned, module);
        for (XjsSymbol s : module.getFunctions().values()) analyzeDependencies(s);
        for (XjsSymbol s : module.getGlobals().values()) analyzeDependencies(s);
        for (XjsSymbol s : module.getDuplicateDefinitions()) analyzeDependencies(s);
    }

    public Set<String> findCalls(String source) {
        Set<String> out = new LinkedHashSet<String>();
        Matcher m = CALL.matcher(new JavaScriptCleaner().clean(source == null ? "" : source));
        while (m.find()) {
            String qualifier = m.group(1);
            String n = m.group(2);
            // Object/component/dataset methods are not project-level function dependencies.
            // Explicit this.fn() is treated as a screen/XJS function call.
            if (qualifier != null && !"this".equals(qualifier)) continue;
            if (!KEYWORDS.contains(n)) out.add(n);
        }
        return out;
    }

    public Set<String> findIdentifiers(String source) {
        Set<String> out = new LinkedHashSet<String>();
        Matcher m = IDENTIFIER.matcher(new JavaScriptCleaner().clean(source == null ? "" : source));
        while (m.find()) {
            String n = m.group(1);
            if (!KEYWORDS.contains(n)) out.add(n);
        }
        return out;
    }

    public ListWithOrder extractIncludes(String source) {
        ListWithOrder result = new ListWithOrder();
        if (source == null || source.length() == 0) return result;
        int i = 0;
        boolean lineStart = true;
        int state = 0; // 0 normal, 1 line comment, 2 block comment, 3 single, 4 double, 5 template
        boolean escaped = false;
        while (i < source.length()) {
            char c = source.charAt(i);
            char n = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
            if (state == 0 && lineStart) {
                int p = i;
                while (p < source.length() && (source.charAt(p) == ' ' || source.charAt(p) == '\t')) p++;
                if (startsWord(source, p, "include")) {
                    int q = p + 7;
                    while (q < source.length() && Character.isWhitespace(source.charAt(q)) && source.charAt(q) != '\r' && source.charAt(q) != '\n') q++;
                    if (q < source.length() && (source.charAt(q) == '\'' || source.charAt(q) == '"')) {
                        char quote = source.charAt(q++);
                        int start = q;
                        while (q < source.length() && source.charAt(q) != quote && source.charAt(q) != '\r' && source.charAt(q) != '\n') q++;
                        if (q < source.length() && source.charAt(q) == quote) result.add(source.substring(start, q));
                    }
                }
            }
            if (state == 0) {
                if (c == '/' && n == '/') { state = 1; i += 2; lineStart = false; continue; }
                if (c == '/' && n == '*') { state = 2; i += 2; lineStart = false; continue; }
                if (c == '\'') { state = 3; escaped = false; }
                else if (c == '"') { state = 4; escaped = false; }
                else if (c == '`') { state = 5; escaped = false; }
            } else if (state == 1) {
                if (c == '\r' || c == '\n') state = 0;
            } else if (state == 2) {
                if (c == '*' && n == '/') { state = 0; i += 2; continue; }
            } else {
                char quote = state == 3 ? '\'' : (state == 4 ? '"' : '`');
                if (c == quote && !escaped) state = 0;
                escaped = c == '\\' && !escaped;
                if (c != '\\') escaped = false;
            }
            if (c == '\r' || c == '\n') lineStart = true;
            else if (lineStart && c != ' ' && c != '\t') lineStart = false;
            i++;
        }
        return result;
    }

    private void extractFunctions(String source, String cleaned, Pattern pattern, XjsModule module) {
        Matcher m = pattern.matcher(cleaned);
        while (m.find()) {
            if (braceDepthAt(cleaned, m.start()) != 0) continue;
            String name = m.group(1);
            int open = m.end() - 1;
            int close = findClosingBrace(cleaned, open);
            if (close < 0) continue;
            int end = close + 1;
            while (end < source.length() && Character.isWhitespace(source.charAt(end)) && source.charAt(end) != '\r' && source.charAt(end) != '\n') end++;
            if (end < source.length() && source.charAt(end) == ';') end++;
            String full = source.substring(m.start(), end);
            String body = source.substring(open + 1, close);
            XjsSymbol symbol = new XjsSymbol(XjsSymbolType.FUNCTION, name,
                    module.getRelativePath(), full, body, lineOf(source, m.start()));
            if (module.getFunctions().containsKey(name)) module.getDuplicateDefinitions().add(symbol);
            else module.getFunctions().put(name, symbol);
        }
    }

    private void extractTopLevelExecutableStatements(String source, String cleaned, XjsModule module) {
        int i = 0;
        while (i < cleaned.length()) {
            while (i < cleaned.length() && (Character.isWhitespace(cleaned.charAt(i)) || cleaned.charAt(i) == ';')) i++;
            if (i >= cleaned.length()) break;
            if (startsWord(cleaned, i, "include")) { i = lineEnd(cleaned, i); continue; }
            Matcher declared = DECLARED_FUNCTION.matcher(cleaned); declared.region(i, cleaned.length());
            if (declared.lookingAt()) {
                int open = declared.end() - 1, close = findClosingBrace(cleaned, open);
                i = close < 0 ? cleaned.length() : close + 1; continue;
            }
            Matcher assigned = ASSIGNED_FUNCTION.matcher(cleaned); assigned.region(i, cleaned.length());
            if (assigned.lookingAt()) {
                int open = assigned.end() - 1, close = findClosingBrace(cleaned, open);
                i = close < 0 ? cleaned.length() : close + 1; continue;
            }
            if (startsWord(cleaned, i, "var") || startsWord(cleaned, i, "let") || startsWord(cleaned, i, "const")) {
                i = findStatementEnd(cleaned, i); continue;
            }
            int end = findTopLevelStatementEnd(cleaned, i);
            if (end <= i) end = i + 1;
            String statement = source.substring(i, Math.min(end, source.length())).trim();
            if (statement.length() > 0) {
                String compact = statement.replace('\r', ' ').replace('\n', ' ');
                if (compact.length() > 120) compact = compact.substring(0, 117) + "...";
                module.getTopLevelExecutableStatements().add("line " + lineOf(source, i) + ": " + compact);
            }
            i = end;
        }
    }

    private int lineEnd(String source, int start) {
        int i = start; while (i < source.length() && source.charAt(i) != '\r' && source.charAt(i) != '\n') i++; return i;
    }

    private int findTopLevelStatementEnd(String source, int start) {
        int paren=0, bracket=0, brace=0;
        for (int i=start; i<source.length(); i++) {
            char c=source.charAt(i);
            if(c=='(') paren++; else if(c==')'&&paren>0) paren--;
            else if(c=='[') bracket++; else if(c==']'&&bracket>0) bracket--;
            else if(c=='{') brace++; else if(c=='}'&&brace>0) brace--;
            if(paren==0&&bracket==0&&brace==0&&(c==';'||c=='\r'||c=='\n')) return i+1;
        }
        return source.length();
    }

    private void extractTopLevelGlobals(String source, String cleaned, XjsModule module) {
        int depth = 0;
        int i = 0;
        while (i < cleaned.length()) {
            char c = cleaned.charAt(i);
            if (c == '{') { depth++; i++; continue; }
            if (c == '}') { if (depth > 0) depth--; i++; continue; }
            if (depth == 0 && (startsWord(cleaned, i, "var") || startsWord(cleaned, i, "let") || startsWord(cleaned, i, "const"))) {
                int kwLen = startsWord(cleaned, i, "const") ? 5 : 3;
                int end = findStatementEnd(cleaned, i + kwLen);
                String statement = source.substring(i, end);
                Set<String> names = declarationNames(cleaned.substring(i + kwLen, end));
                for (String name : names) {
                    XjsSymbol symbol = new XjsSymbol(XjsSymbolType.GLOBAL, name,
                            module.getRelativePath(), statement, statement, lineOf(source, i));
                    if (!module.getGlobals().containsKey(name)) module.getGlobals().put(name, symbol);
                    else module.getDuplicateDefinitions().add(symbol);
                }
                i = end;
                continue;
            }
            i++;
        }
    }

    private Set<String> declarationNames(String body) {
        Set<String> result = new LinkedHashSet<String>();
        int start = 0, paren = 0, bracket = 0, brace = 0;
        for (int i = 0; i <= body.length(); i++) {
            boolean end = i == body.length();
            char c = end ? ',' : body.charAt(i);
            if (!end) {
                if (c == '(') paren++; else if (c == ')' && paren > 0) paren--;
                else if (c == '[') bracket++; else if (c == ']' && bracket > 0) bracket--;
                else if (c == '{') brace++; else if (c == '}' && brace > 0) brace--;
            }
            if ((end || c == ',') && paren == 0 && bracket == 0 && brace == 0) {
                String part = body.substring(start, i).trim();
                Matcher m = Pattern.compile("^([A-Za-z_$][A-Za-z0-9_$]*)").matcher(part);
                if (m.find()) result.add(m.group(1));
                start = i + 1;
            }
        }
        return result;
    }

    private void analyzeDependencies(XjsSymbol symbol) {
        symbol.getCalledFunctions().addAll(findCalls(symbol.getBody()));
        symbol.getReferencedIdentifiers().addAll(findIdentifiers(symbol.getBody()));
        symbol.getReferencedIdentifiers().remove(symbol.getName());
        Set<String> locals = localSymbols(symbol);
        symbol.getReferencedIdentifiers().removeAll(locals);
    }

    private Set<String> localSymbols(XjsSymbol symbol) {
        Set<String> result = new LinkedHashSet<String>();
        Matcher decl = Pattern.compile("\\b(?:var|let|const)\\s+([A-Za-z_$][A-Za-z0-9_$]*)").matcher(
                new JavaScriptCleaner().clean(symbol.getBody()));
        while (decl.find()) result.add(decl.group(1));
        Matcher header = Pattern.compile("function(?:\\s+[A-Za-z_$][A-Za-z0-9_$]*)?\\s*\\(([^)]*)\\)").matcher(
                new JavaScriptCleaner().clean(symbol.getSource()));
        if (header.find()) {
            String[] parts = header.group(1).split(",");
            for (String part : parts) {
                String value = part.trim();
                int colon = value.indexOf(':');
                if (colon >= 0) value = value.substring(0, colon).trim();
                if (value.matches("[A-Za-z_$][A-Za-z0-9_$]*")) result.add(value);
            }
        }
        return result;
    }

    private int braceDepthAt(String source, int position) {
        int depth = 0;
        for (int i = 0; i < position; i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && depth > 0) depth--;
        }
        return depth;
    }

    private int findClosingBrace(String source, int opening) {
        int depth = 0;
        for (int i = opening; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) return i; }
        }
        return -1;
    }

    private int findStatementEnd(String source, int start) {
        int paren = 0, bracket = 0, brace = 0;
        for (int i = start; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '(') paren++; else if (c == ')' && paren > 0) paren--;
            else if (c == '[') bracket++; else if (c == ']' && bracket > 0) bracket--;
            else if (c == '{') brace++; else if (c == '}' && brace > 0) brace--;
            else if (c == ';' && paren == 0 && bracket == 0 && brace == 0) return i + 1;
            else if ((c == '\r' || c == '\n') && paren == 0 && bracket == 0 && brace == 0) return i;
        }
        return source.length();
    }

    private boolean startsWord(String source, int index, String word) {
        if (index < 0 || index + word.length() > source.length()) return false;
        if (!source.regionMatches(index, word, 0, word.length())) return false;
        if (index > 0 && isId(source.charAt(index - 1))) return false;
        int after = index + word.length();
        return after >= source.length() || !isId(source.charAt(after));
    }
    private boolean isId(char c) { return Character.isLetterOrDigit(c) || c == '_' || c == '$'; }
    private int lineOf(String source, int pos) { int line = 1; for (int i = 0; i < pos; i++) if (source.charAt(i) == '\n') line++; return line; }

    public static class ListWithOrder extends java.util.ArrayList<String> {
        private static final long serialVersionUID = 1L;
    }
}
