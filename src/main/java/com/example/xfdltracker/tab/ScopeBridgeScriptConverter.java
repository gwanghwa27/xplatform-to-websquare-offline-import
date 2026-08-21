package com.example.xfdltracker.tab;

import com.example.xfdltracker.util.JavaScriptCleaner;
import com.example.xfdltracker.script.ScriptSymbolTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts separately modeled OwnerFrame / opener references through explicit runtime bridges. */
public class ScopeBridgeScriptConverter {
    private static class R { int a,b; String v; R(int a,int b,String v){this.a=a;this.b=b;this.v=v;} }

    public String convert(String source, TabRuntimePlan plan) {
        if (source == null || source.length() == 0 || plan == null || plan.getScopeBridgeReferences().isEmpty()) return source;
        String out = source;
        out = convertFunctions(out, plan);
        out = convertDatasets(out, plan);
        out = convertAssignments(out, plan);
        out = convertReads(out, plan);
        out = convertArguments(out, plan);
        return out;
    }

    private String convertFunctions(String src, TabRuntimePlan plan) {
        String cleaned = new JavaScriptCleaner().clean(src); List<R> rs = new ArrayList<R>();
        ScriptSymbolTable symbols = symbols(src);
        for (ScopeBridgeReference ref : plan.getScopeBridgeReferences()) {
            if (ref.getStatus() == ScopeBridgeReference.Status.UNRESOLVED || "arguments".equals(ref.getTargetSymbol())) continue;
            String prefix = prefix(ref); if (prefix.length() == 0) continue;
            Matcher m = Pattern.compile(prefix + Pattern.quote(ref.getTargetSymbol()) + "\\s*\\(").matcher(cleaned);
            while (m.find()) {
                if (!matchReference(ref, cleaned, symbols, m.start())) continue;
                int open = m.end() - 1, close = match(src, open); if (close < 0) continue;
                String args = src.substring(open + 1, close).trim();
                String call = ref.getKind() == ScopeBridgeReference.Kind.OWNER_FRAME
                        ? "scwin.__xpTabRuntime.callOwner("+ref.getDepth()+","+q(ref.getTargetSymbol())+",["+args+"])"
                        : "scwin.__xpTabRuntime.callOpener("+q(ref.getTargetSymbol())+",["+args+"])";
                rs.add(new R(m.start(), close + 1, call));
            }
        }
        return apply(src, rs);
    }

    private String convertDatasets(String src, TabRuntimePlan plan) {
        String cleaned = new JavaScriptCleaner().clean(src); List<R> rs = new ArrayList<R>();
        ScriptSymbolTable symbols = symbols(src);
        for (ScopeBridgeReference ref : plan.getScopeBridgeReferences()) {
            if (ref.getStatus() == ScopeBridgeReference.Status.UNRESOLVED || "arguments".equals(ref.getTargetSymbol())) continue;
            String prefix=prefix(ref); if(prefix.length()==0)continue;
            Matcher m=Pattern.compile(prefix+Pattern.quote(ref.getTargetSymbol())+"\\s*\\.\\s*(getRowCount|getColumn|setColumn|clearData)\\s*\\(").matcher(cleaned);
            while(m.find()){if(!matchReference(ref,cleaned,symbols,m.start()))continue;int open=m.end()-1,close=match(src,open);if(close<0)continue;String args=src.substring(open+1,close).trim();String obj=objectExpr(ref);String method=m.group(1);String call;
                if("getColumn".equals(method))call=obj+".getCellData("+args+")";else if("setColumn".equals(method))call=obj+".setCellData("+args+")";else call=obj+"."+method+"("+args+")";rs.add(new R(m.start(),close+1,call));}
        }
        return apply(src,rs);
    }

    private String convertAssignments(String src,TabRuntimePlan plan){
        String cleaned=new JavaScriptCleaner().clean(src);List<R>rs=new ArrayList<R>();ScriptSymbolTable symbols=symbols(src);
        for(ScopeBridgeReference ref:plan.getScopeBridgeReferences()){
            if(ref.getStatus()==ScopeBridgeReference.Status.UNRESOLVED||"arguments".equals(ref.getTargetSymbol()))continue;String prefix=prefix(ref);if(prefix.length()==0)continue;
            Matcher m=Pattern.compile(prefix+Pattern.quote(ref.getTargetSymbol())+"\\s*\\.\\s*(?:value|text)\\s*=").matcher(cleaned);
            while(m.find()){if(!matchReference(ref,cleaned,symbols,m.start()))continue;int vstart=m.end();while(vstart<src.length()&&Character.isWhitespace(src.charAt(vstart)))vstart++;int end=exprEnd(cleaned,vstart);String value=src.substring(vstart,end).trim();String call=ref.getKind()==ScopeBridgeReference.Kind.OWNER_FRAME?"scwin.__xpTabRuntime.setOwnerValue("+ref.getDepth()+","+q(ref.getTargetSymbol())+","+value+")":"scwin.__xpTabRuntime.setOpenerValue("+q(ref.getTargetSymbol())+","+value+")";rs.add(new R(m.start(),end,call));}
        }
        return apply(src,rs);
    }

    private String convertReads(String src,TabRuntimePlan plan){
        String cleaned=new JavaScriptCleaner().clean(src);List<R>rs=new ArrayList<R>();ScriptSymbolTable symbols=symbols(src);
        for(ScopeBridgeReference ref:plan.getScopeBridgeReferences()){
            if(ref.getStatus()==ScopeBridgeReference.Status.UNRESOLVED||"arguments".equals(ref.getTargetSymbol()))continue;String prefix=prefix(ref);if(prefix.length()==0)continue;
            Matcher m=Pattern.compile(prefix+Pattern.quote(ref.getTargetSymbol())+"\\s*\\.\\s*(?:value|text)").matcher(cleaned);
            while(m.find()){if(!matchReference(ref,cleaned,symbols,m.start()))continue;String call=ref.getKind()==ScopeBridgeReference.Kind.OWNER_FRAME?"scwin.__xpTabRuntime.getOwnerValue("+ref.getDepth()+","+q(ref.getTargetSymbol())+")":"scwin.__xpTabRuntime.getOpenerValue("+q(ref.getTargetSymbol())+")";rs.add(new R(m.start(),m.end(),call));}
        }
        return apply(src,rs);
    }

