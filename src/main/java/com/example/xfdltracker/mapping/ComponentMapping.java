package com.example.xfdltracker.mapping;

public class ComponentMapping {
    private final String sourceName;
    private final String targetTag;
    private final SupportLevel supportLevel;
    private final boolean container;
    private final String note;

    public ComponentMapping(String sourceName, String targetTag, SupportLevel supportLevel,
                            boolean container, String note) {
        this.sourceName = sourceName;
        this.targetTag = targetTag;
        this.supportLevel = supportLevel;
        this.container = container;
        this.note = note == null ? "" : note;
    }

    public String getSourceName() { return sourceName; }
    public String getTargetTag() { return targetTag; }
    public SupportLevel getSupportLevel() { return supportLevel; }
    public boolean isContainer() { return container; }
    public String getNote() { return note; }
    public boolean isCreatable() { return targetTag != null && targetTag.length() > 0; }
}
