package com.example.xfdltracker.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PropertyMappingRegistry {
    private static final Map<String, PropertyMapping> MAP = new LinkedHashMap<String, PropertyMapping>();
    static {
        direct("id", "id");
        computed("left", "style.left", "layout geometry");
        computed("top", "style.top", "layout geometry");
        computed("right", "style.width", "requires geometry calculation");
        computed("bottom", "style.height", "requires geometry calculation");
        computed("width", "style.width", "layout geometry");
        computed("height", "style.height", "layout geometry");
        direct("visible", "visible");
        transform("enable", "disabled", "boolean inversion");
        direct("readonly", "readOnly");
        direct("taborder", "tabIndex");
        direct("tooltiptext", "title");
        transform("text", "value", "component-specific value/text semantics");
        direct("value", "value");
        direct("cssclass", "class");
        todo("style", "XPlatform composite style syntax requires semantic parsing");
        todo("font", "XPlatform font syntax is not copied as CSS without parsing");
        computed("color", "style.color", "CSS conversion");
        computed("background", "style.background", "CSS conversion");
        todo("border", "XPlatform border syntax is not copied as CSS without parsing");
        computed("padding", "style.padding", "CSS conversion");
        computed("opacity", "style.opacity", "CSS conversion");
        transform("cursor", "style.cursor", "copied only for CSS-safe cursor keywords");
        direct("maxlength", "maxLength");
        direct("displaynulltext", "placeholder");
        transform("password", "xf:secret", "Edit password=true changes component type");
        transform("index", "selectedTabIndex/getSelectedIndex", "component-specific selected index");
        transform("tabindex", "selectedTabIndex", "Tab selected page index");
        todo("inputtype", "input validation/input mode differs by component");
        todo("autoselect", "focus/selection behavior requires component-specific review");
        todo("usecontextmenu", "browser/context-menu policy differs");
        todo("image", "XPlatform URL/service alias must be resolved before WebSquare src mapping");
        todo("url", "component-specific URL semantics; Tabpage static XFDL URL is handled by TabContentResolver");
        transform("preload", "content.alwaysDraw", "Tab external XFDL eager/lazy loading policy");
        todo("async", "Tabpage URL load completion/callback timing requires runtime-specific review");
        todo("format", "MaskEdit/Calendar format semantics are component-specific");
        transform("innerdataset", "itemset.nodeset", "dataset itemset binding");
        transform("codecolumn", "itemset.value", "dataset itemset binding");
        transform("datacolumn", "itemset.label", "dataset itemset binding");
        direct("dateformat", "displayFormat");
        todo("editformat", "inputCalendar 편집 포맷 의미 차이: 자동 dataFormat 복사 금지");
        script("expr", "", "dynamic expression requires script/grid-specific conversion");
        transform("binddataset", "dataList", "Grid/DataList binding");
        todo("scrollbars", "container scroll behavior differs");
        todo("autosizing", "component/grid specific");
        todo("keystring", "DataList sorting/grouping semantics differ");
    }
    private static void direct(String s, String t) { add(s,t,MappingKind.DIRECT,""); }
    private static void transform(String s, String t, String n) { add(s,t,MappingKind.TRANSFORM,n); }
    private static void computed(String s, String t, String n) { add(s,t,MappingKind.COMPUTED,n); }
    private static void script(String s, String t, String n) { add(s,t,MappingKind.SCRIPT_REQUIRED,n); }
    private static void todo(String s, String n) { add(s,"",MappingKind.TODO,n); }
    private static void add(String s, String t, MappingKind k, String n) { MAP.put(s, new PropertyMapping(s,t,k,n)); }
    public PropertyMapping get(String source) { return MAP.get(source == null ? null : source.toLowerCase()); }
    public List<PropertyMapping> all() { return Collections.unmodifiableList(new ArrayList<PropertyMapping>(MAP.values())); }
}
