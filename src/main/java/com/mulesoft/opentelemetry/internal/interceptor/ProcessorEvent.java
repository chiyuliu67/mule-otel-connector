package com.mulesoft.opentelemetry.internal.interceptor;

import com.mulesoft.opentelemetry.internal.ConnectorConnection;
import com.mulesoft.opentelemetry.internal.Constants;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorParameterValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Component
public class ProcessorEvent implements org.mule.runtime.api.interception.ProcessorInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ProcessorEvent.class);
    private final Supplier<Optional<ConnectorConnection>> connectionSupplier = ConnectorConnection::get;

    @Override
    public void before(ComponentLocation location, Map<String, ProcessorParameterValue> parameters,
            InterceptionEvent event) {
        log.debug(" File:" + location.getFileName().get() + "Root: " + location.getRootContainerName() + " Line:"
                + location.getLine().getAsInt() + " Column:" + location.getColumn().getAsInt() + " Identifier: "
                + location.getComponentIdentifier().getIdentifier().getName());
        connectionSupplier.get().ifPresent(connection -> {
            event.addVariable(Constants.VARIABLE_TRACE_DATA, connection.getTraceContext(event.getCorrelationId()));
        });
    }
}
