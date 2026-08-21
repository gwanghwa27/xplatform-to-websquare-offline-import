package com.example.xfdltracker.tab;

/** Static-analysis representation of an XPlatform runtime Tab operation. */
public class TabOperation {
    public enum Type { SET_URL, ADD_PAGE, INSERT_PAGE, REMOVE_PAGE, SELECT_PAGE }
    public enum Status { SUPPORTED, RUNTIME_DYNAMIC, UNRESOLVED, PARTIAL }

    private final Type type;
    private final String screen;
    private final String functionName;
    private final int line;
    private final int startOffset;
    private final int endOffset;
    private final String originalSource;
    private final String tabExpression;
    private final String tabPath;
    private final String pageExpression;
    private final String pageId;
    private final String urlExpression;
    private final String resolvedSource;
    private final String generatedTarget;
    private final String webSquareSrc;
    private final String resolutionMethod;
    private final Status status;
    private final String message;
    private final String[] arguments;

    public TabOperation(Type type, String screen, String functionName, int line,
                        int startOffset, int endOffset, String originalSource,
                        String tabExpression, String tabPath, String pageExpression, String pageId,
                        String urlExpression, String resolvedSource, String generatedTarget,
                        String webSquareSrc, String resolutionMethod, Status status,
                        String message, String[] arguments) {
        this.type = type; this.screen = safe(screen); this.functionName = safe(functionName); this.line = line;
        this.startOffset = startOffset; this.endOffset = endOffset; this.originalSource = safe(originalSource);
        this.tabExpression = safe(tabExpression); this.tabPath = safe(tabPath); this.pageExpression = safe(pageExpression);
        this.pageId = safe(pageId); this.urlExpression = safe(urlExpression); this.resolvedSource = safe(resolvedSource);
        this.generatedTarget = safe(generatedTarget); this.webSquareSrc = safe(webSquareSrc);
        this.resolutionMethod = safe(resolutionMethod); this.status = status == null ? Status.PARTIAL : status;
        this.message = safe(message); this.arguments = arguments == null ? new String[0] : arguments.clone();
    }
    private static String safe(String v) { return v == null ? "" : v; }
    public Type getType() { return type; } public String getScreen() { return screen; }
    public String getFunctionName() { return functionName; } public int getLine() { return line; }
    public int getStartOffset() { return startOffset; } public int getEndOffset() { return endOffset; }
    public String getOriginalSource() { return originalSource; } public String getTabExpression() { return tabExpression; }
    public String getTabPath() { return tabPath; } public String getPageExpression() { return pageExpression; }
    public String getPageId() { return pageId; } public String getUrlExpression() { return urlExpression; }
    public String getResolvedSource() { return resolvedSource; } public String getGeneratedTarget() { return generatedTarget; }
    public String getWebSquareSrc() { return webSquareSrc; } public String getResolutionMethod() { return resolutionMethod; }
    public Status getStatus() { return status; } public String getMessage() { return message; }
    public String[] getArguments() { return arguments.clone(); }
}
