package com.mulesoft.ot;

import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@SuppressWarnings("unused")
public class ConnectionProvider implements CachedConnectionProvider<ConnectorConnection> {

    private final Logger log = LoggerFactory.getLogger(ConnectionProvider.class);

    @Inject
    NotificationListenerRegistry notificationListenerRegistry;

    @Override
    public ConnectorConnection connect() throws ConnectionException {
        return ConnectorConnection.get().orElseThrow(
                () -> new ConnectionException("Configuration must first start for OpenTelemetry connection."));
    }

    @Override
    public void disconnect(ConnectorConnection connection) {
        try {
            connection.invalidate();
        } catch (Exception e) {
            log.error("Error while disconnecting: " + e.getMessage(), e);
        }
    }

    @Override
    public ConnectionValidationResult validate(ConnectorConnection connection) {
        return ConnectionValidationResult.success();
    }
}
