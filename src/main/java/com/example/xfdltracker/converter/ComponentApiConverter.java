package com.example.xfdltracker.converter;

import com.example.xfdltracker.script.ScriptSymbolTable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Scope-aware mappings for selected XPlatform component APIs that are safe by component type. */
public class ComponentApiConverter {
    private static final String PATH =
            "((?:this\\s*\\.\\s*)?[A-Za-z_$][A-Za-z0-9_$]*"
          + "(?:\\s*\\.\\s*[A-Za-z_$][A-Za-z0-9_$]*)*)";
    private static final Pattern INDEX_ASSIGN = Pattern.compile(
            PATH + "\\s*\\.\\s*index\\s*=(?!=)\\s*([^;\\r\\n]*)(;?)");
    private static final Pattern INDEX_READ = Pattern.compile(
            PATH + "\\s*\\.\\s*index\\b");
    private static final Pattern SET_INDEX = Pattern.compile(
            PATH + "\\s*\\.\\s*set_index\\s*\\(");
    private static final Pattern GET_INDEX = Pattern.compile(
            PATH + "\\s*\\.\\s*get_index\\s*\\(\\s*\\)");
    private static final Pattern GRID_BIND_INDEX = Pattern.compile(
            PATH + "\\s*\\.\\s*getBindCellIndex\\s*\\(\\s*(\"__XPWS_STRING_[0-9]+__\"|\"body\"|'body')\\s*,\\s*([^)]*)\\)",
            Pattern.CASE_INSENSITIVE);

    public String convert(String source, Map<String, String> componentIdMap,
                          Map<String, String> targetTypeMap) {
        return convert(source, componentIdMap, targetTypeMap, Collections.<String, String>emptyMap());
    }

    public String convert(String source, Map<String, String> componentIdMap,
                          Map<String, String> targetTypeMap, Map<String, String> protectedOriginals) {
        if (source == null || source.length() == 0 || componentIdMap == null || componentIdMap.isEmpty()) {
            return source == null ? "" : source;
        }
        ScriptSymbolTable symbols = ScriptSymbolTable.build(
                source, componentRoots(componentIdMap), Collections.<String>emptySet(),
                Collections.<String>emptySet(), Collections.<String>emptySet());
        String out = convertIndexAssignments(source, componentIdMap, targetTypeMap, symbols);
        symbols = ScriptSymbolTable.build(out, componentRoots(componentIdMap), Collections.<String>emptySet(),
                Collections.<String>emptySet(), Collections.<String>emptySet());
        out = convertSimpleIndexMethod(out, componentIdMap, targetTypeMap, symbols, SET_INDEX, "setSelectedIndex(");
        symbols = ScriptSymbolTable.build(out, componentRoots(componentIdMap), Collections.<String>emptySet(),
                Collections.<String>emptySet(), Collections.<String>emptySet());
        out = convertSimpleIndexMethod(out, componentIdMap, targetTypeMap, symbols, GET_INDEX, "getSelectedIndex()");
        symbols = ScriptSymbolTable.build(out, componentRoots(componentIdMap), Collections.<String>emptySet(),
                Collections.<String>emptySet(), Collections.<String>emptySet());
        out = convertIndexReads(out, componentIdMap, targetTypeMap, symbols);
        symbols = ScriptSymbolTable.build(out, componentRoots(componentIdMap), Collections.<String>emptySet(),
                Collections.<String>emptySet(), Collections.<String>emptySet());
        return convertGridBindIndex(out, componentIdMap, targetTypeMap, symbols, protectedOriginals);
    }

    private String convertIndexAssignments(String source, Map<String,String> ids, Map<String,String> types,
                                           ScriptSymbolTable symbols) {
        Matcher m = INDEX_ASSIGN.matcher(source); StringBuffer out = new StringBuffer();
        while (m.find()) {
            String target = safeTarget(m.group(1), m.start(1), ids, types, symbols, true);
            if (target == null) { m.appendReplacement(out, Matcher.quoteReplacement(m.group(0))); continue; }
            String replacement = target + ".setSelectedIndex(" + m.group(2).trim() + ")" + m.group(3);
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out); return out.toString();
    }

    private String convertIndexReads(String source, Map<String,String> ids, Map<String,String> types,
                                     ScriptSymbolTable symbols) {
        Matcher m = INDEX_READ.matcher(source); StringBuffer out = new StringBuffer();
        while (m.find()) {
            String target = safeTarget(m.group(1), m.start(1), ids, types, symbols, true);
            if (target == null) { m.appendReplacement(out, Matcher.quoteReplacement(m.group(0))); continue; }
            m.appendReplacement(out, Matcher.quoteReplacement(target + ".getSelectedIndex()"));
        }
        m.appendTail(out); return out.toString();
    }

