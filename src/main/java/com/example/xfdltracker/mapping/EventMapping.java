package com.example.xfdltracker.mapping;

public class EventMapping {
    private final String sourceName;
    private final String targetName;
    private final SupportLevel supportLevel;
    private final String note;
    public EventMapping(String sourceName, String targetName, SupportLevel supportLevel, String note) {
        this.sourceName = sourceName;
        this.targetName = targetName;
        this.supportLevel = supportLevel;
        this.note = note == null ? "" : note;
    }
    public String getSourceName() { return sourceName; }
    public String getTargetName() { return targetName; }
    public SupportLevel getSupportLevel() { return supportLevel; }
    public String getNote() { return note; }
}
