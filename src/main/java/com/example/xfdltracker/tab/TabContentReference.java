package com.example.xfdltracker.tab;

/**
 * XPlatform Tab/Tabpage 외부 Form 참조를 프로젝트 단위로 표현한다.
 * 화면 파일 변환 중 부모/자식 componentIdMap을 합치지 않기 위한 독립 dependency model이다.
 */
public class TabContentReference {
    public enum Status { RESOLVED, UNRESOLVED, EXTERNAL_URL, DYNAMIC_ONLY }
    public enum LoadingMode { LAZY, EAGER, UNKNOWN }

    private final String parentScreen;
    private final String tabPath;
    private final String tabPagePath;
    private final String tabId;
    private final String tabPageId;
    private final int tabPageIndex;
    private final String sourceAttribute;
    private final String rawReference;
    private final String normalizedReference;
    private final String resolvedSource;
    private final String generatedTarget;
    private final String webSquareSrc;
    private final String resolutionMethod;
    private final Status status;
    private final LoadingMode loadingMode;
    private final boolean mixedInlineExternal;
    private final String message;

    public TabContentReference(
            String parentScreen,
            String tabPath,
            String tabPagePath,
            String tabId,
            String tabPageId,
            int tabPageIndex,
            String sourceAttribute,
            String rawReference,
            String normalizedReference,
            String resolvedSource,
            String generatedTarget,
            String webSquareSrc,
            String resolutionMethod,
            Status status,
            LoadingMode loadingMode,
            boolean mixedInlineExternal,
            String message) {
        this.parentScreen = safe(parentScreen);
        this.tabPath = safe(tabPath);
        this.tabPagePath = safe(tabPagePath);
        this.tabId = safe(tabId);
        this.tabPageId = safe(tabPageId);
        this.tabPageIndex = tabPageIndex;
        this.sourceAttribute = safe(sourceAttribute);
        this.rawReference = safe(rawReference);
        this.normalizedReference = safe(normalizedReference);
        this.resolvedSource = safe(resolvedSource);
        this.generatedTarget = safe(generatedTarget);
        this.webSquareSrc = safe(webSquareSrc);
        this.resolutionMethod = safe(resolutionMethod);
        this.status = status == null ? Status.UNRESOLVED : status;
        this.loadingMode = loadingMode == null ? LoadingMode.UNKNOWN : loadingMode;
        this.mixedInlineExternal = mixedInlineExternal;
        this.message = safe(message);
    }

    private static String safe(String value) { return value == null ? "" : value; }

    public String getParentScreen() { return parentScreen; }
    public String getTabPath() { return tabPath; }
    public String getTabPagePath() { return tabPagePath; }
    public String getTabId() { return tabId; }
    public String getTabPageId() { return tabPageId; }
    public int getTabPageIndex() { return tabPageIndex; }
    public String getSourceAttribute() { return sourceAttribute; }
    public String getRawReference() { return rawReference; }
    public String getNormalizedReference() { return normalizedReference; }
    public String getResolvedSource() { return resolvedSource; }
    public String getGeneratedTarget() { return generatedTarget; }
    public String getWebSquareSrc() { return webSquareSrc; }
    public String getResolutionMethod() { return resolutionMethod; }
    public Status getStatus() { return status; }
    public LoadingMode getLoadingMode() { return loadingMode; }
    public boolean isMixedInlineExternal() { return mixedInlineExternal; }
    public String getMessage() { return message; }
    public boolean isResolved() { return status == Status.RESOLVED; }
}
