package com.example.xfdltracker.binding;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.LinkedHashSet;
import java.util.Set;

/** Parses XFDL BindItem and innerdataset/codecolumn/datacolumn metadata into a shared model. */
public class BindingAnalyzer {
    public BindingModel analyze(Document source) {
        BindingModel model = new BindingModel();
        if (source == null || source.getDocumentElement() == null) return model;
        Set<String> datasetIds = collectDatasetIds(source.getDocumentElement());
        walk(source.getDocumentElement(), "", model, datasetIds);
        return model;
    }

    private void walk(Element element, String parentPath, BindingModel model, Set<String> datasetIds) {
        String tag = localName(element);
        String id = element.getAttribute("id");
        String currentPath = parentPath;
        if (isComponentCandidate(tag) && id.length() > 0) currentPath = join(parentPath, id);

        if ("BindItem".equals(tag)) {
            String comp = first(element, "compid", "componentid");
            String prop = first(element, "propid", "propertyid");
            String ds = normalizeDataset(first(element, "datasetid", "dataset"));
            String col = first(element, "columnid", "column");
            if (comp.length() > 0 && prop.length() > 0 && ds.length() > 0 && col.length() > 0) {
                model.addComponentBinding(new ComponentBinding(comp, prop, ds, col));
                if (!datasetIds.contains(ds)) model.addWarning("BindItem dataset 미해결: " + comp + "." + prop + " -> " + ds + "." + col);
            }
        } else {
            String inner = normalizeDataset(element.getAttribute("innerdataset"));
            String code = element.getAttribute("codecolumn");
            String data = element.getAttribute("datacolumn");
            if (id.length() > 0 && inner.length() > 0) {
                model.addItemset(new ItemsetBinding(currentPath, inner, code, data));
                if (!datasetIds.contains(inner)) model.addWarning("innerdataset 미해결: " + currentPath + " -> " + inner);
                if (code.length() == 0 || data.length() == 0) model.addWarning("itemset code/datacolumn 누락: " + currentPath);
            }
        }

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) walk((Element) n, currentPath, model, datasetIds);
        }
    }

    private Set<String> collectDatasetIds(Element root) {
        Set<String> result = new LinkedHashSet<String>();
        collectDatasetIdsRecursive(root, result);
        return result;
    }
    private void collectDatasetIdsRecursive(Element e, Set<String> out) {
        String tag = localName(e);
        if (("Dataset".equals(tag) || "DataSet".equals(tag)) && e.getAttribute("id").length() > 0) {
            out.add(normalizeDataset(e.getAttribute("id")));
        }
        NodeList children = e.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) if (children.item(i).getNodeType() == Node.ELEMENT_NODE) collectDatasetIdsRecursive((Element) children.item(i), out);
    }

    private boolean isComponentCandidate(String tag) {
        return !("FDL".equals(tag) || "Form".equals(tag) || "Layouts".equals(tag) || "Layout".equals(tag)
                || "Objects".equals(tag) || "Script".equals(tag) || "Dataset".equals(tag) || "DataSet".equals(tag)
                || "ColumnInfo".equals(tag) || "Column".equals(tag) || "Rows".equals(tag) || "Row".equals(tag)
                || "Col".equals(tag) || "Bind".equals(tag) || "BindItem".equals(tag) || "Formats".equals(tag)
                || "Format".equals(tag) || "Band".equals(tag) || "Cell".equals(tag));
    }
    private String first(Element e, String a, String b) { String v = e.getAttribute(a); return v.length() > 0 ? v : e.getAttribute(b); }
    private String normalizeDataset(String v) { String s = v == null ? "" : v.trim(); while (s.startsWith("@")) s = s.substring(1); return s; }
    private String join(String parent, String id) { return parent.length() == 0 ? id : parent + "." + id; }
    private String localName(Element e) { String n = e.getLocalName(); return n != null && n.length() > 0 ? n : (e.getTagName().indexOf(':') >= 0 ? e.getTagName().substring(e.getTagName().indexOf(':') + 1) : e.getTagName()); }
}
