package com.example.xfdltracker.binding;

public class ComponentBinding {
    private final String componentId;
    private final String propertyId;
    private final String datasetId;
    private final String columnId;
    public ComponentBinding(String componentId, String propertyId, String datasetId, String columnId) {
        this.componentId = componentId; this.propertyId = propertyId;
        this.datasetId = datasetId; this.columnId = columnId;
    }
    public String getComponentId() { return componentId; }
    public String getPropertyId() { return propertyId; }
    public String getDatasetId() { return datasetId; }
    public String getColumnId() { return columnId; }
}
