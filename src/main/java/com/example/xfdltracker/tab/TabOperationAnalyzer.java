package com.example.xfdltracker.tab;

import com.example.xfdltracker.util.JavaScriptCleaner;
import com.example.xfdltracker.parser.XfdlReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight Tab runtime-operation scanner. It recognizes member/call boundaries first,
 * then applies deliberately small constant propagation for screen paths.
 */
public class TabOperationAnalyzer {
    private static final String[] METHODS = {
            "set_url", "addTabpage", "insertTabpage", "removeTabpage",
            "set_tabindex", "set_tabpageindex", "set_selectedindex"
    };
    private static final Pattern DECL_STRING = Pattern.compile(
            "(?m)(?:var|let|const)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*([\\\"'][^;\\r\\n]+)");
    private final ScreenTargetRegistry targets;

    public TabOperationAnalyzer(ScreenTargetRegistry targets) { this.targets = targets; }

    public TabRuntimePlan analyze(File screenFile, String screenRelative, String script, TabContentPlan staticPlan) throws Exception {
        TabRuntimePlan plan = new TabRuntimePlan(screenRelative);
        Map<String,String> knownTabs = discoverTabs(screenFile, plan);
        if (staticPlan != null) {
            for (TabContentReference ref : staticPlan.getReferences()) {
                if (ref.isResolved()) {
                    plan.putRuntimePath(ref.getRawReference(), ref.getWebSquareSrc());
                    plan.putRuntimePath(ref.getNormalizedReference(), ref.getWebSquareSrc());
                    plan.putRuntimePath(ref.getResolvedSource(), ref.getWebSquareSrc());
                }
                if (ref.getLoadingMode() != TabContentReference.LoadingMode.UNKNOWN) {
                    plan.setTabEager(ref.getTabPath(), ref.getLoadingMode() == TabContentReference.LoadingMode.EAGER);
                }
                registerKnownTab(knownTabs, ref.getTabId(), ref.getTabPath());
            }
        }
        if (script == null || script.length() == 0) return plan;

        String cleaned = new JavaScriptCleaner().clean(script);
        Map<String,String> globalConstants = topLevelConstants(script, cleaned);
        for (String method : METHODS) scanCalls(screenFile, screenRelative, script, cleaned, method, staticPlan, knownTabs, globalConstants, plan);
        scanUrlAssignments(screenFile, screenRelative, script, cleaned, staticPlan, knownTabs, globalConstants, plan);
        return plan;
    }

    private void scanCalls(File screenFile, String screenRel, String original, String cleaned, String method,
                           TabContentPlan staticPlan, Map<String,String> knownTabs, Map<String,String> globals, TabRuntimePlan out) throws Exception {
        String needle = "." + method;
        int cursor = 0;
        while (true) {
            int methodPos = indexOfToken(cleaned, needle, cursor);
            if (methodPos < 0) break;
            int open = skipWs(cleaned, methodPos + needle.length());
            if (open >= cleaned.length() || cleaned.charAt(open) != '(') { cursor = methodPos + needle.length(); continue; }
            int close = findMatching(cleaned, open, '(', ')');
            if (close < 0) { cursor = open + 1; continue; }
            int receiverStart = findReceiverStart(cleaned, methodPos);
            String receiverRaw = original.substring(receiverStart, methodPos);
            int receiverShift = receiverExpressionOffset(receiverRaw);
            receiverStart += receiverShift;
            String receiver = compact(receiverRaw.substring(receiverShift));
            if (!isScreenTabReceiver(receiver, original, receiverStart, staticPlan, knownTabs)) { cursor = close + 1; continue; }
            String[] args = splitArguments(original.substring(open + 1, close));
            TabOperation.Type type = typeFor(method);
            String tabExpr = receiver;
            String tabPath = extractTabPath(receiver, staticPlan, knownTabs);
            String pageExpr = type == TabOperation.Type.SET_URL ? receiver : "";
            String pageId = type == TabOperation.Type.SET_URL ? extractPageId(receiver, staticPlan) : pageIdFromArguments(type, args);
            String urlExpr = type == TabOperation.Type.SET_URL && args.length > 0 ? args[0].trim() : "";
            ScreenTargetRegistry.Resolution resolution = null;
            TabOperation.Status status = TabOperation.Status.SUPPORTED;
            String message = "";
            String staticPath = "";
            if (type == TabOperation.Type.SET_URL) {
                staticPath = evaluateString(urlExpr, constantsBefore(original, cleaned, receiverStart), globals);
                if (staticPath.length() > 0) {
                    resolution = targets.resolve(screenFile, screenRel, staticPath);
                    if (!resolution.isResolved()) { status = TabOperation.Status.UNRESOLVED; message = resolution.getMessage(); }
                    else {
                        out.putRuntimePath(staticPath, resolution.getWebSquareSrc());
                        out.putRuntimePath(resolution.getResolvedSource(), resolution.getWebSquareSrc());
                    }
                } else {
                    status = TabOperation.Status.RUNTIME_DYNAMIC;
                    message = "정적 target을 확정할 수 없어 runtime path registry로 변환";
                }
            }
            String source = original.substring(receiverStart, close + 1);
            out.addOperation(new TabOperation(type, screenRel, functionAt(cleaned, receiverStart), lineOf(original, receiverStart),
                    receiverStart, close + 1, source, tabExpr, tabPath, pageExpr, pageId, urlExpr,
                    resolution == null ? "" : resolution.getResolvedSource(), resolution == null ? "" : resolution.getGeneratedTarget(),
                    resolution == null ? "" : resolution.getWebSquareSrc(), resolution == null ? "" : resolution.getMethod(), status,
                    message, args));
            cursor = close + 1;
        }
    }

