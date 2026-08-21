package com.example.xfdltracker.binding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BindingModel {
    private final List<ComponentBinding> componentBindings = new ArrayList<ComponentBinding>();
    private final List<ItemsetBinding> itemsets = new ArrayList<ItemsetBinding>();
    private final List<String> warnings = new ArrayList<String>();
    public List<ComponentBinding> getComponentBindings() { return Collections.unmodifiableList(componentBindings); }
    public List<ItemsetBinding> getItemsets() { return Collections.unmodifiableList(itemsets); }
    public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }
    public void addComponentBinding(ComponentBinding value) { componentBindings.add(value); }
    public void addItemset(ItemsetBinding value) { itemsets.add(value); }
    public void addWarning(String value) { warnings.add(value); }

    public ComponentBinding findComponentBinding(String sourcePath, String localId, String property) {
        ComponentBinding exact = null;
        ComponentBinding local = null;
        int localCount = 0;
        String canonical = canonical(sourcePath);
        for (ComponentBinding b : componentBindings) {
            if (property != null && !property.equalsIgnoreCase(b.getPropertyId())) continue;
            String candidate = canonical(b.getComponentId());
            if (candidate.equals(canonical)) exact = b;
            String candidateLocal = local(candidate);
            if (candidateLocal.equals(localId)) { local = b; localCount++; }
        }
        if (exact != null) return exact;
        return localCount == 1 ? local : null;
    }

    public ItemsetBinding findItemset(String sourcePath, String localId) {
        ItemsetBinding exact = null;
        ItemsetBinding local = null;
        int localCount = 0;
        String canonical = canonical(sourcePath);
        for (ItemsetBinding b : itemsets) {
            String candidate = canonical(b.getComponentId());
            if (candidate.equals(canonical)) exact = b;
            if (local(candidate).equals(localId)) { local = b; localCount++; }
        }
        if (exact != null) return exact;
        return localCount == 1 ? local : null;
    }

    private static String canonical(String value) {
        String v = value == null ? "" : value.replaceAll("\\s+", "");
        if (v.startsWith("this.")) v = v.substring(5);
        v = v.replace(".form.", ".");
        while (v.startsWith("form.")) v = v.substring(5);
        return v;
    }
    private static String local(String value) {
        int dot = value.lastIndexOf('.'); return dot >= 0 ? value.substring(dot + 1) : value;
    }
}
