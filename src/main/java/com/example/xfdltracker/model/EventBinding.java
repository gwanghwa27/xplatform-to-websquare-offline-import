package com.example.xfdltracker.model;

public class EventBinding {
    private final String componentId;
    private final String eventName;
    private final String functionName;

    public EventBinding(String componentId, String eventName, String functionName) {
        this.componentId = componentId;
        this.eventName = eventName;
        this.functionName = functionName;
    }

    public String getComponentId() { return componentId; }
    public String getEventName() { return eventName; }
    public String getFunctionName() { return functionName; }
}
