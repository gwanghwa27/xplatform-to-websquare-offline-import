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

        // Root container(grp_resultArea/grp_main): STUDIO_DESIGN_VERIFIED, 폐쇄망 실측 완료.
        // 잔여 gap: V5_RUNTIME_REGRESSION_REQUIRED(xf:group의 getScope() 실제 v5 엔진 지원 여부
        // 미검증) -- 상세: analysis/root-container-fix-chronology.md, ISSUE.md.
        // grp_content wrapper는 이번 라운드에 제거(GLOBAL_GRP_CONTENT_XFDL_COUNT=0), 변환된
        // Div/Layout/Grid 구조가 grp_main 바로 아래 위치. id 예약은 충돌 방지용으로 유지하되
        // TabRuntimeScriptGenerator/XPlatformProjectConverter/registerFormRootMapping의 관련
        // literal은 전부 grp_content -> grp_main으로 함께 이동(EXPECTED_SOURCE_TO_TARGET_MAP_DIFF).
        // 상세: analysis/v6-design-structure-alignment-analysis.md.
        //
        // ROOT_PERCENT_CONTAINING_BLOCK_DEFECT fix: grp_content 제거 이후 percentage 자식들의
        // containing block chain(body -> grp_resultArea -> grp_main -> child%) 어디에도 명시적
        // width가 없어, 실제 폐쇄망 Studio에서 업무 영역이 좌측 좁은 영역으로 collapse함을
        // 재현/확인(STUDIO_DESIGN_FAILED/STUDIO_DESIGN_REPRODUCED). grp_resultArea에도
        // width:100%(구조 상수, 화면별 계산값 아님)를 명시해 체인을 끊지 않는다.
        //
        // GRP_RESULT_AREA_HEIGHT_SOURCE_FORM fix: height도 동일한 이유로 명시가 필요하다 --
        // percentage height 체인이 실제로 resolve되려면 chain 최상단(grp_resultArea)부터
        // 확정 height(auto 아님)가 있어야 한다. grp_main과 동일하게 source Form의 선언
        // design height를 그대로 재사용한다(buildMainAreaStyle 재사용, 신규 함수 없음,
        // 화면별 px 하드코딩 아님). position/overflow는 여전히 emit하지 않는다.
        Element resultArea = out.createElementNS(NS_XF, "xf:group");
        resultArea.setAttribute("id", "grp_resultArea");
        resultArea.setAttribute("style", layoutConverter.buildMainAreaStyle(source));
        body.appendChild(resultArea);

        // NESTED_PERCENT_HEIGHT_REINTERPRETATION fix: grp_main은 grp_resultArea(Form 선언
        // height 고정)와 달리 실제 authored content extent를 height로 사용한다(아래
        // buildMainContentAreaStyle -- VERTICAL_CONTAINER_PERCENT_NESTING = DISALLOWED 원칙).
        // convertLayoutAsTable의 root Layout basisHeight 산정도 동일 값을 공유하므로(같은
        // resolveContentExtentHeight 재사용), 여기서 emit하는 height와 그 아래 percentage
        // 자식들의 분모가 항상 일치한다.
        Element main = out.createElementNS(NS_XF, "xf:group");
        main.setAttribute("id", "grp_main");
        main.setAttribute("style", layoutConverter.buildMainContentAreaStyle(source));
        resultArea.appendChild(main);
        registerFormRootMapping(source);

        // STUDIO_DESIGN_FAILED root cause: source content가 Form 바로 아래(Layouts/Layout
        // wrapper 없이) 있거나 최상위 Layout에 width/height가 없는 실제 업무 화면이 있다 --
        // 초기 basis를 -1(unresolved)로 고정하면 그런 화면은 첫 Layout을 만나기 전까지(또는
        // 영원히) 전부 PIXEL_GEOMETRY_FALLBACK으로 떨어진다. Form 자신의 선언 geometry를
        // 초기 basis로 사용해(findFormGeometry 재사용, 화면별 하드코딩 없음), 첫 Layout을 만나면
        // 그 Layout의 basis로 다시 갱신된다(기존 동작 그대로).
        double[] formBasis = layoutConverter.resolveFormBasis(source);
        double initialBasisWidth = formBasis == null ? -1.0 : formBasis[0];
        double initialBasisHeight = formBasis == null ? -1.0 : formBasis[1];

        Element sourceRoot = source.getDocumentElement();
        convertChildren(
                out,
                sourceRoot,
                main,
                "",
                analysis,
                0,
                null,
                initialBasisWidth,
                initialBasisHeight,
                true);

        finalizePageLoadBinding(body);
        logUnmappedEventBindings(analysis);
        System.out.println(
                "[UI 변환 완료] component count=" + componentIdMap.size());
    }

    /**
     * [WebSquareGenerator] convertChildren -- percent-geometry basis 파라미터 추가(basisWidth/
     * basisHeight/includePosition). onlyChild가 null이면 sourceParent의 모든 element 자식을
     * 순회한다. onlyChild가 non-null이면 그 특정 자식 하나만 처리한다 -- Layout -> Table 구조
     * 변환(convertLayoutAsTable)에서 이미 row/column으로 분류된 셀 하나를 targetParent 계층 안의
     * 정확한 위치에 배치하기 위해, 이 메서드의 나머지 로직(mapped-component 생성, container 재귀,
     * pass-through 재귀 등)을 전혀 수정하지 않고 그대로 재사용하는 용도다.
     *
     * <p>basisWidth/basisHeight는 {@code PERCENT_GEOMETRY_PARENT = IMMEDIATE_SOURCE_CONTAINER}
     * 원칙에 따라 항상 "현재 순회 중인 자식들을 감싸는 가장 가까운 XPlatform Layout 자신의
     * width/height"다. Div/Layouts/FDL/Form 등 pass-through 재귀에서는 이 basis를 그대로
     * 전달하고(그 경계 자체는 좌표계를 바꾸지 않음), 새 {@code Layout}을 만났을 때만
     * (convertLayoutAsTable 내부에서) 그 Layout 자신의 geometry로 basis를 갱신한다. 둘 다 <=0
     * 이면(sentinel -1.0) percent 변환은 시도하지 않고 px로 fallback한다.
     */
    private void convertChildren(
            Document out,
            Element sourceParent,
            Element targetParent,
            String parentPath,
            XfdlAnalysisResult analysis,
            int depth,
            Element onlyChild,
            double basisWidth,
            double basisHeight,
            boolean includePosition) {

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
            if (onlyChild != null && src != onlyChild) {
                continue;
            }
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
                convertTab(
                        out, src, targetParent, parentPath, analysis, depth, componentMapping,
                        basisWidth, basisHeight, includePosition);
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
                copyBasicProperties(src, target, basisWidth, basisHeight, includePosition);
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

                // GRID_GROUP_STRUCTURE: XPlatform Grid는 그 자체로 container가 아니라서(위
                // isContainerComponent 재귀 대상이 아님) 여기서 직접 Group wrapper로 감싼다.
                // wrapper id는 row/col wrapper와 동일 원칙으로 synthetic(componentIdMap에는 추가
                // 안 함, usedTargetIds 충돌 방지만)이며, Grid 자신의 sourcePath/targetId(스크립트가
                // 참조하는 실제 id)는 무변경으로 보존한다. Grid 자신의 style은 wrapper가 위치를
                // 담당하므로 100% fill로 대체한다.
                if ("w2:gridView".equals(targetTag)) {
                    Element gridWrapper = out.createElementNS(NS_XF, "xf:group");
                    String wrapperId = createUniqueTargetId(buildSourcePath(sourcePath, "gridGroup"));
                    gridWrapper.setAttribute("id", wrapperId);
                    String wrapperStyle = layoutConverter.hasGeometry(src)
                            ? ((basisWidth > 0.0 && basisHeight > 0.0)
                                    ? layoutConverter.buildPercentComponentStyle(
                                            src, basisWidth, basisHeight, true)
                                    : null)
                            : "";
                    if (wrapperStyle == null) {
                        wrapperStyle = layoutConverter.buildComponentStyle(src, true);
                        System.out.println(
                                "[UI PERCENT] UNRESOLVED(px fallback, Grid Group) id=" + wrapperId);
                    } else if (wrapperStyle.length() > 0) {
                        System.out.println(
                                "[UI PERCENT] 적용 id=" + wrapperId + " style=" + wrapperStyle);
                    }
                    gridWrapper.setAttribute("style", sanitizeXml10(wrapperStyle));
                    target.setAttribute(
                            "style",
                            "width:" + layoutConverter.formatPercent(100.0)
                                    + ";height:" + layoutConverter.formatPercent(100.0) + ";");
                    gridWrapper.appendChild(target);
                    targetParent.appendChild(gridWrapper);
                    System.out.println(
                            "[UI GRID GROUP] " + sourcePath + " -> Group id=" + wrapperId
                                    + " -> w2:gridView id=" + targetId);
                } else {
                    targetParent.appendChild(target);
                }
                System.out.println(
                        "[UI 변환] " + sourceTag + " " + sourcePath
                                + " -> " + targetTag + " id=" + targetId);

                if (isContainerComponent(sourceTag)) {
                    // COMPONENT_CLIPPING fix: Div/GroupBox/PopupDiv/Tab/Tabpage 같은 container의
                    // 직계 자식이 자기 내부 Layouts/Layout으로 다시 감싸여 있지 않은 경우(예:
                    // GroupBox가 Edit을 직접 자식으로 가짐), 그 자식들은 이 container 자신의
                    // width/height를 기준(PERCENT_GEOMETRY_PARENT = SOURCE_IMMEDIATE_CONTAINER)
                    // 으로 삼아야 한다 -- 이전에는 container를 감싸던 바깥 Layout의 basis를 그대로
                    // 물려받아, container 자신보다 basis가 커서 자식이 실제보다 작게 계산되고
                    // (Calendar/Combo 등 native 위젯의 최소 렌더링 크기보다 작아져) clipping으로
                    // 보이는 문제가 있었다. container에 자기 width/height가 없으면(예: 위치만
                    // 있고 크기가 없는 특수 케이스) 기존처럼 물려받은 basis를 그대로 쓴다. 자식이
                    // 실제로 내부 Layout을 갖는 경우(Div의 일반적 구조)는 convertLayoutAsTable이
                    // 그 Layout 자신의 geometry로 다시 basis를 갱신하므로 이 값과 무관하게 정확하다.
                    double[] ownBasis = layoutConverter.resolveLayoutBasis(src);
                    double childBasisWidth = ownBasis != null ? ownBasis[0] : basisWidth;
                    double childBasisHeight = ownBasis != null ? ownBasis[1] : basisHeight;
                    convertChildren(
                            out,
                            src,
                            target,
                            sourcePath,
                            analysis,
                            depth + 1,
                            null,
                            childBasisWidth,
                            childBasisHeight,
                            true);
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
                if ("Layout".equals(sourceTag)) {
                    convertLayoutAsTable(
                            out, src, targetParent, parentPath, analysis, depth + 1,
                            basisWidth, basisHeight);
                } else {
                    convertChildren(
                            out,
                            src,
                            targetParent,
                            parentPath,
                            analysis,
                            depth + 1,
                            null,
                            basisWidth,
                            basisHeight,
                            includePosition);
                }
            }
        }
    }

    /**
     * XPlatform {@code Layout} 직계 자식들이 table topology({@code TABLE_LAYOUT_HIGH_CONFIDENCE})
     * 로 판정되는 경우 row/column {@code xf:group} 구조를 생성한다. 겹침 등으로 계산이 불가능한
     * 경우({@code ABSOLUTE_LAYOUT_FALLBACK}/{@code UNRESOLVED_LAYOUT})만 flat pass-through로
     * 처리한다({@code Layout} 자체는 target element 없이 targetParent 아래 자식들을 직접 배치).
     * v6 Design Structure + Table + Grid Group + Percentage Geometry Alignment 라운드부터는
     * 1-row/1-column Layout(검색조건/버튼 바 등)도 table 대상이다(14번 규칙, 이전 라운드의
     * row&gt;=2/column&gt;=2 요건 제거).
     *
     * <p>row/column wrapper는 XPlatform source component가 아니므로 componentIdMap에 새 키를
     * 추가하지 않는다({@code usedTargetIds} 등록(충돌 방지)만 발생 -- grp_resultArea/grp_main과
     * 동일한 원칙). 실제 셀 안의 컴포넌트는 원래 sourcePath({@code parentPath} 그대로)를
     * 유지하며, {@link #convertChildren}의 mapped-component 처리 로직을 완전히 무수정으로
     * 재사용한다(onlyChild 필터).
     *
     * <p>이 Layout 자신의 width/height가 이 Layout 직계 자식 전체(및 fallback 경로의 하위
     * 재귀)의 percent 기준(basis)이 된다({@code PERCENT_GEOMETRY_PARENT =
     * IMMEDIATE_SOURCE_CONTAINER}). row wrapper의 height%/cell wrapper의 width%도 동일 basis로
     * 계산한다(19번 규칙 -- source 비율 실측, 균등분할 금지). row/cell 내부 실제 component는
     * structural placement로 위치가 이미 결정되므로 left/top/position은 생성하지 않는다
     * (includePosition=false, 20번 규칙).
     *
     * <p>12번 규칙: Table 판단 대상은 Div 내부 Layout이 핵심이며, Form root Layout 전체는 Table
     * 대상이 아니다({@code parentPath}가 비어 있으면 -- 즉 아직 어떤 Div/container도 거치지 않은
     * 최상위 Form Layout이면 -- classification과 무관하게 강제로 flat pass-through). 목표
     * hierarchy(6번 규칙)가 {@code grp_main} 바로 아래 Div Group/Grid Group이 직접 나타나는
     * 것이기 때문에, root Layout 자체를 1-column table로 감싸면 불필요한 추가 wrapper 계층이
     * 생겨 이 목표와 어긋난다. Div 내부에서 다시 Layout을 만나면(parentPath가 그 Div의
     * sourcePath로 비어있지 않음) 정상적으로 Table 판정 대상이 된다.
     *
     * <p>NATIVE_LAYOUT_CONTAINER_SEMANTIC fix(Root Percentage Containing Block Width Fix 후속
     * 라운드): 실제 폐쇄망 v6 정상 화면(BCI01M0000) source 영상 직접 판독 evidence(
     * {@code analysis/evidence-snapshots/native-layout-container-semantic/v6-video-source-analysis.md})
     * 에서 {@code tagname="table" class="w2tb_tb"} > {@code tagname="tr"}(class 없음) >
     * {@code tagname="td" class="w2tb_td"} 구조가 100% 대응으로 관측됐다(th/td 판정용
     * 신뢰 가능한 source 신호는 없어 th는 적용하지 않음 -- 아래 참고). 이 tagname/class는
     * WebSquare 렌더러가 실제 HTML {@code <table>/<tr>/<td>}로 렌더링하는 구조적 신호이며
     * (skin 목적의 CSS class가 아님), 기존에 이미 구현되어 있던 row/column {@code xf:group}
     * 구조(TABLE_LAYOUT_HIGH_CONFIDENCE)에 그대로 부여 가능하다 -- 아래 3곳(table wrapper 신규
     * 생성/row/cell)에서만 속성을 추가하며, row/cell의 위치·크기 계산(percentage geometry)
     * 로직은 전혀 건드리지 않는다. th(header) vs td(data) 구분은 이 corpus의 실제
     * TABLE_LAYOUT_HIGH_CONFIDENCE 사례 중 label/input 쌍이 아닌 경우(예: 단일 Tab 컴포넌트를
     * 담은 cell)가 존재해 안전하게 일반화할 수 없으므로(evidence 부족), 모든 cell을 동일하게
     * {@code td}/{@code w2tb_td}로만 표시한다(th는 미적용, UNRESOLVED로 유지).
     *
     * <p>XPLATFORM_VISUAL_PARITY(Quick Fix) 라운드: 실제 폐쇄망 Studio 재현에서 Table 변환이
     * container child뿐 아니라 leaf-only Layout(Button 2개가 나란한 검색조건 바 등)에도
     * 적용되면서 균등폭 강제 분할/Calendar·Combo 비노출 등 광범위한 렌더링 실패가 재현됐다
     * ({@code GENERAL_LAYOUT_TABLE_HEURISTIC = PAUSED_FOR_VISUAL_PARITY}). 이전 라운드의
     * container-only 예외({@link #hasContainerChild})로는 leaf-only 케이스를 못 막으므로,
     * 이번 라운드는 root가 아닌 모든 Layout을 일괄적으로 table 미변환(절대좌표 pass-through)
     * 대상으로 둔다. table 생성 코드 자체는 삭제하지 않고 아래 {@code PAUSED} 상수로만
     * 우회한다(원복 시 상수만 되돌리면 됨).
     *
     * <p>NESTED_VERTICAL_PERCENT_DOUBLE_SCALING fix: 이 Layout 자신에게 width/height가 없으면
     * (드물지 않은 실제 XFDL 패턴 -- Div가 자식을 감싸는 내부 Layout에 크기를 따로 선언하지
     * 않는 경우) 예전에는 곧바로 Form 전체 크기로 fallback했다. Div 내부에 중첩된 Layout이면
     * 이는 "root(Form) 기준" basis를 쓰는 것과 같아, 그 Div 자신은 이미 부모 대비 올바른
     * 비율(예: 5.3%)로 배치돼 있는데 그 안의 자식은 Div가 아니라 Form 전체를 기준으로 다시
     * 계산되어(예: 3.8%) 실제 렌더링에서 두 비율이 곱해진 것처럼 극단적으로 축소되는 현상이
     * 재현됐다. 이제는 Form까지 건너뛰지 않고, 호출자(convertChildren)가 이미 올바르게
     * 계산해 둔 {@code inheritedBasisWidth}/{@code inheritedBasisHeight}(이 Layout을 실제로
     * 감싸고 있는 가장 가까운 container의 크기)를 우선 사용한다. 호출자 basis도 없는
     * 경우(최상위 Form Layout 자신에게도 width/height가 없는 극단적 케이스)에만 Form 자신의
     * 선언 geometry로 최종 fallback한다.
     */
    private static final boolean GENERAL_LAYOUT_TABLE_HEURISTIC_PAUSED = true;

    private void convertLayoutAsTable(
            Document out,
            Element layout,
            Element targetParent,
            String parentPath,
            XfdlAnalysisResult analysis,
            int depth,
            double inheritedBasisWidth,
            double inheritedBasisHeight) {

        List<Element> children = directElementChildren(layout);
        boolean isRootFormLayout = parentPath.length() == 0;
        String classification;
        if (isRootFormLayout) {
            classification = "ROOT_FORM_LAYOUT_NOT_A_TABLE_TARGET";
        } else if (GENERAL_LAYOUT_TABLE_HEURISTIC_PAUSED) {
            classification = "GENERAL_LAYOUT_TABLE_HEURISTIC_PAUSED_FOR_VISUAL_PARITY";
        } else {
            classification = layoutConverter.classifyLayoutGeometry(children);
            // XPLATFORM_VISUAL_PARITY 라운드: Div/GroupBox/PopupDiv/Tab/Tabpage처럼 그 자체로
            // 독립된 좌표계를 가진 container child는 table row/cell 구조(structural placement,
            // position 제거)로 병합하지 않는다(TABLE_CONVERSION_SEMANTIC_MISMATCH). 현재는
            // 위 PAUSED 분기가 우선하므로 이 판정은 실행되지 않지만, heuristic을 다시 켜는
            // 경우를 위해 로직은 보존한다.
            if ("TABLE_LAYOUT_HIGH_CONFIDENCE".equals(classification) && hasContainerChild(children)) {
                classification = "TABLE_CONVERSION_SEMANTIC_MISMATCH";
            }
        }
        double[] basis = layoutConverter.resolveLayoutBasis(layout);
        if (basis == null) {
            // 이 Layout 자신에게 width/height가 없으면, Form까지 건너뛰지 않고 이 Layout을
            // 실제로 감싸고 있는 가장 가까운 container의 basis(호출자가 이미 계산해 둔 값)를
            // 먼저 물려받는다(NESTED_VERTICAL_PERCENT_DOUBLE_SCALING fix). 호출자 basis도
            // 없으면(최상위 Form Layout 자신에게도 width/height가 없는 극단적 경우) Form
            // 자신의 선언 geometry로 최종 fallback한다(화면별 하드코딩 없음).
            if (inheritedBasisWidth > 0.0 && inheritedBasisHeight > 0.0) {
                basis = new double[] {inheritedBasisWidth, inheritedBasisHeight};
            } else {
                basis = layoutConverter.resolveFormBasis(layout.getOwnerDocument());
            }
        }
        double basisWidth = basis == null ? -1.0 : basis[0];
        double basisHeight = basis == null ? -1.0 : basis[1];
        // NESTED_PERCENT_HEIGHT_REINTERPRETATION fix: 최상위 Form Layout은 grp_main의 height를
        // 더 이상 Form 선언 height 그대로 쓰지 않고 실제 authored content extent(children의
        // max(top+height))로 산정한다(appendBody의 grp_main style도 동일 값을 사용 --
        // resolveContentExtentHeight 하나만 공유). children의 percentage basis도 반드시 이
        // 값과 일치해야 grp_main의 실제 렌더링 height와 percentage 분모가 어긋나지 않는다
        // (width는 이번 라운드 범위 밖이라 basisWidth는 무변경). content extent가 기존
        // basisHeight보다 작을 때만 축소 적용한다(더 크게 만들지 않음 -- SOURCE_INTENTIONAL_
        // OVERFLOW 케이스를 억지로 줄이지 않기 위함).
        if (isRootFormLayout) {
            double contentExtentHeight = layoutConverter.resolveContentExtentHeight(children);
            if (contentExtentHeight > 0.0 && (basisHeight <= 0.0 || contentExtentHeight < basisHeight)) {
                basisHeight = contentExtentHeight;
            }
        }
        System.out.println(
                "[UI TABLE] Layout " + (parentPath.length() == 0 ? "(root)" : parentPath)
                        + " children=" + children.size() + " classification=" + classification
                        + " basisWidth=" + basisWidth + " basisHeight=" + basisHeight);

        if (!"TABLE_LAYOUT_HIGH_CONFIDENCE".equals(classification)) {
            convertChildren(
                    out, layout, targetParent, parentPath, analysis, depth, null,
                    basisWidth, basisHeight, true);
            return;
        }

        List<List<Element>> rows = layoutConverter.buildTableRows(children);

        Element tableWrapper = out.createElementNS(NS_XF, "xf:group");
        String tableTargetId = createUniqueTargetId(buildSourcePath(parentPath, "layoutTable"));
        tableWrapper.setAttribute("id", tableTargetId);
        tableWrapper.setAttribute("tagname", "table");
        tableWrapper.setAttribute("class", "w2tb_tb");
        tableWrapper.setAttribute("style", "width:" + layoutConverter.formatPercent(100.0) + ";");
        targetParent.appendChild(tableWrapper);

        int rowIndex = 0;
        for (List<Element> row : rows) {
            Element rowGroup = out.createElementNS(NS_XF, "xf:group");
            String rowTargetId = createUniqueTargetId(
                    buildSourcePath(parentPath, "layoutTableRow" + rowIndex));
            rowGroup.setAttribute("id", rowTargetId);
            rowGroup.setAttribute("tagname", "tr");
            String rowStyle = layoutConverter.buildTableRowStyle(row, basisHeight);
            if (rowStyle != null) {
                rowGroup.setAttribute("style", rowStyle);
                System.out.println("[UI PERCENT] 적용 id=" + rowTargetId + " style=" + rowStyle);
            } else {
                System.out.println("[UI PERCENT] UNRESOLVED(no style, row) id=" + rowTargetId);
            }
            tableWrapper.appendChild(rowGroup);

            // NESTED_PERCENT_DOUBLE_SCALING fix: cell 내부 컴포넌트의 percentage는 원래
            // Div/Layout basis(basisWidth/basisHeight)가 아니라, 그 컴포넌트를 담기 위해 이미
            // 그 컴포넌트 자신의 geometry로 계산된 cell/row 자신의 크기(px)를 기준으로 다시
            // 계산해야 한다 -- 그렇지 않으면 "cell width% (basis 기준) x child width% (같은
            // basis 기준)"이 이중으로 곱해져 실제 렌더링 폭/높이가 제곱으로 축소된다(실제 폐쇄망
            // Studio 재현: cell width:6.0345%, child width:6.0345% -> 렌더링 실효 폭 약 0.36%).
            // resolveRowBasisHeight/resolveCellBasisWidth는 buildTableRowStyle/
            // buildTableCellStyle과 완전히 동일한 px 계산을 재사용하므로, 계산 불가 시 null을
            // 반환한 케이스와도 항상 일관된다.
            double rowBasisHeightPx = layoutConverter.resolveRowBasisHeight(row);

            int colIndex = 0;
            for (Element cell : row) {
                Element cellGroup = out.createElementNS(NS_XF, "xf:group");
                String cellTargetId = createUniqueTargetId(
                        buildSourcePath(parentPath, "layoutTableRow" + rowIndex + "Col" + colIndex));
                cellGroup.setAttribute("id", cellTargetId);
                cellGroup.setAttribute("tagname", "td");
                cellGroup.setAttribute("class", "w2tb_td");
                String cellStyle = layoutConverter.buildTableCellStyle(cell, basisWidth);
                if (cellStyle != null) {
                    cellGroup.setAttribute("style", cellStyle);
                    System.out.println("[UI PERCENT] 적용 id=" + cellTargetId + " style=" + cellStyle);
                } else {
                    System.out.println("[UI PERCENT] UNRESOLVED(no style, cell) id=" + cellTargetId);
                }
                rowGroup.appendChild(cellGroup);

                double cellBasisWidthPx = layoutConverter.resolveCellBasisWidth(cell);
                double childBasisWidth = cellBasisWidthPx > 0.0 ? cellBasisWidthPx : basisWidth;
                double childBasisHeight = rowBasisHeightPx > 0.0 ? rowBasisHeightPx : basisHeight;
                convertChildren(
                        out, layout, cellGroup, parentPath, analysis, depth, cell,
                        childBasisWidth, childBasisHeight, false);
                colIndex++;
            }
            rowIndex++;
        }

        System.out.println(
                "[UI TABLE] Layout " + (parentPath.length() == 0 ? "(root)" : parentPath)
                        + " -> table rows=" + rows.size());
    }

    /** children 중 하나라도 container 컴포넌트(Div/GroupBox/PopupDiv/Tab/Tabpage 등)인지 확인. */
    private boolean hasContainerChild(List<Element> children) {
        for (Element child : children) {
            if (isContainerComponent(getSourceTagName(child))) {
                return true;
            }
        }
        return false;
    }

    private List<Element> directElementChildren(Element parent) {
        List<Element> result = new ArrayList<Element>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                result.add((Element) node);
            }
        }
        return result;
    }

    /** Phase 3: XPlatform Tab/Tabpage tree -> WebSquare tabControl tabs/content pairs. */
    private void convertTab(
            Document out,
            Element src,
            Element targetParent,
            String parentPath,
            XfdlAnalysisResult analysis,
            int depth,
            ComponentMapping componentMapping,
            double basisWidth,
            double basisHeight,
            boolean includePosition) {

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
        // 외부 URL page는 w2:content 단위로 alwaysDraw를 override해 preload semantic 보존.
        tabControl.setAttribute("alwaysDraw", "true");
        logPartialComponentMapping(componentMapping, sourcePath);
        copyBasicProperties(src, tabControl, basisWidth, basisHeight, includePosition);
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
            content.setAttribute(
                    "style",
                    "position:relative;width:" + layoutConverter.formatPercent(100.0)
                            + ";height:" + layoutConverter.formatPercent(100.0) + ";");
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
                // 런타임 set_url() 대상: frameMode=wframe 없으면 실제 WFrame이 안 돼 setSrc 불가.
                content.setAttribute("frameMode", "wframe");
                content.setAttribute("scope", "true");
                System.out.println("[TAB CONTENT] " + pagePath + " runtime set_url target -> frameMode=wframe placeholder");
            }
            // Tabpage 내부는 독립 scope(별도 Frame)이므로 percent basis를 상속하지 않고
            // fresh하게 시작한다(appendBody의 최초 진입과 동일 원칙) -- page 자신의 Layout을
            // 만나는 시점에 convertLayoutAsTable이 새 basis를 다시 계산한다.
            convertChildren(out, page, content, pagePath, analysis, depth + 1, null, -1.0, -1.0, true);
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

    /**
     * [WebSquareGenerator] copyBasicProperties -- 기존 오버로드, px/position 항상 포함(basis
     * 정보가 없는 호출부용, 동작 무변경).
     */
    private void copyBasicProperties(Element src, Element target) {
        copyBasicProperties(src, target, -1.0, -1.0, true);
    }

    /**
     * [WebSquareGenerator] copyBasicProperties -- 신규 오버로드. PERCENT_GEOMETRY_PARENT =
     * IMMEDIATE_SOURCE_CONTAINER 원칙에 따라 basisWidth/basisHeight(둘 다 양수일 때만 유효)가
     * 있으면 percentage style을 우선 시도하고, 계산 불가(PERCENT_GEOMETRY_UNRESOLVED)면 px로
     * fallback한다(PIXEL_GEOMETRY_FALLBACK). includePosition=false면 Table 셀 내부처럼 structural
     * placement가 이미 위치를 결정하는 경우로, percent/px 어느 경로든 position/left/top을 생성하지
     * 않는다(20번 규칙).
     */
    private void copyBasicProperties(
            Element src, Element target, double basisWidth, double basisHeight, boolean includePosition) {
        String text = sanitizeXml10(src.getAttribute("text"));
        if (text.length() == 0) {
            text = sanitizeXml10(src.getAttribute("value"));
        }
        if (text.length() > 0) {
            // 실제 엔진은 정적 "value"를 xf:trigger에서만 렌더링 -- data 위젯은 전용 속성 필요:
            // w2:span -> label, xf:input -> initValue. 그 외 태그는 "value" 유지(xf:trigger는
            // 정상 렌더링, textarea/calendar/progressbar는 static-value 속성 자체가 없어 범위 밖).
            String targetTag = target.getTagName();
            if ("w2:span".equals(targetTag)) {
                target.setAttribute("label", text);
            } else if ("xf:input".equals(targetTag)) {
                target.setAttribute("initValue", text);
            } else if ("w2:checkbox".equals(targetTag)) {
                // 실제 w2:checkbox는 정적 value/label 속성을 렌더링하지 않고 빈 shell만 생성 --
                // 실제 input/label은 addItem(value,label) API 호출로만 생성됨(엔진 실측 확인).
                // 선언적 XML 대안이 없어 page-init bootstrap(BIND-1 setRowPosition과 동일 채널)
                // 으로 addItem 호출을 내보낸다. XPlatform "value"->item value, "text"->label.
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

        String targetId = target.getAttribute("id");
        String style;
        if (layoutConverter.hasGeometry(src)) {
            String percentStyle = (basisWidth > 0.0 && basisHeight > 0.0)
                    ? layoutConverter.buildPercentComponentStyle(src, basisWidth, basisHeight, includePosition)
                    : null;
            if (percentStyle != null) {
                style = percentStyle;
                System.out.println("[UI PERCENT] 적용 id=" + targetId + " style=" + percentStyle);
            } else {
                style = layoutConverter.buildComponentStyle(src, includePosition);
                System.out.println("[UI PERCENT] UNRESOLVED(px fallback) id=" + targetId
                        + " basisWidth=" + basisWidth + " basisHeight=" + basisHeight);
            }
        } else {
            style = layoutConverter.buildComponentStyle(src, includePosition);
        }
        style = sanitizeXml10(style);
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

    /**
     * TARGET_STATE_CLASS_POLICY: v6 실제 폐쇄망 화면 영상 판독으로 확인된, target QName(+구분이
     * 필요한 경우 appearance 같은 보조 attribute)만으로 결정되는 고정 disabledClass 값
     * (component-intrinsic 값 -- 개별 인스턴스의 실제 disabled/enable state와 무관하게 항상
     * 선언됨, {@link #resolveVideoEvidenceBaseClass}와 같은 evidence 출처 및 원칙을 공유하는
     * 자매 policy 함수). 관측된 xf:select1(appearance=minimal) 3/3 전부 disabledClass=
     * "w2selectbox_disabled"를 가짐 -- 상세: analysis/v6-video-source-analysis.md. Radio
     * (appearance=full)는 이번 evidence에 없어 매핑하지 않는다(HOLD_INSUFFICIENT_EVIDENCE, null
     * 반환). 다른 target QName/appearance 조합도 evidence가 없으면 null(호출부가 attribute를
     * emit하지 않음).
     */
    private String resolveVideoEvidenceDisabledClass(String targetTag, String appearance) {
        if ("xf:select1".equals(targetTag) && "minimal".equals(appearance)) {
            return "w2selectbox_disabled";
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

    /**
     * TARGET_RENDER_TYPE_POLICY: 실제 폐쇄망 devpack에 배포된 업무 화면(websquare-devpack-copy/
     * tomcat/webapps/ROOT/ui/BM,HM,SP/*.xml, XPlatform 변환물이 아닌 순수 v6 native 화면) 전수
     * 조사 결과, xf:select1 appearance="full"(Radio 계열)은 7/7(100%) 전부 renderType=
     * "radiogroup"을 가짐(예외 0건) -- 상세: analysis/radio-rendertype-evidence.md. 이
     * attribute가 없으면 실제 WebSquare 엔진이 select1을 item 단위 radio-button-group으로
     * 렌더링하지 않는다(Combo의 dropdown shell과 달리 radio는 각 item이 렌더링 단위라, item이
     * 없거나 renderType이 없으면 위젯 자체가 그려지지 않는 것으로 추정 -- 실제 Studio
     * design-time 재현은 폐쇄망에서 사용자가 최종 확인). appearance="minimal"(Combo)은 같은
     * corpus에서 renderType이 47건 중 3건(6%)만 명시적이고 나머지는 생략돼도 실사용에
     * 문제없어 보이므로 매핑하지 않는다(evidence 부족, HOLD).
     */
    private String resolveTargetRenderType(String targetTag, String appearance) {
        if ("xf:select1".equals(targetTag) && "full".equals(appearance)) {
            return "radiogroup";
        }
        return null;
    }

    private void applyComponentSpecificProperties(Element src, Element target, String sourceTag, String sourcePath) {
        if ("Radio".equals(sourceTag)) {
            // xf:select1 appearance=full renders the radio-style selection family.
            String appearance = "full";
            target.setAttribute("appearance", appearance);
            String renderType = resolveTargetRenderType(target.getTagName(), appearance);
            if (renderType != null) {
                target.setAttribute("renderType", renderType);
            }
        } else if ("Combo".equals(sourceTag)) {
            String appearance = "minimal";
            target.setAttribute("appearance", appearance);
            // TARGET_STATE_CLASS_POLICY: sourceTag("Combo") 자체에 문자열을 하드코딩하지 않고,
            // 방금 결정한 target QName+appearance를 resolveVideoEvidenceDisabledClass(evidence
            // 기반 policy 함수, resolveVideoEvidenceBaseClass의 자매 함수)에 넘겨 결정한다 --
            // 같은 QName+appearance 조합이면 어떤 source component/화면에서 오든 항상 같은
            // 결과가 나오는 generic 정책이다.
            String disabledClass = resolveVideoEvidenceDisabledClass(target.getTagName(), appearance);
            if (disabledClass != null) {
                target.setAttribute("disabledClass", disabledClass);
            }
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

    /**
     * [WebSquareGenerator] registerFormRootMapping -- EXPECTED_SOURCE_TO_TARGET_MAP_DIFF: global
     * grp_content wrapper 제거에 맞춰 Form root mapping을 grp_content에서 grp_main으로
     * migration했다(다른 component mapping은 무변경). TabRuntimeScriptGenerator의
     * component('grp_main').getScope()/w.grp_main도 동일 id로 함께 변경됨(id-string 기반 lookup
     * 방식 자체는 무변경).
     */
    private void registerFormRootMapping(Document source) {
        List<Element> forms = findDescendants(source.getDocumentElement(), "Form");
        if (forms.isEmpty()) return;
        String formId = canonicalizePath(sanitizeXml10(forms.get(0).getAttribute("id")));
        if (formId.length() > 0 && !componentIdMap.containsKey(formId)) {
            componentIdMap.put(formId, "grp_main");
            targetComponentTypeMap.put("grp_main", "Form");
            System.out.println("[UI 매핑] Form " + formId + " -> grp_main (lifecycle obj 호환)");
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

            // scalar ref는 w2:dataList의 row cursor가 설정돼야 실제로 값이 보임(기본 unset).
            // XPlatform BindItem엔 row 개념이 없어 0번 row를 안전한 기본값으로 사용, dataset당
            // 1회만 bootstrap.
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
