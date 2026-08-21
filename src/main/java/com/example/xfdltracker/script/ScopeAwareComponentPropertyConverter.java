package com.example.xfdltracker.script;

import com.example.xfdltracker.converter.ComponentPropertyConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Protects local/global JavaScript symbols before regex component conversion. */
public class ScopeAwareComponentPropertyConverter {
    private static final Pattern DOTTED_ROOT = Pattern.compile(
            "(?<![A-Za-z0-9_$])([A-Za-z_$][A-Za-z0-9_$]*)(\\s*\\.)");

    public String convert(String source, Map<String, String> componentIdMap) {
        if (source == null || source.length() == 0 || componentIdMap == null || componentIdMap.isEmpty()) {
            return source == null ? "" : source;
        }
        Set<String> componentRoots = componentRoots(componentIdMap);
        ScriptSymbolTable symbols = ScriptSymbolTable.build(
                source, componentRoots, Collections.<String>emptySet(),
                Collections.<String>emptySet(), Collections.<String>emptySet());
        String cleaned = symbols.getCleanedSource();
        List<Replacement> replacements = new ArrayList<Replacement>();
        Set<String> windowTargetIds = new LinkedHashSet<String>();
        Matcher m = DOTTED_ROOT.matcher(cleaned);
        int token = 0;
        while (m.find()) {
            String name = m.group(1);
            if (!componentRoots.contains(name)) continue;
            boolean explicitThis = isExplicitThis(cleaned, m.start(1));
            if (explicitThis) {
                // XPlatform this.<component> explicitly means the screen object. If a JavaScript
                // local shadows the same ID, a bare WebSquare target would be captured by that local.
                if (symbols.isJavaScriptVariable(name, m.start(1))) {
                    collectTargetsForRoot(name, componentIdMap, windowTargetIds);
                }
                continue;
            }
            if (symbols.isJavaScriptVariable(name, m.start(1))) {
                replacements.add(new Replacement(
                        m.start(1), m.end(1), "__XPWS_LOCAL_" + (token++) + "__", name));
            }
        }
        Map<String, String> effectiveMap = componentMapWithWindowTargets(componentIdMap, windowTargetIds);
        if (replacements.isEmpty()) return new ComponentPropertyConverter().convert(source, effectiveMap);

        Collections.sort(replacements, new Comparator<Replacement>() {
            public int compare(Replacement a, Replacement b) { return b.start - a.start; }
        });
        StringBuilder protectedText = new StringBuilder(source);
        Map<String, String> restore = new HashMap<String, String>();
        for (Replacement r : replacements) {
            protectedText.replace(r.start, r.end, r.token);
            restore.put(r.token, r.original);
        }
        String converted = new ComponentPropertyConverter().convert(protectedText.toString(), effectiveMap);
        for (Map.Entry<String, String> e : restore.entrySet()) {
            converted = converted.replace(e.getKey(), e.getValue());
        }
        return converted;
    }

    private void collectTargetsForRoot(String root, Map<String, String> map, Set<String> out) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key == null) continue;
            String canonical = key.replaceAll("\\s+", "");
            if (canonical.startsWith("this.")) canonical = canonical.substring(5);
            canonical = canonical.replace(".form.", ".");
            int dot = canonical.indexOf('.');
            String keyRoot = dot < 0 ? canonical : canonical.substring(0, dot);
            if (root.equals(keyRoot)) out.add(entry.getValue());
        }
    }

    private Map<String, String> componentMapWithWindowTargets(Map<String, String> source, Set<String> windowTargetIds) {
        if (windowTargetIds == null || windowTargetIds.isEmpty()) return source;
        Map<String, String> out = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String target = entry.getValue();
            out.put(entry.getKey(), windowTargetIds.contains(target) ? windowReference(target) : target);
        }
        return out;
    }

    private String windowReference(String targetId) {
        return "window[\"" + targetId.replace("\\", "\\\\").replace("\"", "\\\"") + "\"]";
    }

    private Set<String> componentRoots(Map<String, String> map) {
        Set<String> result = new LinkedHashSet<String>();
        for (String key : map.keySet()) {
            if (key == null) continue;
            int dot = key.indexOf('.');
            result.add(dot < 0 ? key : key.substring(0, dot));
            int last = key.lastIndexOf('.');
            result.add(last < 0 ? key : key.substring(last + 1));
        }
        return result;
    }

    private boolean isExplicitThis(String source, int start) {
        int p = start - 1;
        while (p >= 0 && Character.isWhitespace(source.charAt(p))) p--;
        if (p < 0 || source.charAt(p) != '.') return false;
        p--;
        while (p >= 0 && Character.isWhitespace(source.charAt(p))) p--;
        int end = p + 1;
        while (p >= 0 && (Character.isLetterOrDigit(source.charAt(p))
                || source.charAt(p) == '_' || source.charAt(p) == '$')) p--;
        return "this".equals(source.substring(p + 1, end));
    }

    private static final class Replacement {
        private final int start, end;
        private final String token, original;
        private Replacement(int start, int end, String token, String original) {
            this.start = start; this.end = end; this.token = token; this.original = original;
        }
    }
}
