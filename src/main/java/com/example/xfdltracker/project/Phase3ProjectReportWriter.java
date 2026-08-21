package com.example.xfdltracker.project;

import com.example.xfdltracker.io.TextFileUtil;
import com.example.xfdltracker.xjs.XjsModule;
import com.example.xfdltracker.xjs.XjsRepository;
import com.example.xfdltracker.xjs.XjsResolution;
import com.example.xfdltracker.xjs.XjsSymbol;
import com.example.xfdltracker.transaction.TransactionCall;
import com.example.xfdltracker.tab.TabContentPlan;
import com.example.xfdltracker.tab.TabContentReference;
import com.example.xfdltracker.tab.TabRuntimePlan;
import com.example.xfdltracker.tab.TabOperation;
import com.example.xfdltracker.tab.CrossScreenReference;
import com.example.xfdltracker.tab.ScopeBridgeReference;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Additional Phase 3 project-level dependency and unresolved-symbol reports. */
public class Phase3ProjectReportWriter {
    public void write(
            File reportDir,
            XjsRepository repository,
            List<XjsResolution> resolutions,
            List<SourceAnalysis> analyses,
            List<ConversionRecord> records,
            List<Phase3ScreenReport> screens,
            List<TabContentPlan> tabPlans,
            List<TabRuntimePlan> runtimePlans) throws Exception {
        if (!reportDir.exists() && !reportDir.mkdirs() && !reportDir.isDirectory()) {
            throw new IllegalStateException("리포트 디렉터리를 생성할 수 없습니다: " + reportDir);
        }
        TextFileUtil.writeUtf8(new File(reportDir, "phase3-project-summary.md"), summary(repository, resolutions, records, tabPlans));
        TextFileUtil.writeUtf8(new File(reportDir, "xjs-dependency.md"), xjsReport(repository, resolutions));
        TextFileUtil.writeUtf8(new File(reportDir, "unresolved-symbols.md"), unresolved(resolutions));
        TextFileUtil.writeUtf8(new File(reportDir, "xjs-dependency.csv"), xjsCsv(resolutions));
        TextFileUtil.writeUtf8(new File(reportDir, "screen-conversion-report.md"), screenReport(screens));
        TextFileUtil.writeUtf8(new File(reportDir, "tab-content-dependency.md"), tabContentReport(tabPlans, resolutions, records));
        TextFileUtil.writeUtf8(new File(reportDir, "tab-content-dependency.csv"), tabContentCsv(tabPlans, resolutions, records));
        TextFileUtil.writeUtf8(new File(reportDir, "tab-content-dependency-graph.md"), tabContentGraph(tabPlans));
        TextFileUtil.writeUtf8(new File(reportDir, "tab-content-unresolved.md"), tabContentUnresolved(tabPlans));
        TextFileUtil.writeUtf8(new File(reportDir, "tab-runtime-operations.md"), tabRuntimeOperations(runtimePlans, resolutions));
        TextFileUtil.writeUtf8(new File(reportDir, "tab-runtime-operations.csv"), tabRuntimeOperationsCsv(runtimePlans));
        TextFileUtil.writeUtf8(new File(reportDir, "tab-dynamic-content-report.md"), tabDynamicContent(runtimePlans, resolutions));
        TextFileUtil.writeUtf8(new File(reportDir, "tab-cross-screen-reference.md"), tabCrossScreen(runtimePlans));
        TextFileUtil.writeUtf8(new File(reportDir, "tab-cross-screen-reference.csv"), tabCrossScreenCsv(runtimePlans));
        TextFileUtil.writeUtf8(new File(reportDir, "tab-owner-opener-reference.md"), tabScopeBridge(runtimePlans));
        TextFileUtil.writeUtf8(new File(reportDir, "tab-owner-opener-reference.csv"), tabScopeBridgeCsv(runtimePlans));
        TextFileUtil.writeUtf8(new File(reportDir, "tab-lifecycle-report.md"), tabLifecycle(runtimePlans, tabPlans));
        TextFileUtil.writeUtf8(new File(reportDir, "tab-instance-model.md"), tabInstanceModel(runtimePlans));
        TextFileUtil.writeUtf8(new File(reportDir, "runtime-async-model.md"), runtimeAsyncModel(runtimePlans));
        TextFileUtil.writeUtf8(new File(reportDir, "runtime-state-model.md"), runtimeStateModel(runtimePlans));
        TextFileUtil.writeUtf8(new File(reportDir, "runtime-verification-required.md"), runtimeVerificationRequired(runtimePlans));
        TextFileUtil.writeUtf8(new File(reportDir, "unsupported-features.md"), unsupportedReport(screens));
        TextFileUtil.writeUtf8(new File(reportDir, "transaction-report.md"), transactionReport(screens));
        TextFileUtil.writeUtf8(new File(reportDir, "transaction-report.csv"), transactionCsv(screens));
        TextFileUtil.writeUtf8(new File(reportDir, "phase3-status.csv"), statusCsv(repository, records, screens));
        TextFileUtil.writeUtf8(new File(reportDir, "severity-report.md"), severityReport(records, screens, resolutions, tabPlans));
        TextFileUtil.writeUtf8(new File(reportDir, "severity-report.csv"), severityCsv(records, screens, resolutions, tabPlans));
    }