    private String convertSimpleIndexMethod(String source, Map<String,String> ids, Map<String,String> types,
                                            ScriptSymbolTable symbols, Pattern pattern, String suffix) {
        Matcher m = pattern.matcher(source); StringBuffer out = new StringBuffer();
        while (m.find()) {
            String target = safeTarget(m.group(1), m.start(1), ids, types, symbols, true);
            if (target == null) { m.appendReplacement(out, Matcher.quoteReplacement(m.group(0))); continue; }
            m.appendReplacement(out, Matcher.quoteReplacement(target + "." + suffix));
        }
        m.appendTail(out); return out.toString();
    }

    private String convertGridBindIndex(String source, Map<String,String> ids, Map<String,String> types,
                                        ScriptSymbolTable symbols, Map<String, String> protectedOriginals) {
        Matcher m = GRID_BIND_INDEX.matcher(source); StringBuffer out = new StringBuffer();
        while (m.find()) {
            String target = safeTarget(m.group(1), m.start(1), ids, types, symbols, false);
            String band = resolveProtectedString(m.group(2), protectedOriginals);
            if (target == null || !"Grid".equals(types.get(targetIdFromExpression(target))) || !"body".equalsIgnoreCase(band)) {
                m.appendReplacement(out, Matcher.quoteReplacement(m.group(0))); continue;
            }
            m.appendReplacement(out, Matcher.quoteReplacement(target + ".getColumnIndex(" + m.group(3).trim() + ")"));
        }
        m.appendTail(out); return out.toString();
    }

    private String resolveProtectedString(String value, Map<String, String> protectedOriginals) {
        if (value == null) return "";
        String original = protectedOriginals == null ? null : protectedOriginals.get(value);
        String text = original == null ? value : original;
        text = text.trim();
        if (text.length() >= 2 && ((text.charAt(0) == '\"' && text.charAt(text.length() - 1) == '\"')
                || (text.charAt(0) == '\'' && text.charAt(text.length() - 1) == '\''))) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    private String safeTarget(String rawPath, int position, Map<String,String> ids, Map<String,String> types,
                              ScriptSymbolTable symbols, boolean selectionOnly) {
        String root = rootName(rawPath);
        boolean explicitThis = isExplicitThis(rawPath);
        boolean shadowed = symbols.isJavaScriptVariable(root, position);
        if (!explicitThis && shadowed) return null;
        String target = resolve(rawPath, ids);
        if (target == null) return null;
        if (selectionOnly) {
            String type = types == null ? null : types.get(target);
            if (!("Combo".equals(type) || "ListBox".equals(type) || "Radio".equals(type))) return null;
        }
        return explicitThis && shadowed ? windowReference(target) : target;
    }

    private String targetIdFromExpression(String target) {
        if (target == null) return null;
        String prefix = "window[\"";
        String suffix = "\"]";
        if (target.startsWith(prefix) && target.endsWith(suffix)) {
            String value = target.substring(prefix.length(), target.length() - suffix.length());
            return value.replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return target;
    }

    private String windowReference(String targetId) {
        return "window[\"" + targetId.replace("\\", "\\\\").replace("\"", "\\\"") + "\"]";
    }

    private String resolve(String rawPath, Map<String,String> ids) {
        String canonical = canonicalize(rawPath);
        String exact = ids.get(canonical);
        if (exact != null) return exact;
        int dot = canonical.lastIndexOf('.'); String local = dot >= 0 ? canonical.substring(dot + 1) : canonical;
        String found = null;
        for (Map.Entry<String,String> e : ids.entrySet()) {
            String key = e.getKey(); int kd = key.lastIndexOf('.'); String kl = kd >= 0 ? key.substring(kd + 1) : key;
            if (!local.equals(kl)) continue;
            if (found != null && !found.equals(e.getValue())) return null;
            found = e.getValue();
        }
        return found;
    }

    private String rootName(String rawPath) {
        String c = canonicalize(rawPath); int dot = c.indexOf('.'); return dot < 0 ? c : c.substring(0, dot);
    }
    private boolean isExplicitThis(String rawPath) { return rawPath != null && rawPath.replaceAll("\\s+", "").startsWith("this."); }
    private String canonicalize(String raw) {
        String v = raw == null ? "" : raw.replaceAll("\\s+", "");
        if (v.startsWith("this.")) v = v.substring(5);
        v = v.replace(".form.", "."); while (v.startsWith("form.")) v = v.substring(5); return v;
    }
    private Set<String> componentRoots(Map<String,String> ids) {
        Set<String> out = new LinkedHashSet<String>();
        for (String key : ids.keySet()) {
            String c = canonicalize(key); int first = c.indexOf('.'); int last = c.lastIndexOf('.');
            if (c.length() > 0) { out.add(first < 0 ? c : c.substring(0, first)); out.add(last < 0 ? c : c.substring(last + 1)); }
        }
        return out;
    }
}
