package com.example.xfdltracker.project;

import com.example.xfdltracker.mapping.ApiMapping;
import com.example.xfdltracker.mapping.ComponentMapping;
import com.example.xfdltracker.mapping.ComponentMappingRegistry;
import com.example.xfdltracker.mapping.EventMapping;
import com.example.xfdltracker.mapping.EventMappingRegistry;
import com.example.xfdltracker.mapping.MappingKind;
import com.example.xfdltracker.mapping.PropertyMapping;
import com.example.xfdltracker.mapping.PropertyMappingRegistry;
import com.example.xfdltracker.mapping.ScriptApiMappingRegistry;
import com.example.xfdltracker.mapping.SupportLevel;
import com.example.xfdltracker.model.EventBinding;
import com.example.xfdltracker.model.XfdlAnalysisResult;
import com.example.xfdltracker.parser.XfdlReader;
import com.example.xfdltracker.transaction.TransactionCall;
import com.example.xfdltracker.tab.TabContentPlan;
import com.example.xfdltracker.tab.TabContentReference;
import com.example.xfdltracker.xjs.XjsResolution;
import com.example.xfdltracker.util.JavaScriptCleaner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

/** Conservative static feature inventory; it reports uncertainty instead of changing conversion semantics. */
public class Phase3ScreenAnalyzer {
    private final ComponentMappingRegistry components = new ComponentMappingRegistry();
    private final PropertyMappingRegistry properties = new PropertyMappingRegistry();
    private final EventMappingRegistry events = new EventMappingRegistry();
    private final ScriptApiMappingRegistry apis = new ScriptApiMappingRegistry();

    public Phase3ScreenReport analyze(File source, String relativePath, String integratedScript,
                                      XfdlAnalysisResult nativeAnalysis, XjsResolution xjs,
                                      List<TransactionCall> transactions, TabContentPlan tabPlan) throws Exception {
        Phase3ScreenReport out = new Phase3ScreenReport(relativePath);
        Document doc = new XfdlReader().read(source);
        inspectElements(doc, out, tabPlan);
        inspectEvents(nativeAnalysis, out);
        out.setDatasets(countTag(doc, "Dataset") + countTag(doc, "DataSet"));
        inspectDatasets(doc, out);
        out.setInternalFunctions(nativeAnalysis == null ? 0 : nativeAnalysis.getFunctions().size());
        out.setExternalFunctions(xjs == null ? 0 : xjs.getImportedFunctions().size());
        out.setExternalGlobals(xjs == null ? 0 : xjs.getImportedGlobals().size());
        out.setUnresolvedFunctions(xjs == null ? 0 : xjs.getUnresolvedFunctions().size() + xjs.getAmbiguousSymbols().size());
        if (xjs != null) {
            for (String v : xjs.getUnresolvedFunctions()) out.addUnsupported("UNRESOLVED FUNCTION: " + v);
            for (String v : xjs.getAmbiguousSymbols()) out.addUnsupported("AMBIGUOUS XJS SYMBOL: " + v);
            for (String v : xjs.getIncludeWarnings()) out.addUnsupported("XJS INCLUDE TODO: " + v);
        }
        out.addTransactions(transactions);
        if (transactions != null && !transactions.isEmpty()) out.addUnsupported("transaction -> WebSquare submission manual migration required");
        inspectApiCandidates(integratedScript, out);
        inspectTabContent(tabPlan, out);
        return out;
    }

