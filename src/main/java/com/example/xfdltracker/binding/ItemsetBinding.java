package com.example.xfdltracker.binding;

public class ItemsetBinding {
    private final String componentId, datasetId, codeColumn, dataColumn;
    public ItemsetBinding(String componentId, String datasetId, String codeColumn, String dataColumn) {
        this.componentId = componentId; this.datasetId = datasetId;
        this.codeColumn = codeColumn; this.dataColumn = dataColumn;
    }
    public String getComponentId() { return componentId; }
    public String getDatasetId() { return datasetId; }
    public String getCodeColumn() { return codeColumn; }
    public String getDataColumn() { return dataColumn; }
}
