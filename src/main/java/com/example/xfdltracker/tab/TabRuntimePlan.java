package com.example.xfdltracker.tab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Per-screen runtime Tab operations, bridge references, and generated ID bindings. */
public class TabRuntimePlan {
    public static class PageBinding {
        private final String sourceTabPath, sourcePagePath, targetTabId, targetHeaderId, targetContentId;
        public PageBinding(String sourceTabPath,String sourcePagePath,String targetTabId,String targetHeaderId,String targetContentId){
            this.sourceTabPath=s(sourceTabPath);this.sourcePagePath=s(sourcePagePath);this.targetTabId=s(targetTabId);this.targetHeaderId=s(targetHeaderId);this.targetContentId=s(targetContentId);
        }
        public String getSourceTabPath(){return sourceTabPath;} public String getSourcePagePath(){return sourcePagePath;}
        public String getTargetTabId(){return targetTabId;} public String getTargetHeaderId(){return targetHeaderId;} public String getTargetContentId(){return targetContentId;}
    }
    private final String screen;
    private final List<TabOperation> operations=new ArrayList<TabOperation>();
    private final List<CrossScreenReference> crossScreenReferences=new ArrayList<CrossScreenReference>();
    private final List<ScopeBridgeReference> scopeBridgeReferences=new ArrayList<ScopeBridgeReference>();
    private final List<String> warnings=new ArrayList<String>();
    private final Map<String,PageBinding> pageBindings=new LinkedHashMap<String,PageBinding>();
    private final Map<String,String> runtimePathMap=new LinkedHashMap<String,String>();
    private final Map<String,Boolean> eagerByTab=new LinkedHashMap<String,Boolean>();
    private boolean bridgeTarget;
    private String runtimeEmptyPageSrc="runtime/xplatform-tab-empty.xml";
    public TabRuntimePlan(String screen){this.screen=s(screen);}
    private static String s(String v){return v==null?"":v;}
    private static String key(String v){return s(v).replaceAll("\\s+","").replace(".form.",".").toLowerCase();}
    public String getScreen(){return screen;} public List<TabOperation> getOperations(){return Collections.unmodifiableList(operations);}
    public List<CrossScreenReference> getCrossScreenReferences(){return Collections.unmodifiableList(crossScreenReferences);}
    public List<ScopeBridgeReference> getScopeBridgeReferences(){return Collections.unmodifiableList(scopeBridgeReferences);}
    public List<String> getWarnings(){return Collections.unmodifiableList(warnings);} public Map<String,String> getRuntimePathMap(){return Collections.unmodifiableMap(runtimePathMap);}
    public void addOperation(TabOperation v){if(v!=null)operations.add(v);} public void addCrossScreenReference(CrossScreenReference v){if(v!=null)crossScreenReferences.add(v);}
    public void addScopeBridgeReference(ScopeBridgeReference v){if(v!=null)scopeBridgeReferences.add(v);}
    public void addWarning(String v){if(v!=null&&v.length()>0&&!warnings.contains(v))warnings.add(v);} public boolean isRuntimeRequired(){return bridgeTarget||!operations.isEmpty()||!crossScreenReferences.isEmpty()||!scopeBridgeReferences.isEmpty();}
    public void putPageBinding(PageBinding v){if(v!=null)pageBindings.put(key(v.getSourcePagePath()),v);} public PageBinding findPageBinding(String sourcePagePath){return pageBindings.get(key(sourcePagePath));}
    public Map<String,PageBinding> getPageBindings(){return Collections.unmodifiableMap(pageBindings);} public void putRuntimePath(String raw,String target){if(raw!=null&&raw.length()>0&&target!=null&&target.length()>0)runtimePathMap.put(raw.replace('\\','/'),target);}
    public void setTabEager(String tabPath, boolean eager){if(tabPath!=null&&tabPath.length()>0)eagerByTab.put(key(tabPath),Boolean.valueOf(eager));}
    public boolean isTabEager(String tabPath){Boolean v=eagerByTab.get(key(tabPath));return v!=null&&v.booleanValue();}
    public Map<String,Boolean> getTabEagerMap(){return Collections.unmodifiableMap(eagerByTab);}
    public boolean needsFullRuntimePathMap(){for(TabOperation o:operations)if(o.getType()==TabOperation.Type.SET_URL&&o.getStatus()==TabOperation.Status.RUNTIME_DYNAMIC)return true;return false;}
    public boolean isBridgeTarget(){return bridgeTarget;} public void markBridgeTarget(){bridgeTarget=true;}
    public String getRuntimeEmptyPageSrc(){return runtimeEmptyPageSrc;} public void setRuntimeEmptyPageSrc(String v){runtimeEmptyPageSrc=s(v);}
}