    private void scanUrlAssignments(File screenFile, String screenRel, String original, String cleaned,
                                    TabContentPlan staticPlan, Map<String,String> knownTabs, Map<String,String> globals, TabRuntimePlan out) throws Exception {
        int cursor = 0;
        while (true) {
            int pos = indexOfToken(cleaned, ".url", cursor);
            if (pos < 0) break;
            int eq = skipWs(cleaned, pos + 4);
            if (eq >= cleaned.length() || cleaned.charAt(eq) != '=' || (eq + 1 < cleaned.length() && cleaned.charAt(eq + 1) == '=')) { cursor = pos + 4; continue; }
            int end = findExpressionEnd(cleaned, eq + 1);
            int start = findReceiverStart(cleaned, pos);
            String receiverRaw = original.substring(start, pos);
            int receiverShift = receiverExpressionOffset(receiverRaw);
            start += receiverShift;
            String receiver = compact(receiverRaw.substring(receiverShift));
            if (!isScreenTabReceiver(receiver, original, start, staticPlan, knownTabs)) { cursor = end; continue; }
            String expr = original.substring(eq + 1, end).trim();
            String staticPath = evaluateString(expr, constantsBefore(original, cleaned, start), globals);
            ScreenTargetRegistry.Resolution resolution = null;
            TabOperation.Status status = TabOperation.Status.RUNTIME_DYNAMIC;
            String message = "정적 target을 확정할 수 없어 runtime path registry로 변환";
            if (staticPath.length() > 0) {
                resolution = targets.resolve(screenFile, screenRel, staticPath);
                if (resolution.isResolved()) { status = TabOperation.Status.SUPPORTED; message = ""; out.putRuntimePath(staticPath, resolution.getWebSquareSrc()); }
                else { status = TabOperation.Status.UNRESOLVED; message = resolution.getMessage(); }
            }
            String source = original.substring(start, end);
            out.addOperation(new TabOperation(TabOperation.Type.SET_URL, screenRel, functionAt(cleaned, start), lineOf(original, start),
                    start, end, source, receiver, extractTabPath(receiver, staticPlan, knownTabs), receiver,
                    extractPageId(receiver, staticPlan), expr,
                    resolution == null ? "" : resolution.getResolvedSource(), resolution == null ? "" : resolution.getGeneratedTarget(),
                    resolution == null ? "" : resolution.getWebSquareSrc(), resolution == null ? "" : resolution.getMethod(), status, message,
                    new String[]{expr}));
            cursor = end;
        }
    }

