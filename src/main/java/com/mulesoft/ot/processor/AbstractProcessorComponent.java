package com.mulesoft.ot.processor;

import java.util.*;

import com.mulesoft.ot.Constants;
import io.opentelemetry.api.trace.SpanKind;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.notification.EnrichedServerNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractProcessorComponent implements ProcessorComponent {
    private final Logger log = LoggerFactory.getLogger(AbstractProcessorComponent.class);

    protected ConfigurationComponentLocator configurationComponentLocator;

    @Override
    public ProcessorComponent withConfigurationComponentLocator(
            ConfigurationComponentLocator configurationComponentLocator) {
        this.configurationComponentLocator = configurationComponentLocator;
        return this;
    }

    protected abstract String getNamespace();

    protected abstract List<String> getOperations();

    protected abstract List<String> getSources();

    protected SpanKind getSpanKind() {
        return SpanKind.INTERNAL;
    }

    @Override
    public boolean canHandle(ComponentIdentifier componentIdentifier) {
        return getNamespace().equalsIgnoreCase(componentIdentifier.getNamespace())
                && (getOperations().contains(componentIdentifier.getName().toLowerCase())
                        || getSources().contains(componentIdentifier.getName().toLowerCase()));
    }

    protected boolean namespaceSupported(ComponentIdentifier componentIdentifier) {
        return getNamespace().equalsIgnoreCase(componentIdentifier.getNamespace().toLowerCase());
    }

    protected boolean operationSupported(ComponentIdentifier componentIdentifier) {
        return getOperations().contains(componentIdentifier.getName().toLowerCase());
    }

    protected boolean sourceSupported(ComponentIdentifier componentIdentifier) {
        return getSources().contains(componentIdentifier.getName().toLowerCase());
    }

    public TraceMetadata getTraceComponentEnd(EnrichedServerNotification enrichedServerNotification) {
        TraceMetadata traceMetadata = new TraceMetadata();
        traceMetadata.setName(enrichedServerNotification.getResourceIdentifier());
        traceMetadata.setCorrelationId(enrichedServerNotification.getEvent().getCorrelationId());
        traceMetadata.setLocation(enrichedServerNotification.getComponent().getLocation().getLocation());
        traceMetadata.setTags(new HashMap<>());
        traceMetadata.setErrorMessage(
                enrichedServerNotification.getEvent().getError().map(Error::getDescription).orElse(null));
        return traceMetadata;
    }

    @Override
    public TraceMetadata getEndTraceComponent(EnrichedServerNotification notification) {
        return getTraceComponentBuilderFor(notification);
    }

    protected TraceMetadata getTraceComponentBuilderFor(EnrichedServerNotification notification) {
        TraceMetadata traceMetadata = new TraceMetadata();
        traceMetadata.setName(notification.getResourceIdentifier());
        traceMetadata.setCorrelationId(getTransactionId(notification));
        traceMetadata.setLocation(notification.getComponent().getLocation().getLocation());
        traceMetadata.setTags(new HashMap<>());
        traceMetadata.setErrorMessage(notification.getEvent().getError().map(Error::getDescription).orElse(null));
        return traceMetadata;
    }

    protected String getTransactionId(EnrichedServerNotification notification) {
        return notification.getEvent().getCorrelationId();
    }

    protected ComponentIdentifier getSourceIdentifier(EnrichedServerNotification notification) {
        ComponentIdentifier sourceIdentifier = null;
        if (notification.getEvent() != null && notification.getEvent().getContext().getOriginatingLocation() != null
                && notification.getResourceIdentifier().equalsIgnoreCase(
                        notification.getEvent().getContext().getOriginatingLocation().getRootContainerName())) {
            sourceIdentifier = notification.getEvent().getContext().getOriginatingLocation().getComponentIdentifier()
                    .getIdentifier();
        }
        return sourceIdentifier;
    }

    protected <A> Map<String, String> getAttributes(Component component, TypedValue<A> attributes) {
        return Collections.emptyMap();
    }

    @Override
    public TraceMetadata getStartTraceComponent(EnrichedServerNotification notification) {
        Map<String, String> tags = new HashMap<>();
        String processorName = notification.getComponent().getIdentifier().getNamespace();
        tags.put(Constants.SERVICE_PROCESSOR_NAMESPACE, processorName);
        tags.put(Constants.SERVICE_PROCESSOR_NAME, notification.getComponent().getIdentifier().getName());

        ComponentWrapper wrapper = new ComponentWrapper(notification.getInfo().getComponent(),
                configurationComponentLocator);

        if (wrapper.getDocName() != null) {
            tags.put(Constants.SERVICE_PROCESSOR_DOCNAME, wrapper.getDocName());
        }

        if (wrapper.getConfigRef() != null) {
            tags.put(Constants.PROCESSOR_CONFIGREF, wrapper.getConfigRef());
        }

        tags.putAll(getAttributes(notification.getInfo().getComponent(),
                notification.getEvent().getMessage().getAttributes()));

        String spanName = processorName.concat(":")
                .concat(tags.getOrDefault(Constants.SERVICE_PROCESSOR_DOCNAME, processorName));

        TraceMetadata traceMetadata = new TraceMetadata();
        traceMetadata.setName(notification.getComponent().getLocation().getLocation());
        traceMetadata.setLocation(notification.getComponent().getLocation().getLocation());
        traceMetadata.setSpanName(spanName);
        traceMetadata.setTags(tags);
        traceMetadata.setSpanKind(getSpanKind());
        traceMetadata.setCorrelationId(getTransactionId(notification));
        log.trace("Span name {}", spanName);
        return traceMetadata;
    }

    protected void addTagIfPresent(Map<String, String> sourceMap, String sourceKey, Map<String, String> targetMap,
            String targetKey) {
        if (sourceMap.containsKey(sourceKey))
            targetMap.put(targetKey, sourceMap.get(sourceKey));
    }
}
