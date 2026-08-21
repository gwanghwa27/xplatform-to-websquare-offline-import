package com.example.xfdltracker.project;

import com.example.xfdltracker.XfdlFunctionTracker;
import com.example.xfdltracker.converter.WebSquareGenerator;
import com.example.xfdltracker.io.ConsoleLog;
import com.example.xfdltracker.model.XfdlAnalysisResult;
import com.example.xfdltracker.parser.XfdlReader;
import com.example.xfdltracker.xjs.XjsDependencyResolver;
import com.example.xfdltracker.xjs.XjsRepository;
import com.example.xfdltracker.xjs.XjsResolution;
import com.example.xfdltracker.transaction.TransactionAnalyzer;
import com.example.xfdltracker.transaction.TransactionCall;
import com.example.xfdltracker.tab.TabContentPlan;
import com.example.xfdltracker.tab.TabContentResolver;
import com.example.xfdltracker.tab.ScreenTargetRegistry;
import com.example.xfdltracker.tab.TabOperationAnalyzer;
import com.example.xfdltracker.tab.TabRuntimePlan;
import com.example.xfdltracker.tab.TabRuntimeScriptGenerator;
import com.example.xfdltracker.tab.CrossScreenReferenceAnalyzer;
import com.example.xfdltracker.tab.ScreenSymbolCatalog;
import com.example.xfdltracker.tab.ScopeBridgeReferenceAnalyzer;
import com.example.xfdltracker.io.TextFileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 프로젝트 루트 변환기. XPlatform 소스 트리를 읽어 별도의 WebSquare 마이그레이션 트리를 생성한다.
 * JDK 1.8.0_111 호환이며 외부 라이브러리를 사용하지 않는다.
 */
public class XPlatformProjectConverter {

    public static void main(String[] args) throws Exception {
        ConsoleLog.install(new File("logs", "converter.log"), false);
        if (args.length < 2 || args.length > 3) {
            usage();
            System.exit(1);
        }

        File sourceRoot = new File(args[0]).getCanonicalFile();
        File outputRoot = new File(args[1]).getCanonicalFile();
        String encoding = args.length == 3 ? args[2] : "UTF-8";

        if (!sourceRoot.isDirectory()) {
            System.err.println("소스 프로젝트 디렉터리를 찾을 수 없습니다: " + sourceRoot);
            System.exit(2);
        }
        if (sourceRoot.equals(outputRoot)) {
            System.err.println("출력 디렉터리는 소스 디렉터리와 달라야 합니다.");
            System.exit(3);
        }

        XPlatformProjectConverter converter = new XPlatformProjectConverter();
        try {
            converter.convert(sourceRoot, outputRoot, encoding);
        } catch (ProjectConversionFailedException e) {
            System.err.println(e.getMessage());
            System.exit(10);
        }
    }

    public void convert(final File sourceRoot, final File outputRoot, final String encoding) throws Exception {
        final String outputCanonical = outputRoot.getCanonicalPath();
        final List<File> sourceFiles = new ArrayList<File>();

        Files.walkFileTree(sourceRoot.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                File f = dir.toFile().getCanonicalFile();
                String p = f.getCanonicalPath();
                if (p.equals(outputCanonical) || p.startsWith(outputCanonical + File.separator)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String n = file.getFileName().toString().toLowerCase();
                if (n.endsWith(".xfdl") || n.endsWith(".xjs")) sourceFiles.add(file.toFile());
                return FileVisitResult.CONTINUE;
            }
        });

        Collections.sort(sourceFiles, new Comparator<File>() {
            public int compare(File a, File b) { return a.getAbsolutePath().compareToIgnoreCase(b.getAbsolutePath()); }
        });

        if (!outputRoot.exists() && !outputRoot.mkdirs() && !outputRoot.isDirectory()) {
            throw new IllegalStateException("출력 디렉터리를 생성할 수 없습니다: " + outputRoot);
        }

        // Phase 3 pass 1: build a project-wide XJS symbol repository before converting any screen.
        XjsRepository xjsRepository = new XjsRepository(sourceRoot);
        for (File source : sourceFiles) {
            if (!source.getName().toLowerCase().endsWith(".xjs")) continue;
            try {
                xjsRepository.add(source, encoding);
            } catch (Exception e) {
                System.err.println("[XJS INDEX ERROR] " + relative(sourceRoot, source) + " : " + safeMessage(e));
            }
        }
        XjsDependencyResolver xjsResolver = new XjsDependencyResolver(xjsRepository);
        ScreenTargetRegistry screenTargets = new ScreenTargetRegistry(sourceRoot, sourceFiles);
        TabContentResolver tabResolver = new TabContentResolver(sourceRoot, screenTargets);
        TabOperationAnalyzer tabOperationAnalyzer = new TabOperationAnalyzer(screenTargets);

