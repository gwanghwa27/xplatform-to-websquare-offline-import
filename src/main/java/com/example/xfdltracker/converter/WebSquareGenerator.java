package com.example.xfdltracker.converter;

import com.example.xfdltracker.model.EventBinding;
import com.example.xfdltracker.binding.BindingAnalyzer;
import com.example.xfdltracker.binding.BindingModel;
import com.example.xfdltracker.binding.ComponentBinding;
import com.example.xfdltracker.binding.ItemsetBinding;
import com.example.xfdltracker.mapping.ComponentMapping;
import com.example.xfdltracker.mapping.ComponentMappingRegistry;
import com.example.xfdltracker.mapping.EventMapping;
import com.example.xfdltracker.mapping.EventMappingRegistry;
import com.example.xfdltracker.mapping.SupportLevel;
import com.example.xfdltracker.model.XfdlAnalysisResult;
import com.example.xfdltracker.parser.XfdlReader;
import com.example.xfdltracker.tab.TabContentPlan;
import com.example.xfdltracker.tab.TabContentReference;
import com.example.xfdltracker.tab.TabOperation;
import com.example.xfdltracker.tab.TabRuntimePlan;
import com.example.xfdltracker.tab.TabRuntimeScriptGenerator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** XFDL 파일에서 WebSquare 형태의 XML 페이지를 생성한다. */
public class WebSquareGenerator {

    private static class TabEventAdapterDef {
        private final String targetId;
        private final String beforeName;
        private final String changeName;
        private String canChangeFunction = "";
        private String changedFunction = "";
        private TabEventAdapterDef(String targetId,String beforeName,String changeName){this.targetId=targetId;this.beforeName=beforeName;this.changeName=changeName;}
    }

    private static final String NS_XHTML = "http://www.w3.org/1999/xhtml";
    private static final String NS_W2 = "http://www.inswave.com/websquare";
    private static final String NS_XF = "http://www.w3.org/2002/xforms";
    private static final String NS_EV = "http://www.w3.org/2001/xml-events";
    private static final String NS_XMLNS = "http://www.w3.org/2000/xmlns/";

    private static final Map<String, String> COMPONENT_MAP =
            new LinkedHashMap<String, String>();

    static {
        COMPONENT_MAP.put("Button", "xf:trigger");
        COMPONENT_MAP.put("Edit", "xf:input");
        COMPONENT_MAP.put("MaskEdit", "xf:input");
        COMPONENT_MAP.put("TextArea", "xf:textarea");
        COMPONENT_MAP.put("Combo", "xf:select1");
        // Real WebSquare has no xf:selectBoolean widget (absent from every shipped sample/doc;
        // silently unrendered by the real engine) - the actual checkbox component is w2:checkbox
        // (verified against a shipped KMS sample: <w2:checkbox ref="">).
        COMPONENT_MAP.put("CheckBox", "w2:checkbox");
        COMPONENT_MAP.put("Radio", "xf:select1");
        COMPONENT_MAP.put("Calendar", "w2:calendar");
        COMPONENT_MAP.put("Grid", "w2:gridView");
        COMPONENT_MAP.put("Div", "w2:group");
        COMPONENT_MAP.put("Static", "w2:span");
    }

    private final Map<String, String> componentIdMap =
            new LinkedHashMap<String, String>();

    private final Set<String> usedTargetIds =
            new LinkedHashSet<String>();

    private final Map<String, String> targetComponentTypeMap =
            new LinkedHashMap<String, String>();

    private final ComponentLayoutConverter layoutConverter =
            new ComponentLayoutConverter();

    private final GridFormatConverter gridFormatConverter =
            new GridFormatConverter();

    private final ComponentMappingRegistry componentMappings =
            new ComponentMappingRegistry();

    private final EventMappingRegistry eventMappings =
            new EventMappingRegistry();

    private BindingModel bindingModel = new BindingModel();
    private final List<String> pageLoadStatements = new ArrayList<String>();
    private final Set<String> rowPositionBootstrapped = new LinkedHashSet<String>();
    private final List<TabEventAdapterDef> tabEventAdapters = new ArrayList<TabEventAdapterDef>();
    private String formOnloadFunction = "";
    private TabContentPlan tabContentPlan;
    private TabRuntimePlan tabRuntimePlan;

    public void generate(
            File xfdlFile,
            File outputFile,
            XfdlAnalysisResult analysis) throws Exception {
        generate(xfdlFile, outputFile, analysis, null, null, null);
    }

    /** Phase 3 overload: integratedScript may contain selected XJS dependencies. */
    public void generate(
            File xfdlFile,
            File outputFile,
            XfdlAnalysisResult analysis,
            String integratedScript) throws Exception {
        generate(xfdlFile, outputFile, analysis, integratedScript, null, null);
    }

    /** Phase 3 Tab external-content overload. */
    public void generate(
            File xfdlFile, File outputFile, XfdlAnalysisResult analysis,
            String integratedScript, TabContentPlan tabContentPlan) throws Exception {
        generate(xfdlFile, outputFile, analysis, integratedScript, tabContentPlan, null);
    }

    /** Phase 3 Tab runtime overload. */
    public void generate(
            File xfdlFile,
            File outputFile,
            XfdlAnalysisResult analysis,
            String integratedScript,
            TabContentPlan tabContentPlan,
            TabRuntimePlan tabRuntimePlan) throws Exception {

        if (xfdlFile == null || outputFile == null) {
            throw new IllegalArgumentException("입력/출력 파일은 null일 수 없습니다.");
        }
        if (xfdlFile.getCanonicalFile().equals(outputFile.getCanonicalFile())) {
            throw new IllegalArgumentException(
                    "출력 파일은 원본 XFDL과 달라야 합니다: " + xfdlFile.getCanonicalPath());
        }

        componentIdMap.clear();
        targetComponentTypeMap.clear();
        usedTargetIds.clear();
        pageLoadStatements.clear();
        rowPositionBootstrapped.clear();
        tabEventAdapters.clear();
        formOnloadFunction = "";
        this.tabContentPlan = tabContentPlan;
        this.tabRuntimePlan = tabRuntimePlan;
        // 고정 루트 group ID와 원본 컴포넌트 ID가 충돌하지 않도록 선예약한다.
        usedTargetIds.add("grp_content");
        // V6_STRUCTURE_PARTIAL_ALIGNMENT: WebSquare AI v6 실제 화면의 body > grp_resultArea >
        // grp_main > (content) 구조를 부분 반영하기 위한 outer wrapper 2개도 동일하게 선예약한다.
        // WRAPPER_ID_COLLISION pre-check (전체 corpus 136개 reference XML 스캔) = 0건 확인됨.
        usedTargetIds.add("grp_resultArea");
        usedTargetIds.add("grp_main");

        XfdlReader reader = new XfdlReader();
        Document source = reader.read(xfdlFile);
        bindingModel = new BindingAnalyzer().analyze(source);
        for (String warning : bindingModel.getWarnings()) System.out.println("[BINDING TODO] " + warning);
        String xfdlScript = reader.extractScript(source);
        String originalScript = integratedScript == null ? xfdlScript : integratedScript;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document out = dbf.newDocumentBuilder().newDocument();

        Element html = out.createElementNS(NS_XHTML, "html");
        html.setAttributeNS(NS_XMLNS, "xmlns:w2", NS_W2);
        html.setAttributeNS(NS_XMLNS, "xmlns:xf", NS_XF);
        html.setAttributeNS(NS_XMLNS, "xmlns:ev", NS_EV);
        out.appendChild(html);

        Element head = out.createElementNS(NS_XHTML, "head");
        Element body = out.createElementNS(NS_XHTML, "body");
        html.appendChild(head);
        html.appendChild(body);

        appendWebSquareHead(out, head, source);

        // 스크립트 변환 전에 componentIdMap이 완성되도록 body를 먼저 생성한다.
        appendBody(out, body, source, analysis);
        appendScript(out, head, originalScript, analysis, collectDatasetIds(source), buildBindingBootstrapScript());
        appendStyle(out, head);

        write(out, outputFile);
    }

