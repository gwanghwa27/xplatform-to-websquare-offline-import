package com.example.xfdltracker.project;

import com.example.xfdltracker.io.TextFileUtil;
import com.example.xfdltracker.model.FunctionInfo;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProjectReportWriter {
    private static final Set<String> MIGRATION_APIS = new LinkedHashSet<String>();
    static {
        Collections.addAll(MIGRATION_APIS, "transaction", "open", "close", "setTimer", "killTimer", "lookup",
                "getOwnerFrame", "getApplication", "addRow", "deleteRow", "setColumn", "getColumn",
                "getCellProperty", "setCellProperty", "addColumn", "deleteColumn", "loadXML", "saveXML");
    }

    public void write(File reportDir, List<SourceAnalysis> sources, List<ConversionRecord> records) throws Exception {
        if (!reportDir.exists()) reportDir.mkdirs();
        TextFileUtil.writeUtf8(new File(reportDir, "summary.csv"), buildSummary(records));
        TextFileUtil.writeUtf8(new File(reportDir, "function-call.csv"), buildCalls(sources));
        TextFileUtil.writeUtf8(new File(reportDir, "function-index.csv"), buildFunctionIndex(sources));
        TextFileUtil.writeUtf8(new File(reportDir, "unsupported-api.csv"), buildMigrationApis(sources));
        TextFileUtil.writeUtf8(new File(reportDir, "duplicate-function.csv"), buildDuplicateFunctions(sources));
    }

    private String buildSummary(List<ConversionRecord> records) {
        StringBuilder sb = new StringBuilder("source,output,type,status,message\n");
        for (ConversionRecord r : records) {
            row(sb, r.getSource(), r.getOutput(), r.getType(), r.getStatus(), r.getMessage());
        }
        return sb.toString();
    }

    private String buildCalls(List<SourceAnalysis> sources) {
        Map<String, List<String>> index = functionIndex(sources);
        StringBuilder sb = new StringBuilder("source,function,line,called,resolution,targetFiles\n");
        for (SourceAnalysis s : sources) {
            for (FunctionInfo fn : s.getAnalysis().getFunctions().values()) {
                for (String called : fn.getCalledFunctions()) {
                    List<String> targets = index.get(called);
                    String resolution = targets == null ? "EXTERNAL_OR_API" : (targets.size() == 1 ? "RESOLVED" : "AMBIGUOUS");
                    row(sb, s.getRelativePath(), fn.getName(), String.valueOf(fn.getStartLine()), called,
                            resolution, targets == null ? "" : join(targets, "|"));
                }
            }
        }
        return sb.toString();
    }

    private String buildFunctionIndex(List<SourceAnalysis> sources) {
        StringBuilder sb = new StringBuilder("source,type,function,line,calls\n");
        for (SourceAnalysis s : sources) {
            for (FunctionInfo fn : s.getAnalysis().getFunctions().values()) {
                row(sb, s.getRelativePath(), s.getSourceType(), fn.getName(), String.valueOf(fn.getStartLine()),
                        join(new ArrayList<String>(fn.getCalledFunctions()), "|"));
            }
        }
        return sb.toString();
    }

    private String buildMigrationApis(List<SourceAnalysis> sources) {
        StringBuilder sb = new StringBuilder("source,function,line,api\n");
        for (SourceAnalysis s : sources) {
            for (FunctionInfo fn : s.getAnalysis().getFunctions().values()) {
                for (String called : fn.getCalledFunctions()) {
                    if (MIGRATION_APIS.contains(called)) row(sb, s.getRelativePath(), fn.getName(), String.valueOf(fn.getStartLine()), called);
                }
            }
        }
        return sb.toString();
    }

    private String buildDuplicateFunctions(List<SourceAnalysis> sources) {
        Map<String, List<String>> index = functionIndex(sources);
        List<String> names = new ArrayList<String>(index.keySet());
        Collections.sort(names);
        StringBuilder sb = new StringBuilder("function,count,files\n");
        for (String name : names) {
            List<String> files = index.get(name);
            if (files.size() > 1) row(sb, name, String.valueOf(files.size()), join(files, "|"));
        }
        return sb.toString();
    }

    private Map<String, List<String>> functionIndex(List<SourceAnalysis> sources) {
        Map<String, List<String>> index = new LinkedHashMap<String, List<String>>();
        List<SourceAnalysis> ordered = new ArrayList<SourceAnalysis>(sources);
        Collections.sort(ordered, new Comparator<SourceAnalysis>() {
            public int compare(SourceAnalysis a, SourceAnalysis b) { return a.getRelativePath().compareToIgnoreCase(b.getRelativePath()); }
        });
        for (SourceAnalysis s : ordered) {
            for (String name : s.getAnalysis().getFunctions().keySet()) {
                List<String> files = index.get(name);
                if (files == null) { files = new ArrayList<String>(); index.put(name, files); }
                files.add(s.getRelativePath());
            }
        }
        return index;
    }

    private void row(StringBuilder sb, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(csv(values[i]));
        }
        sb.append('\n');
    }

    private String csv(String value) {
        if (value == null) return "";
        String v = value.replace("\"", "\"\"");
        return "\"" + v + "\"";
    }

    private String join(List<String> list, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(sep);
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
