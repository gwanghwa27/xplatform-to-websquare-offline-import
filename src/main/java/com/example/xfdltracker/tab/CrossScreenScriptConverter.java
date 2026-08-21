package com.example.xfdltracker.tab;

import com.example.xfdltracker.util.JavaScriptCleaner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts validated parent/child Form access through WebSquare WFrame scope boundaries. */
public class CrossScreenScriptConverter {
    private static class R{int a,b;String v;R(int a,int b,String v){this.a=a;this.b=b;this.v=v;}}
    public String convert(String source,TabRuntimePlan plan,Map<String,String> componentIdMap){if(source==null||plan==null||plan.getCrossScreenReferences().isEmpty())return source;String out=source;
        // Function calls are structurally strongest; replace them before value/member reads.
        out=convertFunctionCalls(out,plan,componentIdMap);
        out=convertDatasetCalls(out,plan,componentIdMap);
        out=convertValueAssignments(out,plan,componentIdMap);
        out=convertValueReads(out,plan,componentIdMap);
        return out;}

    private String convertFunctionCalls(String src,TabRuntimePlan plan,Map<String,String> map){String cleaned=TabScriptTextSupport.restoreQuotedTabpageSelectors(src, new JavaScriptCleaner().clean(src));List<R>rs=new ArrayList<R>();for(CrossScreenReference ref:plan.getCrossScreenReferences())if(ref.getSymbolType()==CrossScreenReference.SymbolType.FUNCTION&&ref.getStatus()!=CrossScreenReference.Status.UNRESOLVED){String prefix=prefix(ref,plan), needle=prefix+ref.getTargetSymbol();Pattern p=Pattern.compile(needle+"\\s*\\(");Matcher m=p.matcher(cleaned);while(m.find()){int open=cleaned.indexOf('(',m.start());int close=match(src,open);if(close<0)continue;String args=src.substring(open+1,close).trim();String call;if(ref.getDirection()==CrossScreenReference.Direction.PARENT_TO_CHILD)call="scwin.__xpTabRuntime.callChild("+q(tabTarget(ref.getTabPath(),map))+","+pageRef(ref,plan)+","+q(ref.getTargetSymbol())+",["+args+"])";else call="scwin.__xpTabRuntime.callParent("+ref.getParentDepth()+","+q(ref.getTargetSymbol())+",["+args+"])";rs.add(new R(m.start(),close+1,call));}}return apply(src,rs);}

    private String convertDatasetCalls(String src,TabRuntimePlan plan,Map<String,String> map){String cleaned=TabScriptTextSupport.restoreQuotedTabpageSelectors(src, new JavaScriptCleaner().clean(src));List<R>rs=new ArrayList<R>();for(CrossScreenReference ref:plan.getCrossScreenReferences())if(ref.getSymbolType()==CrossScreenReference.SymbolType.DATASET&&ref.getStatus()!=CrossScreenReference.Status.UNRESOLVED){String base=prefix(ref,plan)+ref.getTargetSymbol()+"\\s*\\.\\s*(getRowCount|getColumn|setColumn|clearData)\\s*\\(";Matcher m=Pattern.compile(base).matcher(cleaned);while(m.find()){int open=cleaned.indexOf('(',m.start());int close=match(src,open);if(close<0)continue;String args=src.substring(open+1,close).trim(),obj=objectExpr(ref,plan,map);String method=m.group(1),call;if("getColumn".equals(method))call=obj+".getCellData("+args+")";else if("setColumn".equals(method))call=obj+".setCellData("+args+")";else call=obj+"."+method+"("+args+")";rs.add(new R(m.start(),close+1,call));}}return apply(src,rs);}

