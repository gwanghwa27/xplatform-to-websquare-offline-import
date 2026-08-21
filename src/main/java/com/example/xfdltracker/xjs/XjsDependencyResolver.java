package com.example.xfdltracker.xjs;

import com.example.xfdltracker.model.XfdlAnalysisResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Resolves only actually referenced XJS symbols, following XJS-to-XJS includes and dependencies. */
public class XjsDependencyResolver {
    private static final Set<String> KNOWN_NON_XJS_CALLS = new HashSet<String>(Arrays.asList(
            "trace","alert","confirm","transaction","open","close","showModal","setTimer","killTimer",
            "getEnvironmentVariable","setEnvironmentVariable","getApplication","getOwnerFrame","lookup",
            "addRow","deleteRow","getColumn","setColumn","getRowCount","clearData","copyData","filter","findRow",
            "getCellProperty","setCellProperty","getBindCellIndex","set_value","get_value","set_text","get_text",
            "set_enable","set_readonly","get_readonly","get_id","Math","String","Number","Boolean","Date","Array","Object",
            "parseInt","parseFloat","isNaN","encodeURIComponent","decodeURIComponent","setTimeout","clearTimeout"));

    private final XjsRepository repository;
    private final JavaScriptSymbolScanner scanner = new JavaScriptSymbolScanner();

    public XjsDependencyResolver(XjsRepository repository) { this.repository = repository; }

    public XjsResolution resolve(String screenRelativePath, String screenScript, XfdlAnalysisResult screenAnalysis) {
        return resolve(screenRelativePath, screenScript, screenAnalysis, Collections.<String>emptySet());
    }

    public XjsResolution resolve(String screenRelativePath, String screenScript, XfdlAnalysisResult screenAnalysis,
                                 Set<String> screenOwnedSymbols) {
        XjsResolution result = new XjsResolution(screenRelativePath);
        Set<String> ownedSymbols = screenOwnedSymbols == null
                ? Collections.<String>emptySet() : new LinkedHashSet<String>(screenOwnedSymbols);
        List<XjsModule> includeOrder = buildIncludeOrder(scanner.extractIncludes(screenScript), result);
        Set<String> internalFunctions = screenAnalysis == null
                ? Collections.<String>emptySet()
                : new LinkedHashSet<String>(screenAnalysis.getFunctions().keySet());

        // Use the Phase 3 scanner for external roots. The legacy call analyzer intentionally remains
        // compatible with Phase 1/2 and may classify component methods (obj.set_index()) as functions.
        Set<String> roots = new LinkedHashSet<String>();
        roots.addAll(scanner.findCalls(screenScript));
        roots.removeAll(internalFunctions);

        Set<String> visiting = new LinkedHashSet<String>();
        Set<String> visited = new LinkedHashSet<String>();
        Map<String, XjsSymbol> selectedByName = new LinkedHashMap<String, XjsSymbol>();

        for (String root : roots) {
            if (KNOWN_NON_XJS_CALLS.contains(root)) continue;
            resolveFunction(root, "SCREEN", includeOrder, internalFunctions,
                    result, visiting, visited, selectedByName);
        }

        // Direct XFDL references to XJS globals are dependencies even when no external function is called.
        XjsModule screenModule = new XjsModule(null, screenRelativePath, screenScript == null ? "" : screenScript);
        scanner.parseModule(screenModule);
        Set<String> screenGlobals = new LinkedHashSet<String>(screenModule.getGlobals().keySet());
        for (XjsSymbol function : screenModule.getFunctions().values()) {
            for (String identifier : function.getReferencedIdentifiers()) {
                if (!screenGlobals.contains(identifier) && !ownedSymbols.contains(identifier)
                        && !repository.findGlobals(identifier).isEmpty()) {
                    resolveGlobal(identifier, "SCREEN:" + function.getName(), includeOrder, internalFunctions,
                            result, visiting, visited, selectedByName);
                }
            }
        }
        for (XjsSymbol global : screenModule.getGlobals().values()) {
            for (String identifier : global.getReferencedIdentifiers()) {
                if (!screenGlobals.contains(identifier) && !ownedSymbols.contains(identifier)
                        && !repository.findGlobals(identifier).isEmpty()) {
                    resolveGlobal(identifier, "SCREEN_GLOBAL:" + global.getName(), includeOrder, internalFunctions,
                            result, visiting, visited, selectedByName);
                }
            }
        }

        return result;
    }

