package com.mulesoft.ot;

import com.mulesoft.ot.listeners.ProcessorListener;
import com.mulesoft.ot.listeners.FlowListener;
import com.mulesoft.ot.processor.MuleNotificationProcessor;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Configuration to send the traces to an Open Telemetry collector. The
 * connector implements OpenTelemetry SDK Autoconfigure.
 */
@Configuration
public class ConnectorConfiguration implements Startable {

    private final Logger log = LoggerFactory.getLogger(ConnectorConfiguration.class);

    @Parameter
    @Summary("Service name for this application")
    @Expression(ExpressionSupport.NOT_SUPPORTED)
    String serviceName;

    @Parameter
    @Summary("Additional optional service tags, key=value separated by commas")
    @Expression(ExpressionSupport.NOT_SUPPORTED)
    @Optional
    @Example(value = "environment=${env}, layer=papi")
    String additionalTags;

    @Parameter
    @Optional
    @Summary(value = "The endpoint to send the Open Telemetry traces, by default http://localhost:4317")
    @Example(value = "http://localhost:4317")
    String collectorEndpoint;

    @Inject
    NotificationListenerRegistry notificationListenerRegistry;

    @Inject
    MuleNotificationProcessor muleNotificationProcessor;

    @Override
    public void start() {
        log.debug("OpenTelemetry Connector Initialization, registering listeners and configuration");

        muleNotificationProcessor
                .init(() -> ConnectorConnection.getInstance(serviceName, additionalTags, collectorEndpoint));

        notificationListenerRegistry.registerListener(new ProcessorListener(muleNotificationProcessor));
        notificationListenerRegistry.registerListener(new FlowListener(muleNotificationProcessor));
    }
}
