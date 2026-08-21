package com.example.xfdltracker.mapping;

/** Script API mapping metadata used by conversion and Phase 3 reports. */
public class ApiMapping {
    private final String category;
    private final String sourceName;
    private final String targetName;
    private final MappingKind kind;
    private final SupportLevel supportLevel;
    private final String note;

    public ApiMapping(String category, String sourceName, String targetName,
                      MappingKind kind, SupportLevel supportLevel, String note) {
        this.category = category == null ? "" : category;
        this.sourceName = sourceName == null ? "" : sourceName;
        this.targetName = targetName == null ? "" : targetName;
        this.kind = kind;
        this.supportLevel = supportLevel;
        this.note = note == null ? "" : note;
    }
    public String getCategory() { return category; }
    public String getSourceName() { return sourceName; }
    public String getTargetName() { return targetName; }
    public MappingKind getKind() { return kind; }
    public SupportLevel getSupportLevel() { return supportLevel; }
    public String getNote() { return note; }
}
