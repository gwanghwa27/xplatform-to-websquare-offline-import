package com.example.xfdltracker.project;

import com.example.xfdltracker.transaction.TransactionCall;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Per-screen Phase 3 feature inventory used by project migration reports. */
public class Phase3ScreenReport {
    private final String screen;
    private int componentsTotal, componentsSupported, componentsPartial, componentsTodo;
    private int propertiesMapped, propertiesTodo, eventsTotal, eventsMapped, eventsTodo, datasets;
    private int internalFunctions, externalFunctions, externalGlobals, unresolvedFunctions;
    private final List<String> unsupportedFeatures = new ArrayList<String>();
    private final List<String> apiCandidates = new ArrayList<String>();
    private final List<TransactionCall> transactions = new ArrayList<TransactionCall>();

    public Phase3ScreenReport(String screen) { this.screen = screen == null ? "" : screen; }
    public String getScreen() { return screen; }
    public int getComponentsTotal() { return componentsTotal; }
    public int getComponentsSupported() { return componentsSupported; }
    public int getComponentsPartial() { return componentsPartial; }
    public int getComponentsTodo() { return componentsTodo; }
    public int getPropertiesMapped() { return propertiesMapped; }
    public int getPropertiesTodo() { return propertiesTodo; }
    public int getEventsTotal() { return eventsTotal; }
    public int getEventsMapped() { return eventsMapped; }
    public int getEventsTodo() { return eventsTodo; }
    public int getDatasets() { return datasets; }
    public int getInternalFunctions() { return internalFunctions; }
    public int getExternalFunctions() { return externalFunctions; }
    public int getExternalGlobals() { return externalGlobals; }
    public int getUnresolvedFunctions() { return unresolvedFunctions; }
    public List<String> getUnsupportedFeatures() { return Collections.unmodifiableList(unsupportedFeatures); }
    public List<String> getApiCandidates() { return Collections.unmodifiableList(apiCandidates); }
    public List<TransactionCall> getTransactions() { return Collections.unmodifiableList(transactions); }

    void componentSupported() { componentsTotal++; componentsSupported++; }
    void componentPartial() { componentsTotal++; componentsPartial++; }
    void componentTodo() { componentsTotal++; componentsTodo++; }
    void propertyMapped() { propertiesMapped++; }
    void propertyTodo() { propertiesTodo++; }
    void eventMapped() { eventsTotal++; eventsMapped++; }
    void eventTodo() { eventsTotal++; eventsTodo++; }
    void setDatasets(int v) { datasets = v; }
    void setInternalFunctions(int v) { internalFunctions = v; }
    void setExternalFunctions(int v) { externalFunctions = v; }
    void setExternalGlobals(int v) { externalGlobals = v; }
    void setUnresolvedFunctions(int v) { unresolvedFunctions = v; }
    void addUnsupported(String v) { if (v != null && v.length() > 0 && !unsupportedFeatures.contains(v)) unsupportedFeatures.add(v); }
    void addApiCandidate(String v) { if (v != null && v.length() > 0 && !apiCandidates.contains(v)) apiCandidates.add(v); }
    void addTransactions(List<TransactionCall> values) { if (values != null) transactions.addAll(values); }
}
