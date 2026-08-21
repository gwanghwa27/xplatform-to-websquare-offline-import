package com.example.xfdltracker.tab;

import com.example.xfdltracker.script.ScriptSymbolTable;
import com.example.xfdltracker.util.JavaScriptCleaner;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Separately inventories XPlatform getOwnerFrame()/opener references.
 * Parent-form references stay in CrossScreenReferenceAnalyzer because their Tab ancestry is known.
 */
public class ScopeBridgeReferenceAnalyzer {
    private static final Pattern OWNER_FORM_SYMBOL = Pattern.compile(
            "(?<![A-Za-z0-9_$])(?:this\\s*\\.\\s*)?"
          + "((?:getOwnerFrame\\s*\\(\\s*\\)\\s*\\.\\s*)+)"
          + "form\\s*\\.\\s*([A-Za-z_$][A-Za-z0-9_$]*)");
    private static final Pattern OWNER_ARGUMENTS = Pattern.compile(
            "(?<![A-Za-z0-9_$])(?:this\\s*\\.\\s*)?"
          + "((?:getOwnerFrame\\s*\\(\\s*\\)\\s*\\.\\s*)+)arguments\\b");
    private static final Pattern OPENER_SYMBOL = Pattern.compile(
            "(?<![A-Za-z0-9_$])(?:this\\s*\\.\\s*)?opener\\s*\\.\\s*([A-Za-z_$][A-Za-z0-9_$]*)");

    public void analyze(Map<String,String> scripts,
                        Map<String,TabRuntimePlan> runtimePlans,
                        Map<String,ScreenSymbolCatalog> catalogs) {
        if (scripts == null || runtimePlans == null) return;
        for (Map.Entry<String,String> entry : scripts.entrySet()) {
            String screen = entry.getKey();
            String script = entry.getValue();
            TabRuntimePlan plan = runtimePlans.get(screen);
            if (plan == null || script == null || script.length() == 0) continue;
            ScreenSymbolCatalog ownCatalog = catalogs == null ? null : catalogs.get(screen);
            scanOwner(screen, script, plan, ownCatalog);
            scanOpener(screen, script, plan);
        }
    }

    private void scanOwner(String screen, String script, TabRuntimePlan plan, ScreenSymbolCatalog ownCatalog) {
        String cleaned = new JavaScriptCleaner().clean(script);
        ScriptSymbolTable symbols = ScriptSymbolTable.build(script,
                Collections.<String>emptySet(), Collections.<String>emptySet(),
                Collections.<String>emptySet(), Collections.<String>emptySet());

        Matcher m = OWNER_FORM_SYMBOL.matcher(cleaned);
        while (m.find()) {
            if (isBareOwnerShadowed(cleaned, symbols, m.start())) continue;
            int depth = countOwnerCalls(m.group(1)) - 1;
            String symbol = m.group(2);
            CrossScreenReference.SymbolType type = depth == 0 && ownCatalog != null
                    ? ownCatalog.typeOf(symbol) : CrossScreenReference.SymbolType.UNKNOWN;
            ScopeBridgeReference.Status status;
            String message;
            if (depth == 0 && type != CrossScreenReference.SymbolType.UNKNOWN) {
                status = ScopeBridgeReference.Status.RESOLVED;
                message = "getOwnerFrame().form은 현재 Form의 owner WFrame scope로 변환";
            } else if (depth == 0) {
                status = ScopeBridgeReference.Status.UNRESOLVED;
                message = "현재 Form symbol을 찾지 못함";
            } else {
                status = ScopeBridgeReference.Status.RUNTIME_VERIFY_REQUIRED;
                message = "다중 getOwnerFrame() chain은 Frame tree 문맥이 필요하여 runtime owner resolver 사용";
            }
            plan.addScopeBridgeReference(new ScopeBridgeReference(
                    screen, functionAt(cleaned, m.start()), ScopeBridgeReference.Kind.OWNER_FRAME, depth,
                    depth == 0 ? screen : "", symbol, type, status,
                    lineOf(script, m.start()), line(script, m.start()), message));
        }

        Matcher a = OWNER_ARGUMENTS.matcher(cleaned);
        while (a.find()) {
            if (isBareOwnerShadowed(cleaned, symbols, a.start())) continue;
            int depth = countOwnerCalls(a.group(1)) - 1;
            plan.addScopeBridgeReference(new ScopeBridgeReference(
                    screen, functionAt(cleaned, a.start()), ScopeBridgeReference.Kind.OWNER_FRAME, depth,
                    depth == 0 ? screen : "", "arguments", CrossScreenReference.SymbolType.UNKNOWN,
                    ScopeBridgeReference.Status.RUNTIME_VERIFY_REQUIRED,
                    lineOf(script, a.start()), line(script, a.start()),
                    "Owner Frame arguments는 WFrame parameter/dataObject bridge로 변환하며 실제 argument reference semantics는 runtime 확인 필요"));
        }
    }

    private void scanOpener(String screen, String script, TabRuntimePlan plan) {
        String cleaned = new JavaScriptCleaner().clean(script);
        ScriptSymbolTable symbols = ScriptSymbolTable.build(script,
                Collections.<String>emptySet(), Collections.<String>emptySet(),
                Collections.<String>emptySet(), Collections.<String>emptySet());
        Matcher m = OPENER_SYMBOL.matcher(cleaned);
        while (m.find()) {
            if (symbols.isJavaScriptVariable("opener", m.start())) continue;
            String symbol = m.group(1);
            plan.addScopeBridgeReference(new ScopeBridgeReference(
                    screen, functionAt(cleaned, m.start()), ScopeBridgeReference.Kind.POPUP_OPENER, 0,
                    "", symbol, CrossScreenReference.SymbolType.UNKNOWN,
                    ScopeBridgeReference.Status.RUNTIME_VERIFY_REQUIRED,
                    lineOf(script, m.start()), line(script, m.start()),
                    "Popup opener는 Tab parent와 별도 scope; 명시적 opener scope/window가 있을 때 runtime bridge 사용"));
        }
    }

    private static boolean isBareOwnerShadowed(String cleaned, ScriptSymbolTable symbols, int pos) {
        int p = pos;
        while (p < cleaned.length() && Character.isWhitespace(cleaned.charAt(p))) p++;
        boolean explicitThis = cleaned.startsWith("this", p);
        return !explicitThis && symbols.resolve("getOwnerFrame", pos) != ScriptSymbolTable.Kind.UNRESOLVED;
    }
    private static int countOwnerCalls(String v){Matcher m=Pattern.compile("getOwnerFrame").matcher(v);int n=0;while(m.find())n++;return n;}
    private static int lineOf(String s,int pos){int n=1;for(int i=0;i<pos&&i<s.length();i++)if(s.charAt(i)=='\n')n++;return n;}
    private static String line(String s,int pos){int a=s.lastIndexOf('\n',Math.max(0,pos-1));a=a<0?0:a+1;int b=s.indexOf('\n',pos);if(b<0)b=s.length();return s.substring(a,b).replaceAll("\\s+"," ").trim();}
    private static String functionAt(String s,int pos){Pattern p=Pattern.compile("function\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\([^)]*\\)\\s*\\{");Matcher m=p.matcher(s);String best="";int bs=-1;while(m.find()){if(m.start()>pos)break;int close=closing(s,m.end()-1);if(close>=pos&&m.start()>bs){best=m.group(1);bs=m.start();}}return best;}
    private static int closing(String s,int o){int d=0;for(int i=o;i<s.length();i++){if(s.charAt(i)=='{')d++;else if(s.charAt(i)=='}'&&--d==0)return i;}return -1;}
}
