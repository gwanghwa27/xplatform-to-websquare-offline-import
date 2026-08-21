package com.example.xfdltracker.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EventMappingRegistry {
    private static final Map<String, EventMapping> MAP = new LinkedHashMap<String, EventMapping>();
    static {
        supported("onclick", "onclick", "same semantic family");
        supported("ondblclick", "ondblclick", "same semantic family");
        supported("onkeyup", "onkeyup", "keyboard event");
        supported("onkeydown", "onkeydown", "keyboard event");
        supported("onsetfocus", "onfocus", "focus acquired");
        supported("onkillfocus", "onblur", "focus lost");
        partial("onchanged", "onchange", "old/new value payload differs by component");
        partial("canchange", "onbeforeselect", "Tab cancellable pre/post index payload requires adapter");
        partial("onitemchanged", "onchange", "item payload differs by component");
        partial("onmouseenter", "onmouseover", "mouseenter vs mouseover bubbling semantics differ");
        partial("onmouseleave", "onmouseout", "mouseleave vs mouseout bubbling semantics differ");
        partial("ondropdown", "ondropdown", "component-specific support");
        partial("oncloseup", "oncloseup", "component-specific support");
        partial("oncolumnchanged", "onchange", "Dataset/Grid payload differs; script review required");
        partial("onrowposchanged", "onrowpositionchange", "DataList payload differs");
        supported("onload", "onpageload", "Form lifecycle only");
        partial("onsize", "onresize", "resize payload differs");
        todo("ontimer", "timer lifecycle requires script wrapper");
    }
    private static void supported(String s, String t, String n) { add(s,t,SupportLevel.SUPPORTED,n); }
    private static void partial(String s, String t, String n) { add(s,t,SupportLevel.PARTIAL,n); }
    private static void todo(String s, String n) { add(s,"",SupportLevel.TODO,n); }
    private static void add(String s, String t, SupportLevel l, String n) { MAP.put(s, new EventMapping(s,t,l,n)); }
    public EventMapping get(String source) { return MAP.get(source == null ? null : source.toLowerCase()); }
    public String targetEvent(String source) {
        EventMapping m = get(source);
        return m == null || m.getTargetName().length() == 0 ? source : m.getTargetName();
    }
    public List<EventMapping> all() { return Collections.unmodifiableList(new ArrayList<EventMapping>(MAP.values())); }
}
