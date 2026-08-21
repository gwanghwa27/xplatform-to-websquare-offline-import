package com.example.xfdltracker.parser;

import com.example.xfdltracker.model.EventBinding;
import com.example.xfdltracker.model.XfdlAnalysisResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class XfdlReader {
    public Document read(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        try { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); } catch (Exception ignored) {}
        try { factory.setFeature("http://xml.org/sax/features/external-general-entities", false); } catch (Exception ignored) {}
        try { factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false); } catch (Exception ignored) {}
        try { factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); } catch (Exception ignored) {}
        try { factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); } catch (Exception ignored) {}
        Document doc = factory.newDocumentBuilder().parse(file);
        doc.getDocumentElement().normalize();
        return doc;
    }

    public String extractScript(Document document) {
        List<Element> scripts = findElements(document, "Script");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < scripts.size(); i++) {
            String script = scripts.get(i).getTextContent();
            if (script != null && script.trim().length() > 0) {
                sb.append(script).append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    public void extractEvents(Document document, XfdlAnalysisResult result) {
        NodeList elements = document.getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            String componentId = buildComponentPath(element);
            if (componentId.length() == 0) continue;

            NamedNodeMap attrs = element.getAttributes();
            for (int j = 0; j < attrs.getLength(); j++) {
                Node attr = attrs.item(j);
                String name = attr.getNodeName();
                if (name == null) continue;
                String lowerName = name.toLowerCase();
                // XPlatform/Nexacro Tab uses canchange (without the "on" prefix) as a cancellable event.
                if (!lowerName.startsWith("on") && !"canchange".equals(lowerName)) continue;
                String fn = normalizeFunctionName(attr.getNodeValue());
                if (fn.length() > 0) result.getEvents().add(new EventBinding(componentId, name, fn));
            }
        }

        List<Element> bindEvents = findElements(document, "BindEvent");
        for (int i = 0; i < bindEvents.size(); i++) {
            Element e = bindEvents.get(i);
            String id = e.getAttribute("id");
            String eventId = e.getAttribute("eventid");
            String handler = normalizeFunctionName(e.getAttribute("handler"));
            if (id.length() > 0 && eventId.length() > 0 && handler.length() > 0) {
                result.getEvents().add(new EventBinding(id, eventId, handler));
            }
        }
    }


    private List<Element> findElements(Document document, String tagName) {
        List<Element> result = new ArrayList<Element>();
        if (document == null || tagName == null) return result;
        NodeList elements = document.getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            if (tagName.equals(getTagName(element))) {
                result.add(element);
            }
        }
        return result;
    }


    private String buildComponentPath(Element element) {
        if (element == null) return "";
        String ownId = element.getAttribute("id");
        if (ownId == null || ownId.trim().length() == 0) return "";

        String path = ownId.trim();
        Node parent = element.getParentNode();
        while (parent instanceof Element) {
            Element parentElement = (Element) parent;
            String tag = getTagName(parentElement);
            if ("Form".equals(tag)) break;
            if ("Div".equals(tag)) {
                String parentId = parentElement.getAttribute("id");
                if (parentId != null && parentId.trim().length() > 0) {
                    path = parentId.trim() + "." + path;
                }
            }
            parent = parent.getParentNode();
        }
        return path;
    }

    private String getTagName(Element element) {
        if (element == null) return "";
        String localName = element.getLocalName();
        if (localName != null && localName.length() > 0) return localName;
        String tagName = element.getTagName();
        int colon = tagName == null ? -1 : tagName.indexOf(':');
        return colon >= 0 ? tagName.substring(colon + 1) : (tagName == null ? "" : tagName);
    }

    private String normalizeFunctionName(String value) {
        if (value == null) return "";
        String r = value.trim();
        if (r.startsWith("this.")) r = r.substring(5);
        int p = r.indexOf('(');
        if (p >= 0) r = r.substring(0, p);
        return r.trim();
    }
}
