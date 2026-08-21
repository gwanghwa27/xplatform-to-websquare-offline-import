package com.example.xfdltracker.script;

import com.example.xfdltracker.util.JavaScriptCleaner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight JavaScript lexical symbol table used by Phase 3.
 * It deliberately prefers under-conversion to converting a shadowed local as an XFDL object.
 * No external JavaScript parser is required; source offsets are preserved by JavaScriptCleaner.clean().
 */
public class ScriptSymbolTable {
    public enum Kind {
        PARAMETER,
        LOCAL,
        CATCH_VARIABLE,
        GLOBAL,
        FUNCTION,
        COMPONENT,
        DATASET,
        EXTERNAL_FUNCTION,
        EXTERNAL_GLOBAL,
        UNRESOLVED
    }

    private static final Pattern FUNCTION = Pattern.compile(
            "(?:(?:scwin\\s*\\.\\s*)?([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*)?"
          + "function(?:\\s+([A-Za-z_$][A-Za-z0-9_$]*))?\\s*\\(([^)]*)\\)\\s*\\{");
    private static final Pattern DECLARATION = Pattern.compile(
            "\\b(?:var|let|const)\\s+([A-Za-z_$][A-Za-z0-9_$]*)");
    private static final Pattern CATCH = Pattern.compile(
            "\\bcatch\\s*\\(\\s*([A-Za-z_$][A-Za-z0-9_$]*)");

    private final String cleaned;
    private final List<Scope> scopes;
    private final Set<String> globals;
    private final Set<String> functions;
    private final Set<String> components;
    private final Set<String> datasets;
    private final Set<String> externalFunctions;
    private final Set<String> externalGlobals;

    private ScriptSymbolTable(
            String cleaned,
            List<Scope> scopes,
            Set<String> globals,
            Set<String> functions,
            Set<String> components,
            Set<String> datasets,
            Set<String> externalFunctions,
            Set<String> externalGlobals) {
        this.cleaned = cleaned;
        this.scopes = scopes;
        this.globals = globals;
        this.functions = functions;
        this.components = components;
        this.datasets = datasets;
        this.externalFunctions = externalFunctions;
        this.externalGlobals = externalGlobals;
    }

    public static ScriptSymbolTable build(
            String source,
            Set<String> componentIds,
            Set<String> datasetIds,
            Set<String> externalFunctions,
            Set<String> externalGlobals) {
        String cleaned = new JavaScriptCleaner().clean(source == null ? "" : source);
        List<Scope> scopes = parseScopes(cleaned);
        Set<String> globals = new LinkedHashSet<String>();
        Set<String> functions = new LinkedHashSet<String>();

        Matcher declaration = DECLARATION.matcher(cleaned);
        while (declaration.find()) {
            Scope scope = innermost(scopes, declaration.start());
            if (scope == null) {
                globals.add(declaration.group(1));
            } else {
                scope.locals.add(declaration.group(1));
            }
        }

        Matcher catches = CATCH.matcher(cleaned);
        while (catches.find()) {
            Scope scope = innermost(scopes, catches.start());
            if (scope != null) scope.catchVariables.add(catches.group(1));
        }

        Matcher function = FUNCTION.matcher(cleaned);
        while (function.find()) {
            String assigned = function.group(1);
            String declared = function.group(2);
            if (assigned != null && assigned.length() > 0) functions.add(assigned);
            if (declared != null && declared.length() > 0) functions.add(declared);
        }

        assignParents(scopes);
        return new ScriptSymbolTable(
                cleaned,
                scopes,
                globals,
                functions,
                copy(componentIds),
                copy(datasetIds),
                copy(externalFunctions),
                copy(externalGlobals));
    }

    public boolean isJavaScriptVariable(String name, int position) {
        if (name == null || name.length() == 0) return false;
        if (globals.contains(name)) return true;
        Scope scope = innermost(scopes, position);
        while (scope != null) {
            if (scope.parameters.contains(name)
                    || scope.locals.contains(name)
                    || scope.catchVariables.contains(name)) return true;
            scope = scope.parent;
        }
        return false;
    }

    public Kind resolve(String name, int position) {
        if (name == null || name.length() == 0) return Kind.UNRESOLVED;
        Scope scope = innermost(scopes, position);
        while (scope != null) {
            if (scope.parameters.contains(name)) return Kind.PARAMETER;
            if (scope.locals.contains(name)) return Kind.LOCAL;
            if (scope.catchVariables.contains(name)) return Kind.CATCH_VARIABLE;
            scope = scope.parent;
        }
        if (globals.contains(name)) return Kind.GLOBAL;
        if (functions.contains(name)) return Kind.FUNCTION;
        if (components.contains(name)) return Kind.COMPONENT;
        if (datasets.contains(name)) return Kind.DATASET;
        if (externalFunctions.contains(name)) return Kind.EXTERNAL_FUNCTION;
        if (externalGlobals.contains(name)) return Kind.EXTERNAL_GLOBAL;
        return Kind.UNRESOLVED;
    }

    public Set<String> getGlobals() { return Collections.unmodifiableSet(globals); }
    public Set<String> getFunctions() { return Collections.unmodifiableSet(functions); }
    public String getCleanedSource() { return cleaned; }

    private static List<Scope> parseScopes(String cleaned) {
        List<Scope> result = new ArrayList<Scope>();
        Matcher m = FUNCTION.matcher(cleaned);
        while (m.find()) {
            int open = m.end() - 1;
            int close = findClosingBrace(cleaned, open);
            if (close < 0) continue;
            Scope scope = new Scope(m.start(), close + 1);
            addParameters(scope.parameters, m.group(3));
            result.add(scope);
        }
        return result;
    }

    private static void assignParents(List<Scope> scopes) {
        for (Scope child : scopes) {
            Scope parent = null;
            for (Scope candidate : scopes) {
                if (candidate == child) continue;
                if (child.start > candidate.start && child.end <= candidate.end) {
                    if (parent == null
                            || (candidate.end - candidate.start) < (parent.end - parent.start)) {
                        parent = candidate;
                    }
                }
            }
            child.parent = parent;
        }
    }

    private static Scope innermost(List<Scope> scopes, int position) {
        Scope result = null;
        for (Scope scope : scopes) {
            if (position >= scope.start && position < scope.end) {
                if (result == null || (scope.end - scope.start) < (result.end - result.start)) {
                    result = scope;
                }
            }
        }
        return result;
    }

    private static void addParameters(Set<String> target, String params) {
        if (params == null || params.trim().length() == 0) return;
        String[] items = params.split(",");
        for (String item : items) {
            String value = item.trim();
            int colon = value.indexOf(':');
            if (colon >= 0) value = value.substring(0, colon).trim();
            int equals = value.indexOf('=');
            if (equals >= 0) value = value.substring(0, equals).trim();
            if (value.matches("[A-Za-z_$][A-Za-z0-9_$]*")) target.add(value);
        }
    }

    private static int findClosingBrace(String source, int opening) {
        int depth = 0;
        for (int i = opening; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static Set<String> copy(Set<String> values) {
        return values == null
                ? new LinkedHashSet<String>()
                : new LinkedHashSet<String>(values);
    }

    private static final class Scope {
        private final int start;
        private final int end;
        private final Set<String> parameters = new LinkedHashSet<String>();
        private final Set<String> locals = new LinkedHashSet<String>();
        private final Set<String> catchVariables = new LinkedHashSet<String>();
        private Scope parent;
        private Scope(int start, int end) { this.start = start; this.end = end; }
    }
}
