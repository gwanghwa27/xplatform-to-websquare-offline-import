package com.example.xfdltracker.project;

import com.example.xfdltracker.mapping.ComponentMappingRegistry;
import com.example.xfdltracker.parser.XfdlReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

/** Collects screen-owned component/Dataset identifiers before XJS global resolution. */
public class XfdlScreenSymbolInventory {
    private final ComponentMappingRegistry components = new ComponentMappingRegistry();

    public Set<String> collect(File source) throws Exception {
        Set<String> result = new LinkedHashSet<String>();
        Document document = new XfdlReader().read(source);
        NodeList all = document.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Node node = all.item(i);
            if (!(node instanceof Element)) continue;
            Element element = (Element) node;
            String tag = localName(element);
            if (components.get(tag) == null && !"Dataset".equals(tag) && !"DataSet".equals(tag)) continue;
            String id = element.getAttribute("id");
            if (id != null && id.trim().matches("[A-Za-z_$][A-Za-z0-9_$]*")) result.add(id.trim());
        }
        return result;
    }

    private String localName(Element element) {
        String value = element.getLocalName();
        if (value != null && value.length() > 0) return value;
        value = element.getTagName();
        int colon = value.indexOf(':');
        return colon >= 0 ? value.substring(colon + 1) : value;
    }
}
