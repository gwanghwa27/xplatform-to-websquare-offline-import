package com.example.xfdltracker.mapping;

public class PropertyMapping {
    private final String sourceName;
    private final String targetName;
    private final MappingKind kind;
    private final String note;

    public PropertyMapping(String sourceName, String targetName, MappingKind kind, String note) {
        this.sourceName = sourceName;
        this.targetName = targetName;
        this.kind = kind;
        this.note = note == null ? "" : note;
    }
    public String getSourceName() { return sourceName; }
    public String getTargetName() { return targetName; }
    public MappingKind getKind() { return kind; }
    public String getNote() { return note; }
}
