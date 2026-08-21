package com.example.xfdltracker.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Central Phase 3 inventory for global, Dataset and selected component API mappings. */
public class ScriptApiMappingRegistry {
    private static final Map<String, ApiMapping> GLOBAL = new LinkedHashMap<String, ApiMapping>();
    private static final Map<String, ApiMapping> DATASET = new LinkedHashMap<String, ApiMapping>();
    private static final Map<String, ApiMapping> COMPONENT = new LinkedHashMap<String, ApiMapping>();
    static {
        global("trace", "console.log", MappingKind.TRANSFORM, SupportLevel.SUPPORTED, "direct diagnostic mapping");
        global("alert", "$p.alert", MappingKind.TRANSFORM, SupportLevel.SUPPORTED, "dialog payload should still be reviewed");
        global("confirm", "$p.confirm", MappingKind.TRANSFORM, SupportLevel.PARTIAL, "return/async behavior differs by WebSquare version");
        global("transaction", "scwin.xpTransaction", MappingKind.SCRIPT_REQUIRED, SupportLevel.PARTIAL, "structured transaction report; submission manual migration");
        global("open", "$p.openPopup", MappingKind.SCRIPT_REQUIRED, SupportLevel.PARTIAL, "argument model differs; not auto-rewritten");
        global("close", "", MappingKind.SCRIPT_REQUIRED, SupportLevel.TODO, "frame/popup ownership required");
        global("showModal", "$p.openPopup", MappingKind.SCRIPT_REQUIRED, SupportLevel.PARTIAL, "modal option/callback conversion required");
        global("setTimer", "", MappingKind.SCRIPT_REQUIRED, SupportLevel.TODO, "timer owner/lifecycle wrapper required");
        global("killTimer", "", MappingKind.SCRIPT_REQUIRED, SupportLevel.TODO, "timer owner/lifecycle wrapper required");
        global("getEnvironmentVariable", "", MappingKind.SCRIPT_REQUIRED, SupportLevel.TODO, "project environment mapping required");
        global("setEnvironmentVariable", "", MappingKind.SCRIPT_REQUIRED, SupportLevel.TODO, "project environment mapping required");
        global("application", "", MappingKind.SCRIPT_REQUIRED, SupportLevel.TODO, "application object requires project wrapper");
        global("system", "", MappingKind.UNSUPPORTED, SupportLevel.TODO, "browser security/runtime semantics differ");
        global("event", "e", MappingKind.TRANSFORM, SupportLevel.PARTIAL, "event object payload differs");

        dataset("addRow", "scwin.xpAddDataListRow", MappingKind.TRANSFORM, SupportLevel.SUPPORTED, "preserves returned row index");
        dataset("getColumn", "getCellData", MappingKind.TRANSFORM, SupportLevel.SUPPORTED, "row,column order preserved");
        dataset("setColumn", "setCellData", MappingKind.TRANSFORM, SupportLevel.SUPPORTED, "row,column,value order preserved");
        dataset("getRowCount", "getRowCount", MappingKind.DIRECT, SupportLevel.SUPPORTED, "same semantic family");
        dataset("clearData", "removeAll", MappingKind.TRANSFORM, SupportLevel.PARTIAL, "row status/event semantics require review");
        dataset("deleteRow", "", MappingKind.SCRIPT_REQUIRED, SupportLevel.TODO, "delete/remove status semantics differ");
        dataset("copyData", "", MappingKind.SCRIPT_REQUIRED, SupportLevel.TODO, "source/target and status semantics differ");
        dataset("filter", "", MappingKind.SCRIPT_REQUIRED, SupportLevel.TODO, "filter expression grammar differs");
        dataset("findRow", "", MappingKind.SCRIPT_REQUIRED, SupportLevel.TODO, "argument/return semantics require helper");
        dataset("rowposition", "getRowPosition/setRowPosition", MappingKind.TRANSFORM, SupportLevel.SUPPORTED, "getter/setter conversion");

        component("value", "getValue/setValue", MappingKind.TRANSFORM, SupportLevel.SUPPORTED, "read/write");
        component("text", "getValue/setValue", MappingKind.TRANSFORM, SupportLevel.PARTIAL, "label/text semantics component-specific");
        component("enable", "getDisabled/setDisabled", MappingKind.TRANSFORM, SupportLevel.SUPPORTED, "boolean inversion");
        component("readonly", "getReadOnly/setReadOnly", MappingKind.TRANSFORM, SupportLevel.SUPPORTED, "read/write");
        component("index", "getSelectedIndex/setSelectedIndex", MappingKind.TRANSFORM, SupportLevel.PARTIAL, "select/radio only");
        component("getCellProperty", "", MappingKind.SCRIPT_REQUIRED, SupportLevel.TODO, "GridView property API is not 1:1");
        component("setCellProperty", "", MappingKind.SCRIPT_REQUIRED, SupportLevel.TODO, "GridView property API is not 1:1");
        component("getBindCellIndex", "getColumnIndex", MappingKind.TRANSFORM, SupportLevel.PARTIAL, "body bind column only");
        component("set_url", "scwin.__xpTabRuntime.setUrl", MappingKind.SCRIPT_REQUIRED, SupportLevel.SUPPORTED, "screen target registry + WFrame setSrc/addTab exist + lifecycle-aware queue");
        component("addTabpage", "scwin.__xpTabRuntime.addPage", MappingKind.SCRIPT_REQUIRED, SupportLevel.PARTIAL, "common id/label/data pattern mapped to async addTab; uncommon overloads require review");
        component("insertTabpage", "scwin.__xpTabRuntime.insertPage", MappingKind.SCRIPT_REQUIRED, SupportLevel.PARTIAL, "mapped with addTabIndex; uncommon overloads require review");
        component("removeTabpage", "scwin.__xpTabRuntime.removePage", MappingKind.SCRIPT_REQUIRED, SupportLevel.SUPPORTED, "deleteTab + runtime ID registry/operation queue cleanup");
        component("set_tabindex", "scwin.__xpTabRuntime.selectPage", MappingKind.SCRIPT_REQUIRED, SupportLevel.SUPPORTED, "WebSquare setSelectedTabIndex; 0-based index preserved");
    }

    private static void global(String s,String t,MappingKind k,SupportLevel l,String n){put(GLOBAL,"GLOBAL",s,t,k,l,n);}
    private static void dataset(String s,String t,MappingKind k,SupportLevel l,String n){put(DATASET,"DATASET",s,t,k,l,n);}
    private static void component(String s,String t,MappingKind k,SupportLevel l,String n){put(COMPONENT,"COMPONENT",s,t,k,l,n);}
    private static void put(Map<String,ApiMapping> map,String c,String s,String t,MappingKind k,SupportLevel l,String n){map.put(s,new ApiMapping(c,s,t,k,l,n));}

    public ApiMapping getGlobal(String name) { return GLOBAL.get(name); }
    public ApiMapping getDataset(String name) { return DATASET.get(name); }
    public ApiMapping getComponent(String name) { return COMPONENT.get(name); }
    public List<ApiMapping> all() {
        List<ApiMapping> out = new ArrayList<ApiMapping>();
        out.addAll(GLOBAL.values()); out.addAll(DATASET.values()); out.addAll(COMPONENT.values());
        return Collections.unmodifiableList(out);
    }
}