        List<SourceAnalysis> analyses = new ArrayList<SourceAnalysis>();
        List<ConversionRecord> records = new ArrayList<ConversionRecord>();
        List<XjsResolution> xjsResolutions = new ArrayList<XjsResolution>();
        List<Phase3ScreenReport> phase3Screens = new ArrayList<Phase3ScreenReport>();
        List<TabContentPlan> tabContentPlans = new ArrayList<TabContentPlan>();
        List<TabRuntimePlan> tabRuntimePlans = new ArrayList<TabRuntimePlan>();
        XfdlFunctionTracker xfdlTracker = new XfdlFunctionTracker();
        XjsFunctionTracker xjsTracker = new XjsFunctionTracker();
        WebSquareGenerator pageGenerator = new WebSquareGenerator();
        WebSquareCommonScriptGenerator commonGenerator = new WebSquareCommonScriptGenerator();

        // Phase 3 Tab runtime pre-pass: all screens are analyzed before generation so parent/child
        // references can be validated without depending on source-file sort order.
        Map<String, PreparedScreen> preparedScreens = new LinkedHashMap<String, PreparedScreen>();
        Map<String, Exception> preparationErrors = new LinkedHashMap<String, Exception>();
        Map<String, String> integratedScriptsByScreen = new LinkedHashMap<String, String>();
        Map<String, TabContentPlan> tabPlansByScreen = new LinkedHashMap<String, TabContentPlan>();
        Map<String, TabRuntimePlan> runtimePlansByScreen = new LinkedHashMap<String, TabRuntimePlan>();
        Map<String, ScreenSymbolCatalog> symbolCatalogs = new LinkedHashMap<String, ScreenSymbolCatalog>();
        for (File source : sourceFiles) {
            if (!source.getName().toLowerCase().endsWith(".xfdl")) continue;
            String rel = relative(sourceRoot, source);
            try {
                XfdlAnalysisResult nativeAnalysis = xfdlTracker.analyze(source);
                XfdlReader reader = new XfdlReader();
                String nativeScript = reader.extractScript(reader.read(source));
                TabContentPlan tabPlan = tabResolver.analyze(source, rel);
                XjsResolution resolution = xjsResolver.resolve(rel, nativeScript, nativeAnalysis,
                        new XfdlScreenSymbolInventory().collect(source));
                String integratedScript = nativeScript + resolution.buildExternalScript();
                XfdlAnalysisResult integratedAnalysis = xfdlTracker.analyze(source, integratedScript);
                TabRuntimePlan runtimePlan = tabOperationAnalyzer.analyze(source, rel, integratedScript, tabPlan);
                if (runtimePlan.needsFullRuntimePathMap()) {
                    Map<String,String> runtimePaths = screenTargets.buildRuntimePathMap(source, rel);
                    for (Map.Entry<String,String> path : runtimePaths.entrySet()) runtimePlan.putRuntimePath(path.getKey(), path.getValue());
                }
                runtimePlan.setRuntimeEmptyPageSrc(relativeTarget(replaceExtension(rel, ".xml"), "runtime/xplatform-tab-empty.xml"));
                PreparedScreen prepared = new PreparedScreen(nativeAnalysis, integratedAnalysis, tabPlan, runtimePlan,
                        resolution, integratedScript, new TransactionAnalyzer().analyze(integratedScript));
                preparedScreens.put(rel, prepared);
                integratedScriptsByScreen.put(rel, integratedScript);
                tabPlansByScreen.put(rel, tabPlan);
                runtimePlansByScreen.put(rel, runtimePlan);
                symbolCatalogs.put(rel, ScreenSymbolCatalog.build(source, rel, integratedAnalysis));
            } catch (Exception e) {
                preparationErrors.put(rel, e);
            }
        }
        new CrossScreenReferenceAnalyzer().analyze(
                integratedScriptsByScreen, tabPlansByScreen, runtimePlansByScreen, symbolCatalogs);
        new ScopeBridgeReferenceAnalyzer().analyze(
                integratedScriptsByScreen, runtimePlansByScreen, symbolCatalogs);

        int success = 0;
        int failure = 0;

        System.out.println("소스 : " + sourceRoot.getAbsolutePath());
        System.out.println("출력 : " + outputRoot.getAbsolutePath());
        System.out.println("XJS 대체 인코딩: " + encoding);
        System.out.println("파일 수 : " + sourceFiles.size());
        System.out.println();

