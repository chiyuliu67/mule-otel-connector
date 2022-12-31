package com.mulesoft.ot.processor;

import com.mulesoft.ot.ConnectorConfiguration;
import com.mulesoft.ot.OpenTelemetryConnection;
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

    private Supplier<OpenTelemetryConnection> connectionSupplier;
    private OpenTelemetryConnection openTelemetryConnection;

    @Inject
    ConfigurationComponentLocator configurationComponentLocator;

    private ProcessorComponentService processorComponentService;

    public MuleNotificationProcessor() {
    }

    public void init(Supplier<OpenTelemetryConnection> connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
        processorComponentService = ProcessorComponentService.getInstance();
    }

    private void init() {
        if (openTelemetryConnection == null) {
            openTelemetryConnection = connectionSupplier.get();
        }
    }

    public void handleProcessorStartEvent(MessageProcessorNotification notification) {
        try {
            getProcessorComponent(notification).ifPresent(processor -> {
                log.debug("Processor: {}:{} start event", notification.getResourceIdentifier(),
                        notification.getComponent().getIdentifier());
                init();
                TraceMetadata traceMetadata = processor.getStartTraceComponent(notification);
                SpanBuilder spanBuilder = openTelemetryConnection.spanBuilder(traceMetadata.getSpanName())
                        .setSpanKind(traceMetadata.getSpanKind())
                        .setStartTimestamp(Instant.ofEpochMilli(notification.getTimestamp()));
                traceMetadata.getTags().forEach(spanBuilder::setAttribute);
                openTelemetryConnection.getTraceVault().startSpan(traceMetadata.getCorrelationId(),
                        traceMetadata.getLocation(), spanBuilder);
            });

        } catch (Exception ex) {
            log.error("Error in handling processor start event", ex);
            throw ex;
        }
    }

    private Optional<ProcessorComponent> getProcessorComponent(MessageProcessorNotification notification) {
        return processorComponentService.getProcessorComponentFor(notification.getComponent().getIdentifier(),
                configurationComponentLocator);
    }

    public void handleProcessorEndEvent(MessageProcessorNotification notification) {
        try {
            getProcessorComponent(notification).ifPresent(processorComponent -> {
                log.debug("Processor: {}:{}, end event ", notification.getResourceIdentifier(),
                        notification.getComponent().getIdentifier());
                init();
                TraceMetadata traceMetadata = processorComponent.getEndTraceComponent(notification);
                openTelemetryConnection.getTraceVault().endSpan(traceMetadata.getCorrelationId(),
                        traceMetadata.getLocation(), span -> {
                            if (notification.getEvent().getError().isPresent()) {
                                Error error = notification.getEvent().getError().get();
                                span.recordException(error.getCause());
                            }
                            setSpanStatus(traceMetadata, span);
                            if (traceMetadata.getTags() != null)
                                traceMetadata.getTags().forEach(span::setAttribute);
                        }, Instant.ofEpochMilli(notification.getTimestamp()));
            });
        } catch (Exception ex) {
            log.error("Error in processor end event", ex);
            throw ex;
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void handleFlowStartEvent(PipelineMessageNotification notification) {
        try {
            log.debug("Resource: {}, flow start", notification.getResourceIdentifier());
            init();
            ProcessorComponent flowProcessorComponent = new FlowProcessorComponent()
                    .withConfigurationComponentLocator(configurationComponentLocator);
            TraceMetadata traceMetadata = flowProcessorComponent
                    .getSourceStartTraceComponent(notification, openTelemetryConnection).get();
            SpanBuilder spanBuilder = openTelemetryConnection.spanBuilder(traceMetadata.getSpanName())
                    .setSpanKind(traceMetadata.getSpanKind()).setParent(traceMetadata.getContext())
                    .setStartTimestamp(Instant.ofEpochMilli(notification.getTimestamp()));
            traceMetadata.getTags().forEach(spanBuilder::setAttribute);
            openTelemetryConnection.getTraceVault().start(traceMetadata.getCorrelationId(), traceMetadata.getName(),
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
                    .getSourceEndTraceComponent(notification, openTelemetryConnection).get();

            openTelemetryConnection.getTraceVault().end(traceMetadata.getCorrelationId(), traceMetadata.getName(),
                    rootSpan -> {
                        traceMetadata.getTags().forEach(rootSpan::setAttribute);
                        setSpanStatus(traceMetadata, rootSpan);
                        if (notification.getException() != null) {
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
