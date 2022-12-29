package com.mulesoft.ot;

import com.mulesoft.ot.listeners.ProcessorListener;
import com.mulesoft.ot.listeners.FlowListener;
import com.mulesoft.ot.processor.MuleNotificationProcessor;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Configuration to send the traces to a collector. The connector implements
 * OpenTelemetry SDK Autoconfigure. More info please visit the connector GitHub
 * repository.
 */
@Configuration
public class ConnectorConfiguration implements Startable {

    private final Logger log = LoggerFactory.getLogger(ConnectorConfiguration.class);

    @ParameterGroup(name = "Service")
    @Placement(order = 1)
    @Summary("Service attributes")
    private ServiceAttributes serviceAttributes;

    public ServiceAttributes getServiceAttributes() {
        return serviceAttributes;
    }

    @Inject
    NotificationListenerRegistry notificationListenerRegistry;

    @Inject
    MuleNotificationProcessor muleNotificationProcessor;

    @Override
    public void start() {
        log.debug("OpenTelemetry Connector Initialization, registering listeners and configuration");
        muleNotificationProcessor
                .init(() -> ConnectorConnection.getInstance(new ConfigurationParameters(getServiceAttributes())));
        notificationListenerRegistry.registerListener(new ProcessorListener(muleNotificationProcessor));
        notificationListenerRegistry.registerListener(new FlowListener(muleNotificationProcessor));
    }
}