        for (File source : sourceFiles) {
            String rel = relative(sourceRoot, source);
            String lower = source.getName().toLowerCase();
            String type = lower.endsWith(".xfdl") ? "XFDL" : "XJS";
            File output = outputFor(outputRoot, rel, type);
            try {
                XfdlAnalysisResult analysis;
                XfdlAnalysisResult analysisForLegacyReport;
                if ("XFDL".equals(type)) {
                    Exception preparationError = preparationErrors.get(rel);
                    if (preparationError != null) throw preparationError;
                    PreparedScreen prepared = preparedScreens.get(rel);
                    if (prepared == null) throw new IllegalStateException("XFDL pre-analysis 결과 없음: " + rel);
                    analysisForLegacyReport = prepared.nativeAnalysis;
                    analysis = prepared.integratedAnalysis;
                    tabContentPlans.add(prepared.tabPlan);
                    tabRuntimePlans.add(prepared.runtimePlan);
                    pageGenerator.generate(source, output, analysis, prepared.integratedScript, prepared.tabPlan, prepared.runtimePlan);
                    xjsResolutions.add(prepared.xjsResolution);
                    phase3Screens.add(new Phase3ScreenAnalyzer().analyze(
                            source, rel, prepared.integratedScript, prepared.nativeAnalysis, prepared.xjsResolution,
                            prepared.transactionCalls, prepared.tabPlan));
                    logXjsResolution(prepared.xjsResolution);
                    logTabContentPlan(prepared.tabPlan);
                } else {
                    analysis = xjsTracker.analyze(source, encoding);
                    analysisForLegacyReport = analysis;
                    commonGenerator.generate(source, output, analysis, encoding);
                }
                // Legacy function reports must describe source-owned definitions only.
                // Imported XJS symbols are already reported by Phase3ProjectReportWriter and must not
                // appear as duplicate XFDL definitions in the Phase 2-compatible reports.
                analyses.add(new SourceAnalysis(source, rel, type, analysisForLegacyReport));
                records.add(new ConversionRecord(rel, relative(outputRoot, output), type, "SUCCESS", ""));
                success++;
                System.out.println("[성공] " + rel + " -> " + relative(outputRoot, output));
            } catch (Exception e) {
                records.add(new ConversionRecord(rel, relative(outputRoot, output), type, "FAIL", safeMessage(e)));
                failure++;
                System.err.println("[실패] " + rel + " : " + safeMessage(e));
            }
        }

        if (hasRuntimeTabs(tabRuntimePlans)) writeTabRuntimeResources(outputRoot);
        File reportDir = new File(outputRoot, "conversion-report");
        new ProjectReportWriter().write(reportDir, analyses, records);
        new Phase3ProjectReportWriter().write(reportDir, xjsRepository, xjsResolutions, analyses, records, phase3Screens, tabContentPlans, tabRuntimePlans);

