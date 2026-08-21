package com.example.xfdltracker.xjs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class XjsResolution {
    private final String screenRelativePath;
    private final List<XjsSymbol> selectedSymbols = new ArrayList<XjsSymbol>();
    private final Set<String> referencedModules = new LinkedHashSet<String>();
    private final Set<String> importedFunctions = new LinkedHashSet<String>();
    private final Set<String> importedGlobals = new LinkedHashSet<String>();
    private final Set<String> unresolvedFunctions = new LinkedHashSet<String>();
    private final Set<String> ambiguousSymbols = new LinkedHashSet<String>();
    private final List<String> dependencyEdges = new ArrayList<String>();
    private final List<String> includeWarnings = new ArrayList<String>();

    public XjsResolution(String screenRelativePath) { this.screenRelativePath = screenRelativePath; }
    public String getScreenRelativePath() { return screenRelativePath; }
    public List<XjsSymbol> getSelectedSymbols() { return selectedSymbols; }
    public Set<String> getReferencedModules() { return referencedModules; }
    public Set<String> getImportedFunctions() { return importedFunctions; }
    public Set<String> getImportedGlobals() { return importedGlobals; }
    public Set<String> getUnresolvedFunctions() { return unresolvedFunctions; }
    public Set<String> getAmbiguousSymbols() { return ambiguousSymbols; }
    public List<String> getDependencyEdges() { return dependencyEdges; }
    public List<String> getIncludeWarnings() { return includeWarnings; }

    public String buildExternalScript() {
        StringBuilder out = new StringBuilder();
        if (selectedSymbols.isEmpty()) return "";
        out.append("\n// ============================================================\n");
        out.append("// [Phase 3: selected external XJS dependencies]\n");
        out.append("// ============================================================\n");
        Set<String> emitted = new LinkedHashSet<String>();
        for (XjsSymbol symbol : selectedSymbols) {
            String key = symbol.getType() == XjsSymbolType.GLOBAL
                    ? symbol.getRelativePath() + "|GLOBAL_DECL|" + symbol.getLine() + "|" + symbol.getSource()
                    : symbol.key();
            if (!emitted.add(key)) continue;
            out.append("// [XJS SOURCE] ").append(symbol.getRelativePath())
               .append(':').append(symbol.getLine()).append(" symbol=").append(symbol.getName()).append('\n');
            out.append(symbol.getSource());
            if (symbol.getSource().length() > 0) {
                char last = symbol.getSource().charAt(symbol.getSource().length() - 1);
                if (last != '\n' && last != '\r') out.append('\n');
            }
        }
        return out.toString();
    }
}
