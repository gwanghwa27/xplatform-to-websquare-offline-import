package com.example.xfdltracker.tab;

import com.example.xfdltracker.util.JavaScriptCleaner;
import com.example.xfdltracker.script.ScriptSymbolTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Project-level parent/child screen reference analysis with cycle-safe screen links. */
public class CrossScreenReferenceAnalyzer {
    private static class Link { String parent,child,tabPath,pageId; int index; Link(String p,String c,String t,String id,int i){parent=p;child=c;tabPath=t;pageId=id;index=i;} }

    public void analyze(Map<String,String> scripts, Map<String,TabContentPlan> staticPlans,
                        Map<String,TabRuntimePlan> runtimePlans, Map<String,ScreenSymbolCatalog> catalogs) {
        List<Link> links=buildLinks(staticPlans,runtimePlans);
        Map<String,List<Link>> parentsByChild=new LinkedHashMap<String,List<Link>>();
        for(Link l:links){List<Link>x=parentsByChild.get(key(l.child));if(x==null){x=new ArrayList<Link>();parentsByChild.put(key(l.child),x);}x.add(l);}
        for(Link link:links)scanParentToChild(link,scripts.get(link.parent),runtimePlans,catalogs);
        for(Map.Entry<String,List<Link>> e:parentsByChild.entrySet()){
            for(Link childLink:e.getValue())scanChildToParent(childLink,scripts.get(childLink.child),runtimePlans,catalogs,parentsByChild);
        }
        detectCycles(links,runtimePlans);
    }

    private List<Link> buildLinks(Map<String,TabContentPlan> statics,Map<String,TabRuntimePlan> runtimes){List<Link>out=new ArrayList<Link>();
        if(statics!=null)for(TabContentPlan p:statics.values())for(TabContentReference r:p.getReferences())if(r.isResolved())addLink(out,new Link(p.getScreenRelativePath(),r.getResolvedSource(),r.getTabPath(),r.getTabPageId(),r.getTabPageIndex()));
        if(runtimes!=null)for(TabRuntimePlan p:runtimes.values())for(TabOperation o:p.getOperations())if(o.getType()==TabOperation.Type.SET_URL&&o.getResolvedSource().length()>0)addLink(out,new Link(p.getScreen(),o.getResolvedSource(),o.getTabPath(),o.getPageId(),-1));
        return out;}
    private void addLink(List<Link>out,Link n){for(Link l:out)if(key(l.parent).equals(key(n.parent))&&key(l.child).equals(key(n.child))&&key(l.tabPath).equals(key(n.tabPath))&&key(l.pageId).equals(key(n.pageId)))return;out.add(n);}

    private void scanParentToChild(Link link,String script,Map<String,TabRuntimePlan> plans,Map<String,ScreenSymbolCatalog> catalogs){if(script==null)return;TabRuntimePlan plan=plans.get(link.parent);if(plan==null)return;ScreenSymbolCatalog target=catalogs.get(link.child);String cleaned=TabScriptTextSupport.restoreQuotedTabpageSelectors(script, new JavaScriptCleaner().clean(script));
        String tab=local(link.tabPath),page=link.pageId;String base="(?<![A-Za-z0-9_$])(?:this\\s*\\.\\s*)?"+Pattern.quote(tab)+"\\s*\\.\\s*"+pageSelectorPattern(page,link.index)+"\\s*\\.\\s*(?:form\\s*\\.\\s*)?([A-Za-z_$][A-Za-z0-9_$]*)";
        Matcher m=Pattern.compile(base).matcher(cleaned);while(m.find()){String sym=m.group(1);
            if(isTabPageRuntimeMember(sym))continue;
            CrossScreenReference.SymbolType type=target==null?CrossScreenReference.SymbolType.UNKNOWN:target.typeOf(sym);CrossScreenReference.Status st=type==CrossScreenReference.SymbolType.UNKNOWN?CrossScreenReference.Status.UNRESOLVED:CrossScreenReference.Status.RESOLVED;
            if(isLazy(link,plans)&&type!=CrossScreenReference.SymbolType.UNKNOWN)st=CrossScreenReference.Status.RUNTIME_VERIFY_REQUIRED;
            String note="";
            if(st==CrossScreenReference.Status.RUNTIME_VERIFY_REQUIRED){
                note=type==CrossScreenReference.SymbolType.FUNCTION
                        ? "lazy child 함수 호출은 activateTab 이후 Promise 반환 가능"
                        : "lazy child Component/Dataset 동기 접근은 child render 전 사용할 수 없어 runtime 확인 필요";
            }
            plan.addCrossScreenReference(new CrossScreenReference(link.parent,functionAt(cleaned,m.start()),CrossScreenReference.Direction.PARENT_TO_CHILD,link.child,link.tabPath,link.pageId,sym,type,st,lineOf(script,m.start()),0,line(script,m.start()),note));
            if(type!=CrossScreenReference.SymbolType.UNKNOWN){TabRuntimePlan child=plans.get(link.child);if(child!=null)child.markBridgeTarget();}}
    }
    private static String pageSelectorPattern(String page,int index){
        String p=page==null?"":page;
        StringBuilder s=new StringBuilder("(?:");
        boolean first=true;
        if(p.matches("[0-9]+")){
            s.append("tabpages\\s*\\[\\s*").append(Pattern.quote(p)).append("\\s*\\]");
            first=false;
        } else if(p.length()>0){
            s.append(Pattern.quote(p));
            s.append("|tabpages\\s*\\[\\s*['\"]").append(Pattern.quote(p)).append("['\"]\\s*\\]");
            first=false;
        }
        if(index>=0){
            if(!first)s.append('|');
            s.append("tabpages\\s*\\[\\s*").append(index).append("\\s*\\]");
        }
        return s.append(')').toString();
    }

