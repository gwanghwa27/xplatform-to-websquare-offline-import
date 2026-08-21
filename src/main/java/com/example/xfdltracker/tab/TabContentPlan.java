package com.example.xfdltracker.tab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Per-XFDL Tab external-content analysis result. */
public class TabContentPlan {
    private final String screenRelativePath;
    private final List<TabContentReference> references = new ArrayList<TabContentReference>();
    private final Map<String, TabContentReference> byPagePath = new LinkedHashMap<String, TabContentReference>();
    private final List<String> dynamicUsages = new ArrayList<String>();
    private final List<String> parentChildUsages = new ArrayList<String>();
    private final List<String> warnings = new ArrayList<String>();

    public TabContentPlan(String screenRelativePath) {
        this.screenRelativePath = screenRelativePath == null ? "" : screenRelativePath;
    }

    public String getScreenRelativePath() { return screenRelativePath; }
    public List<TabContentReference> getReferences() { return Collections.unmodifiableList(references); }
    public List<String> getDynamicUsages() { return Collections.unmodifiableList(dynamicUsages); }
    public List<String> getParentChildUsages() { return Collections.unmodifiableList(parentChildUsages); }
    public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }

    public void addReference(TabContentReference value) {
        if (value == null) return;
        references.add(value);
        String key = canonical(value.getTabPagePath());
        if (!byPagePath.containsKey(key)) byPagePath.put(key, value);
        else warnings.add("duplicate tab page path: " + value.getTabPagePath());
    }

    public void addDynamicUsage(String value) { addUnique(dynamicUsages, value); }
    public void addParentChildUsage(String value) { addUnique(parentChildUsages, value); }
    public void addWarning(String value) { addUnique(warnings, value); }

    public TabContentReference findByPagePath(String sourcePath) {
        return byPagePath.get(canonical(sourcePath));
    }

    private static void addUnique(List<String> target, String value) {
        if (value != null && value.length() > 0 && !target.contains(value)) target.add(value);
    }

    private static String canonical(String value) {
        if (value == null) return "";
        String v = value.replaceAll("\\s+", "");
        if (v.startsWith("this.")) v = v.substring(5);
        v = v.replace(".form.", ".");
        while (v.startsWith("form.")) v = v.substring(5);
        return v.toLowerCase();
    }
}
