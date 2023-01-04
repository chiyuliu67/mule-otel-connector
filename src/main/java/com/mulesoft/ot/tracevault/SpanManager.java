package com.mulesoft.ot.tracevault;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SpanManager implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(SpanManager.class);

    private final String flowName;
    private final Span span;
    private boolean ending = false;
    private final Map<String, Span> childSpans = new ConcurrentHashMap<>();
    private boolean ended = false;

    public SpanManager(String flowName, Span span) {
        this.flowName = flowName;
        this.span = span;
    }

    public Span getSpan() {
        return span;
    }

    public Span addSpan(String location, SpanBuilder spanBuilder) {
        if (ending || ended)
            throw new UnsupportedOperationException(
                    "Flow: " + flowName + ", span: " + (ended ? ", end" : "is finishing"));
        Span span = spanBuilder.setParent(Context.current().with(getSpan())).startSpan();
        childSpans.put(location, span);
        log.debug("Start span: {}, location: {}", span.getSpanContext().getSpanId(), location);
        return span;
    }

    public void endSpan(String location, Consumer<Span> spanUpdater, Instant endTime) {
        if ((!ending || ended) && childSpans.containsKey(location)) {
            Span removed = childSpans.remove(location);
            if (spanUpdater != null) {
                spanUpdater.accept(removed);
            }
            log.debug("End span: {}, location: {}", removed.getSpanContext().getSpanId(), location);
            removed.end(endTime);
        }
    }

    public void end(Instant endTime) {
        ending = true;
        childSpans.forEach((location, span) -> span.end(endTime));
        span.end(endTime);
        log.debug("End span: {}", span.getSpanContext().getSpanId());
        ended = true;
    }
}
