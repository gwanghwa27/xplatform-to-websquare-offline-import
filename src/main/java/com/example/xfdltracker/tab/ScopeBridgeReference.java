package com.example.xfdltracker.tab;

/** Owner-frame / opener scope reference kept separate from normal Tab parent references. */
public class ScopeBridgeReference {
    public enum Kind { OWNER_FRAME, POPUP_OPENER }
    public enum Status { RESOLVED, RUNTIME_VERIFY_REQUIRED, UNRESOLVED, TODO }

    private final String sourceScreen, sourceFunction, targetScreen, targetSymbol, sourceText, message;
    private final Kind kind;
    private final CrossScreenReference.SymbolType symbolType;
    private final Status status;
    private final int line;
    private final int depth;

    public ScopeBridgeReference(String sourceScreen, String sourceFunction, Kind kind, int depth,
                                String targetScreen, String targetSymbol,
                                CrossScreenReference.SymbolType symbolType, Status status,
                                int line, String sourceText, String message) {
        this.sourceScreen=s(sourceScreen); this.sourceFunction=s(sourceFunction); this.kind=kind;
        this.depth=depth; this.targetScreen=s(targetScreen); this.targetSymbol=s(targetSymbol);
        this.symbolType=symbolType==null?CrossScreenReference.SymbolType.UNKNOWN:symbolType;
        this.status=status==null?Status.TODO:status; this.line=line; this.sourceText=s(sourceText); this.message=s(message);
    }
    private static String s(String v){return v==null?"":v;}
    public String getSourceScreen(){return sourceScreen;} public String getSourceFunction(){return sourceFunction;}
    public Kind getKind(){return kind;} public int getDepth(){return depth;} public String getTargetScreen(){return targetScreen;}
    public String getTargetSymbol(){return targetSymbol;} public CrossScreenReference.SymbolType getSymbolType(){return symbolType;}
    public Status getStatus(){return status;} public int getLine(){return line;} public String getSourceText(){return sourceText;}
    public String getMessage(){return message;}
}
