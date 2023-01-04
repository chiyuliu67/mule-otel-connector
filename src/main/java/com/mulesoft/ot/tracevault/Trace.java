package com.mulesoft.ot.tracevault;

import java.io.Serializable;

public class Trace implements Serializable {
    private final String rootFlowName;
    private final SpanManager rootFlowSpan;
    private final String traceId;

    public Trace(String traceId, String rootFlowName, SpanManager rootFlowSpan) {
        this.rootFlowName = rootFlowName;
        this.rootFlowSpan = rootFlowSpan;
        this.traceId = traceId;
    }

    public String getRootFlowName() {
        return rootFlowName;
    }

    public SpanManager getRootFlowSpan() {
        return rootFlowSpan;
    }

    public String getTraceId() {
        return traceId;
    }
}
