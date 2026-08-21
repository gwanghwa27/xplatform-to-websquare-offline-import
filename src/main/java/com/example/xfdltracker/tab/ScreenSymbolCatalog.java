package com.example.xfdltracker.tab;

import com.example.xfdltracker.mapping.ComponentMappingRegistry;
import com.example.xfdltracker.model.XfdlAnalysisResult;
import com.example.xfdltracker.parser.XfdlReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Screen-owned symbol kinds used to validate cross-screen references. */
public class ScreenSymbolCatalog {
    private final String screen; private final Set<String> components=new LinkedHashSet<String>(), datasets=new LinkedHashSet<String>(), functions=new LinkedHashSet<String>();
    private ScreenSymbolCatalog(String screen){this.screen=screen==null?"":screen;}
    public static ScreenSymbolCatalog build(File file,String screen,XfdlAnalysisResult analysis)throws Exception{
        ScreenSymbolCatalog out=new ScreenSymbolCatalog(screen);ComponentMappingRegistry mappings=new ComponentMappingRegistry();Document d=new XfdlReader().read(file);NodeList all=d.getElementsByTagName("*");
        for(int i=0;i<all.getLength();i++){Node n=all.item(i);if(!(n instanceof Element))continue;Element e=(Element)n;String tag=local(e),id=e.getAttribute("id").trim();if(id.length()==0)continue;if("Dataset".equals(tag)||"DataSet".equals(tag))out.datasets.add(id);else if(mappings.get(tag)!=null)out.components.add(id);}
        if(analysis!=null)out.functions.addAll(analysis.getFunctions().keySet());return out;
    }
    private static String local(Element e){String v=e.getLocalName();if(v!=null&&v.length()>0)return v;v=e.getTagName();int c=v.indexOf(':');return c>=0?v.substring(c+1):v;}
    public String getScreen(){return screen;}public Set<String> getComponents(){return Collections.unmodifiableSet(components);}public Set<String> getDatasets(){return Collections.unmodifiableSet(datasets);}public Set<String> getFunctions(){return Collections.unmodifiableSet(functions);}
    public CrossScreenReference.SymbolType typeOf(String id){if(functions.contains(id))return CrossScreenReference.SymbolType.FUNCTION;if(components.contains(id))return CrossScreenReference.SymbolType.COMPONENT;if(datasets.contains(id))return CrossScreenReference.SymbolType.DATASET;return CrossScreenReference.SymbolType.UNKNOWN;}
}
