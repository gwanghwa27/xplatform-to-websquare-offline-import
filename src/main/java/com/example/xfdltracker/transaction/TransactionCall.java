package com.example.xfdltracker.transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parsed XPlatform transaction invocation. Raw expressions are kept to avoid unsafe guessing. */
public class TransactionCall {
    private final int line;
    private final String serviceId;
    private final String url;
    private final String inputDatasets;
    private final String outputDatasets;
    private final String arguments;
    private final String callback;
    private final String asyncExpression;
    private final List<String> extraArguments;
    private final String rawCall;

    public TransactionCall(int line, List<String> args, String rawCall) {
        this.line = line;
        this.serviceId = at(args, 0);
        this.url = at(args, 1);
        this.inputDatasets = at(args, 2);
        this.outputDatasets = at(args, 3);
        this.arguments = at(args, 4);
        this.callback = at(args, 5);
        this.asyncExpression = at(args, 6);
        List<String> extra = new ArrayList<String>();
        if (args != null && args.size() > 7) extra.addAll(args.subList(7, args.size()));
        this.extraArguments = Collections.unmodifiableList(extra);
        this.rawCall = rawCall == null ? "" : rawCall;
    }

    private static String at(List<String> values, int index) {
        return values != null && index >= 0 && index < values.size() ? values.get(index).trim() : "";
    }

    public int getLine() { return line; }
    public String getServiceId() { return serviceId; }
    public String getUrl() { return url; }
    public String getInputDatasets() { return inputDatasets; }
    public String getOutputDatasets() { return outputDatasets; }
    public String getArguments() { return arguments; }
    public String getCallback() { return callback; }
    public String getAsyncExpression() { return asyncExpression; }
    public List<String> getExtraArguments() { return extraArguments; }
    public String getRawCall() { return rawCall; }
}
