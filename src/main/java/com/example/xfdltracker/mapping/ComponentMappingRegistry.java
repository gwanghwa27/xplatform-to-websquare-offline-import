package com.example.xfdltracker.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Phase 3 component mapping inventory. Conservative mappings only become generated XML. */
public class ComponentMappingRegistry {
    private static final Map<String, ComponentMapping> MAP = new LinkedHashMap<String, ComponentMapping>();

    static {
        add("Button", "xf:trigger", SupportLevel.SUPPORTED, false, "Phase 1/2 baseline");
        add("Static", "w2:span", SupportLevel.SUPPORTED, false, "Phase 1/2 baseline");
        add("Edit", "xf:input", SupportLevel.SUPPORTED, false, "Phase 1/2 baseline");
        add("MaskEdit", "xf:input", SupportLevel.PARTIAL, false, "mask semantics require property/script review");
        add("TextArea", "xf:textarea", SupportLevel.SUPPORTED, false, "Phase 1/2 baseline");
        add("Combo", "xf:select1", SupportLevel.PARTIAL, false, "itemset binding supported in Phase 3");
        add("ListBox", "xf:select1", SupportLevel.PARTIAL, false, "visual list behavior may differ");
        add("Radio", "xf:select1", SupportLevel.PARTIAL, false, "itemset binding supported in Phase 3");
        add("CheckBox", "w2:checkbox", SupportLevel.PARTIAL, false, "single-value checkbox baseline");
        add("Calendar", "w2:inputCalendar", SupportLevel.PARTIAL, false, "date/edit format partially mapped; uiplugin.inputCalendar (edit box + picker), not bare uiplugin.calendar (picker-only) -- see V6_COMPONENT_MAPPING_MISMATCH fix");
        add("Spin", "w2:spinner", SupportLevel.PARTIAL, false, "basic geometry/value only");
        add("Grid", "w2:gridView", SupportLevel.PARTIAL, false, "Formats/head/body/bind and selected input types");
        // Div만 xf:group(Design 렌더링 개선 확인); GroupBox/PopupDiv/Tabpage는 evidence 없어 w2:group 유지.
        add("Div", "xf:group", SupportLevel.SUPPORTED, true, "child coordinate system preserved");
        add("GroupBox", "w2:group", SupportLevel.PARTIAL, true, "group semantics/title require review");
        add("PopupDiv", "w2:group", SupportLevel.PARTIAL, true, "popup runtime behavior requires manual migration");
        add("ImageViewer", "w2:image", SupportLevel.PARTIAL, false, "basic image source/property mapping only");
        add("ProgressBar", "w2:progressbar", SupportLevel.PARTIAL, false, "basic value/property mapping only");
        add("Tab", "w2:tabControl", SupportLevel.PARTIAL, true, "inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review");
        add("Tabpage", "w2:group", SupportLevel.PARTIAL, true, "inline tree preserved; external XFDL remains an independent WFrame content page");
        add("WebBrowser", "w2:wframe", SupportLevel.PARTIAL, false, "URL/navigation semantics require review");
        add("FileUpload", "w2:upload", SupportLevel.PARTIAL, false, "server protocol/manual migration required");
        add("FileDownload", null, SupportLevel.TODO, false, "no safe static UI mapping selected");
        add("Dataset", null, SupportLevel.SUPPORTED, false, "converted to w2:dataList");
        add("DataSet", null, SupportLevel.SUPPORTED, false, "converted to w2:dataList");
        add("Form", null, SupportLevel.PARTIAL, true, "page body/root group + lifecycle mapping");
    }

    private static void add(String source, String target, SupportLevel level, boolean container, String note) {
        MAP.put(source, new ComponentMapping(source, target, level, container, note));
    }

    public ComponentMapping get(String sourceName) { return MAP.get(sourceName); }
    public String getTargetTag(String sourceName) {
        ComponentMapping m = MAP.get(sourceName);
        return m == null ? null : m.getTargetTag();
    }
    public boolean isContainer(String sourceName) {
        ComponentMapping m = MAP.get(sourceName);
        return m != null && m.isContainer();
    }
    public List<ComponentMapping> all() {
        return Collections.unmodifiableList(new ArrayList<ComponentMapping>(MAP.values()));
    }
}
