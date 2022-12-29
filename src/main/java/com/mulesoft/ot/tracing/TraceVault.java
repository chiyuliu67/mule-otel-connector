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
            log.trace("Start transaction {} for flow '{}' - Adding to existing transaction", transactionId,
                    rootFlowName);
            transaction.get().getRootFlowSpan().addProcessorSpan(rootFlowName, rootFlowSpan);
        } else {
            Span span = rootFlowSpan.startSpan();
            log.trace("Start transaction {} for flow '{}': OT SpanId {}, TraceId {}", transactionId, rootFlowName,
                    span.getSpanContext().getSpanId(), span.getSpanContext().getTraceId());
            transactionMap.put(transactionId, new Trace(transactionId, span.getSpanContext().getTraceId(), rootFlowName,
                    new FlowSpan(rootFlowName, span)));
        }
    }

    public void end(String transactionId, String rootFlowName, Consumer<Span> spanUpdater, Instant endTime) {
        log.trace("End transaction {} for flow '{}'", transactionId, rootFlowName);
        getTransaction(transactionId).filter(t -> rootFlowName.equalsIgnoreCase(t.getRootFlowName()))
                .ifPresent(trace -> {
                    Trace removed = transactionMap.remove(transactionId);
                    Span rootSpan = removed.getRootFlowSpan().getSpan();
                    if (spanUpdater != null)
                        spanUpdater.accept(rootSpan);
                    removed.getRootFlowSpan().end(endTime);
                    log.trace("Ended trace {} for flow '{}': OT SpanId {}, TraceId {}", transactionId, rootFlowName,
                            rootSpan.getSpanContext().getSpanId(), rootSpan.getSpanContext().getTraceId());
                });
    }

    public void startSpan(String transactionId, String location, SpanBuilder spanBuilder) {
        getTransaction(transactionId).ifPresent(trace -> {
            log.trace("Adding Processor span to trace {} for location '{}'", transactionId, location);
            Span span = trace.getRootFlowSpan().addProcessorSpan(location, spanBuilder);
            log.trace("Adding Processor span to trace {} for locator span '{}': OT SpanId {}, TraceId {}",
                    transactionId, location, span.getSpanContext().getSpanId(), span.getSpanContext().getTraceId());
        });
    }

    public void endSpan(String transactionId, String location, Consumer<Span> spanUpdater, Instant endTime) {
        log.trace("Ending Processor span of transaction {} for location '{}'", transactionId, location);
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
        if (transaction.isPresent()) {
            return transaction.get().getTraceId();
        } else {
            return null;
        }
    }
}
