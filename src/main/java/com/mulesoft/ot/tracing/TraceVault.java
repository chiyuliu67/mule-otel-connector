package com.mulesoft.ot.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.context.Context;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceVault {

    private static final Logger log = LoggerFactory.getLogger(TraceVault.class);
    private static TraceVault instance;
    private final ConcurrentHashMap<String, Trace> transactionMap = new ConcurrentHashMap<>();

    public static synchronized TraceVault getInstance() {
        if (instance == null) {
            instance = new TraceVault();
        }
        return instance;
    }

    public void start(final String transactionId, final String rootFlowName, SpanBuilder rootFlowSpan) {
        Optional<Trace> transaction = getTransaction(transactionId);
        if (transaction.isPresent()) {
            log.debug("Start transaction: {}, flow: {}", transactionId, rootFlowName);
            transaction.get().getRootFlowSpan().addProcessorSpan(rootFlowName, rootFlowSpan);
        } else {
            Span span = rootFlowSpan.startSpan();
            log.debug("Start transaction: {}, flow: {}, spanId {}, traceId {}", transactionId, rootFlowName,
                    span.getSpanContext().getSpanId(), span.getSpanContext().getTraceId());
            transactionMap.put(transactionId,
                    new Trace(span.getSpanContext().getTraceId(), rootFlowName, new FlowSpan(rootFlowName, span)));
        }
    }

    public void end(String transactionId, String rootFlowName, Consumer<Span> spanUpdater, Instant endTime) {
        log.debug("End transaction: {}, flow: {}", transactionId, rootFlowName);
        getTransaction(transactionId).filter(t -> rootFlowName.equalsIgnoreCase(t.getRootFlowName()))
                .ifPresent(trace -> {
                    Trace removed = transactionMap.remove(transactionId);
                    Span rootSpan = removed.getRootFlowSpan().getSpan();
                    if (spanUpdater != null)
                        spanUpdater.accept(rootSpan);
                    removed.getRootFlowSpan().end(endTime);
                    log.debug("Removing span, transaction: {}, flow: {}, spanId: {}, traceId: {}", transactionId,
                            rootFlowName, rootSpan.getSpanContext().getSpanId(),
                            rootSpan.getSpanContext().getTraceId());
                });
    }

    public void startSpan(String transactionId, String location, SpanBuilder spanBuilder) {
        getTransaction(transactionId).ifPresent(trace -> {
            Span span = trace.getRootFlowSpan().addProcessorSpan(location, spanBuilder);
            log.debug("Start span, transaction: {}, location: {}, spanId: {}, traceId: {}", transactionId, location,
                    span.getSpanContext().getSpanId(), span.getSpanContext().getTraceId());
        });
    }

    public void endSpan(String transactionId, String location, Consumer<Span> spanUpdater, Instant endTime) {
        log.trace("End span, transaction: {}, location: {}", transactionId, location);
        getTransaction(transactionId)
                .ifPresent(trace -> trace.getRootFlowSpan().endProcessorSpan(location, spanUpdater, endTime));
    }

    private Optional<Trace> getTransaction(String transactionId) {
        return Optional.ofNullable(transactionMap.get(transactionId));
    }

    public Context getContext(String transactionId) {
        return getTransaction(transactionId).map(Trace::getRootFlowSpan).map(FlowSpan::getSpan)
                .map(s -> s.storeInContext(Context.current())).orElse(Context.current());
    }

    public String getTraceIdForTransaction(String transactionId) {
        Optional<Trace> transaction = getTransaction(transactionId);
        return transaction.map(Trace::getTraceId).orElse(null);
    }
}
