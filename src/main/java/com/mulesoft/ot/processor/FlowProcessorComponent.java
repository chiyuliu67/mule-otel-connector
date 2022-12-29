package com.mulesoft.ot.processor;

import com.mulesoft.ot.Constants;
import com.mulesoft.ot.ContextHandler;
import io.opentelemetry.api.trace.SpanKind;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.notification.EnrichedServerNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import java.util.*;

public class FlowProcessorComponent extends AbstractProcessorComponent {

    private final Logger log = LoggerFactory.getLogger(FlowProcessorComponent.class);

    @Override
    public boolean canHandle(ComponentIdentifier componentIdentifier) {
        return namespaceSupported(componentIdentifier) && operationSupported(componentIdentifier);
    }

    @Override
    protected String getNamespace() {
        return Constants.NAMESPACE_MULE;
    }

    @Override
    protected List<String> getSources() {
        return Collections.emptyList();
    }

    @Override
    protected List<String> getOperations() {
        return Collections.singletonList("flow");
    }

    @Override
    public TraceMetadata getStartTraceComponent(EnrichedServerNotification enrichedServerNotification) {

        if (!canHandle(enrichedServerNotification.getComponent().getIdentifier())) {
            throw new RuntimeException("Unsupported component "
                    + enrichedServerNotification.getComponent().getIdentifier().toString() + " for flow processor.");
        }

        TraceMetadata traceMetadata = new TraceMetadata();
        traceMetadata.setName(enrichedServerNotification.getResourceIdentifier());

        Map<String, String> tags = new HashMap<>();
        tags.put(Constants.SERVICE_FLOW_NAME, enrichedServerNotification.getResourceIdentifier());
        tags.put(Constants.SERVER_ID, enrichedServerNotification.getServerId());
        traceMetadata.setTags(tags);
        traceMetadata.setCorrelationId(getTransactionId(enrichedServerNotification));
        traceMetadata.setSpanName(enrichedServerNotification.getResourceIdentifier());

        return traceMetadata;
    }

    @Override
    public Optional<TraceMetadata> getSourceStartTraceComponent(EnrichedServerNotification notification,
            ContextHandler contextHandler) {
        TraceMetadata traceMetadata = getStartTraceComponent(notification);
        traceMetadata.setSpanKind(SpanKind.SERVER);

        ComponentIdentifier sourceIdentifier = getSourceIdentifier(notification);
        if (sourceIdentifier == null) {
            return Optional.of(traceMetadata);
        }
        traceMetadata.getTags().put(Constants.SERVICE_FLOW_SOURCE_NAME, sourceIdentifier.getName());
        traceMetadata.getTags().put(Constants.SERVICE_FLOW_SOURCE_NAMESPACE, sourceIdentifier.getNamespace());
        Component sourceComponent = configurationComponentLocator.find(Location.builderFromStringRepresentation(
                notification.getEvent().getContext().getOriginatingLocation().getLocation()).build()).get();
        ComponentWrapper sourceWrapper = new ComponentWrapper(sourceComponent, configurationComponentLocator);
        traceMetadata.getTags().put(Constants.SERVICE_FLOW_SOURCE_CONFIGREF, sourceWrapper.getConfigRef());

        // Find if there is a processor component to handle flow source component.
        // If exists, allow it to process notification and build any additional tags to
        // include in a trace.
        ProcessorComponentService.getInstance()
                .getProcessorComponentFor(sourceIdentifier, configurationComponentLocator)
                .flatMap(processorComponent -> processorComponent.getSourceStartTraceComponent(notification,
                        contextHandler))
                .ifPresent(sourceTrace -> {
                    SpanKind sourceKind = sourceTrace.getSpanKind() != null
                            ? sourceTrace.getSpanKind()
                            : SpanKind.SERVER;
                    traceMetadata.getTags().putAll(sourceTrace.getTags());
                    traceMetadata.setSpanKind(sourceKind);
                    traceMetadata.setSpanName(sourceTrace.getSpanName());
                    traceMetadata.setCorrelationId(sourceTrace.getCorrelationId());
                    traceMetadata.setContext(sourceTrace.getContext());
                });
        return Optional.of(traceMetadata);
    }

    @Override
    public Optional<TraceMetadata> getSourceEndTraceComponent(EnrichedServerNotification notification,
            ContextHandler contextHandler) {

        // Add flow tags to the trace
        TraceMetadata traceMetadata = getTraceComponentEnd(notification);
        traceMetadata.setSpanKind(SpanKind.SERVER);
        traceMetadata.getTags().put(Constants.CORRELATION_ID,
                notification.getInfo().getEvent().getContext().getCorrelationId());

        ComponentIdentifier sourceIdentifier = getSourceIdentifier(notification);
        if (sourceIdentifier == null) {
            return Optional.of(traceMetadata);
        }

        // Add Custom Business Tags
        TypedValue openTelemetryTags = notification.getEvent().getVariables().get(Constants.VARIABLE_RUNTIME_TAGS);
        if (openTelemetryTags != null) {
            LinkedHashMap objectList = (LinkedHashMap) openTelemetryTags.getValue();
            objectList.forEach((key, value) -> {
                traceMetadata.getTags().put(key.toString(), value.toString());
                log.trace("Custom Tag found. key:{}, value:{}", key, value);
            });
        }

        // Find if there is a processor component to handle flow source component.
        // If exists, allow it to process notification and build any additional tags to
        // include in a trace.
        ProcessorComponentService.getInstance()
                .getProcessorComponentFor(sourceIdentifier, configurationComponentLocator)
                .flatMap(processorComponent -> processorComponent.getSourceEndTraceComponent(notification,
                        contextHandler))
                .ifPresent(sourceTrace -> {
                    traceMetadata.getTags().putAll(sourceTrace.getTags());
                    traceMetadata.setStatusCode(sourceTrace.getStatusCode());
                    traceMetadata.setTags(traceMetadata.getTags());
                });
        return Optional.of(traceMetadata);
    }
}