    private String convertValueAssignments(String src,TabRuntimePlan plan,Map<String,String> map){String cleaned=TabScriptTextSupport.restoreQuotedTabpageSelectors(src, new JavaScriptCleaner().clean(src));List<R>rs=new ArrayList<R>();for(CrossScreenReference ref:plan.getCrossScreenReferences())if((ref.getSymbolType()==CrossScreenReference.SymbolType.COMPONENT)&&ref.getStatus()!=CrossScreenReference.Status.UNRESOLVED){String base=prefix(ref,plan)+ref.getTargetSymbol()+"\\s*\\.\\s*(?:value|text)\\s*=";Matcher m=Pattern.compile(base).matcher(cleaned);while(m.find()){int vstart=m.end();while(vstart<src.length()&&Character.isWhitespace(src.charAt(vstart)))vstart++;int end=exprEnd(cleaned,vstart);String value=src.substring(vstart,end).trim();String call=ref.getDirection()==CrossScreenReference.Direction.PARENT_TO_CHILD?"scwin.__xpTabRuntime.setChildValue("+q(tabTarget(ref.getTabPath(),map))+","+pageRef(ref,plan)+","+q(ref.getTargetSymbol())+","+value+")":"scwin.__xpTabRuntime.setParentValue("+ref.getParentDepth()+","+q(ref.getTargetSymbol())+","+value+")";rs.add(new R(m.start(),end,call));}}return apply(src,rs);}
    private String convertValueReads(String src,TabRuntimePlan plan,Map<String,String> map){String cleaned=TabScriptTextSupport.restoreQuotedTabpageSelectors(src, new JavaScriptCleaner().clean(src));List<R>rs=new ArrayList<R>();for(CrossScreenReference ref:plan.getCrossScreenReferences())if(ref.getSymbolType()==CrossScreenReference.SymbolType.COMPONENT&&ref.getStatus()!=CrossScreenReference.Status.UNRESOLVED){String pat=prefix(ref,plan)+ref.getTargetSymbol()+"\\s*\\.\\s*(?:value|text)";Matcher m=Pattern.compile(pat).matcher(cleaned);while(m.find()){String call=ref.getDirection()==CrossScreenReference.Direction.PARENT_TO_CHILD?"scwin.__xpTabRuntime.getChildValue("+q(tabTarget(ref.getTabPath(),map))+","+pageRef(ref,plan)+","+q(ref.getTargetSymbol())+")":"scwin.__xpTabRuntime.getParentValue("+ref.getParentDepth()+","+q(ref.getTargetSymbol())+")";rs.add(new R(m.start(),m.end(),call));}}return apply(src,rs);}

