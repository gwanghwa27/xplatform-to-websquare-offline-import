package com.example.xfdltracker.tab;

/** Parent/child screen-scope reference discovered in XPlatform script. */
public class CrossScreenReference {
    public enum Direction { PARENT_TO_CHILD, CHILD_TO_PARENT }
    public enum SymbolType { FUNCTION, COMPONENT, DATASET, UNKNOWN }
    public enum Status { RESOLVED, UNRESOLVED, RUNTIME_VERIFY_REQUIRED, TODO }

    private final String sourceScreen, sourceFunction, targetScreen, tabPath, pageId, targetSymbol, sourceText, message;
    private final Direction direction; private final SymbolType symbolType; private final Status status;
    private final int line; private final int parentDepth;

    public CrossScreenReference(String sourceScreen, String sourceFunction, Direction direction, String targetScreen,
                                String tabPath, String pageId, String targetSymbol, SymbolType symbolType,
                                Status status, int line, int parentDepth, String sourceText, String message) {
        this.sourceScreen=s(sourceScreen); this.sourceFunction=s(sourceFunction); this.direction=direction;
        this.targetScreen=s(targetScreen); this.tabPath=s(tabPath); this.pageId=s(pageId); this.targetSymbol=s(targetSymbol);
        this.symbolType=symbolType==null?SymbolType.UNKNOWN:symbolType; this.status=status==null?Status.TODO:status;
        this.line=line; this.parentDepth=parentDepth; this.sourceText=s(sourceText); this.message=s(message);
    }
    private static String s(String v){return v==null?"":v;}
    public String getSourceScreen(){return sourceScreen;} public String getSourceFunction(){return sourceFunction;}
    public Direction getDirection(){return direction;} public String getTargetScreen(){return targetScreen;}
    public String getTabPath(){return tabPath;} public String getPageId(){return pageId;} public String getTargetSymbol(){return targetSymbol;}
    public SymbolType getSymbolType(){return symbolType;} public Status getStatus(){return status;} public int getLine(){return line;}
    public int getParentDepth(){return parentDepth;} public String getSourceText(){return sourceText;} public String getMessage(){return message;}
}
