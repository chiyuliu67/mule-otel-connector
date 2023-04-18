package com.mulesoft.ot.processor;

import com.mulesoft.ot.ConnectorConfiguration;
import com.mulesoft.ot.tracevault.OtelConnection;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Notification Processor bean. This is injected through registry-bootstrap into
 * Extension configuration, see {@link ConnectorConfiguration}.
 */
public class MuleNotificationProcessor {

    private static final Logger log = LoggerFactory.getLogger(MuleNotificationProcessor.class);

    private Supplier<OtelConnection> connectionSupplier;
    private OtelConnection otelConnection;

    @Inject
    ConfigurationComponentLocator configurationComponentLocator;

    private ProcessorComponentService processorComponentService;

    public MuleNotificationProcessor() {
    }

    public void init(Supplier<OtelConnection> connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
        processorComponentService = ProcessorComponentService.getInstance();
    }

    private void init() {
        if (otelConnection == null) {
            otelConnection = connectionSupplier.get();
        }
    }

    public void handleProcessorStartEvent(MessageProcessorNotification notification) {
        getProcessorComponent(notification).ifPresent(processor -> {
            log.debug("Processor: {}:{} start event", notification.getResourceIdentifier(),
                    notification.getComponent().getIdentifier());
            init();
            TraceMetadata traceMetadata = processor.getStartTraceComponent(notification);
            SpanBuilder spanBuilder = otelConnection.spanBuilder(traceMetadata.getSpanName())
                    .setSpanKind(traceMetadata.getSpanKind())
                    .setStartTimestamp(Instant.ofEpochMilli(notification.getTimestamp()));
            traceMetadata.getTags().forEach(spanBuilder::setAttribute);
            otelConnection.getTraceVault().startSpan(traceMetadata.getCorrelationId(), traceMetadata.getLocation(),
                    spanBuilder);
        });
    }

    public void handleProcessorEndEvent(MessageProcessorNotification notification) {
        getProcessorComponent(notification).ifPresent(processorComponent -> {
            log.debug("Processor: {}:{}, end event ", notification.getResourceIdentifier(),
                    notification.getComponent().getIdentifier());
            init();
            TraceMetadata traceMetadata = processorComponent.getEndTraceComponent(notification);
            otelConnection.getTraceVault().endSpan(traceMetadata.getCorrelationId(), traceMetadata.getLocation(),
                    span -> {
                        // Verify if an error happened
                        if (notification.getEvent().getError().isPresent()) {
                            log.debug("spanId: {}, log the error into the span", span.getSpanContext().getSpanId());
                            Error error = notification.getEvent().getError().get();
                            span.recordException(error.getCause());
                        }

                        setSpanStatus(traceMetadata, span);
                        if (traceMetadata.getTags() != null)
                            traceMetadata.getTags().forEach(span::setAttribute);
                    }, Instant.ofEpochMilli(notification.getTimestamp()));
        });
    }

    private Optional<ProcessorComponent> getProcessorComponent(MessageProcessorNotification notification) {
        return processorComponentService.getProcessorComponentFor(notification.getComponent().getIdentifier(),
                configurationComponentLocator);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void handleFlowStartEvent(PipelineMessageNotification notification) {
        try {
            log.debug("Resource: {}, flow start", notification.getResourceIdentifier());
            init();
            ProcessorComponent flowProcessorComponent = new FlowProcessorComponent()
                    .withConfigurationComponentLocator(configurationComponentLocator);
            TraceMetadata traceMetadata = flowProcessorComponent
                    .getSourceStartTraceComponent(notification, otelConnection).get();
            SpanBuilder spanBuilder = otelConnection.spanBuilder(traceMetadata.getSpanName())
                    .setSpanKind(traceMetadata.getSpanKind()).setParent(traceMetadata.getContext())
                    .setStartTimestamp(Instant.ofEpochMilli(notification.getTimestamp()));
            traceMetadata.getTags().forEach(spanBuilder::setAttribute);
            otelConnection.getTraceVault().start(traceMetadata.getCorrelationId(), traceMetadata.getName(),
                    spanBuilder);
        } catch (Exception ex) {
            log.error("Error resource: " + notification.getResourceIdentifier() + " flow start", ex);
            throw ex;
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void handleFlowEndEvent(PipelineMessageNotification notification) {
        try {
            log.debug("Resource: {}, flow end", notification.getResourceIdentifier());
            init();
            ProcessorComponent flowProcessorComponent = new FlowProcessorComponent()
                    .withConfigurationComponentLocator(configurationComponentLocator);

            TraceMetadata traceMetadata = flowProcessorComponent
                    .getSourceEndTraceComponent(notification, otelConnection).get();

            otelConnection.getTraceVault().end(traceMetadata.getCorrelationId(), traceMetadata.getName(), rootSpan -> {
                traceMetadata.getTags().forEach(rootSpan::setAttribute);
                setSpanStatus(traceMetadata, rootSpan);
                if (notification.getException() != null) {
                    log.debug("spanId: {}, log the error in the span", rootSpan.getSpanContext().getSpanId());
                    rootSpan.recordException(notification.getException());
                }
            }, Instant.ofEpochMilli(notification.getTimestamp()));
        } catch (Exception ex) {
            log.error("Error resource: " + notification.getResourceIdentifier() + " flow end", ex);
            throw ex;
        }
    }

    private void setSpanStatus(TraceMetadata traceMetadata, Span span) {
        if (traceMetadata.getStatusCode() != null && !StatusCode.UNSET.equals(traceMetadata.getStatusCode())) {
            span.setStatus(traceMetadata.getStatusCode());
        }
    }
}