    private String objectExpr(CrossScreenReference r,TabRuntimePlan p,Map<String,String>m){return r.getDirection()==CrossScreenReference.Direction.PARENT_TO_CHILD?"scwin.__xpTabRuntime.childObject("+q(tabTarget(r.getTabPath(),m))+","+pageRef(r,p)+","+q(r.getTargetSymbol())+")":"scwin.__xpTabRuntime.parentObject("+r.getParentDepth()+","+q(r.getTargetSymbol())+")";}
    private String prefix(CrossScreenReference r,TabRuntimePlan p){
        if(r.getDirection()==CrossScreenReference.Direction.CHILD_TO_PARENT){
            StringBuilder s=new StringBuilder("(?<![A-Za-z0-9_$])(?:this\\s*\\.\\s*)?");
            for(int i=0;i<r.getParentDepth()*2;i++)s.append("parent\\s*\\.\\s*");
            return s.toString();
        }
        String tab=Pattern.quote(local(r.getTabPath())),page=pageSelectorPattern(r,p);
        return "(?<![A-Za-z0-9_$])(?:this\\s*\\.\\s*)?"+tab+"\\s*\\.\\s*"+page+"\\s*\\.\\s*(?:form\\s*\\.\\s*)?";
    }
    private String pageSelectorPattern(CrossScreenReference r,TabRuntimePlan p){
        String id=r.getPageId()==null?"":r.getPageId();
        StringBuilder s=new StringBuilder("(?:");
        boolean first=true;
        if(id.matches("[0-9]+")){
            s.append("tabpages\\s*\\[\\s*").append(Pattern.quote(id)).append("\\s*\\]");
            first=false;
        } else if(id.length()>0){
            s.append(Pattern.quote(id));
            s.append("|tabpages\\s*\\[\\s*['\"]").append(Pattern.quote(id)).append("['\"]\\s*\\]");
            first=false;
        }
        int idx=pageIndex(r,p);
        if(idx>=0){
            if(!first)s.append('|');
            s.append("tabpages\\s*\\[\\s*").append(idx).append("\\s*\\]");
        }
        return s.append(')').toString();
    }
    private int pageIndex(CrossScreenReference r,TabRuntimePlan p){
        int i=0;
        for(TabRuntimePlan.PageBinding b:p.getPageBindings().values()){
            if(eq(b.getSourceTabPath(),r.getTabPath())){
                if(b.getSourcePagePath().endsWith("."+r.getPageId())||b.getSourcePagePath().equals(r.getPageId()))return i;
                i++;
            }
        }
        return -1;
    }
    private String pageRef(CrossScreenReference r,TabRuntimePlan p){for(TabRuntimePlan.PageBinding b:p.getPageBindings().values())if(eq(b.getSourceTabPath(),r.getTabPath())&&(b.getSourcePagePath().endsWith("."+r.getPageId())||b.getSourcePagePath().equals(r.getPageId())))return q(b.getTargetHeaderId());if(r.getPageId().matches("[0-9]+"))return r.getPageId();return q(r.getPageId());}
    private String tabTarget(String path,Map<String,String>map){String c=canon(path),v=map.get(c);if(v!=null)return v;String l=local(c),f=null;for(Map.Entry<String,String>e:map.entrySet())if(local(e.getKey()).equals(l)){if(f!=null&&!f.equals(e.getValue()))return l;f=e.getValue();}return f==null?l:f;}
    private String apply(String src,List<R>rs){Collections.sort(rs,new Comparator<R>(){public int compare(R x,R y){return y.a-x.a;}});StringBuilder b=new StringBuilder(src);int last=src.length()+1;for(R r:rs){if(r.b>last)continue;b.replace(r.a,r.b,r.v);last=r.a;}return b.toString();}
    private static int match(String s,int o){int d=0;char q=0;boolean esc=false;for(int i=o;i<s.length();i++){char c=s.charAt(i);if(q!=0){if(esc){esc=false;continue;}if(c=='\\'){esc=true;continue;}if(c==q)q=0;continue;}if(c=='\''||c=='\"'){q=c;continue;}if(c=='(')d++;else if(c==')'&&--d==0)return i;}return -1;}
    private static int exprEnd(String s,int from){int pa=0,br=0,bc=0;char q=0;boolean esc=false;for(int i=from;i<s.length();i++){char c=s.charAt(i);if(q!=0){if(esc){esc=false;continue;}if(c=='\\'){esc=true;continue;}if(c==q)q=0;continue;}if(c=='\''||c=='\"'){q=c;continue;}if(c=='(')pa++;else if(c==')'&&pa>0)pa--;else if(c=='[')br++;else if(c==']'&&br>0)br--;else if(c=='{')bc++;else if(c=='}'&&bc>0)bc--;else if((c==';'||c=='\n'||c=='\r')&&pa==0&&br==0&&bc==0)return i;}return s.length();}
    private static String local(String p){int d=p==null?-1:p.lastIndexOf('.');return d>=0?p.substring(d+1):p;}private static String canon(String v){String x=v==null?"":v.replaceAll("\\s+","");if(x.startsWith("this."))x=x.substring(5);return x.replace(".form.",".");}private static boolean eq(String a,String b){return canon(a).equals(canon(b));}private static String q(String v){return "\""+(v==null?"":v).replace("\\","\\\\").replace("\"","\\\"")+"\"";}
}