    private boolean isScreenTabReceiver(String receiver, String source, int offset, TabContentPlan staticPlan, Map<String,String> knownTabs) {
        String r = receiver.replaceAll("\\s+", "");
        boolean explicitThis = r.startsWith("this.");
        if (explicitThis) r = r.substring(5);
        String tabPath = matchKnownTabPath(r, staticPlan, knownTabs);
        if (tabPath.length() == 0) return false;
        if (explicitThis) return true;
        String first = firstIdentifier(r);
        if (first.length() == 0) return false;
        String prefix = source.substring(0, Math.max(0, offset));
        String fn = functionAt(new JavaScriptCleaner().clean(source), offset);
        int fnPos = fn.length() == 0 ? 0 : Math.max(0, prefix.lastIndexOf("function " + fn));
        String scope = prefix.substring(fnPos);
        Pattern shadow = Pattern.compile("(?:var|let|const|function)\\s+" + Pattern.quote(first) + "\\b");
        return !shadow.matcher(scope).find();
    }

    private static String matchKnownTabPath(String receiver, TabContentPlan plan, Map<String,String> knownTabs) {
        String r=receiver==null?"":receiver.replaceAll("\\s+","");
        String best="";
        if(plan!=null)for(TabContentReference ref:plan.getReferences()){
            String p=ref.getTabPath(); if(pathPrefix(r,p)&&p.length()>best.length())best=p;
        }
        if(knownTabs!=null)for(String p:knownTabs.values()){
            if(p!=null&&p.length()>0&&pathPrefix(r,p)&&p.length()>best.length())best=p;
        }
        return best;
    }
    private static boolean pathPrefix(String receiver,String path){if(path==null||path.length()==0)return false;return receiver.equals(path)||receiver.startsWith(path+".")||receiver.startsWith(path+".tabpages[");}

    private Map<String,String> discoverTabs(File screenFile, TabRuntimePlan plan) throws Exception {
        Map<String,String> out=new LinkedHashMap<String,String>();
        if(screenFile==null||!screenFile.isFile())return out;
        Document doc=new XfdlReader().read(screenFile);
        NodeList all=doc.getElementsByTagName("*");
        for(int i=0;i<all.getLength();i++){
            Element e=(Element)all.item(i);
            if(!"Tab".equals(localTag(e)))continue;
            String id=e.getAttribute("id").trim();if(id.length()==0)continue;
            String path=componentPath(e);
            registerKnownTab(out,id,path);
            String preload=e.getAttribute("preload");
            plan.setTabEager(path,"true".equalsIgnoreCase(preload)||"1".equals(preload));
        }
        return out;
    }
    private static void registerKnownTab(Map<String,String> map,String id,String path){
        if(map==null||id==null||id.length()==0)return;
        String old=map.get(id);
        if(old==null)map.put(id,path==null?id:path);
        else if(!old.equals(path))map.put(id,""); // duplicate local ID: only explicit/full path may be used safely
    }
    private static String componentPath(Element e){
        List<String> ids=new ArrayList<String>();
        String own=e.getAttribute("id").trim();if(own.length()>0)ids.add(own);
        Node n=e.getParentNode();
        while(n instanceof Element){Element p=(Element)n;String tag=localTag(p);if("Form".equals(tag))break;
            if(isPathContainer(tag)){String id=p.getAttribute("id").trim();if(id.length()>0)ids.add(0,id);}
            n=p.getParentNode();}
        StringBuilder sb=new StringBuilder();for(String id:ids){if(sb.length()>0)sb.append('.');sb.append(id);}return sb.toString();
    }
    private static boolean isPathContainer(String tag){return "Div".equals(tag)||"Tab".equals(tag)||"Tabpage".equals(tag)||"GroupBox".equals(tag)||"PopupDiv".equals(tag);}
    private static String localTag(Element e){String n=e.getTagName();int c=n==null?-1:n.indexOf(':');return c>=0?n.substring(c+1):(n==null?"":n);}