    private List<XjsModule> buildIncludeOrder(List<String> rootIncludes, XjsResolution result) {
        List<XjsModule> ordered = new ArrayList<XjsModule>();
        Set<String> visited = new LinkedHashSet<String>();
        for (String include : rootIncludes) addIncludeRecursive(include, ordered, visited, result);
        return ordered;
    }

    private void addIncludeRecursive(String include, List<XjsModule> ordered, Set<String> visited, XjsResolution result) {
        List<XjsModule> candidates = repository.resolveInclude(include);
        if (candidates.isEmpty()) {
            result.getIncludeWarnings().add("UNRESOLVED INCLUDE: " + include);
            return;
        }
        if (candidates.size() > 1) {
            StringBuilder files = new StringBuilder();
            for (XjsModule c : candidates) { if (files.length() > 0) files.append('|'); files.append(c.getRelativePath()); }
            result.getIncludeWarnings().add("AMBIGUOUS INCLUDE: " + include + " -> " + files);
            return;
        }
        XjsModule module = candidates.get(0);
        if (!visited.add(module.getRelativePath().toLowerCase())) return;
        ordered.add(module);
        if (!module.getTopLevelExecutableStatements().isEmpty()) {
            result.getIncludeWarnings().add("TOP_LEVEL_XJS_INIT: " + module.getRelativePath()
                    + " -> " + module.getTopLevelExecutableStatements());
        }
        for (String nested : module.getIncludes()) addIncludeRecursive(nested, ordered, visited, result);
    }

    private void resolveFunction(String name, String from, List<XjsModule> includeOrder,
                                 Set<String> internalFunctions, XjsResolution result,
                                 Set<String> visiting, Set<String> visited,
                                 Map<String, XjsSymbol> selectedByName) {
        if (name == null || name.length() == 0 || internalFunctions.contains(name) || KNOWN_NON_XJS_CALLS.contains(name)) return;
        String visitKey = "F:" + name;
        if (visited.contains(visitKey)) return;
        if (!visiting.add(visitKey)) {
            result.getDependencyEdges().add(from + " -> " + name + " [CYCLE]");
            return;
        }

        ResolutionChoice choice = chooseFunction(name, includeOrder);
        if (choice.ambiguous) {
            result.getAmbiguousSymbols().add("FUNCTION " + name + " -> " + choice.description);
            visiting.remove(visitKey);
            visited.add(visitKey);
            return;
        }
        if (choice.symbol == null) {
            result.getUnresolvedFunctions().add(name);
            result.getDependencyEdges().add(from + " -> " + name + " [UNRESOLVED]");
            visiting.remove(visitKey);
            visited.add(visitKey);
            return;
        }

        XjsSymbol existing = selectedByName.get(name);
        if (existing != null && !existing.key().equals(choice.symbol.key())) {
            result.getAmbiguousSymbols().add("FUNCTION " + name + " selected=" + existing.getRelativePath()
                    + " conflicting=" + choice.symbol.getRelativePath());
            visiting.remove(visitKey);
            visited.add(visitKey);
            return;
        }
        selectedByName.put(name, choice.symbol);
        select(choice.symbol, result);
        result.getDependencyEdges().add(from + " -> " + name + " [" + choice.symbol.getRelativePath() + "]");

        for (String called : choice.symbol.getCalledFunctions()) {
            if (!called.equals(name)) resolveFunction(called, name, includeOrder, internalFunctions,
                    result, visiting, visited, selectedByName);
        }
        for (String identifier : choice.symbol.getReferencedIdentifiers()) {
            if (repository.findGlobals(identifier).isEmpty()) continue;
            resolveGlobal(identifier, name, includeOrder, internalFunctions,
                    result, visiting, visited, selectedByName);
        }

        visiting.remove(visitKey);
        visited.add(visitKey);
    }

