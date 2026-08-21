package com.example.xfdltracker.tab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Replaces analyzed XPlatform Tab runtime operations using generated component/page ID mappings. */
public class TabRuntimeScriptConverter {
    private static class Replacement { int start,end; String value; Replacement(int s,int e,String v){start=s;end=e;value=v;} }

    public String convert(String source, TabRuntimePlan plan, Map<String,String> componentIdMap) {
        if(source==null||source.length()==0||plan==null||plan.getOperations().isEmpty())return source;
        List<Replacement> rs=new ArrayList<Replacement>();
        for(TabOperation op:plan.getOperations()){
            if(op.getStartOffset()<0||op.getEndOffset()>source.length()||op.getStartOffset()>=op.getEndOffset())continue;
            String actual=source.substring(op.getStartOffset(),op.getEndOffset());
            if(!compact(actual).equals(compact(op.getOriginalSource())))continue; // integrated source changed unexpectedly; fail closed
            String tabTarget=resolveTabTarget(op.getTabPath(),componentIdMap);
            if(tabTarget.length()==0)continue;
            String call=operationCall(op,plan,tabTarget);
            if(call.length()>0)rs.add(new Replacement(op.getStartOffset(),op.getEndOffset(),call));
        }
        Collections.sort(rs,new Comparator<Replacement>(){public int compare(Replacement a,Replacement b){return b.start-a.start;}});
        StringBuilder out=new StringBuilder(source);int last=source.length()+1;
        for(Replacement r:rs){if(r.end>last)continue;out.replace(r.start,r.end,r.value);last=r.start;}
        return out.toString();
    }

    private String operationCall(TabOperation op,TabRuntimePlan plan,String tabTarget){
        String tab=q(tabTarget), page=pageExpression(op,plan), eager=plan.isTabEager(op.getTabPath())?"true":"false";
        String[] a=op.getArguments();
        if(op.getType()==TabOperation.Type.SET_URL){
            String raw=op.getUrlExpression().length()==0?"null":op.getUrlExpression();
            String fixed=op.getWebSquareSrc().length()==0?"null":q(op.getWebSquareSrc());
            return "scwin.__xpTabRuntime.setUrl("+tab+","+page+","+raw+","+fixed+","+eager+")";
        }
        if(op.getType()==TabOperation.Type.ADD_PAGE){
            String id=a.length>0?a[0]:q(op.getPageId()); String label=a.length>1?a[1]:id; String data=a.length>2?a[2]:"undefined";
            return "scwin.__xpTabRuntime.addPage("+tab+","+id+","+label+","+data+","+eager+")";
        }
        if(op.getType()==TabOperation.Type.INSERT_PAGE){
            String id=a.length>0?a[0]:q(op.getPageId()); String index=a.length>1?a[1]:"-1"; String label=a.length>2?a[2]:id; String data=a.length>3?a[3]:"undefined";
            return "scwin.__xpTabRuntime.insertPage("+tab+","+id+","+index+","+label+","+data+","+eager+")";
        }
        if(op.getType()==TabOperation.Type.REMOVE_PAGE){String ref=a.length>0?a[0]:page;return "scwin.__xpTabRuntime.removePage("+tab+","+ref+")";}
        if(op.getType()==TabOperation.Type.SELECT_PAGE){String ref=a.length>0?a[0]:page;return "scwin.__xpTabRuntime.selectPage("+tab+","+ref+")";}
        return "";
    }
    private String pageExpression(TabOperation op,TabRuntimePlan plan){
        TabRuntimePlan.PageBinding b=findBinding(plan,op.getTabPath(),op.getPageId()); if(b!=null)return q(b.getTargetHeaderId());
        String id=op.getPageId(); if(id.matches("[0-9]+"))return id; if(id.length()>0)return q(id); return "null";
    }
    private TabRuntimePlan.PageBinding findBinding(TabRuntimePlan plan,String tab,String page){for(TabRuntimePlan.PageBinding b:plan.getPageBindings().values()){if(eq(b.getSourceTabPath(),tab)&&(b.getSourcePagePath().endsWith("."+page)||b.getSourcePagePath().equals(page)))return b;}return null;}
    private String resolveTabTarget(String sourcePath,Map<String,String> map){if(map==null)return "";String exact=map.get(canon(sourcePath));if(exact!=null)return exact;String local=sourcePath;int dot=local.lastIndexOf('.');if(dot>=0)local=local.substring(dot+1);String found=null;for(Map.Entry<String,String>e:map.entrySet()){String k=e.getKey(),l=k;int d=l.lastIndexOf('.');if(d>=0)l=l.substring(d+1);if(l.equals(local)){if(found!=null&&!found.equals(e.getValue()))return "";found=e.getValue();}}return found==null?"":found;}
    private static boolean eq(String a,String b){return canon(a).equals(canon(b));} private static String canon(String v){String x=v==null?"":v.replaceAll("\\s+","");if(x.startsWith("this."))x=x.substring(5);return x.replace(".form.",".");}
    private static String compact(String v){return v==null?"":v.replaceAll("\\s+","").replace(";","");}
    private static String q(String v){String x=v==null?"":v;return "\""+x.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
}