    private static TabOperation.Type typeFor(String method) {
        if ("set_url".equals(method)) return TabOperation.Type.SET_URL;
        if ("addTabpage".equals(method)) return TabOperation.Type.ADD_PAGE;
        if ("insertTabpage".equals(method)) return TabOperation.Type.INSERT_PAGE;
        if ("removeTabpage".equals(method)) return TabOperation.Type.REMOVE_PAGE;
        return TabOperation.Type.SELECT_PAGE;
    }
    private static String pageIdFromArguments(TabOperation.Type type, String[] args) {
        if ((type == TabOperation.Type.ADD_PAGE || type == TabOperation.Type.INSERT_PAGE || type == TabOperation.Type.REMOVE_PAGE) && args.length > 0) return stripQuoted(args[0].trim());
        return "";
    }
    private static String extractTabPath(String receiver, TabContentPlan plan, Map<String,String> knownTabs) {
        String r = receiver.replaceAll("\\s+", ""); if (r.startsWith("this.")) r=r.substring(5);
        String known=matchKnownTabPath(r,plan,knownTabs); if(known.length()>0)return known;
        int dot = r.indexOf('.'); return dot < 0 ? r : r.substring(0,dot);
    }
    private static String extractPageId(String receiver, TabContentPlan plan) {
        String r=receiver.replaceAll("\\s+",""); if(r.startsWith("this."))r=r.substring(5);
        int tp=r.indexOf(".tabpages[");
        if(tp>=0){int b=r.indexOf('[',tp),e=r.indexOf(']',b); if(e>b)return stripQuoted(r.substring(b+1,e));}
        String[] p=r.split("\\."); if(p.length>=2)return "form".equals(p[p.length-1])&&p.length>=3?p[p.length-2]:p[p.length-1];
        return "";
    }

    private static Map<String,String> topLevelConstants(String original,String cleaned){
        Map<String,String> out=new LinkedHashMap<String,String>();
        Matcher m=DECL_STRING.matcher(original);
        while(m.find()){
            if(m.start()>=cleaned.length()||cleaned.charAt(m.start())==' ')continue;
            if(braceDepthAt(cleaned,m.start())!=0)continue;
            String expr=m.group(2);int semi=expr.indexOf(';');if(semi>=0)expr=expr.substring(0,semi);
            String v=evaluateLiteralConcat(expr,out);if(v.length()>0)out.put(m.group(1),v);
        }
        return out;
    }
    private static Map<String,String> constantsBefore(String original, String cleaned, int end) {
        Map<String,String> out=new LinkedHashMap<String,String>();
        int limit=Math.min(end,original.length());
        int scopeStart=functionBodyStartAt(cleaned,limit);
        Matcher m=DECL_STRING.matcher(original.substring(scopeStart,limit));
        while(m.find()){
            int absolute=scopeStart+m.start();
            if(absolute>=cleaned.length()||cleaned.charAt(absolute)==' ')continue;
            String expr=m.group(2); int semi=expr.indexOf(';'); if(semi>=0)expr=expr.substring(0,semi);
            String v=evaluateLiteralConcat(expr,out); if(v.length()>0)out.put(m.group(1),v);
        }
        return out;
    }
    private static int functionBodyStartAt(String source,int offset){
        Pattern p=Pattern.compile("function\\s+[A-Za-z_$][A-Za-z0-9_$]*\\s*\\([^)]*\\)\\s*\\{");
        Matcher m=p.matcher(source);int best=0,bestStart=-1;
        while(m.find()){if(m.start()>offset)break;int open=m.end()-1,close=findMatching(source,open,'{','}');if(close>=offset&&m.start()>bestStart){best=open+1;bestStart=m.start();}}
        return best;
    }
    private static int braceDepthAt(String source,int end){int d=0;for(int i=0;i<end&&i<source.length();i++){char c=source.charAt(i);if(c=='{')d++;else if(c=='}'&&d>0)d--;}return d;}
    private static String evaluateString(String expr, Map<String,String> locals, Map<String,String> globals) {
        String e=expr==null?"":expr.trim(); if(e.length()==0)return "";
        String literal=evaluateLiteralConcat(e,merge(globals,locals)); if(literal.length()>0)return literal;
        String id=e.replaceAll("\\s+",""); String v=locals.get(id); if(v==null)v=globals.get(id); return v==null?"":v;
    }
    private static Map<String,String> merge(Map<String,String>a,Map<String,String>b){Map<String,String>m=new LinkedHashMap<String,String>();m.putAll(a);m.putAll(b);return m;}
    private static String evaluateLiteralConcat(String expr, Map<String,String> constants) {
        List<String> parts=splitTopLevel(expr,'+'); if(parts.isEmpty())return ""; StringBuilder sb=new StringBuilder();
        for(String p:parts){String t=p.trim(); if(isQuoted(t))sb.append(unquote(t)); else {String v=constants.get(t); if(v==null)return ""; sb.append(v);}}
        return sb.toString();
    }

