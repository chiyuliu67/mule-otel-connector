package com.mulesoft.ot.tracing;

import java.io.Serializable;

public class Trace implements Serializable {
    private final String rootFlowName;
    private final FlowSpan rootFlowSpan;
    private final String traceId;

    public Trace(String traceId, String rootFlowName, FlowSpan rootFlowSpan) {
        this.rootFlowName = rootFlowName;
        this.rootFlowSpan = rootFlowSpan;
        this.traceId = traceId;
    }

    public String getRootFlowName() {
        return rootFlowName;
    }

    public FlowSpan getRootFlowSpan() {
        return rootFlowSpan;
    }

    public String getTraceId() {
        return traceId;
    }
}