    private String convertArguments(String src,TabRuntimePlan plan){
        String cleaned=new JavaScriptCleaner().clean(src);List<R>rs=new ArrayList<R>();ScriptSymbolTable symbols=symbols(src);
        for(ScopeBridgeReference ref:plan.getScopeBridgeReferences())if(ref.getKind()==ScopeBridgeReference.Kind.OWNER_FRAME&&"arguments".equals(ref.getTargetSymbol())){String chain=ownerChain(ref.getDepth());Matcher m=Pattern.compile(chain+"arguments\\b").matcher(cleaned);while(m.find()){if(!matchReference(ref,cleaned,symbols,m.start()))continue;rs.add(new R(m.start(),m.end(),"scwin.__xpTabRuntime.ownerArguments("+ref.getDepth()+")"));}}
        return apply(src,rs);
    }

    private ScriptSymbolTable symbols(String src){return ScriptSymbolTable.build(src,java.util.Collections.<String>emptySet(),java.util.Collections.<String>emptySet(),java.util.Collections.<String>emptySet(),java.util.Collections.<String>emptySet());}
    private boolean matchReference(ScopeBridgeReference ref,String cleaned,ScriptSymbolTable symbols,int pos){
        String fn=functionAt(cleaned,pos);
        if(ref.getSourceFunction().length()>0&&!ref.getSourceFunction().equals(fn))return false;
        if(ref.getKind()==ScopeBridgeReference.Kind.POPUP_OPENER&&symbols.isJavaScriptVariable("opener",pos))return false;
        if(ref.getKind()==ScopeBridgeReference.Kind.OWNER_FRAME){int p=pos;while(p<cleaned.length()&&Character.isWhitespace(cleaned.charAt(p)))p++;boolean explicitThis=cleaned.startsWith("this",p);if(!explicitThis&&symbols.resolve("getOwnerFrame",pos)!=ScriptSymbolTable.Kind.UNRESOLVED)return false;}
        return true;
    }
    private static String functionAt(String s,int pos){Pattern p=Pattern.compile("function\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\([^)]*\\)\\s*\\{");Matcher m=p.matcher(s);String best="";int bs=-1;while(m.find()){if(m.start()>pos)break;int close=closing(s,m.end()-1);if(close>=pos&&m.start()>bs){best=m.group(1);bs=m.start();}}return best;}
    private static int closing(String s,int o){int d=0;for(int i=o;i<s.length();i++){if(s.charAt(i)=='{')d++;else if(s.charAt(i)=='}'&&--d==0)return i;}return -1;}

    private String objectExpr(ScopeBridgeReference r){return r.getKind()==ScopeBridgeReference.Kind.OWNER_FRAME?"scwin.__xpTabRuntime.ownerObject("+r.getDepth()+","+q(r.getTargetSymbol())+")":"scwin.__xpTabRuntime.openerObject("+q(r.getTargetSymbol())+")";}
    private String prefix(ScopeBridgeReference r){if(r.getKind()==ScopeBridgeReference.Kind.OWNER_FRAME)return ownerChain(r.getDepth())+"form\\s*\\.\\s*";return "(?<![A-Za-z0-9_$])(?:this\\s*\\.\\s*)?opener\\s*\\.\\s*";}
    private String ownerChain(int depth){StringBuilder s=new StringBuilder("(?<![A-Za-z0-9_$])(?:this\\s*\\.\\s*)?");for(int i=0;i<=depth;i++)s.append("getOwnerFrame\\s*\\(\\s*\\)\\s*\\.\\s*");return s.toString();}
    private String apply(String src,List<R>rs){Collections.sort(rs,new Comparator<R>(){public int compare(R x,R y){return y.a-x.a;}});StringBuilder b=new StringBuilder(src);int last=src.length()+1;for(R r:rs){if(r.b>last)continue;b.replace(r.a,r.b,r.v);last=r.a;}return b.toString();}
    private static int match(String s,int o){int d=0;char q=0;boolean esc=false;for(int i=o;i<s.length();i++){char c=s.charAt(i);if(q!=0){if(esc){esc=false;continue;}if(c=='\\'){esc=true;continue;}if(c==q)q=0;continue;}if(c=='\''||c=='\"'){q=c;continue;}if(c=='(')d++;else if(c==')'&&--d==0)return i;}return -1;}
    private static int exprEnd(String s,int from){int pa=0,br=0,bc=0;char q=0;boolean esc=false;for(int i=from;i<s.length();i++){char c=s.charAt(i);if(q!=0){if(esc){esc=false;continue;}if(c=='\\'){esc=true;continue;}if(c==q)q=0;continue;}if(c=='\''||c=='\"'){q=c;continue;}if(c=='(')pa++;else if(c==')'&&pa>0)pa--;else if(c=='[')br++;else if(c==']'&&br>0)br--;else if(c=='{')bc++;else if(c=='}'&&bc>0)bc--;else if((c==';'||c=='\n'||c=='\r')&&pa==0&&br==0&&bc==0)return i;}return s.length();}
    private static String q(String v){return "\""+(v==null?"":v).replace("\\","\\\\").replace("\"","\\\"")+"\"";}
}