    private void appendWebSquareHead(Document out, Element head, Document source) {
        Element type = out.createElementNS(NS_W2, "w2:type");
        type.appendChild(out.createTextNode("DEFAULT"));
        head.appendChild(type);

        Element buildDate = out.createElementNS(NS_W2, "w2:buildDate");
        head.appendChild(buildDate);

        Element model = out.createElementNS(NS_XF, "xf:model");
        head.appendChild(model);

        Element instance = out.createElementNS(NS_XF, "xf:instance");
        Element data = out.createElementNS("", "data");
        data.setAttribute("xmlns", "");
        instance.appendChild(data);
        model.appendChild(instance);

        Element dataCollection = out.createElementNS(NS_W2, "w2:dataCollection");
        dataCollection.setAttribute("baseNode", "map");
        Set<String> usedDataListIds = new LinkedHashSet<String>();
        appendDatasets(out, dataCollection, source, "Dataset", usedDataListIds);
        appendDatasets(out, dataCollection, source, "DataSet", usedDataListIds);
        model.appendChild(dataCollection);

        Element workflowCollection = out.createElementNS(NS_W2, "w2:workflowCollection");
        model.appendChild(workflowCollection);
    }

    private int appendDatasets(
            Document out,
            Element dataCollection,
            Document source,
            String tagName,
            Set<String> usedDataListIds) {

        List<Element> datasets = findDescendants(source.getDocumentElement(), tagName);
        int added = 0;

        for (int i = 0; i < datasets.size(); i++) {
            Element ds = datasets.get(i);
            String id = sanitizeXml10(ds.getAttribute("id"));
            if (id.length() == 0) {
                continue;
            }
            if (!usedDataListIds.add(id)) {
                System.out.println("[DATA TODO] 중복 Dataset id 건너뜀: " + id);
                continue;
            }

            Element dataList = out.createElementNS(NS_W2, "w2:dataList");
            dataList.setAttribute("id", id);
            dataList.setAttribute("baseNode", "list");
            dataList.setAttribute("repeatNode", "map");

            Element columnInfo = out.createElementNS(NS_W2, "w2:columnInfo");
            Set<String> usedColumnIds = new LinkedHashSet<String>();
            List<Element> columns = findDescendants(ds, "Column");
            for (int c = 0; c < columns.size(); c++) {
                Element col = columns.get(c);
                String colId = sanitizeXml10(col.getAttribute("id"));
                if (colId.length() == 0) {
                    continue;
                }
                if (!usedColumnIds.add(colId)) {
                    System.out.println(
                            "[DATA TODO] 중복 Dataset Column id 건너뜀: "
                                    + id + "." + colId);
                    continue;
                }
                Element webSquareColumn = out.createElementNS(NS_W2, "w2:column");
                webSquareColumn.setAttribute("id", colId);
                webSquareColumn.setAttribute(
                        "dataType",
                        mapDataType(col.getAttribute("type")));
                columnInfo.appendChild(webSquareColumn);
            }

            dataList.appendChild(columnInfo);
            appendDatasetInitialData(out, dataList, ds, usedColumnIds, id);
            List<Element> constColumns = findDescendants(ds, "ConstColumn");
            if (!constColumns.isEmpty()) {
                System.out.println("[DATA TODO] ConstColumn은 DataList와 의미가 달라 자동 변환 보류: " + id
                        + " count=" + constColumns.size());
            }
            dataCollection.appendChild(dataList);
            added++;
        }

        return added;
    }

    private void appendDatasetInitialData(
            Document out,
            Element dataList,
            Element dataset,
            Set<String> columnIds,
            String datasetId) {
        Element rows = findDirectChild(dataset, "Rows");
        if (rows == null) return;
        List<Element> sourceRows = directChildren(rows, "Row");
        if (sourceRows.isEmpty()) return;
        Element data = out.createElementNS(NS_W2, "w2:data");
        data.setAttribute("use", "true");
        for (Element sourceRow : sourceRows) {
            String rowType = sanitizeXml10(sourceRow.getAttribute("type"));
            if (rowType.length() > 0) {
                System.out.println("[DATA TODO] 초기 Row type 상태는 WebSquare DataList에 그대로 보존하지 않음: "
                        + datasetId + " type=" + rowType);
            }
            Element row = out.createElementNS(NS_W2, "w2:row");
            for (Element col : directChildren(sourceRow, "Col")) {
                String colId = sanitizeXml10(col.getAttribute("id"));
                if (colId.length() == 0 || !columnIds.contains(colId)) continue;
                Element value = out.createElementNS("", colId);
                appendCDataSafe(out, value, sanitizeXml10(col.getTextContent()));
                row.appendChild(value);
            }
            data.appendChild(row);
        }
        dataList.appendChild(data);
    }

    private Element findDirectChild(Element parent, String tag) {
        for (Element child : directChildren(parent, null)) if (tag.equals(getSourceTagName(child))) return child;
        return null;
    }

