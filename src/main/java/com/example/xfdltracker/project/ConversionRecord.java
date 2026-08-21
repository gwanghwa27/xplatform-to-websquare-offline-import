package com.example.xfdltracker.project;

public class ConversionRecord {
    private final String source;
    private final String output;
    private final String type;
    private final String status;
    private final String message;

    public ConversionRecord(String source, String output, String type, String status, String message) {
        this.source = source;
        this.output = output;
        this.type = type;
        this.status = status;
        this.message = message;
    }

    public String getSource() { return source; }
    public String getOutput() { return output; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
}