    private static List<String> splitTopLevel(String source,char delimiter){List<String>out=new ArrayList<String>();int start=0,pa=0,br=0,bc=0;char quote=0;boolean esc=false;
        for(int i=0;i<source.length();i++){char c=source.charAt(i);if(quote!=0){if(esc){esc=false;continue;}if(c=='\\'){esc=true;continue;}if(c==quote)quote=0;continue;}if(c=='\''||c=='\"'){quote=c;continue;}if(c=='(')pa++;else if(c==')')pa--;else if(c=='[')br++;else if(c==']')br--;else if(c=='{')bc++;else if(c=='}')bc--;else if(c==delimiter&&pa==0&&br==0&&bc==0){out.add(source.substring(start,i));start=i+1;}}out.add(source.substring(start));return out;}
    private static String[] splitArguments(String s){List<String>p=splitTopLevel(s,',');List<String>o=new ArrayList<String>();for(String v:p)if(v.trim().length()>0)o.add(v.trim());return o.toArray(new String[o.size()]);}
    private static int findReceiverStart(String s,int end){int i=end-1,br=0;while(i>=0){char c=s.charAt(i);if(c==']')br++;else if(c=='['&&br>0)br--;if(br==0&&(c==';'||c=='{'||c=='}'||c=='\n'||c=='\r'||c=='='||c==','||c=='('||c==')'))break;i--;}return i+1;}
    private static int findExpressionEnd(String s,int from){int pa=0,br=0,bc=0;for(int i=from;i<s.length();i++){char c=s.charAt(i);if(c=='(')pa++;else if(c==')'){if(pa==0)return i;pa--;}else if(c=='[')br++;else if(c==']'&&br>0)br--;else if(c=='{')bc++;else if(c=='}'){if(bc==0)return i;bc--;}else if((c==';'||c=='\n'||c=='\r')&&pa==0&&br==0&&bc==0)return i;}return s.length();}
    private static int findMatching(String s,int open,char left,char right){int d=0;for(int i=open;i<s.length();i++){char c=s.charAt(i);if(c==left)d++;else if(c==right){d--;if(d==0)return i;}}return -1;}
    private static int skipWs(String s,int i){while(i<s.length()&&Character.isWhitespace(s.charAt(i)))i++;return i;}
    private static int indexOfToken(String s,String needle,int from){int p=from;while((p=s.indexOf(needle,p))>=0){int after=p+needle.length();if(after>=s.length()||!Character.isJavaIdentifierPart(s.charAt(after)))return p;p=after;}return -1;}
    private static String firstIdentifier(String s){int i=0;if(i<s.length()&&Character.isJavaIdentifierStart(s.charAt(i))){i++;while(i<s.length()&&Character.isJavaIdentifierPart(s.charAt(i)))i++;return s.substring(0,i);}return "";}
    private static String functionAt(String source,int offset){Pattern p=Pattern.compile("function\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\([^)]*\\)\\s*\\{");Matcher m=p.matcher(source);String best="";int bestStart=-1;while(m.find()){if(m.start()>offset)break;int close=findMatching(source,m.end()-1,'{','}');if(close>=offset&&m.start()>bestStart){best=m.group(1);bestStart=m.start();}}return best;}
    private static int lineOf(String s,int off){int line=1;for(int i=0;i<off&&i<s.length();i++)if(s.charAt(i)=='\n')line++;return line;}
    private static int receiverExpressionOffset(String raw){if(raw==null)return 0;int i=0;while(i<raw.length()&&Character.isWhitespace(raw.charAt(i)))i++;boolean again=true;while(again){again=false;String[] words={"else","return"};for(String w:words){if(raw.regionMatches(i,w,0,w.length())){int e=i+w.length();if(e==raw.length()||Character.isWhitespace(raw.charAt(e))){i=e;while(i<raw.length()&&Character.isWhitespace(raw.charAt(i)))i++;again=true;break;}}}}return i;}
    private static String compact(String s){return s==null?"":s.replaceAll("\\s+"," ").trim();}
    private static boolean isQuoted(String s){return s.length()>=2&&((s.charAt(0)=='\"'&&s.charAt(s.length()-1)=='\"')||(s.charAt(0)=='\''&&s.charAt(s.length()-1)=='\''));}
    private static String stripQuoted(String s){return isQuoted(s)?unquote(s):s;}
    private static String unquote(String s){if(!isQuoted(s))return "";String body=s.substring(1,s.length()-1);return body.replace("\\\\","\\").replace("\\\"","\"").replace("\\'","'");}
}