    private boolean isLazy(Link link,Map<String,TabRuntimePlan>plans){TabRuntimePlan p=plans.get(link.parent);return p==null||!p.isTabEager(link.tabPath);}

    private void scanChildToParent(Link immediate,String script,Map<String,TabRuntimePlan> plans,Map<String,ScreenSymbolCatalog> catalogs,Map<String,List<Link>> parentsByChild){if(script==null)return;TabRuntimePlan plan=plans.get(immediate.child);if(plan==null)return;String cleaned=TabScriptTextSupport.restoreQuotedTabpageSelectors(script, new JavaScriptCleaner().clean(script));
        ScriptSymbolTable lexical=ScriptSymbolTable.build(script,Collections.<String>emptySet(),Collections.<String>emptySet(),Collections.<String>emptySet(),Collections.<String>emptySet());
        Pattern p=Pattern.compile("(?<![A-Za-z0-9_$])(?:this\\s*\\.\\s*)?((?:parent\\s*\\.\\s*){2,})([A-Za-z_$][A-Za-z0-9_$]*)");Matcher m=p.matcher(cleaned);while(m.find()){
            if(lexical.isJavaScriptVariable("parent",m.start()))continue;
            int count=countParent(m.group(1));if((count%2)!=0){plan.addCrossScreenReference(new CrossScreenReference(immediate.child,functionAt(cleaned,m.start()),CrossScreenReference.Direction.CHILD_TO_PARENT,"",immediate.tabPath,immediate.pageId,m.group(2),CrossScreenReference.SymbolType.UNKNOWN,CrossScreenReference.Status.TODO,lineOf(script,m.start()),count/2,line(script,m.start()),"odd parent depth는 Tab object/Form 경계가 불명확"));continue;}int depth=count/2;String parent=ancestor(immediate.parent,depth-1,parentsByChild);ScreenSymbolCatalog target=catalogs.get(parent);CrossScreenReference.SymbolType type=target==null?CrossScreenReference.SymbolType.UNKNOWN:target.typeOf(m.group(2));CrossScreenReference.Status st=type==CrossScreenReference.SymbolType.UNKNOWN?CrossScreenReference.Status.UNRESOLVED:CrossScreenReference.Status.RESOLVED;plan.addCrossScreenReference(new CrossScreenReference(immediate.child,functionAt(cleaned,m.start()),CrossScreenReference.Direction.CHILD_TO_PARENT,parent,immediate.tabPath,immediate.pageId,m.group(2),type,st,lineOf(script,m.start()),depth,line(script,m.start()),""));TabRuntimePlan parentPlan=plans.get(parent);if(parentPlan!=null)parentPlan.markBridgeTarget();}
    }
    private String ancestor(String start,int levels,Map<String,List<Link>> parentsByChild){String cur=start;for(int i=0;i<levels;i++){List<Link> ps=parentsByChild.get(key(cur));if(ps==null||ps.size()!=1)return "";cur=ps.get(0).parent;}return cur;}

    private void detectCycles(List<Link>links,Map<String,TabRuntimePlan>plans){Map<String,List<String>>g=new LinkedHashMap<String,List<String>>();for(Link l:links){List<String>x=g.get(key(l.parent));if(x==null){x=new ArrayList<String>();g.put(key(l.parent),x);}x.add(l.child);}for(Link l:links)if(reaches(g,key(l.child),key(l.parent),new java.util.HashSet<String>())){TabRuntimePlan p=plans.get(l.parent);if(p!=null)p.addWarning("TAB SCREEN DEPENDENCY CYCLE: "+l.parent+" -> "+l.child+" -> ... -> "+l.parent);}}
    private boolean reaches(Map<String,List<String>>g,String cur,String target,java.util.Set<String>vis){if(cur.equals(target))return true;if(!vis.add(cur))return false;List<String>n=g.get(cur);if(n!=null)for(String v:n)if(reaches(g,key(v),target,vis))return true;return false;}

    private static boolean isTabPageRuntimeMember(String s){return "set_url".equals(s)||"url".equals(s)||"setFocus".equals(s)||"set_visible".equals(s)||"set_enable".equals(s);}
    private static int countParent(String v){Matcher m=Pattern.compile("parent").matcher(v);int n=0;while(m.find())n++;return n;}
    private static String local(String path){int d=path==null?-1:path.lastIndexOf('.');return d>=0?path.substring(d+1):path;}
    private static String key(String v){return v==null?"":v.replace('\\','/').toLowerCase();}
    private static int lineOf(String s,int pos){int n=1;for(int i=0;i<pos&&i<s.length();i++)if(s.charAt(i)=='\n')n++;return n;}
    private static String line(String s,int pos){int a=s.lastIndexOf('\n',Math.max(0,pos-1));a=a<0?0:a+1;int b=s.indexOf('\n',pos);if(b<0)b=s.length();return s.substring(a,b).replaceAll("\\s+"," ").trim();}
    private static String functionAt(String s,int pos){Pattern p=Pattern.compile("function\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\([^)]*\\)\\s*\\{");Matcher m=p.matcher(s);String best="";int bs=-1;while(m.find()){if(m.start()>pos)break;int close=closing(s,m.end()-1);if(close>=pos&&m.start()>bs){best=m.group(1);bs=m.start();}}return best;}
    private static int closing(String s,int o){int d=0;for(int i=o;i<s.length();i++){if(s.charAt(i)=='{')d++;else if(s.charAt(i)=='}'&&--d==0)return i;}return -1;}
}