    private void resolveGlobal(String name, String from, List<XjsModule> includeOrder,
                               Set<String> internalFunctions, XjsResolution result,
                               Set<String> visiting, Set<String> visited,
                               Map<String, XjsSymbol> selectedByName) {
        String visitKey = "G:" + name;
        if (visited.contains(visitKey)) return;
        if (!visiting.add(visitKey)) {
            result.getDependencyEdges().add(from + " -> " + name + " [GLOBAL CYCLE]");
            return;
        }
        ResolutionChoice choice = chooseGlobal(name, includeOrder);
        if (choice.ambiguous) {
            result.getAmbiguousSymbols().add("GLOBAL " + name + " -> " + choice.description);
            visiting.remove(visitKey); visited.add(visitKey); return;
        }
        if (choice.symbol == null) {
            visiting.remove(visitKey); visited.add(visitKey); return;
        }
        select(choice.symbol, result);
        result.getDependencyEdges().add(from + " -> " + name + " [GLOBAL " + choice.symbol.getRelativePath() + "]");
        for (String called : choice.symbol.getCalledFunctions()) resolveFunction(called, name, includeOrder, internalFunctions,
                result, visiting, visited, selectedByName);
        for (String identifier : choice.symbol.getReferencedIdentifiers()) {
            if (!identifier.equals(name) && !repository.findGlobals(identifier).isEmpty()) {
                resolveGlobal(identifier, name, includeOrder, internalFunctions,
                        result, visiting, visited, selectedByName);
            }
        }
        visiting.remove(visitKey); visited.add(visitKey);
    }

    private void select(XjsSymbol symbol, XjsResolution result) {
        boolean found = false;
        for (XjsSymbol existing : result.getSelectedSymbols()) if (existing.key().equals(symbol.key())) { found = true; break; }
        if (!found) result.getSelectedSymbols().add(symbol);
        result.getReferencedModules().add(symbol.getRelativePath());
        if (symbol.getType() == XjsSymbolType.FUNCTION) result.getImportedFunctions().add(symbol.getName());
        else result.getImportedGlobals().add(symbol.getName());
    }

    private ResolutionChoice chooseFunction(String name, List<XjsModule> includeOrder) {
        return choose(name, includeOrder, true);
    }
    private ResolutionChoice chooseGlobal(String name, List<XjsModule> includeOrder) {
        return choose(name, includeOrder, false);
    }

    private ResolutionChoice choose(String name, List<XjsModule> includeOrder, boolean function) {
        List<XjsSymbol> included = new ArrayList<XjsSymbol>();
        for (XjsModule module : includeOrder) {
            XjsSymbol symbol = function ? module.getFunctions().get(name) : module.getGlobals().get(name);
            if (symbol != null) included.add(symbol);
            for (XjsSymbol duplicate : module.getDuplicateDefinitions()) {
                if (duplicate.getName().equals(name)
                        && (function ? duplicate.getType() == XjsSymbolType.FUNCTION
                                     : duplicate.getType() == XjsSymbolType.GLOBAL)) {
                    included.add(duplicate);
                }
            }
        }
        if (included.size() == 1) return new ResolutionChoice(included.get(0), false, "");
        if (included.size() > 1) return new ResolutionChoice(null, true, describe(included));

        List<XjsSymbol> all = function ? repository.findFunctions(name) : repository.findGlobals(name);
        if (all.size() == 1) return new ResolutionChoice(all.get(0), false, "project-unique fallback");
        if (all.size() > 1) return new ResolutionChoice(null, true, describe(all));
        return new ResolutionChoice(null, false, "");
    }

    private String describe(List<XjsSymbol> symbols) {
        StringBuilder sb = new StringBuilder();
        for (XjsSymbol s : symbols) { if (sb.length() > 0) sb.append('|'); sb.append(s.getRelativePath()).append(':').append(s.getLine()); }
        return sb.toString();
    }

    private static class ResolutionChoice {
        private final XjsSymbol symbol;
        private final boolean ambiguous;
        private final String description;
        private ResolutionChoice(XjsSymbol symbol, boolean ambiguous, String description) {
            this.symbol = symbol; this.ambiguous = ambiguous; this.description = description;
        }
    }
}
