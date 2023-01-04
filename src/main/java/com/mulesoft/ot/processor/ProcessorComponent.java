package com.mulesoft.ot.processor;

import com.mulesoft.ot.tracevault.ContextPropagation;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.notification.EnrichedServerNotification;

import java.util.Optional;

public interface ProcessorComponent {
    boolean canHandle(ComponentIdentifier componentIdentifier);

    ProcessorComponent withConfigurationComponentLocator(ConfigurationComponentLocator configurationComponentLocator);

    /**
     * Build a {@link TraceMetadata} for start of a flow-like container or a message
     * processor.
     *
     * @param notification
     *            {@link EnrichedServerNotification}
     * @return {@link TraceMetadata}
     */
    TraceMetadata getStartTraceComponent(EnrichedServerNotification notification);

    /**
     * Build a {@link TraceMetadata} for end of a flow-like container or a message
     * processor. This may need light processing compared to
     * {@link #getStartTraceComponent(EnrichedServerNotification)}.
     *
     * @param notification
     *            {@link EnrichedServerNotification}
     * @return {@link TraceMetadata}
     */
    TraceMetadata getEndTraceComponent(EnrichedServerNotification notification);

    /**
     * If a message processor has a source variation, then this implementation can
     * do more processing of a component.
     *
     * @param notification
     *            {@link EnrichedServerNotification}
     * @param contextPropagation
     *            {@link ContextPropagation} to help extract OpenTelemetry context
     * @return {@link Optional}
     */
    default Optional<TraceMetadata> getSourceStartTraceComponent(EnrichedServerNotification notification,
            ContextPropagation contextPropagation) {
        return Optional.empty();
    }

    /**
     * For flows with a source component, this method can allow processor components
     * to build source specific traces.
     *
     * @param notification
     * @param contextPropagation
     * @return Optional trace component
     */
    default Optional<TraceMetadata> getSourceEndTraceComponent(EnrichedServerNotification notification,
            ContextPropagation contextPropagation) {
        return Optional.empty();
    }
}