        System.out.println();
        System.out.println("완료. 성공=" + success + ", 실패=" + failure);
        System.out.println("리포트: " + reportDir.getAbsolutePath());
        if (failure > 0) {
            throw new ProjectConversionFailedException(
                    "일부 파일 변환에 실패했습니다. 실패=" + failure
                            + ", 리포트=" + reportDir.getAbsolutePath());
        }
    }

    private static boolean hasRuntimeTabs(List<TabRuntimePlan> plans) {
        if (plans != null) for (TabRuntimePlan plan : plans) if (plan != null && plan.isRuntimeRequired()) return true;
        return false;
    }

    private static void writeTabRuntimeResources(File outputRoot) throws Exception {
        File runtimeDir = new File(outputRoot, "runtime");
        if (!runtimeDir.exists() && !runtimeDir.mkdirs() && !runtimeDir.isDirectory())
            throw new IllegalStateException("Tab runtime 디렉터리를 생성할 수 없습니다: " + runtimeDir);
        TextFileUtil.writeUtf8(new File(runtimeDir, "xplatform-tab-runtime.js"),
                new TabRuntimeScriptGenerator().generateStandaloneReference());
        String empty = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:w2=\"http://www.inswave.com/websquare\">\n"
                + "  <head><script type=\"text/javascript\"><![CDATA[var scwin=(typeof scwin===\"undefined\")?{}:scwin;]]></script></head>\n"
                + "  <body><w2:group id=\"grp_content\" style=\"position:relative;width:100%;height:100%;\"/></body>\n"
                + "</html>\n";
        TextFileUtil.writeUtf8(new File(runtimeDir, "xplatform-tab-empty.xml"), empty);
    }

    private static String replaceExtension(String path, String ext) {
        String v=path.replace('\\','/'); int dot=v.lastIndexOf('.'), slash=v.lastIndexOf('/'); return dot>slash?v.substring(0,dot)+ext:v+ext;
    }
    private static String relativeTarget(String parentTarget, String childTarget) {
        String p=parentTarget.replace('\\','/'); int slash=p.lastIndexOf('/'); String dir=slash>=0?p.substring(0,slash):"";
        try { return java.nio.file.Paths.get(dir.length()==0?".":dir).relativize(java.nio.file.Paths.get(childTarget.replace('\\','/'))).toString().replace('\\','/'); }
        catch(Exception e) { return childTarget.replace('\\','/'); }
    }

    private static void logTabContentPlan(TabContentPlan plan) {
        if (plan == null) return;
        System.out.println("[TAB] screen=" + plan.getScreenRelativePath()
                + " external=" + plan.getReferences().size()
                + " dynamic=" + plan.getDynamicUsages().size()
                + " parentAccess=" + plan.getParentChildUsages().size());
        for (com.example.xfdltracker.tab.TabContentReference ref : plan.getReferences()) {
            if (!ref.isResolved()) {
                System.out.println("[TAB CONTENT UNRESOLVED] tab=" + ref.getTabPath()
                        + " tabPage=" + ref.getTabPagePath()
                        + " content=" + ref.getRawReference()
                        + " reason=" + ref.getMessage());
            }
        }
        for (String value : plan.getDynamicUsages()) System.out.println("[TAB TODO] dynamic content loading: " + value);
        for (String value : plan.getParentChildUsages()) System.out.println("[TAB TODO] parent/child scope API: " + value);
        for (String value : plan.getWarnings()) System.out.println("[TAB TODO] " + value);
    }

    private static void logXjsResolution(XjsResolution resolution) {
        System.out.println("[XJS] screen=" + resolution.getScreenRelativePath()
                + " modules=" + resolution.getReferencedModules().size()
                + " functions=" + resolution.getImportedFunctions().size()
                + " globals=" + resolution.getImportedGlobals().size()
                + " unresolved=" + resolution.getUnresolvedFunctions().size()
                + " ambiguous=" + resolution.getAmbiguousSymbols().size());
        for (String value : resolution.getIncludeWarnings()) System.out.println("[XJS TODO] " + value);
        for (String value : resolution.getAmbiguousSymbols()) System.out.println("[XJS TODO] " + value);
        for (String value : resolution.getUnresolvedFunctions()) System.out.println("[UNRESOLVED FUNCTION] " + value);
    }

    private static final class PreparedScreen {
        private final XfdlAnalysisResult nativeAnalysis;
        private final XfdlAnalysisResult integratedAnalysis;
        private final TabContentPlan tabPlan;
        private final TabRuntimePlan runtimePlan;
        private final XjsResolution xjsResolution;
        private final String integratedScript;
        private final List<TransactionCall> transactionCalls;
        private PreparedScreen(XfdlAnalysisResult nativeAnalysis, XfdlAnalysisResult integratedAnalysis,
                               TabContentPlan tabPlan, TabRuntimePlan runtimePlan, XjsResolution xjsResolution,
                               String integratedScript, List<TransactionCall> transactionCalls) {
            this.nativeAnalysis=nativeAnalysis; this.integratedAnalysis=integratedAnalysis; this.tabPlan=tabPlan;
            this.runtimePlan=runtimePlan; this.xjsResolution=xjsResolution; this.integratedScript=integratedScript;
            this.transactionCalls=transactionCalls;
        }
    }

    private static final class ProjectConversionFailedException extends Exception {
        private static final long serialVersionUID = 1L;

        private ProjectConversionFailedException(String message) {
            super(message);
        }
    }

    private static File outputFor(File outputRoot, String relativePath, String type) {
        String normalized = relativePath.replace('\\', '/');
        int dot = normalized.lastIndexOf('.');
        String base = dot >= 0 ? normalized.substring(0, dot) : normalized;
        return new File(outputRoot, base + ("XFDL".equals(type) ? ".xml" : ".js"));
    }

    private static String relative(File root, File file) throws IOException {
        Path rootPath = root.getCanonicalFile().toPath();
        Path filePath = file.getCanonicalFile().toPath();
        return rootPath.relativize(filePath).toString().replace('\\', '/');
    }

    private static String safeMessage(Exception e) {
        String m = e.getMessage();
        if (m == null || m.length() == 0) m = e.getClass().getName();
        return m.replace('\r', ' ').replace('\n', ' ');
    }

    private static void usage() {
        System.err.println("사용법:");
        System.err.println("  java -cp bin com.example.xfdltracker.project.XPlatformProjectConverter <xplatform-project-dir> <websquare-output-dir> [xjs-encoding]");
        System.err.println("예시:");
        System.err.println("  java -cp bin com.example.xfdltracker.project.XPlatformProjectConverter C:\\\\work\\XPlatformApp C:\\\\work\\WebSquareApp MS949");
    }
}