    private void inspectElements(Document doc, Phase3ScreenReport out, TabContentPlan tabPlan) {
        NodeList all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Node n = all.item(i); if (!(n instanceof Element)) continue;
            Element e = (Element)n; String tag = localName(e);
            ComponentMapping cm = components.get(tag);
            if (cm == null || "Form".equals(tag) || "Dataset".equals(tag) || "DataSet".equals(tag)) continue;
            if (cm.getSupportLevel() == SupportLevel.SUPPORTED) out.componentSupported();
            else if (cm.getTargetTag() == null || cm.getSupportLevel() == SupportLevel.TODO || cm.getSupportLevel() == SupportLevel.UNSUPPORTED) {
                out.componentTodo(); out.addUnsupported("COMPONENT " + tag + ": " + cm.getNote());
            } else { out.componentPartial(); out.addUnsupported("PARTIAL COMPONENT " + tag + ": " + cm.getNote()); }
            NamedNodeMap attrs = e.getAttributes();
            for (int a = 0; a < attrs.getLength(); a++) {
                Node attr = attrs.item(a); String name = attr.getNodeName(); int colon = name.indexOf(':'); if (colon >= 0) name = name.substring(colon + 1);
                String low = name.toLowerCase();
                if ("id".equals(low) || low.startsWith("on") || "xmlns".equals(low)) continue;
                if (isManagedTabContentProperty(tag, attr.getNodeValue(), low, tabPlan)) {
                    out.propertyMapped();
                    continue;
                }
                PropertyMapping pm = properties.get(low);
                if (pm == null) { out.propertyTodo(); out.addUnsupported("PROPERTY " + tag + "." + name + " (inventory 없음)"); }
                else if (pm.getKind() == MappingKind.TODO || pm.getKind() == MappingKind.UNSUPPORTED || pm.getKind() == MappingKind.SCRIPT_REQUIRED) {
                    out.propertyTodo(); out.addUnsupported("PROPERTY " + tag + "." + name + ": " + pm.getNote());
                } else out.propertyMapped();
            }
        }
    }

    private boolean isManagedTabContentProperty(String tag, String value, String property, TabContentPlan plan) {
        if (!"Tabpage".equals(tag) || plan == null) return false;
        if (!("url".equals(property) || "formurl".equals(property) || "contenturl".equals(property)
                || "src".equals(property) || "source".equals(property))) return false;
        String raw = value == null ? "" : value.trim();
        for (TabContentReference ref : plan.getReferences()) {
            if (raw.equals(ref.getRawReference())) return true;
        }
        return false;
    }

    private void inspectTabContent(TabContentPlan plan, Phase3ScreenReport out) {
        if (plan == null) return;
        for (TabContentReference ref : plan.getReferences()) {
            if (!ref.isResolved()) {
                out.addUnsupported("TAB CONTENT UNRESOLVED: " + ref.getTabPagePath() + " -> "
                        + ref.getRawReference() + " : " + ref.getMessage());
            }
            if (ref.isMixedInlineExternal()) {
                out.addUnsupported("TAB MIXED INLINE/EXTERNAL: " + ref.getTabPagePath()
                        + " -> external screen kept; inline children require manual composition");
            }
        }
        for (String value : plan.getDynamicUsages())
            out.addUnsupported("TAB dynamic content loading: " + value);
        for (String value : plan.getParentChildUsages())
            out.addUnsupported("TAB child/parent scope access review: " + value);
        for (String value : plan.getWarnings())
            out.addUnsupported("TAB ANALYSIS WARNING: " + value);
    }

    private void inspectDatasets(Document doc, Phase3ScreenReport out) {
        NodeList all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            if (!(all.item(i) instanceof Element)) continue;
            Element e = (Element) all.item(i); String tag = localName(e);
            if ("ConstColumn".equals(tag)) out.addUnsupported("DATASET ConstColumn -> DataList 직접 의미 대응 보류");
            if ("Row".equals(tag) && e.hasAttribute("type") && e.getAttribute("type").trim().length() > 0)
                out.addUnsupported("DATASET initial Row.type status -> 자동 보존 안 함");
            if ("Dataset".equals(tag) || "DataSet".equals(tag)) {
                String[] attrs = {"filter", "keystring", "rowposition", "enableevent", "updatecontrol"};
                for (String attr : attrs) if (e.hasAttribute(attr) && e.getAttribute(attr).trim().length() > 0)
                    out.addUnsupported("DATASET PROPERTY " + attr + "=" + e.getAttribute(attr) + " -> TODO");
            }
        }
    }

    private void inspectEvents(XfdlAnalysisResult analysis, Phase3ScreenReport out) {
        if (analysis == null) return;
        for (EventBinding b : analysis.getEvents()) {
            EventMapping em = events.get(b.getEventName());
            if (em != null && em.getTargetName().length() > 0) {
                out.eventMapped();
                if (em.getSupportLevel() != SupportLevel.SUPPORTED) out.addUnsupported("PARTIAL EVENT " + b.getEventName() + ": " + em.getNote());
            } else { out.eventTodo(); out.addUnsupported("EVENT " + b.getEventName() + " -> " + b.getFunctionName()); }
        }
    }

    private void inspectApiCandidates(String script, Phase3ScreenReport out) {
        if (script == null) return;
        String cleaned = new JavaScriptCleaner().clean(script);
        for (ApiMapping api : apis.all()) {
            if (api.getSupportLevel() == SupportLevel.SUPPORTED) continue;
            String name = api.getSourceName();
            Pattern p;
            if ("COMPONENT".equals(api.getCategory()) || "DATASET".equals(api.getCategory())) {
                p = Pattern.compile("\\.\\s*" + Pattern.quote(name) + "\\b");
            } else if ("application".equals(name) || "system".equals(name) || "event".equals(name)) {
                p = Pattern.compile("(?<![A-Za-z0-9_$])" + Pattern.quote(name) + "\\s*\\.");
            } else {
                p = Pattern.compile("(?<![A-Za-z0-9_$])" + Pattern.quote(name) + "\\s*\\(");
            }
            if (p.matcher(cleaned).find()) out.addApiCandidate(api.getCategory() + " " + name + " -> "
                    + (api.getTargetName().length() == 0 ? "TODO" : api.getTargetName()) + " : " + api.getNote());
        }
    }

    private int countTag(Document doc, String wanted) {
        int count = 0; NodeList all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) if (all.item(i) instanceof Element && wanted.equals(localName((Element)all.item(i)))) count++;
        return count;
    }
    private String localName(Element e) { String v=e.getLocalName(); if(v!=null&&v.length()>0)return v; v=e.getTagName(); int c=v.indexOf(':'); return c>=0?v.substring(c+1):v; }
}
