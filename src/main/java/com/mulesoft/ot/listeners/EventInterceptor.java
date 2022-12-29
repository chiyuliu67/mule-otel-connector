package com.mulesoft.ot.listeners;

import com.mulesoft.ot.ConnectorConnection;
import com.mulesoft.ot.Constants;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorParameterValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * www.mulesoft.org/docs/site/4.3.0/apidocs/org/mule/runtime/api/interception/ProcessorInterceptor.html
 * Provides a way to hook behavior around a component that is not a SOURCE
 *
 */
@Component
public class EventInterceptor implements org.mule.runtime.api.interception.ProcessorInterceptor {

    private static final Logger log = LoggerFactory.getLogger(EventInterceptor.class);
    private final Supplier<Optional<ConnectorConnection>> connectionSupplier = ConnectorConnection::get;

    @Override
    public void before(ComponentLocation location, Map<String, ProcessorParameterValue> parameters,
            InterceptionEvent event) {

        connectionSupplier.get().ifPresent(connection -> {
            if (log.isDebugEnabled()) {
                StringBuilder localization = new StringBuilder("Adds a new tracing context to the component. ");
                location.getFileName().ifPresent(filename -> localization.append("File: ").append(filename));
                localization.append(" Root: ").append(location.getRootContainerName());
                location.getLine().ifPresent(line -> localization.append(" Line: ").append(line));
                location.getColumn().ifPresent(column -> localization.append(" Column: ").append(column));
                log.debug(localization.toString());
            }

            event.addVariable(Constants.VARIABLE_TRACE_DATA, connection.getTraceContext(event.getCorrelationId()));
        });
    }
}