    private List<Element> directChildren(Element parent, String tag) {
        List<Element> result = new ArrayList<Element>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element e = (Element) n;
            if (tag == null || tag.equals(getSourceTagName(e))) result.add(e);
        }
        return result;
    }

    private String mapDataType(String type) {
        if (type == null) {
            return "text";
        }
        String t = type.toUpperCase();
        if (t.indexOf("INT") >= 0
                || t.indexOf("FLOAT") >= 0
                || t.indexOf("DOUBLE") >= 0
                || t.indexOf("DECIMAL") >= 0
                || t.indexOf("NUMBER") >= 0) {
            return "number";
        }
        if (t.indexOf("DATE") >= 0 || t.indexOf("TIME") >= 0) {
            return "date";
        }
        if (t.indexOf("BOOL") >= 0) {
            return "boolean";
        }
        return "text";
    }

    private void appendScript(
            Document out,
            Element head,
            String originalScript,
            XfdlAnalysisResult analysis,
            Set<String> datasetIds,
            String bindingBootstrapScript) {

        Element script = out.createElementNS(NS_XHTML, "script");
        script.setAttribute("type", "text/javascript");

        String converted = new WebSquareScriptConverter().convert(
                originalScript,
                analysis,
                componentIdMap,
                datasetIds,
                targetComponentTypeMap,
                tabRuntimePlan);

        if (tabRuntimePlan != null && tabRuntimePlan.isRuntimeRequired()) {
            String runtime = new TabRuntimeScriptGenerator().generate(
                    tabRuntimePlan, componentIdMap, tabRuntimePlan.getRuntimeEmptyPageSrc());
            converted = runtime + "\n" + converted;
        }
        if (bindingBootstrapScript != null && bindingBootstrapScript.length() > 0) {
            converted = converted + "\n" + bindingBootstrapScript;
        }
        String tabEventScript = buildTabEventAdapterScript();
        if (tabEventScript.length() > 0) converted = converted + "\n" + tabEventScript;
        appendCDataSafe(out, script, sanitizeXml10(converted));
        head.appendChild(script);
    }

    private void appendStyle(Document out, Element head) {
        Element style = out.createElementNS(NS_XHTML, "style");
        style.setAttribute("type", "text/css");
        appendCDataSafe(out, style, "");
        head.appendChild(style);
    }

    private void appendBody(
            Document out,
            Element body,
            Document source,
            XfdlAnalysisResult analysis) {

        bindFormLifecycle(body, source, analysis);

        // WebSquare AI v6 Studio Design Canvas root container: FIXED / STUDIO_DESIGN_VERIFIED /
        // REGRESSION_VERIFIED / PATCH_READY (user-confirmed on real closed-network Studio).
        // Root-only: does NOT affect Div/GroupBox/PopupDiv/Tabpage, which stay w2:group via
        // ComponentMappingRegistry/convertChildren (a separate code path from this hardcoded
        // root). Remaining gap: V5_RUNTIME_REGRESSION_REQUIRED -- whether xf:group supports
        // TabRuntimeScriptGenerator's component('grp_content').getScope() WFrame call (narrow
        // Tab-runtime-scope subset) is unverified in a real v5 engine. Full chronology/evidence:
        // work/closed-network-support/issues/ISSUE-20260818-001-studio-design-blank/analysis/root-container-fix-chronology.md
        // and ISSUE.md.
        //
        // V6_STRUCTURE_PARTIAL_ALIGNMENT (this round): real closed-network v6 evidence shows
        // body > grp_resultArea > grp_main > (content directly) -- one id level shallower than
        // this converter's output. grp_content is kept as-is (id/namespace/style/semantics
        // untouched, only its DOM ancestor chain changes) specifically to protect the
        // Form->grp_content componentIdMap registration (registerFormRootMapping below) and the
        // id-based (not depth-based) TabRuntimeScriptGenerator literals component('grp_content')
        // .getScope()/w.grp_content, whose real-engine behavior stays V5_RUNTIME_REGRESSION_REQUIRED
        // and must not be additionally risked this round. Two new outer wrappers are added instead
        // of renaming/removing grp_content. See analysis/v6-root-body-structure-analysis.md.
        Element resultArea = out.createElementNS(NS_XF, "xf:group");
        resultArea.setAttribute("id", "grp_resultArea");
        resultArea.setAttribute("style", "");
        body.appendChild(resultArea);

        Element main = out.createElementNS(NS_XF, "xf:group");
        main.setAttribute("id", "grp_main");
        main.setAttribute("style", layoutConverter.buildMainAreaStyle(source));
        resultArea.appendChild(main);

        Element root = out.createElementNS(NS_XF, "xf:group");
        root.setAttribute("id", "grp_content");
        root.setAttribute("style", layoutConverter.buildRootStyle(source));
        main.appendChild(root);
        registerFormRootMapping(source);

        Element sourceRoot = source.getDocumentElement();
        convertChildren(
                out,
                sourceRoot,
                root,
                "",
                analysis,
                0);

        finalizePageLoadBinding(body);
        logUnmappedEventBindings(analysis);
        System.out.println(
                "[UI 변환 완료] component count=" + componentIdMap.size());
    }

    private void convertChildren(
            Document out,
            Element sourceParent,
            Element targetParent,
            String parentPath,
            XfdlAnalysisResult analysis,
            int depth) {

        if (depth > 200) {
            throw new IllegalStateException(
                    "UI 중첩 깊이가 200을 초과했습니다. path=" + parentPath);
        }

        NodeList children = sourceParent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element src = (Element) node;
            String sourceTag = getSourceTagName(src);

            if ("Layouts".equals(getSourceTagName(sourceParent))
                    && "Layout".equals(sourceTag)
                    && !isFirstDirectLayout(sourceParent, src)) {
                System.out.println(
                        "[UI TODO] 대체 Layout 건너뜀: " + describeLayout(src)
                                + " (현재 Phase 2는 첫 번째 Layout만 변환)");
                continue;
            }

            ComponentMapping componentMapping = componentMappings.get(sourceTag);
            String targetTag = componentMapping == null ? null : componentMapping.getTargetTag();
            if ("Edit".equals(sourceTag) && "true".equalsIgnoreCase(src.getAttribute("password"))) {
                targetTag = "xf:secret";
            }

            if ("Tab".equals(sourceTag) && componentMapping != null && targetTag != null) {
                convertTab(out, src, targetParent, parentPath, analysis, depth, componentMapping);
                continue;
            }

            if (targetTag != null) {
                String localId = sanitizeXml10(src.getAttribute("id"));
                if (localId.length() == 0) {
                    System.out.println("[UI 건너뜀] id 없음: " + sourceTag);
                    continue;
                }

                String sourcePath = buildSourcePath(parentPath, localId);
                String targetId = createUniqueTargetId(sourcePath);
                String canonicalPath = canonicalizePath(sourcePath);
                if (!componentIdMap.containsKey(canonicalPath)) {
                    componentIdMap.put(canonicalPath, targetId);
                } else {
                    System.out.println(
                            "[UI TODO] 중복 source path: " + sourcePath
                                    + " -> script mapping은 첫 번째 ID 유지, 생성 ID=" + targetId);
                }

                Element target = createTargetElement(out, targetTag);
                target.setAttribute("id", targetId);
                targetComponentTypeMap.put(targetId, sourceTag);
                logPartialComponentMapping(componentMapping, sourcePath);
                copyBasicProperties(src, target);
                applyComponentSpecificProperties(src, target, sourceTag, sourcePath);
                applyBindings(src, target, sourcePath, localId, targetId, sourceTag);
                bindEvents(target, sourcePath, localId, analysis);

                if ("w2:gridView".equals(targetTag)) {
                    String bindDataset = normalizeDatasetId(
                            sanitizeXml10(src.getAttribute("binddataset")));
                    if (bindDataset.length() > 0) {
                        target.setAttribute("dataList", bindDataset);
                    }
                    gridFormatConverter.convert(out, src, target);
                }

                if (layoutConverter.hasUnsupportedRelativeLayout(src)) {
                    System.out.println(
                            "[UI TODO] 상대 position2 수동 확인 필요: "
                                    + sourcePath + " ("
                                    + layoutConverter.describeLayoutSource(src) + ")");
                }
                if (layoutConverter.hasInvalidSize(src)) {
                    System.out.println(
                            "[UI TODO] 크기 계산 불가/비정상 값 생략: "
                                    + sourcePath + " ("
                                    + layoutConverter.describeLayoutSource(src) + ")");
                }

                targetParent.appendChild(target);
                System.out.println(
                        "[UI 변환] " + sourceTag + " " + sourcePath
                                + " -> " + targetTag + " id=" + targetId);

                if (isContainerComponent(sourceTag)) {
                    convertChildren(
                            out,
                            src,
                            target,
                            sourcePath,
                            analysis,
                            depth + 1);
                }

                continue;
            }

            if (shouldTraverseUnknownElement(sourceTag)) {
                String wrapperId = sanitizeXml10(src.getAttribute("id"));
                if (wrapperId.length() > 0
                        && !"FDL".equals(sourceTag)
                        && !"Form".equals(sourceTag)
                        && !"Layouts".equals(sourceTag)
                        && !"Layout".equals(sourceTag)) {
                    System.out.println(
                            "[UI TODO] 미지원 UI/컨테이너 후보를 wrapper로 재귀 탐색: "
                                    + sourceTag + " id=" + wrapperId
                                    + " (자식 좌표 기준 수동 확인 필요)");
                }
                convertChildren(
                        out,
                        src,
                        targetParent,
                        parentPath,
                        analysis,
                        depth + 1);
            }
        }
    }

    /** Phase 3: XPlatform Tab/Tabpage tree -> WebSquare tabControl tabs/content pairs. */
    private void convertTab(
            Document out,
            Element src,
            Element targetParent,
            String parentPath,
            XfdlAnalysisResult analysis,
            int depth,
            ComponentMapping componentMapping) {

        String localId = sanitizeXml10(src.getAttribute("id"));
        if (localId.length() == 0) {
            System.out.println("[UI 건너뜀] id 없음: Tab");
            return;
        }
        String sourcePath = buildSourcePath(parentPath, localId);
        String targetId = createUniqueTargetId(sourcePath);
        String canonicalPath = canonicalizePath(sourcePath);
        if (!componentIdMap.containsKey(canonicalPath)) componentIdMap.put(canonicalPath, targetId);
        else System.out.println("[UI TODO] 중복 source path: " + sourcePath + " -> " + targetId);

        Element tabControl = createTargetElement(out, "w2:tabControl");
        tabControl.setAttribute("id", targetId);
        targetComponentTypeMap.put(targetId, "Tab");
        // Inline Tabpage behavior keeps the Phase 3 baseline. External URL pages override
        // alwaysDraw per w2:content so XPlatform preload semantics can be preserved.
        tabControl.setAttribute("alwaysDraw", "true");
        logPartialComponentMapping(componentMapping, sourcePath);
        copyBasicProperties(src, tabControl);
        applyComponentSpecificProperties(src, tabControl, "Tab", sourcePath);
        applyBindings(src, tabControl, sourcePath, localId, targetId, "Tab");
        bindEvents(tabControl, sourcePath, localId, analysis);
        String tabIndex = sanitizeXml10(src.getAttribute("tabindex"));
        if (tabIndex.length() == 0) tabIndex = sanitizeXml10(src.getAttribute("index"));
        if (tabIndex.matches("-?[0-9]+")) tabControl.setAttribute("selectedTabIndex", tabIndex);

        targetParent.appendChild(tabControl);
        System.out.println("[UI 변환] Tab " + sourcePath + " -> w2:tabControl id=" + targetId);

        List<Element> pages = directTabpages(src);
        if (pages.isEmpty()) {
            System.out.println("[UI TODO] Tabpage 없음: " + sourcePath);
            return;
        }
        for (int i = 0; i < pages.size(); i++) {
            Element page = pages.get(i);
            String pageLocalId = sanitizeXml10(page.getAttribute("id"));
            if (pageLocalId.length() == 0) pageLocalId = "tabpage" + i;
            String pagePath = buildSourcePath(sourcePath, pageLocalId);
            String tabHeaderId = createUniqueTargetId(pagePath + ".tab");
            String contentId = createUniqueTargetId(pagePath + ".content");

            Element tabs = createTargetElement(out, "w2:tabs");
            tabs.setAttribute("id", tabHeaderId);
            String label = sanitizeXml10(page.getAttribute("text"));
            if (label.length() == 0) label = sanitizeXml10(page.getAttribute("titletext"));
            if (label.length() == 0) label = pageLocalId;
            tabs.setAttribute("label", label);
            if ("false".equalsIgnoreCase(page.getAttribute("enable"))) tabs.setAttribute("disabled", "true");
            if ("false".equalsIgnoreCase(page.getAttribute("visible"))) tabs.setAttribute("hidden", "true");
            copyAttributeIfPresent(page, tabs, "tooltiptext", "title");
            copyAttributeIfPresent(page, tabs, "cssclass", "class");
            tabControl.appendChild(tabs);

            Element content = createTargetElement(out, "w2:content");
            content.setAttribute("id", contentId);
            targetComponentTypeMap.put(contentId, "Tabpage");
            content.setAttribute("style", "position:relative;width:100%;height:100%;");
            tabControl.appendChild(content);

            String canonicalPagePath = canonicalizePath(pagePath);
            if (!componentIdMap.containsKey(canonicalPagePath)) componentIdMap.put(canonicalPagePath, contentId);
            if (tabRuntimePlan != null) {
                tabRuntimePlan.putPageBinding(new TabRuntimePlan.PageBinding(
                        sourcePath, pagePath, targetId, tabHeaderId, contentId));
            }
            bindEvents(content, pagePath, pageLocalId, analysis);

            TabContentReference external = tabContentPlan == null ? null : tabContentPlan.findByPagePath(pagePath);
            if (external != null) {
                applyExternalTabContent(content, external);
                // URL-linked XFDL is an independent Form/scope. Never flatten its components into
                // the parent componentIdMap or parent WebSquare DOM.
                continue;
            }

            String legacyUrl = sanitizeXml10(page.getAttribute("url"));
            if (legacyUrl.length() > 0) {
                System.out.println("[TAB CONTENT UNRESOLVED] project path context 없음: "
                        + pagePath + " url=" + legacyUrl);
                continue;
            }
            if (isRuntimeSetUrlTarget(localId, pageLocalId)) {
                // No initial url, but script assigns content dynamically via set_url() at runtime.
                // Without frameMode=wframe the content element never becomes a real WFrame, so
                // frame.setSrc is unavailable when the runtime adapter later calls set_url.
                content.setAttribute("frameMode", "wframe");
                content.setAttribute("scope", "true");
                System.out.println("[TAB CONTENT] " + pagePath + " runtime set_url target -> frameMode=wframe placeholder");
            }
            convertChildren(out, page, content, pagePath, analysis, depth + 1);
        }
    }


    /** True when the given Tabpage (no initial url) is assigned content via a runtime set_url() call. */
    private boolean isRuntimeSetUrlTarget(String tabLocalId, String pageLocalId) {
        if (tabRuntimePlan == null || tabLocalId == null || pageLocalId == null) return false;
        for (TabOperation op : tabRuntimePlan.getOperations()) {
            if (op.getType() != TabOperation.Type.SET_URL) continue;
            if (pageLocalId.equals(op.getPageId()) && tabLocalId.equals(op.getTabPath())) return true;
        }
        return false;
    }

    private void applyExternalTabContent(Element content, TabContentReference external) {
        if (external.isResolved()) {
            content.setAttribute("src", external.getWebSquareSrc());
            content.setAttribute("frameMode", "wframe");
            content.setAttribute("scope", "true");
            content.setAttribute("alwaysDraw",
                    external.getLoadingMode() == TabContentReference.LoadingMode.EAGER ? "true" : "false");
            System.out.println("[TAB CONTENT] " + external.getTabPagePath()
                    + " -> " + external.getResolvedSource()
                    + " target=" + external.getGeneratedTarget()
                    + " loading=" + external.getLoadingMode());
            if (external.isMixedInlineExternal()) {
                System.out.println("[TAB TODO] inline + external content 혼합: "
                        + external.getTabPagePath()
                        + " (외부 화면 scope 유지 위해 inline child 자동 생성 안 함)");
            }
            return;
        }
        System.out.println("[TAB CONTENT UNRESOLVED] screen=" + external.getParentScreen()
                + " tab=" + external.getTabPath()
                + " tabPage=" + external.getTabPagePath()
                + " content=" + external.getRawReference()
                + " reason=" + external.getMessage());
    }

    private List<Element> directTabpages(Element tab) {
        List<Element> result = new ArrayList<Element>();
        NodeList children = tab.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element)) continue;
            Element child = (Element) node;
            String tag = getSourceTagName(child);
            if ("Tabpage".equals(tag)) result.add(child);
            else if ("Tabpages".equals(tag)) {
                NodeList pages = child.getChildNodes();
                for (int p = 0; p < pages.getLength(); p++) {
                    Node pn = pages.item(p);
                    if (pn instanceof Element && "Tabpage".equals(getSourceTagName((Element) pn))) {
                        result.add((Element) pn);
                    }
                }
            }
        }
        return result;
    }

    private void logPartialComponentMapping(ComponentMapping mapping, String sourcePath) {
        if (mapping == null) return;
        if (mapping.getSupportLevel() == SupportLevel.PARTIAL
                || mapping.getSupportLevel() == SupportLevel.TODO) {
            System.out.println("[UI TODO] 부분 지원 Component: " + sourcePath
                    + " source=" + mapping.getSourceName()
                    + " note=" + mapping.getNote());
        }
    }

    private String getSourceTagName(Element element) {
        if (element == null) {
            return "";
        }

        String localName = element.getLocalName();
        if (localName != null && localName.length() > 0) {
            return localName;
        }

        String tagName = element.getTagName();
        if (tagName == null) {
            return "";
        }

        int colon = tagName.indexOf(':');
        if (colon >= 0 && colon + 1 < tagName.length()) {
            return tagName.substring(colon + 1);
        }
        return tagName;
    }

    private List<Element> findDescendants(Element parent, String tagName) {
        List<Element> result = new ArrayList<Element>();
        if (parent == null) {
            return result;
        }
        NodeList descendants = parent.getElementsByTagName("*");
        for (int i = 0; i < descendants.getLength(); i++) {
            Node node = descendants.item(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                if (tagName.equals(getSourceTagName(element))) {
                    result.add(element);
                }
            }
        }
        if (tagName.equals(getSourceTagName(parent))) {
            result.add(0, parent);
        }
        return result;
    }

    private boolean shouldTraverseUnknownElement(String sourceTag) {
        if (sourceTag == null || sourceTag.length() == 0) {
            return false;
        }

        // UI 트리와 무관하거나 별도 변환하는 설정/데이터 영역은 내려가지 않는다.
        return !("Script".equals(sourceTag)
                || "Dataset".equals(sourceTag)
                || "DataSet".equals(sourceTag)
                || "Bind".equals(sourceTag)
                || "BindEvent".equals(sourceTag)
                || "Formats".equals(sourceTag)
                || "Format".equals(sourceTag)
                || "Columns".equals(sourceTag)
                || "Rows".equals(sourceTag)
                || "Band".equals(sourceTag)
                || "Cell".equals(sourceTag));
    }

    private boolean isContainerComponent(String sourceTag) {
        return componentMappings.isContainer(sourceTag);
    }

    private Element createTargetElement(Document out, String targetTag) {
        if (targetTag.startsWith("xf:")) {
            return out.createElementNS(NS_XF, targetTag);
        }
        if (targetTag.startsWith("w2:")) {
            return out.createElementNS(NS_W2, targetTag);
        }
        return out.createElementNS(NS_XHTML, targetTag);
    }

    private String buildSourcePath(String parentPath, String localId) {
        if (parentPath == null || parentPath.length() == 0) {
            return localId;
        }
        return parentPath + "." + localId;
    }

    private String createUniqueTargetId(String sourcePath) {
        String base = createTargetId(sourcePath);
        String candidate = base;
        int suffix = 2;
        while (usedTargetIds.contains(candidate)) {
            candidate = base + "_" + suffix;
            suffix++;
        }
        usedTargetIds.add(candidate);
        if (!candidate.equals(base)) {
            System.out.println(
                    "[UI ID 보정] 정규화 ID 충돌: " + sourcePath + " -> " + candidate);
        }
        return candidate;
    }

    private String createTargetId(String sourcePath) {
        String canonical = canonicalizePath(sourcePath);
        StringBuilder out = new StringBuilder(canonical.length() + 1);

        for (int i = 0; i < canonical.length(); i++) {
            char ch = canonical.charAt(i);
            if (ch == '.') {
                out.append('_');
            } else if ((ch >= 'A' && ch <= 'Z')
                    || (ch >= 'a' && ch <= 'z')
                    || (ch >= '0' && ch <= '9')
                    || ch == '_'
                    || ch == '$') {
                out.append(ch);
            } else {
                out.append('_');
            }
        }

        if (out.length() == 0) {
            out.append("_component");
        } else {
            char first = out.charAt(0);
            if (first >= '0' && first <= '9') {
                out.insert(0, '_');
            }
        }
        return out.toString();
    }

    private boolean isFirstDirectLayout(Element layouts, Element candidate) {
        if (layouts == null || candidate == null) {
            return true;
        }
        NodeList children = layouts.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element child = (Element) node;
            if ("Layout".equals(getSourceTagName(child))) {
                return child == candidate;
            }
        }
        return true;
    }

    private String describeLayout(Element layout) {
        if (layout == null) {
            return "Layout";
        }
        String name = sanitizeXml10(layout.getAttribute("name"));
        String width = sanitizeXml10(layout.getAttribute("width"));
        String height = sanitizeXml10(layout.getAttribute("height"));
        return "name=" + (name.length() == 0 ? "(없음)" : name)
                + ", width=" + width + ", height=" + height;
    }

    private String canonicalizePath(String rawPath) {
        if (rawPath == null) {
            return "";
        }
        String value = rawPath.replaceAll("\\s+", "");
        if (value.startsWith("this.")) {
            value = value.substring(5);
        }
        value = value.replace(".form.", ".");
        while (value.startsWith("form.")) {
            value = value.substring(5);
        }
        return value;
    }

    private String normalizeDatasetId(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.startsWith("@") ? normalized.substring(1) : normalized;
    }

    private void copyBasicProperties(Element src, Element target) {
        String text = sanitizeXml10(src.getAttribute("text"));
        if (text.length() == 0) {
            text = sanitizeXml10(src.getAttribute("value"));
        }
        if (text.length() > 0) {
            // Real WebSquare only honors a plain static "value" attribute for action-caption
            // widgets (xf:trigger). Data-bearing widgets need their own real static-content
            // attribute name instead (confirmed against shipped docs/samples: a bare "value" is
            // silently ignored by the real engine for these): w2:span -> label ("컴포넌트의
            // value를 화면에 출력하려는 텍스트"), xf:input -> initValue ("초기의 input에 지정할
            // 초기값"). Other target tags keep "value" unchanged (xf:trigger already renders it
            // correctly; other data-bearing tags such as xf:textarea/w2:calendar/w2:progressbar
            // have no documented static-value attribute at all and are intentionally left as-is,
            // out of this fix's scope).
            String targetTag = target.getTagName();
            if ("w2:span".equals(targetTag)) {
                target.setAttribute("label", text);
            } else if ("xf:input".equals(targetTag)) {
                target.setAttribute("initValue", text);
            } else if ("w2:checkbox".equals(targetTag)) {
                // Real w2:checkbox (uiplugin.checkbox) renders an empty shell (<table
                // class="w2checkbox_main"></table>, no <input>, no label) when only a static
                // "value"/"label" attribute is set on the tag -- confirmed live against the
                // shipped engine (getConfiguredOptions() shows value/title are read but do not
                // drive rendering; the widget only produces a real <input type="checkbox">+
                // <label> row, and a working getValue()/click round-trip, once populated via its
                // real addItem(value, label) API). There is no shipped standalone w2:checkbox
                // sample to confirm a declarative XML equivalent, so the addItem call is emitted
                // into the same page-init bootstrap channel already used by BIND-1's
                // setRowPosition bootstrap. XPlatform CheckBox "value" becomes the checkbox
                // item's value; "text" becomes its visible label (falls back to the label text
                // when no separate "value" attribute is present in the source).
                String checkboxValue = sanitizeXml10(src.getAttribute("value"));
                if (checkboxValue.length() == 0) checkboxValue = text;
                String targetId = target.getAttribute("id");
                pageLoadStatements.add(targetId + ".addItem(\"" + jsString(checkboxValue) + "\", \"" + jsString(text) + "\");");
                System.out.println("[CHECKBOX 변환] " + targetId + ".addItem(value=" + checkboxValue
                        + ", label=" + text + ") 부트스트랩 추가 (real w2:checkbox는 정적 value/label 속성을 렌더링하지 않음)");
            } else {
                target.setAttribute("value", text);
            }
        }

        String style = sanitizeXml10(layoutConverter.buildComponentStyle(src));
        if (style.length() > 0) {
            target.setAttribute("style", style);
        }

        if ("true".equalsIgnoreCase(src.getAttribute("readonly"))) {
            target.setAttribute("readOnly", "true");
        }
        if ("false".equalsIgnoreCase(src.getAttribute("enable"))) {
            target.setAttribute("disabled", "true");
        }

        copyAttributeIfPresent(src, target, "cssclass", "class");
        copyAttributeIfPresent(src, target, "tooltiptext", "title");
        copyAttributeIfPresent(src, target, "taborder", "tabIndex");
        copyAttributeIfPresent(src, target, "displaynulltext", "placeholder");
        copyAttributeIfPresent(src, target, "maxlength", "maxLength");

        // WebSquare AI v6 실제 폐쇄망 정상 화면(BCI01M0000) XML source 영상 직접 판독 evidence
        // 기반 base class (component type과 1:1 대응, 이번 화면 내 예외 0건 -- 상세:
        // analysis/v6-video-source-analysis.md). cssclass로 이미 설정된 class(위 줄)와 병합하고
        // 중복 토큰은 추가하지 않는다. 다른 target QName에는 아무 영향 없음.
        String videoBaseClass = resolveVideoEvidenceBaseClass(target.getTagName());
        if (videoBaseClass != null) {
            appendClassTokenIfAbsent(target, videoBaseClass);
        }
    }

    /**
     * v6 실제 폐쇄망 화면 영상 판독으로 확인된, component type만으로 결정되는 base class.
     * 다른 QName은 이번 evidence로 확정 근거가 없어 매핑하지 않는다(UNRESOLVED) --
     * analysis/v6-class-profile.md 참고.
     */
    private String resolveVideoEvidenceBaseClass(String targetTag) {
        if ("xf:trigger".equals(targetTag)) {
            return "btn_cm";
        }
        if ("w2:gridView".equals(targetTag)) {
            return "wq_gvw";
        }
        return null;
    }

    /** class 속성에 token을 공백으로 추가한다. 이미 존재하면(중복 방지) 아무것도 하지 않는다. */
    private void appendClassTokenIfAbsent(Element target, String token) {
        String existing = target.getAttribute("class");
        if (existing.length() == 0) {
            target.setAttribute("class", token);
            return;
        }
        String[] parts = existing.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            if (token.equals(parts[i])) {
                return;
            }
        }
        target.setAttribute("class", existing + " " + token);
    }

    private void applyComponentSpecificProperties(Element src, Element target, String sourceTag, String sourcePath) {
        if ("Radio".equals(sourceTag)) {
            // xf:select1 appearance=full renders the radio-style selection family.
            target.setAttribute("appearance", "full");
        } else if ("Combo".equals(sourceTag)) {
            target.setAttribute("appearance", "minimal");
            // WebSquare AI v6 실제 폐쇄망 정상 화면(BCI01M0000) XML source 영상 직접 판독 evidence:
            // 관측된 xf:select1(appearance=minimal) 3/3 전부 disabledClass="w2selectbox_disabled"를
            // 가짐(component-intrinsic 고정값, source 조건 없음) -- 상세: analysis/v6-video-source-analysis.md.
            // Radio(appearance=full)는 이번 evidence에 없어 별도 취급하지 않는다.
            target.setAttribute("disabledClass", "w2selectbox_disabled");
        } else if ("Calendar".equals(sourceTag)) {
            String dateFormat = sanitizeXml10(src.getAttribute("dateformat"));
            if (dateFormat.length() > 0) target.setAttribute("displayFormat", dateFormat);
            if (src.hasAttribute("editformat") && sanitizeXml10(src.getAttribute("editformat")).length() > 0) {
                System.out.println("[PROPERTY TODO] Calendar editformat 수동 확인: " + sourcePath
                        + " value=" + sanitizeXml10(src.getAttribute("editformat")));
            }
        } else if ("MaskEdit".equals(sourceTag)) {
            String format = sanitizeXml10(src.getAttribute("format"));
            if (format.length() > 0) {
                System.out.println("[PROPERTY TODO] MaskEdit format 수동 확인: " + sourcePath + " value=" + format);
            }
        } else if ("ImageViewer".equals(sourceTag)) {
            String image = sanitizeXml10(src.getAttribute("image"));
            if (image.length() > 0) {
                System.out.println("[PROPERTY TODO] ImageViewer image URL/service alias 수동 확인: "
                        + sourcePath + " value=" + image);
            }
        } else if ("WebBrowser".equals(sourceTag)) {
            String url = sanitizeXml10(src.getAttribute("url"));
            if (url.length() > 0) {
                System.out.println("[PROPERTY TODO] WebBrowser url/WFrame 경로 정책 수동 확인: "
                        + sourcePath + " value=" + url);
            }
        }
    }

    private void copyAttributeIfPresent(
            Element src,
            Element target,
            String sourceName,
            String targetName) {

        String value = sanitizeXml10(src.getAttribute(sourceName));
        if (value.length() > 0) {
            target.setAttribute(targetName, value);
        }
    }

    private void bindEvents(
            Element target,
            String sourcePath,
            String localId,
            XfdlAnalysisResult analysis) {

        if (analysis == null) {
            return;
        }

        String canonicalPath = canonicalizePath(sourcePath);
        for (EventBinding event : analysis.getEvents()) {
            String bindingId = canonicalizePath(event.getComponentId());
            if (!canonicalPath.equals(bindingId) && !localId.equals(bindingId)) {
                continue;
            }

            String eventName = sanitizeXml10(event.getEventName());
            if (eventName.length() == 0) {
                continue;
            }
            EventMapping mapping = eventMappings.get(eventName);
            if (mapping == null || mapping.getTargetName().length() == 0) {
                System.out.println("[EVENT TODO] 미지원/미확정 이벤트: "
                        + sourcePath + "." + eventName + " -> " + event.getFunctionName());
                continue;
            }
            String targetId = target.getAttribute("id");
            if ("Tab".equals(targetComponentTypeMap.get(targetId))
                    && ("onchanged".equalsIgnoreCase(eventName) || "canchange".equalsIgnoreCase(eventName))) {
                String safeTarget = sanitizeJsIdentifier(targetId);
                String safeFunction = sanitizeXml10(event.getFunctionName());
                String beforeName = "__xpTabBefore_" + safeTarget;
                String changeName = "__xpTabChanged_" + safeTarget;
                TabEventAdapterDef def = findOrCreateTabEventAdapter(targetId, beforeName, changeName);
                target.setAttributeNS(NS_EV, "ev:onbeforeselect", "scwin." + beforeName);
                if ("canchange".equalsIgnoreCase(eventName)) {
                    def.canChangeFunction = safeFunction;
                } else {
                    def.changedFunction = safeFunction;
                    target.setAttributeNS(NS_EV, "ev:onchange", "scwin." + changeName);
                }
                continue;
            }
            target.setAttributeNS(
                    NS_EV,
                    "ev:" + mapping.getTargetName(),
                    "scwin." + sanitizeXml10(event.getFunctionName()));
        }
    }

    private String buildTabEventAdapterScript() {
        if (tabEventAdapters.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("// [Phase 3 Tab Event Adapter] XPlatform canchange/onchanged preindex/postindex compatibility\n");
        sb.append("scwin.__xpTabEventState = scwin.__xpTabEventState || {};\n");
        for (TabEventAdapterDef d : tabEventAdapters) {
            sb.append("scwin.").append(d.beforeName).append(" = function(selectedIndex, index) { var obj=$p.getComponentById(")
              .append(jsQuote(d.targetId)).append("); scwin.__xpTabEventState[").append(jsQuote(d.targetId))
              .append("] = selectedIndex; var e={preindex:selectedIndex,postindex:index,pretext:'',posttext:''}; ")
              .append("try{if(obj&&obj.getLabelText){if(typeof selectedIndex==='number'&&selectedIndex>=0)e.pretext=obj.getLabelText(selectedIndex);if(typeof index==='number'&&index>=0)e.posttext=obj.getLabelText(index);}}catch(ignore){} ");
            if (d.canChangeFunction.length() > 0) {
                sb.append("var r=scwin.").append(d.canChangeFunction).append("(e); return r===false?false:true; };\n");
            } else {
                sb.append("return true; };\n");
            }
            if (d.changedFunction.length() > 0) {
                sb.append("scwin.").append(d.changeName).append(" = function(tabId, index, userTabId) { var obj=$p.getComponentById(")
                  .append(jsQuote(d.targetId)).append("); var pre=scwin.__xpTabEventState[").append(jsQuote(d.targetId))
                  .append("]; var e={preindex:pre,postindex:index,pretext:'',posttext:'',tabid:tabId,userTabId:userTabId}; ")
                  .append("try{if(obj&&obj.getLabelText){if(typeof pre==='number'&&pre>=0)e.pretext=obj.getLabelText(pre);if(typeof index==='number'&&index>=0)e.posttext=obj.getLabelText(index);}}catch(ignore){} ")
                  .append("scwin.__xpTabEventState[").append(jsQuote(d.targetId)).append("]=index; return scwin.")
                  .append(d.changedFunction).append("(e); };\n");
            }
        }
        return sb.toString();
    }

    private TabEventAdapterDef findOrCreateTabEventAdapter(String targetId, String beforeName, String changeName) {
        for (TabEventAdapterDef def : tabEventAdapters) if (def.targetId.equals(targetId)) return def;
        TabEventAdapterDef created = new TabEventAdapterDef(targetId, beforeName, changeName);
        tabEventAdapters.add(created);
        return created;
    }

    private static String sanitizeJsIdentifier(String value) {
        if (value == null || value.length() == 0) return "tab";
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<value.length();i++){char c=value.charAt(i);if((i==0&&Character.isJavaIdentifierStart(c))||(i>0&&Character.isJavaIdentifierPart(c)))sb.append(c);else sb.append('_');}
        return sb.toString();
    }
    private static String jsQuote(String value) { String v=value==null?"":value; return "\""+v.replace("\\","\\\\").replace("\"","\\\"")+"\""; }

    private void logUnmappedEventBindings(XfdlAnalysisResult analysis) {
        if (analysis == null || analysis.getEvents().isEmpty()) {
            return;
        }

        Set<String> logged = new LinkedHashSet<String>();
        for (EventBinding event : analysis.getEvents()) {
            String sourceId = canonicalizePath(event.getComponentId());
            if (sourceId.length() == 0 || resolveMappedTargetId(sourceId) != null) {
                continue;
            }
            String key = sourceId + "|" + event.getEventName() + "|" + event.getFunctionName();
            if (logged.add(key)) {
                System.out.println(
                        "[UI TODO] 이벤트 대상 컴포넌트 미생성/미해석: "
                                + sourceId + "." + event.getEventName()
                                + " -> " + event.getFunctionName());
            }
        }
    }

    private String resolveMappedTargetId(String canonicalSourceId) {
        String exact = componentIdMap.get(canonicalSourceId);
        if (exact != null) {
            return exact;
        }
        int dot = canonicalSourceId.lastIndexOf('.');
        String local = dot >= 0 ? canonicalSourceId.substring(dot + 1) : canonicalSourceId;
        String found = null;
        for (Map.Entry<String, String> entry : componentIdMap.entrySet()) {
            String key = entry.getKey();
            int keyDot = key.lastIndexOf('.');
            String keyLocal = keyDot >= 0 ? key.substring(keyDot + 1) : key;
            if (!local.equals(keyLocal)) {
                continue;
            }
            if (found != null && !found.equals(entry.getValue())) {
                return null;
            }
            found = entry.getValue();
        }
        return found;
    }

    private void registerFormRootMapping(Document source) {
        List<Element> forms = findDescendants(source.getDocumentElement(), "Form");
        if (forms.isEmpty()) return;
        String formId = canonicalizePath(sanitizeXml10(forms.get(0).getAttribute("id")));
        if (formId.length() > 0 && !componentIdMap.containsKey(formId)) {
            componentIdMap.put(formId, "grp_content");
            targetComponentTypeMap.put("grp_content", "Form");
            System.out.println("[UI 매핑] Form " + formId + " -> grp_content (lifecycle obj 호환)");
        }
    }

    private Set<String> collectDatasetIds(Document source) {
        Set<String> ids = new LinkedHashSet<String>();
        List<Element> datasets = new ArrayList<Element>();
        datasets.addAll(findDescendants(source.getDocumentElement(), "Dataset"));
        datasets.addAll(findDescendants(source.getDocumentElement(), "DataSet"));
        for (Element ds : datasets) {
            String id = normalizeDatasetId(sanitizeXml10(ds.getAttribute("id")));
            if (id.length() > 0) ids.add(id);
        }
        return ids;
    }

    /** Form lifecycle is bound to WebSquare body instead of being left as an uncalled function. */
    private void bindFormLifecycle(Element body, Document source, XfdlAnalysisResult analysis) {
        if (analysis == null) return;
        List<Element> forms = findDescendants(source.getDocumentElement(), "Form");
        Set<String> formIds = new LinkedHashSet<String>();
        for (Element form : forms) {
            String id = canonicalizePath(sanitizeXml10(form.getAttribute("id")));
            if (id.length() > 0) formIds.add(id);
        }
        for (EventBinding event : analysis.getEvents()) {
            String sourceId = canonicalizePath(event.getComponentId());
            if (!formIds.contains(sourceId)) continue;
            EventMapping mapping = eventMappings.get(event.getEventName());
            if (mapping == null || mapping.getTargetName().length() == 0) {
                System.out.println("[EVENT TODO] Form lifecycle 미지원: "
                        + event.getEventName() + " -> " + event.getFunctionName());
                continue;
            }
            if (!("onload".equalsIgnoreCase(event.getEventName())
                    || "onsize".equalsIgnoreCase(event.getEventName()))) {
                System.out.println("[EVENT TODO] Form 이벤트 lifecycle 수동 확인: "
                        + event.getEventName() + " -> " + event.getFunctionName());
                continue;
            }
            body.setAttributeNS(NS_EV, "ev:" + mapping.getTargetName(),
                    "scwin." + sanitizeXml10(event.getFunctionName()));
            if ("onload".equalsIgnoreCase(event.getEventName())) {
                if (formOnloadFunction.length() == 0) formOnloadFunction = sanitizeXml10(event.getFunctionName());
                else if (!formOnloadFunction.equals(event.getFunctionName())) {
                    System.out.println("[EVENT TODO] Form onload 다중 handler: " + event.getFunctionName());
                }
            }
            System.out.println("[EVENT 변환] Form " + event.getEventName()
                    + " -> body." + mapping.getTargetName() + " / scwin." + event.getFunctionName());
        }
    }

    private void applyBindings(
            Element src,
            Element target,
            String sourcePath,
            String localId,
            String targetId,
            String sourceTag) {
        ComponentBinding valueBinding = bindingModel.findComponentBinding(sourcePath, localId, "value");
        if (valueBinding == null) valueBinding = bindingModel.findComponentBinding(sourcePath, localId, "text");
        if (valueBinding != null) {
            target.setAttribute("ref", "data:" + valueBinding.getDatasetId() + "." + valueBinding.getColumnId());
            System.out.println("[BINDING 변환] " + sourcePath + " -> data:"
                    + valueBinding.getDatasetId() + "." + valueBinding.getColumnId());

            // A scalar "data:dataset.column" ref only resolves once the target w2:dataList's
            // internal row cursor is set (real WebSquare: w2:dataList.setRowPosition(rowIndex);
            // unset by default, so the field renders blank even though the ref is correct).
            // XPlatform BindItem has no explicit row concept for a plain component bind, so the
            // first row (0) is the only safe, generalizable default. One bootstrap call per
            // dataset regardless of how many components bind into it.
            String datasetId = valueBinding.getDatasetId();
            if (datasetId != null && datasetId.length() > 0 && rowPositionBootstrapped.add(datasetId)) {
                pageLoadStatements.add(datasetId + ".setRowPosition(0);");
                System.out.println("[BINDING 변환] " + datasetId
                        + ".setRowPosition(0) 부트스트랩 추가 (repeating dataList scalar bind 해석 위해 필요)");
            }
        }

        ItemsetBinding itemset = bindingModel.findItemset(sourcePath, localId);
        if (itemset != null) {
            if ("Combo".equals(sourceTag) || "ListBox".equals(sourceTag) || "Radio".equals(sourceTag)) {
                if (itemset.getCodeColumn().length() > 0 && itemset.getDataColumn().length() > 0) {
                    pageLoadStatements.add(targetId + ".setNodeSet(\"data:"
                            + jsString(itemset.getDatasetId()) + "\", \""
                            + jsString(itemset.getDataColumn()) + "\", \""
                            + jsString(itemset.getCodeColumn()) + "\");");
                    System.out.println("[ITEMSET 변환] " + sourcePath + " -> " + itemset.getDatasetId()
                            + " label=" + itemset.getDataColumn() + " value=" + itemset.getCodeColumn());
                }
            } else {
                System.out.println("[BINDING TODO] innerdataset 지원 대상 아님: " + sourcePath + " tag=" + sourceTag);
            }
        }
    }

    private void finalizePageLoadBinding(Element body) {
        if (pageLoadStatements.isEmpty() && !needsRuntimeReadyHook()) return;
        body.setAttributeNS(NS_EV, "ev:onpageload", "scwin.__xpws_onpageload");
    }

    /** External WFrame bridge targets expose READY only after their converted Form onload succeeds. */
    private boolean needsRuntimeReadyHook() {
        return tabRuntimePlan != null && tabRuntimePlan.isBridgeTarget();
    }

    private String buildBindingBootstrapScript() {
        boolean runtimeReadyHook = needsRuntimeReadyHook();
        if (pageLoadStatements.isEmpty() && !runtimeReadyHook) return "";
        StringBuilder out = new StringBuilder();
        if (!pageLoadStatements.isEmpty()) {
            out.append("// Phase 3: Dataset itemset binding bootstrap.\n");
            out.append("scwin.__xpws_initBindings = function() {\n");
            for (String statement : pageLoadStatements) out.append("    ").append(statement).append('\n');
            out.append("};\n\n");
        }
        if (runtimeReadyHook) {
            out.append("// Phase 3 Runtime Finalization: WFrame child lifecycle READY contract.\n");
            out.append("scwin.__xpRuntimePageReady = false;\n");
            out.append("scwin.__xpRuntimePageLoadError = null;\n");
            out.append("scwin.__xpws_markRuntimeReady = function() {\n");
            out.append("    scwin.__xpRuntimePageLoadError = null;\n");
            out.append("    scwin.__xpRuntimePageReady = true;\n");
            out.append("};\n\n");
        }
        out.append("scwin.__xpws_onpageload = function(e) {\n");
        if (runtimeReadyHook) {
            out.append("    scwin.__xpRuntimePageReady = false;\n");
            out.append("    scwin.__xpRuntimePageLoadError = null;\n");
            out.append("    try {\n");
        }
        String indent = runtimeReadyHook ? "        " : "    ";
        if (!pageLoadStatements.isEmpty()) {
            out.append(indent).append("scwin.__xpws_initBindings();\n");
        }
        if (formOnloadFunction.length() > 0) {
            if (runtimeReadyHook) {
                out.append(indent).append("var __xpResult;\n");
                out.append(indent).append("if (typeof scwin.").append(formOnloadFunction).append(" === \"function\") {\n");
                out.append(indent).append("    __xpResult = scwin.").append(formOnloadFunction).append("(e);\n");
                out.append(indent).append("}\n");
                out.append(indent).append("if (__xpResult && typeof __xpResult.then === \"function\") {\n");
                out.append(indent).append("    return __xpResult.then(function(v) {\n");
                out.append(indent).append("        scwin.__xpws_markRuntimeReady();\n");
                out.append(indent).append("        return v;\n");
                out.append(indent).append("    }, function(err) {\n");
                out.append(indent).append("        scwin.__xpRuntimePageLoadError = err;\n");
                out.append(indent).append("        scwin.__xpRuntimePageReady = false;\n");
                out.append(indent).append("        throw err;\n");
                out.append(indent).append("    });\n");
                out.append(indent).append("}\n");
            } else {
                out.append(indent).append("if (typeof scwin.").append(formOnloadFunction).append(" === \"function\") {\n");
                out.append(indent).append("    return scwin.").append(formOnloadFunction).append("(e);\n");
                out.append(indent).append("}\n");
            }
        }
        if (runtimeReadyHook) {
            out.append("        scwin.__xpws_markRuntimeReady();\n");
            if (formOnloadFunction.length() > 0) out.append("        return __xpResult;\n");
            out.append("    } catch (err) {\n");
            out.append("        scwin.__xpRuntimePageLoadError = err;\n");
            out.append("        scwin.__xpRuntimePageReady = false;\n");
            out.append("        throw err;\n");
            out.append("    }\n");
        }
        out.append("};\n");
        return out.toString();
    }

    private String jsString(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
    }

    private void appendCDataSafe(Document out, Element parent, String text) {
        String safe = text == null ? "" : text;
        int start = 0;
        int marker;

        while ((marker = safe.indexOf("]]>", start)) >= 0) {
            parent.appendChild(out.createCDATASection(safe.substring(start, marker + 2)));
            start = marker + 2;
        }
        parent.appendChild(out.createCDATASection(safe.substring(start)));
    }

    private String sanitizeXml10(String value) {
        if (value == null || value.length() == 0) {
            return value == null ? "" : value;
        }

        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length();) {
            int cp = value.codePointAt(i);
            if (isValidXml10Character(cp)) {
                out.append(Character.toChars(cp));
            }
            i += Character.charCount(cp);
        }
        return out.toString();
    }

    private boolean isValidXml10Character(int cp) {
        return cp == 0x09
                || cp == 0x0A
                || cp == 0x0D
                || (cp >= 0x20 && cp <= 0xD7FF)
                || (cp >= 0xE000 && cp <= 0xFFFD)
                || (cp >= 0x10000 && cp <= 0x10FFFF);
    }

    private void write(Document document, File outputFile) throws Exception {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IllegalStateException("출력 디렉터리를 생성할 수 없습니다: " + parent);
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        try {
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        } catch (Exception ignored) {
        }
        try {
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (Exception ignored) {
        }

        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        try {
            transformer.setOutputProperty(
                    "{http://xml.apache.org/xslt}indent-amount",
                    "4");
        } catch (Exception ignored) {
        }

        transformer.transform(
                new DOMSource(document),
                new StreamResult(outputFile));
    }
}
