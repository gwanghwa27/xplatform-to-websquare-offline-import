package com.example.xfdltracker.converter;

import com.example.xfdltracker.script.ScriptSymbolTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative, scope-aware XPlatform Dataset -> WebSquare DataList API mapping. */
public class DatasetApiConverter {
    private static final Pattern DOTTED_ROOT = Pattern.compile(
            "(?<![A-Za-z0-9_$])([A-Za-z_$][A-Za-z0-9_$]*)(\\s*\\.)");

    public String convert(String source, Set<String> datasetIds) {
        if (source == null || source.length() == 0 || datasetIds == null || datasetIds.isEmpty()) {
            return source == null ? "" : source;
        }

        ProtectedSource protectedSource = protectShadowedDatasets(source, datasetIds);
        String out = protectedSource.source;
        for (String id : datasetIds) {
            if (id == null || !id.matches("[A-Za-z_$][A-Za-z0-9_$]*")) continue;
            String q = Pattern.quote(id);
            // XPlatform this.Dataset is an explicit Form member reference. Keep it out of lexical shadowing
            // by resolving the WebSquare DataList through the browser global object.
            out = convertExplicitThisMethods(out, id);
            out = out.replaceAll("\\b" + q + "\\s*\\.\\s*getColumn\\s*\\(", Matcher.quoteReplacement(id + ".getCellData("));
            out = out.replaceAll("\\b" + q + "\\s*\\.\\s*setColumn\\s*\\(", Matcher.quoteReplacement(id + ".setCellData("));
            out = out.replaceAll("\\b" + q + "\\s*\\.\\s*clearData\\s*\\(\\s*\\)", Matcher.quoteReplacement(id + ".removeAll()"));
            out = out.replaceAll("\\b" + q + "\\s*\\.\\s*getRowCount\\s*\\(\\s*\\)", Matcher.quoteReplacement(id + ".getRowCount()"));
            out = out.replaceAll("\\b" + q + "\\s*\\.\\s*get_rowposition\\s*\\(\\s*\\)", Matcher.quoteReplacement(id + ".getRowPosition()"));
            out = out.replaceAll("\\b" + q + "\\s*\\.\\s*set_rowposition\\s*\\(", Matcher.quoteReplacement(id + ".setRowPosition("));
            out = out.replaceAll("\\b" + q + "\\s*\\.\\s*rowposition\\b", Matcher.quoteReplacement(id + ".getRowPosition()"));
            out = convertAddRow(out, id);
        }
        return protectedSource.restore(out);
    }

    private ProtectedSource protectShadowedDatasets(String source, Set<String> datasetIds) {
        ScriptSymbolTable table = ScriptSymbolTable.build(
                source, Collections.<String>emptySet(), datasetIds,
                Collections.<String>emptySet(), Collections.<String>emptySet());
        Matcher m = DOTTED_ROOT.matcher(table.getCleanedSource());
        List<Replacement> replacements = new ArrayList<Replacement>();
        int index = 0;
        while (m.find()) {
            String name = m.group(1);
            if (!datasetIds.contains(name)) continue;
            if (isExplicitThis(table.getCleanedSource(), m.start(1))) continue;
            if (table.isJavaScriptVariable(name, m.start(1))) {
                replacements.add(new Replacement(m.start(1), m.end(1),
                        "__XPWS_DATA_LOCAL_" + (index++) + "__", name));
            }
        }
        if (replacements.isEmpty()) return new ProtectedSource(source, Collections.<String, String>emptyMap());
        Collections.sort(replacements, new Comparator<Replacement>() {
            public int compare(Replacement a, Replacement b) { return b.start - a.start; }
        });
        StringBuilder text = new StringBuilder(source);
        Map<String, String> restore = new LinkedHashMap<String, String>();
        for (Replacement r : replacements) {
            text.replace(r.start, r.end, r.token);
            restore.put(r.token, r.original);
        }
        return new ProtectedSource(text.toString(), restore);
    }


    private String convertExplicitThisMethods(String source, String id) {
        String q = Pattern.quote(id);
        String prefix = "\\bthis\\s*\\.\\s*" + q + "\\s*\\.\\s*";
        String global = "window[\"" + id + "\"]";
        String out = source;
        out = out.replaceAll(prefix + "getColumn\\s*\\(", Matcher.quoteReplacement(global + ".getCellData("));
        out = out.replaceAll(prefix + "setColumn\\s*\\(", Matcher.quoteReplacement(global + ".setCellData("));
        out = out.replaceAll(prefix + "clearData\\s*\\(\\s*\\)", Matcher.quoteReplacement(global + ".removeAll()"));
        out = out.replaceAll(prefix + "getRowCount\\s*\\(\\s*\\)", Matcher.quoteReplacement(global + ".getRowCount()"));
        out = out.replaceAll(prefix + "get_rowposition\\s*\\(\\s*\\)", Matcher.quoteReplacement(global + ".getRowPosition()"));
        out = out.replaceAll(prefix + "set_rowposition\\s*\\(", Matcher.quoteReplacement(global + ".setRowPosition("));
        out = out.replaceAll(prefix + "rowposition\\b", Matcher.quoteReplacement(global + ".getRowPosition()"));
        out = out.replaceAll(prefix + "addRow\\s*\\(\\s*\\)", Matcher.quoteReplacement("scwin.xpAddDataListRow(" + global + ")"));
        return out;
    }

    private String convertAddRow(String source, String id) {
        Pattern p = Pattern.compile("\\b" + Pattern.quote(id) + "\\s*\\.\\s*addRow\\s*\\(\\s*\\)");
        Matcher m = p.matcher(source);
        return m.replaceAll(Matcher.quoteReplacement("scwin.xpAddDataListRow(" + id + ")"));
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
        private final int start, end; private final String token, original;
        private Replacement(int start, int end, String token, String original) {
            this.start = start; this.end = end; this.token = token; this.original = original;
        }
    }
    private static final class ProtectedSource {
        private final String source; private final Map<String, String> restore;
        private ProtectedSource(String source, Map<String, String> restore) { this.source = source; this.restore = restore; }
        private String restore(String text) {
            String out = text;
            for (Map.Entry<String, String> e : restore.entrySet()) out = out.replace(e.getKey(), e.getValue());
            return out;
        }
    }
}