    private String summary(XjsRepository repository, List<XjsResolution> resolutions, List<ConversionRecord> records, List<TabContentPlan> tabPlans) {
        int success = 0, fail = 0, partial = 0;
        for (ConversionRecord r : records) {
            if ("SUCCESS".equals(r.getStatus())) success++; else fail++;
        }
        for (XjsResolution r : resolutions) {
            if (!r.getUnresolvedFunctions().isEmpty() || !r.getAmbiguousSymbols().isEmpty()
                    || !r.getIncludeWarnings().isEmpty()) partial++;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("# Phase 3 프로젝트 변환 요약\n\n");
        sb.append("- 변환 성공 파일: ").append(success).append('\n');
        sb.append("- 변환 실패 파일: ").append(fail).append('\n');
        sb.append("- XFDL 화면: ").append(resolutions.size()).append('\n');
        sb.append("- XJS 모듈: ").append(repository.allModules().size()).append('\n');
        sb.append("- XJS 미해결/충돌이 있는 부분 성공 화면: ").append(partial).append('\n');
        int tabRefs = 0, tabResolved = 0, tabUnresolved = 0, dynamicTabs = 0;
        if (tabPlans != null) for (TabContentPlan plan : tabPlans) {
            tabRefs += plan.getReferences().size();
            dynamicTabs += plan.getDynamicUsages().size();
            for (TabContentReference ref : plan.getReferences()) {
                if (ref.isResolved()) tabResolved++; else tabUnresolved++;
            }
        }
        sb.append("- Tab 외부 화면 참조: total=").append(tabRefs).append(", resolved=").append(tabResolved)
          .append(", unresolved=").append(tabUnresolved).append('\n');
        sb.append("- Tab 동적 content/API 검토 항목: ").append(dynamicTabs).append('\n');
        sb.append("\n미해결 XJS symbol은 빈 함수를 만들거나 임의 파일을 선택하지 않습니다. 상세 내용은 xjs-dependency.md와 unresolved-symbols.md를 확인하세요.\n");
        sb.append("Tab 외부 XFDL 관계는 tab-content-dependency.md/csv/graph.md에서 확인할 수 있습니다.\n");
        return sb.toString();
    }

    private String xjsReport(XjsRepository repository, List<XjsResolution> resolutions) {
        StringBuilder sb = new StringBuilder("# Phase 3 XJS Dependency Report\n\n");
        sb.append("## Repository\n\n");
        List<XjsModule> modules = new ArrayList<XjsModule>(repository.allModules());
        Collections.sort(modules, new Comparator<XjsModule>() {
            public int compare(XjsModule a, XjsModule b) { return a.getRelativePath().compareToIgnoreCase(b.getRelativePath()); }
        });
        for (XjsModule module : modules) {
            sb.append("- `").append(module.getRelativePath()).append("`: functions=")
              .append(module.getFunctions().size()).append(", globals=").append(module.getGlobals().size())
              .append(", includes=").append(module.getIncludes())
              .append(", topLevelInit=").append(module.getTopLevelExecutableStatements().size()).append('\n');
        }
        for (XjsResolution r : resolutions) {
            sb.append("\n## Screen: `").append(r.getScreenRelativePath()).append("`\n\n");
            sb.append("- Referenced XJS: ").append(r.getReferencedModules()).append('\n');
            sb.append("- Imported functions: ").append(r.getImportedFunctions()).append('\n');
            sb.append("- Imported globals: ").append(r.getImportedGlobals()).append('\n');
            sb.append("- Unresolved: ").append(r.getUnresolvedFunctions()).append('\n');
            sb.append("- Ambiguous: ").append(r.getAmbiguousSymbols()).append('\n');
            sb.append("- Include warnings: ").append(r.getIncludeWarnings()).append('\n');
            sb.append("\n### Dependency chain\n\n");
            if (r.getDependencyEdges().isEmpty()) sb.append("- 없음\n");
            else for (String edge : r.getDependencyEdges()) sb.append("- ").append(edge).append('\n');
            sb.append("\n### Selected symbols\n\n");
            for (XjsSymbol symbol : r.getSelectedSymbols()) {
                sb.append("- ").append(symbol.getType()).append(' ').append(symbol.getName())
                  .append(" — `").append(symbol.getRelativePath()).append(':').append(symbol.getLine()).append("`\n");
            }
            sb.append("\n### Unused functions in referenced XJS\n\n");
            boolean unusedAny = false;
            for (String modulePath : r.getReferencedModules()) {
                XjsModule module = repository.getModule(modulePath);
                if (module == null) continue;
                for (String functionName : module.getFunctions().keySet()) {
                    if (!r.getImportedFunctions().contains(functionName)) {
                        unusedAny = true;
                        sb.append("- `").append(module.getRelativePath()).append("`: ").append(functionName).append('\n');
                    }
                }
            }
            if (!unusedAny) sb.append("- 없음\n");
        }
        return sb.toString();
    }

    private String unresolved(List<XjsResolution> resolutions) {
        StringBuilder sb = new StringBuilder("# Phase 3 Unresolved Symbols\n\n");
        boolean any = false;
        for (XjsResolution r : resolutions) {
            if (r.getUnresolvedFunctions().isEmpty() && r.getAmbiguousSymbols().isEmpty()
                    && r.getIncludeWarnings().isEmpty()) continue;
            any = true;
            sb.append("## `").append(r.getScreenRelativePath()).append("`\n\n");
            for (String fn : r.getUnresolvedFunctions()) sb.append("- [UNRESOLVED FUNCTION] `").append(fn).append("`\n");
            for (String a : r.getAmbiguousSymbols()) sb.append("- [AMBIGUOUS XJS SYMBOL] ").append(a).append('\n');
            for (String w : r.getIncludeWarnings()) sb.append("- [XJS INCLUDE TODO] ").append(w).append('\n');
            sb.append('\n');
        }
        if (!any) sb.append("미해결 XJS symbol 없음.\n");
        return sb.toString();
    }

    private String xjsCsv(List<XjsResolution> resolutions) {
        StringBuilder sb = new StringBuilder("screen,kind,name,detail\n");
        for (XjsResolution r : resolutions) {
            for (String v : r.getReferencedModules()) row(sb, r.getScreenRelativePath(), "MODULE", v, "resolved");
            for (String v : r.getImportedFunctions()) row(sb, r.getScreenRelativePath(), "FUNCTION", v, "imported");
            for (String v : r.getImportedGlobals()) row(sb, r.getScreenRelativePath(), "GLOBAL", v, "imported");
            for (String v : r.getUnresolvedFunctions()) row(sb, r.getScreenRelativePath(), "UNRESOLVED_FUNCTION", v, "");
            for (String v : r.getAmbiguousSymbols()) row(sb, r.getScreenRelativePath(), "AMBIGUOUS", v, "");
        }
        return sb.toString();
    }

    private String screenReport(List<Phase3ScreenReport> screens) {
        StringBuilder sb = new StringBuilder("# Phase 3 Screen Conversion Report\n\n");
        for (Phase3ScreenReport r : screens) {
            sb.append("## `").append(r.getScreen()).append("`\n\n");
            sb.append("- Components: total=").append(r.getComponentsTotal()).append(", supported=")
              .append(r.getComponentsSupported()).append(", partial=").append(r.getComponentsPartial())
              .append(", TODO=").append(r.getComponentsTodo()).append('\n');
            sb.append("- Properties: mapped=").append(r.getPropertiesMapped()).append(", TODO=").append(r.getPropertiesTodo()).append('\n');
            sb.append("- Events: total=").append(r.getEventsTotal()).append(", mapped=").append(r.getEventsMapped())
              .append(", TODO=").append(r.getEventsTodo()).append('\n');
            sb.append("- Datasets: ").append(r.getDatasets()).append('\n');
            sb.append("- Scripts: internal=").append(r.getInternalFunctions()).append(", external functions=")
              .append(r.getExternalFunctions()).append(", external globals=").append(r.getExternalGlobals())
              .append(", unresolved/ambiguous=").append(r.getUnresolvedFunctions()).append('\n');
            sb.append("- Transactions: ").append(r.getTransactions().size()).append('\n');
            if (!r.getApiCandidates().isEmpty()) {
                sb.append("- API review candidates:\n"); for (String v : r.getApiCandidates()) sb.append("  - ").append(v).append('\n');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String unsupportedReport(List<Phase3ScreenReport> screens) {
        StringBuilder sb = new StringBuilder("# Phase 3 Unsupported / TODO Features\n\n"); boolean any=false;
        for (Phase3ScreenReport r : screens) {
            if (r.getUnsupportedFeatures().isEmpty() && r.getApiCandidates().isEmpty()) continue;
            any=true; sb.append("## `").append(r.getScreen()).append("`\n\n");
            for (String v : r.getUnsupportedFeatures()) sb.append("- ").append(v).append('\n');
            for (String v : r.getApiCandidates()) sb.append("- API: ").append(v).append('\n');
            sb.append('\n');
        }
        if(!any) sb.append("현재 fixture 기준 미지원/TODO 항목 없음.\n"); return sb.toString();
    }


    private String tabRuntimeOperations(List<TabRuntimePlan> plans, List<XjsResolution> resolutions) {
        StringBuilder sb=new StringBuilder("# Phase 3 Tab Runtime Operations\n\n"); boolean any=false;
        if(plans!=null)for(TabRuntimePlan p:plans){
            for(TabOperation o:p.getOperations()){any=true;
                XjsResolution child=findResolution(resolutions,o.getResolvedSource());
                sb.append("## `").append(p.getScreen()).append("` line ").append(o.getLine()).append("\n\n");
                sb.append("- Function: `").append(o.getFunctionName()).append("`\n");
                sb.append("- Operation: `").append(o.getType()).append("`\n");
                sb.append("- Tab: `").append(o.getTabPath()).append("`\n");
                sb.append("- TabPage: `").append(o.getPageId()).append("`\n");
                sb.append("- Source Expression: `").append(o.getUrlExpression()).append("`\n");
                sb.append("- Resolved Source: `").append(o.getResolvedSource()).append("`\n");
                sb.append("- Generated Target: `").append(o.getGeneratedTarget()).append("`\n");
                sb.append("- WebSquare src: `").append(o.getWebSquareSrc()).append("`\n");
                sb.append("- Loading Policy: `").append(p.isTabEager(o.getTabPath())?"EAGER":"LAZY").append("`\n");
                sb.append("- Runtime Conversion: `").append(o.getStatus()).append("`\n");
                if(o.getType()==TabOperation.Type.SET_URL) sb.append("- State Policy: `REPLACE_INSTANCE`\n");
                if(child!=null) sb.append("- Child XJS Dependencies: ").append(child.getReferencedModules()).append("\n");
                if(o.getMessage().length()>0)sb.append("- Message: ").append(o.getMessage()).append("\n");
                sb.append('\n');
            }
            for(String warning:p.getWarnings()){any=true;sb.append("- [WARNING] `").append(p.getScreen()).append("`: ").append(warning).append("\n");}
        }
        if(!any)sb.append("동적 Tab operation 없음.\n");return sb.toString();
    }

    private String tabRuntimeOperationsCsv(List<TabRuntimePlan> plans){
        StringBuilder sb=new StringBuilder("screen,function,line,operation,tab,tabPage,expression,resolvedSource,target,webSquareSrc,loading,status,message\n");
        if(plans!=null)for(TabRuntimePlan p:plans)for(TabOperation o:p.getOperations())
            row(sb,p.getScreen(),o.getFunctionName(),String.valueOf(o.getLine()),o.getType().name(),o.getTabPath(),o.getPageId(),o.getUrlExpression(),o.getResolvedSource(),o.getGeneratedTarget(),o.getWebSquareSrc(),p.isTabEager(o.getTabPath())?"EAGER":"LAZY",o.getStatus().name(),o.getMessage());
        return sb.toString();
    }

    private String tabDynamicContent(List<TabRuntimePlan> plans,List<XjsResolution> resolutions){
        StringBuilder sb=new StringBuilder("# Phase 3 Tab Dynamic Content\n\n");boolean any=false;
        if(plans!=null)for(TabRuntimePlan p:plans)for(TabOperation o:p.getOperations())if(o.getType()==TabOperation.Type.SET_URL){any=true;XjsResolution child=findResolution(resolutions,o.getResolvedSource());
            sb.append("## `").append(p.getScreen()).append(':').append(o.getLine()).append("`\n\n")
              .append("- Function: `").append(o.getFunctionName()).append("`\n")
              .append("- Expression: `").append(o.getUrlExpression()).append("`\n")
              .append("- Status: `").append(o.getStatus()).append("`\n")
              .append("- Resolved Source: `").append(o.getResolvedSource()).append("`\n")
              .append("- Generated Target: `").append(o.getGeneratedTarget()).append("`\n")
              .append("- Runtime target: `").append(o.getWebSquareSrc()).append("`\n")
              .append("- Child XJS: ").append(child==null?"[]":child.getReferencedModules()).append("\n\n");}
        if(!any)sb.append("동적 SET_URL 없음.\n");return sb.toString();
    }

    private String tabCrossScreen(List<TabRuntimePlan> plans){
        StringBuilder sb=new StringBuilder("# Phase 3 Tab Cross-Screen Reference\n\n");boolean any=false;
        if(plans!=null)for(TabRuntimePlan p:plans)for(CrossScreenReference r:p.getCrossScreenReferences()){any=true;
            sb.append("## `").append(r.getSourceScreen()).append(':').append(r.getLine()).append("`\n\n")
              .append("- Function: `").append(r.getSourceFunction()).append("`\n")
              .append("- Direction: `").append(r.getDirection()).append("`\n")
              .append("- Target Screen: `").append(r.getTargetScreen()).append("`\n")
              .append("- Tab / Page: `").append(r.getTabPath()).append("` / `").append(r.getPageId()).append("`\n")
              .append("- Target Symbol: `").append(r.getTargetSymbol()).append("`\n")
              .append("- Symbol Type: `").append(r.getSymbolType()).append("`\n")
              .append("- Status: `").append(r.getStatus()).append("`\n");
            if(r.getMessage().length()>0)sb.append("- Message: ").append(r.getMessage()).append("\n");
            sb.append('\n');}
        if(!any)sb.append("분석된 cross-screen reference 없음.\n");return sb.toString();
    }

    private String tabCrossScreenCsv(List<TabRuntimePlan> plans){
        StringBuilder sb=new StringBuilder("source,function,direction,target,tab,page,symbol,type,status,line,parentDepth,message\n");
        if(plans!=null)for(TabRuntimePlan p:plans)for(CrossScreenReference r:p.getCrossScreenReferences())
            row(sb,r.getSourceScreen(),r.getSourceFunction(),r.getDirection().name(),r.getTargetScreen(),r.getTabPath(),r.getPageId(),r.getTargetSymbol(),r.getSymbolType().name(),r.getStatus().name(),String.valueOf(r.getLine()),String.valueOf(r.getParentDepth()),r.getMessage());
        return sb.toString();
    }

    private String tabScopeBridge(List<TabRuntimePlan> plans){
        StringBuilder sb=new StringBuilder("# Phase 3 Parent / Owner Frame / Opener Reference\n\n");boolean any=false;
        if(plans!=null)for(TabRuntimePlan p:plans)for(ScopeBridgeReference r:p.getScopeBridgeReferences()){any=true;
            sb.append("## `").append(r.getSourceScreen()).append(':').append(r.getLine()).append("`\n\n")
              .append("- Function: `").append(r.getSourceFunction()).append("`\n")
              .append("- Kind: `").append(r.getKind()).append("`\n")
              .append("- Depth: `").append(r.getDepth()).append("`\n")
              .append("- Target Screen: `").append(r.getTargetScreen()).append("`\n")
              .append("- Target Symbol: `").append(r.getTargetSymbol()).append("`\n")
              .append("- Symbol Type: `").append(r.getSymbolType()).append("`\n")
              .append("- Status: `").append(r.getStatus()).append("`\n")
              .append("- Expression: `").append(r.getSourceText().replace("`","'")).append("`\n")
              .append("- Message: ").append(r.getMessage()).append("\n\n");}
        if(!any)sb.append("Owner Frame / opener reference 없음.\n");return sb.toString();
    }

    private String tabScopeBridgeCsv(List<TabRuntimePlan> plans){
        StringBuilder sb=new StringBuilder("screen,function,kind,depth,targetScreen,targetSymbol,symbolType,status,line,expression,message\n");
        if(plans!=null)for(TabRuntimePlan p:plans)for(ScopeBridgeReference r:p.getScopeBridgeReferences())
            row(sb,r.getSourceScreen(),r.getSourceFunction(),r.getKind().name(),String.valueOf(r.getDepth()),r.getTargetScreen(),r.getTargetSymbol(),r.getSymbolType().name(),r.getStatus().name(),String.valueOf(r.getLine()),r.getSourceText(),r.getMessage());
        return sb.toString();
    }

    private String runtimeAsyncModel(List<TabRuntimePlan> plans){
        int operations=0,lazyRefs=0; if(plans!=null)for(TabRuntimePlan p:plans){operations+=p.getOperations().size();for(CrossScreenReference r:p.getCrossScreenReferences())if(r.getStatus()==CrossScreenReference.Status.RUNTIME_VERIFY_REQUIRED)lazyRefs++;}
        return "# Phase 3 Runtime Async Model\n\n"
          + "- Queue scope: TabControl + logical TabPage key.\n"
          + "- READY gate: child `scwin.__xpRuntimePageReady === true`; WFrame existence alone is not READY.\n"
          + "- `setSrc`/`addTab`/`deleteTab`/`activateTab` return values are checked at runtime; thenables are chained, synchronous returns remain synchronous where safe.\n"
          + "- Loaded child function calls preserve synchronous return when no pending page operation exists.\n"
          + "- Lazy child function calls may return a Promise after activation/READY wait; synchronous Component/DataList reads before READY raise `UNSUPPORTED_SYNC_SEMANTIC`.\n"
          + "- Generation/state checks ignore callbacks from removed/replaced instances.\n"
          + "- Runtime operations analyzed: "+operations+"; runtime-verification cross-screen refs: "+lazyRefs+".\n";
    }

    private String runtimeStateModel(List<TabRuntimePlan> plans){
        return "# Phase 3 Runtime State Model\n\n"
          + "States: `NOT_CREATED → CREATED → LOADING → LOADED → READY`; replacement uses `REPLACING`, removal uses `DESTROYING → DESTROYED`, failures use `ERROR`.\n\n"
          + "Each logical page tracks runtime ID, source/target, generation, arguments, selected flag, last operation, pending operation count and error. "
          + "A URL replacement changes generation; a plain Tab switch does not replace the child instance. Remove clears instance/dynamic ID/argument registries while generation counters remain monotonic to reject stale callbacks.\n";
    }

    private String runtimeVerificationRequired(List<TabRuntimePlan> plans){
        StringBuilder sb=new StringBuilder("# Phase 3 Runtime Verification Required\n\n");boolean any=false;
        if(plans!=null)for(TabRuntimePlan p:plans){for(CrossScreenReference r:p.getCrossScreenReferences())if(r.getStatus()==CrossScreenReference.Status.RUNTIME_VERIFY_REQUIRED){any=true;sb.append("- [REAL_RUNTIME_REQUIRED] `").append(r.getSourceScreen()).append(':').append(r.getLine()).append("` ").append(r.getDirection()).append(' ').append(r.getTargetSymbol()).append(" — ").append(r.getMessage()).append('\n');}for(ScopeBridgeReference r:p.getScopeBridgeReferences())if(r.getStatus()==ScopeBridgeReference.Status.RUNTIME_VERIFY_REQUIRED){any=true;sb.append("- [REAL_RUNTIME_REQUIRED] `").append(r.getSourceScreen()).append(':').append(r.getLine()).append("` ").append(r.getKind()).append(" depth=").append(r.getDepth()).append(" — ").append(r.getMessage()).append('\n');}}
        if(!any)sb.append("- 없음\n");return sb.toString();
    }

    private String tabLifecycle(List<TabRuntimePlan> runtime,List<TabContentPlan> statics){
        StringBuilder sb=new StringBuilder("# Phase 3 Tab Lifecycle Report\n\n");
        sb.append("- Static LAZY: `alwaysDraw=false`; child lifecycle begins when its Content is rendered/activated.\n");
        sb.append("- Static EAGER: `alwaysDraw=true`; exact parent/child onload ordering remains REAL_RUNTIME_REQUIRED.\n");
        sb.append("- WFrame `onwframeload` means rendering completed, but cross-screen READY additionally requires generated child `__xpRuntimePageReady=true`.\n");
        sb.append("- Generated child wrapper marks READY only after converted Form onload succeeds; onload throw/reject stores `__xpRuntimePageLoadError` and remains non-READY.\n");
        sb.append("- Loaded set_url: WFrame `setSrc(src, dataObject options)` and a new generation; Tab switch alone never calls setSrc.\n");
        sb.append("- Lazy child call: `activateTab` then READY wait. If the caller needs an immediate synchronous value before creation, semantic equivalence is not claimed.\n\n");
        if(runtime!=null)for(TabRuntimePlan p:runtime){for(String w:p.getWarnings())sb.append("- [WARNING] `").append(p.getScreen()).append("`: ").append(w).append("\n");}
        return sb.toString();
    }

    private String tabInstanceModel(List<TabRuntimePlan> plans){return "# Phase 3 Tab Instance Model\n\n- Converted XML file is shared; each Tab Content/WFrame remains an independent runtime instance.\n- Per logical page state contains generation/state/target/arguments/pending operation metadata.\n- Page-scoped queues preserve dependent operation order without globally serializing unrelated Tabs.\n- URL replacement increments generation and resets child lifecycle; plain selection retains the loaded instance and its Component/DataList/local state.\n- removeTabpage destroys the registry entry and dynamic ID/argument references; the monotonic generation counter is retained only to reject stale callbacks after ID reuse.\n- Same XFDL can therefore be loaded by multiple scoped WFrames without sharing instance state.\n";}

    private String tabContentReport(List<TabContentPlan> plans, List<XjsResolution> resolutions,
                                    List<ConversionRecord> records) {
        StringBuilder sb = new StringBuilder("# Phase 3 Tab External Content Dependency\n\n");
        boolean any = false;
        if (plans != null) for (TabContentPlan plan : plans) {
            for (TabContentReference ref : plan.getReferences()) {
                any = true;
                XjsResolution childXjs = findResolution(resolutions, ref.getResolvedSource());
                TabContentPlan childPlan = findTabPlan(plans, ref.getResolvedSource());
                String status = tabConversionStatus(ref, records);
                sb.append("## `").append(plan.getScreenRelativePath()).append("` → `")
                  .append(ref.getTabPagePath()).append("`\n\n");
                sb.append("- Parent Screen: `").append(plan.getScreenRelativePath()).append("`\n");
                sb.append("- Tab: `").append(ref.getTabPath()).append("`\n");
                sb.append("- TabPage: `").append(ref.getTabPagePath()).append("`\n");
                sb.append("- Content: `").append(ref.getRawReference()).append("`\n");
                sb.append("- Source Attribute: `").append(ref.getSourceAttribute()).append("`\n");
                sb.append("- Resolved File: `").append(ref.getResolvedSource()).append("`\n");
                sb.append("- Generated Target: `").append(ref.getGeneratedTarget()).append("`\n");
                sb.append("- WebSquare Content src: `").append(ref.getWebSquareSrc()).append("`\n");
                sb.append("- Resolution: `").append(ref.getResolutionMethod()).append("`\n");
                sb.append("- Loading Mode: `").append(ref.getLoadingMode()).append("`\n");
                sb.append("- Runtime Scope: `independent wframe scope`\n");
                sb.append("- Status: `").append(status).append("`\n");
                sb.append("- XJS Dependencies: ").append(childXjs == null ? "[]" : childXjs.getReferencedModules()).append('\n');
                if (childPlan != null && !childPlan.getParentChildUsages().isEmpty())
                    sb.append("- Child Parent/Owner Access Review: ").append(childPlan.getParentChildUsages()).append('\n');
                if (ref.isMixedInlineExternal()) sb.append("- Warnings: mixed inline/external; external page wins, inline children are not flattened\n");
                else if (ref.getMessage().length() > 0) sb.append("- Note: ").append(ref.getMessage()).append('\n');
                else sb.append("- Warnings: none\n");
                sb.append('\n');
            }
            if (!plan.getDynamicUsages().isEmpty()) {
                any = true;
                sb.append("## Dynamic Tab Review: `").append(plan.getScreenRelativePath()).append("`\n\n");
                for (String value : plan.getDynamicUsages()) sb.append("- [TAB TODO] ").append(value).append('\n');
                sb.append('\n');
            }
        }
        appendSharedTabContentSummary(sb, plans);
        if (!any) sb.append("Tab 외부 XFDL 참조 없음.\n");
        return sb.toString();
    }

    private void appendSharedTabContentSummary(StringBuilder sb, List<TabContentPlan> plans) {
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<String, Integer>();
        if (plans != null) for (TabContentPlan plan : plans) for (TabContentReference ref : plan.getReferences()) {
            if (!ref.isResolved()) continue;
            Integer old = counts.get(ref.getResolvedSource());
            counts.put(ref.getResolvedSource(), old == null ? 1 : old + 1);
        }
        boolean heading = false;
        for (java.util.Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue().intValue() < 2) continue;
            if (!heading) {
                sb.append("## Shared converted screen reuse\n\n");
                sb.append("동일 XFDL은 출력 파일을 한 번만 생성하며, 각 Tab Content는 별도 wframe/scope 인스턴스로 해당 파일을 로드합니다.\n\n");
                heading = true;
            }
            sb.append("- `").append(e.getKey()).append("`: referenced by ").append(e.getValue()).append(" Tab contents\n");
        }
        if (heading) sb.append('\n');
    }

    private String tabContentCsv(List<TabContentPlan> plans, List<XjsResolution> resolutions,
                                 List<ConversionRecord> records) {
        StringBuilder sb = new StringBuilder("parentScreen,tab,tabPage,content,resolvedFile,generatedTarget,webSquareSrc,loadingMode,status,resolutionMethod,xjsDependencies,warning\n");
        if (plans != null) for (TabContentPlan plan : plans) for (TabContentReference ref : plan.getReferences()) {
            XjsResolution child = findResolution(resolutions, ref.getResolvedSource());
            String xjs = child == null ? "" : child.getReferencedModules().toString();
            String warning = ref.isMixedInlineExternal() ? "mixed inline/external" : ref.getMessage();
            row(sb, plan.getScreenRelativePath(), ref.getTabPath(), ref.getTabPagePath(), ref.getRawReference(),
                    ref.getResolvedSource(), ref.getGeneratedTarget(), ref.getWebSquareSrc(), ref.getLoadingMode().name(),
                    tabConversionStatus(ref, records), ref.getResolutionMethod(), xjs, warning);
        }
        return sb.toString();
    }

    private String tabContentGraph(List<TabContentPlan> plans) {
        StringBuilder sb = new StringBuilder("# Phase 3 Tab Content Dependency Graph\n\n");
        boolean any = false;
        if (plans != null) for (TabContentPlan plan : plans) for (TabContentReference ref : plan.getReferences()) {
            any = true;
            sb.append("- `").append(plan.getScreenRelativePath()).append("` SOURCE_SCREEN")
              .append(" → `").append(ref.getTabPath()).append("` TAB_COMPONENT")
              .append(" → `").append(ref.getTabPagePath()).append("` TAB_PAGE")
              .append(" → `").append(ref.isResolved() ? ref.getResolvedSource() : ref.getRawReference())
              .append("` CONTENT_SCREEN [").append(ref.isResolved() ? "RESOLVED" : "UNRESOLVED").append("]\n");
            TabContentPlan child = findTabPlan(plans, ref.getResolvedSource());
            if (child != null && !child.getParentChildUsages().isEmpty()) {
                sb.append("  - `").append(ref.getResolvedSource()).append("` CONTENT_SCREEN → `")
                  .append(plan.getScreenRelativePath()).append("` PARENT_SCREEN [ACCESS_REVIEW] ")
                  .append(child.getParentChildUsages()).append('\n');
            }
        }
        if (!any) sb.append("Tab 외부 XFDL dependency 없음.\n");
        return sb.toString();
    }

    private String tabContentUnresolved(List<TabContentPlan> plans) {
        StringBuilder sb = new StringBuilder("# Phase 3 Tab Content Unresolved / Dynamic\n\n");
        boolean any = false;
        if (plans != null) for (TabContentPlan plan : plans) {
            for (TabContentReference ref : plan.getReferences()) if (!ref.isResolved()) {
                any = true;
                sb.append("## [TAB CONTENT UNRESOLVED] `").append(plan.getScreenRelativePath()).append("`\n\n")
                  .append("- tab: `").append(ref.getTabPath()).append("`\n")
                  .append("- tabPage: `").append(ref.getTabPagePath()).append("`\n")
                  .append("- content: `").append(ref.getRawReference()).append("`\n")
                  .append("- reason: ").append(ref.getMessage()).append("\n\n");
            }
            if (!plan.getDynamicUsages().isEmpty()) {
                any = true;
                sb.append("## [TAB TODO] dynamic content loading `").append(plan.getScreenRelativePath()).append("`\n\n");
                for (String value : plan.getDynamicUsages()) sb.append("- ").append(value).append('\n');
                sb.append('\n');
            }
        }
        if (!any) sb.append("미해결/동적 Tab content 항목 없음.\n");
        return sb.toString();
    }

    private String tabConversionStatus(TabContentReference ref, List<ConversionRecord> records) {
        if (ref == null || !ref.isResolved()) return "UNRESOLVED";
        ConversionRecord record = findRecord(records, ref.getResolvedSource());
        if (record == null) return "RESOLVED_NOT_CONVERTED";
        return "SUCCESS".equals(record.getStatus()) ? "CONVERTED" : "RESOLVED_CONVERSION_FAILED";
    }

    private ConversionRecord findRecord(List<ConversionRecord> records, String source) {
        if (records == null || source == null) return null;
        for (ConversionRecord record : records) if (source.equals(record.getSource())) return record;
        return null;
    }

    private TabContentPlan findTabPlan(List<TabContentPlan> plans, String screen) {
        if (plans == null || screen == null || screen.length() == 0) return null;
        for (TabContentPlan plan : plans) if (screen.equals(plan.getScreenRelativePath())) return plan;
        return null;
    }

    private XjsResolution findResolution(List<XjsResolution> resolutions, String screen) {
        if (resolutions == null || screen == null || screen.length() == 0) return null;
        for (XjsResolution r : resolutions) if (screen.equals(r.getScreenRelativePath())) return r;
        return null;
    }

    private String transactionReport(List<Phase3ScreenReport> screens) {
        StringBuilder sb = new StringBuilder("# Phase 3 Transaction Analysis\n\n"); boolean any=false;
        for (Phase3ScreenReport r : screens) for (TransactionCall t : r.getTransactions()) {
            if(!any){ sb.append("XPlatform transaction은 구조를 파싱하지만 WebSquare submission을 임의 생성하지 않습니다.\n\n"); any=true; }
            sb.append("## `").append(r.getScreen()).append("` line ").append(t.getLine()).append("\n\n")
              .append("- serviceId: `").append(t.getServiceId()).append("`\n")
              .append("- url: `").append(t.getUrl()).append("`\n")
              .append("- input datasets: `").append(t.getInputDatasets()).append("`\n")
              .append("- output datasets: `").append(t.getOutputDatasets()).append("`\n")
              .append("- arguments: `").append(t.getArguments()).append("`\n")
              .append("- callback: `").append(t.getCallback()).append("`\n")
              .append("- async: `").append(t.getAsyncExpression()).append("`\n\n");
        }
        if(!any) sb.append("transaction 호출 없음.\n"); return sb.toString();
    }

    private String transactionCsv(List<Phase3ScreenReport> screens) {
        StringBuilder sb = new StringBuilder("screen,line,serviceId,url,inputDatasets,outputDatasets,arguments,callback,async\n");
        for (Phase3ScreenReport r : screens) for (TransactionCall t : r.getTransactions())
            row(sb,r.getScreen(),String.valueOf(t.getLine()),t.getServiceId(),t.getUrl(),t.getInputDatasets(),t.getOutputDatasets(),t.getArguments(),t.getCallback(),t.getAsyncExpression());
        return sb.toString();
    }

    private String statusCsv(XjsRepository repository, List<ConversionRecord> records, List<Phase3ScreenReport> screens) {
        StringBuilder sb = new StringBuilder("source,type,status,reason\n");
        for (ConversionRecord record : records) {
            if (!"SUCCESS".equals(record.getStatus())) {
                row(sb, record.getSource(), record.getType(), "FAIL", record.getMessage());
                continue;
            }
            String status = "SUCCESS";
            String reason = "";
            if ("XFDL".equals(record.getType())) {
                Phase3ScreenReport screen = findScreen(screens, record.getSource());
                if (screen != null && isPartial(screen)) {
                    status = "PARTIAL";
                    reason = "TODO/partial/unresolved/API review/transaction exists";
                }
            } else if ("XJS".equals(record.getType())) {
                XjsModule module = repository.getModule(record.getSource());
                if (module != null && (!module.getDuplicateDefinitions().isEmpty()
                        || !module.getTopLevelExecutableStatements().isEmpty())) {
                    status = "PARTIAL";
                    reason = "duplicate symbol or top-level executable initialization requires review";
                }
            }
            row(sb, record.getSource(), record.getType(), status, reason);
        }
        return sb.toString();
    }

    private String severityReport(List<ConversionRecord> records, List<Phase3ScreenReport> screens,
                                  List<XjsResolution> resolutions, List<TabContentPlan> tabPlans) {
        StringBuilder sb = new StringBuilder("# Phase 3 Severity Report\n\n");
        int error = 0, warning = 0, todo = 0, info = 0;
        List<String> lines = new ArrayList<String>();
        for (ConversionRecord record : records) {
            if (!"SUCCESS".equals(record.getStatus())) {
                error++;
                lines.add("- [ERROR] `" + record.getSource() + "`: " + record.getMessage());
            } else {
                info++;
            }
        }
        for (XjsResolution resolution : resolutions) {
            for (String value : resolution.getUnresolvedFunctions()) {
                warning++; lines.add("- [WARNING] `" + resolution.getScreenRelativePath() + "` unresolved function: " + value);
            }
            for (String value : resolution.getAmbiguousSymbols()) {
                warning++; lines.add("- [WARNING] `" + resolution.getScreenRelativePath() + "` ambiguous XJS symbol: " + value);
            }
            for (String value : resolution.getIncludeWarnings()) {
                todo++; lines.add("- [TODO] `" + resolution.getScreenRelativePath() + "` XJS include/init: " + value);
            }
        }
        for (Phase3ScreenReport screen : screens) {
            for (String value : screen.getUnsupportedFeatures()) {
                if (value.startsWith("UNRESOLVED FUNCTION:") || value.startsWith("AMBIGUOUS XJS SYMBOL:")) continue;
                if (value.startsWith("XJS INCLUDE TODO:")) continue;
                if (value.startsWith("TAB CONTENT UNRESOLVED:") || value.startsWith("TAB dynamic content loading:")
                        || value.startsWith("TAB MIXED INLINE/EXTERNAL:") || value.startsWith("TAB child/parent scope access review:")
                        || value.startsWith("TAB ANALYSIS WARNING:")) continue;
                todo++; lines.add("- [TODO] `" + screen.getScreen() + "`: " + value);
            }
            for (String value : screen.getApiCandidates()) {
                todo++; lines.add("- [TODO] `" + screen.getScreen() + "` API: " + value);
            }
        }
        if (tabPlans != null) for (TabContentPlan plan : tabPlans) {
            for (TabContentReference ref : plan.getReferences()) {
                if (!ref.isResolved()) {
                    warning++; lines.add("- [WARNING] `" + plan.getScreenRelativePath() + "` TAB CONTENT UNRESOLVED "
                            + ref.getTabPagePath() + " -> " + ref.getRawReference() + " : " + ref.getMessage());
                }
                if (ref.isMixedInlineExternal()) {
                    todo++; lines.add("- [TODO] `" + plan.getScreenRelativePath() + "` mixed inline/external Tabpage: " + ref.getTabPagePath());
                }
            }
            for (String value : plan.getDynamicUsages()) {
                todo++; lines.add("- [TODO] `" + plan.getScreenRelativePath() + "` dynamic Tab content/API: " + value);
            }
            for (String value : plan.getParentChildUsages()) {
                todo++; lines.add("- [TODO] `" + plan.getScreenRelativePath() + "` child/parent scope review: " + value);
            }
            for (String value : plan.getWarnings()) {
                warning++; lines.add("- [WARNING] `" + plan.getScreenRelativePath() + "` Tab analysis: " + value);
            }
        }
        sb.append("- FATAL: 0 (startup/output-root failures abort before a complete project report can be written)\n");
        sb.append("- ERROR: ").append(error).append('\n');
        sb.append("- WARNING: ").append(warning).append('\n');
        sb.append("- TODO: ").append(todo).append('\n');
        sb.append("- INFO(successful file conversions): ").append(info).append("\n\n");
        if (lines.isEmpty()) sb.append("기록할 ERROR/WARNING/TODO 없음.\n");
        else for (String line : lines) sb.append(line).append('\n');
        return sb.toString();
    }

    private String severityCsv(List<ConversionRecord> records, List<Phase3ScreenReport> screens,
                               List<XjsResolution> resolutions, List<TabContentPlan> tabPlans) {
        StringBuilder sb = new StringBuilder("severity,source,kind,detail\n");
        for (ConversionRecord record : records) {
            row(sb, "SUCCESS".equals(record.getStatus()) ? "INFO" : "ERROR", record.getSource(),
                    "CONVERSION", "SUCCESS".equals(record.getStatus()) ? "converted" : record.getMessage());
        }
        for (XjsResolution resolution : resolutions) {
            for (String value : resolution.getUnresolvedFunctions()) row(sb, "WARNING", resolution.getScreenRelativePath(), "UNRESOLVED_FUNCTION", value);
            for (String value : resolution.getAmbiguousSymbols()) row(sb, "WARNING", resolution.getScreenRelativePath(), "AMBIGUOUS_XJS_SYMBOL", value);
            for (String value : resolution.getIncludeWarnings()) row(sb, "TODO", resolution.getScreenRelativePath(), "XJS_INCLUDE", value);
        }
        for (Phase3ScreenReport screen : screens) {
            for (String value : screen.getUnsupportedFeatures()) {
                if (value.startsWith("UNRESOLVED FUNCTION:") || value.startsWith("AMBIGUOUS XJS SYMBOL:")
                        || value.startsWith("XJS INCLUDE TODO:") || value.startsWith("TAB CONTENT UNRESOLVED:")
                        || value.startsWith("TAB dynamic content loading:") || value.startsWith("TAB MIXED INLINE/EXTERNAL:")
                        || value.startsWith("TAB child/parent scope access review:") || value.startsWith("TAB ANALYSIS WARNING:")) continue;
                row(sb, "TODO", screen.getScreen(), "FEATURE", value);
            }
            for (String value : screen.getApiCandidates()) row(sb, "TODO", screen.getScreen(), "API", value);
        }
        if (tabPlans != null) for (TabContentPlan plan : tabPlans) {
            for (TabContentReference ref : plan.getReferences()) {
                if (!ref.isResolved()) row(sb, "WARNING", plan.getScreenRelativePath(), "TAB_CONTENT_UNRESOLVED",
                        ref.getTabPagePath() + " -> " + ref.getRawReference() + " : " + ref.getMessage());
                if (ref.isMixedInlineExternal()) row(sb, "TODO", plan.getScreenRelativePath(), "TAB_MIXED_CONTENT", ref.getTabPagePath());
            }
            for (String value : plan.getDynamicUsages()) row(sb, "TODO", plan.getScreenRelativePath(), "TAB_DYNAMIC_CONTENT", value);
            for (String value : plan.getParentChildUsages()) row(sb, "TODO", plan.getScreenRelativePath(), "TAB_PARENT_CHILD_SCOPE", value);
            for (String value : plan.getWarnings()) row(sb, "WARNING", plan.getScreenRelativePath(), "TAB_ANALYSIS", value);
        }
        return sb.toString();
    }

    private Phase3ScreenReport findScreen(List<Phase3ScreenReport> screens, String source) {
        for (Phase3ScreenReport screen : screens) if (screen.getScreen().equals(source)) return screen;
        return null;
    }

    private boolean isPartial(Phase3ScreenReport screen) {
        return screen.getComponentsPartial() > 0 || screen.getComponentsTodo() > 0
                || screen.getPropertiesTodo() > 0 || screen.getEventsTodo() > 0
                || screen.getUnresolvedFunctions() > 0 || !screen.getUnsupportedFeatures().isEmpty()
                || !screen.getApiCandidates().isEmpty() || !screen.getTransactions().isEmpty();
    }

    private void row(StringBuilder sb, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            String v = values[i] == null ? "" : values[i].replace("\"", "\"\"");
            sb.append('"').append(v).append('"');
        }
        sb.append('\n');
    }
}
