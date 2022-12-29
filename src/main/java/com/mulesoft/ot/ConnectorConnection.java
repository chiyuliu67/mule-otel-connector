package com.mulesoft.ot;

import com.mulesoft.ot.tracing.TraceVault;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class represents an extension connection.
 *
 * <p>
 * The configuration of the library is based:
 * github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure
 *
 * <p>
 * The guide to configure the library for Manual Instrumentation
 * opentelemetry.io/docs/instrumentation/java/manual/
 */
public class ConnectorConnection implements ContextHandler {

    private final Logger log = LoggerFactory.getLogger(ConnectorConnection.class);

    private final TraceVault traceVault;
    private static ConnectorConnection connectorConnection;
    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;

    /*
     * Set the configuration for the Open Telemetry library
     */
    private ConnectorConnection(String serviceName, String additionalTags, String collectorEndpoint) {
        final Map<String, String> configMap = new HashMap<>();

        AutoConfiguredOpenTelemetrySdkBuilder builder = AutoConfiguredOpenTelemetrySdk.builder();
        configMap.put(Constants.OTEL_METRICS_EXPORTER, Constants.NONE);
        configMap.put(Constants.OTEL_SERVICE_NAME, serviceName);
        configMap.put(Constants.OTEL_RESOURCE_ATTRIBUTES, additionalTags);
        configMap.put(Constants.OTEL_TRACES_EXPORTER, Constants.OTLP);
        configMap.put(Constants.OTEL_EXPORTER_OTLP_ENDPOINT, collectorEndpoint);
        builder.addPropertiesSupplier(() -> Collections.unmodifiableMap(configMap));
        log.debug("Configuration: {}", configMap);
        log.debug("Initializing Open Telemetry");

        builder.setServiceClassLoader(AutoConfiguredOpenTelemetrySdkBuilder.class.getClassLoader());
        openTelemetry = builder.build().getOpenTelemetrySdk();
        tracer = openTelemetry.getTracer(Constants.LIBRARY_NAME, Constants.LIBRARY_VERSION);
        traceVault = TraceVault.getInstance();
    }

    public static Optional<ConnectorConnection> get() {
        return Optional.ofNullable(connectorConnection);
    }

    public static synchronized ConnectorConnection getInstance(String serviceName, String additionalTags,
            String collectorEndpoint) {
        if (connectorConnection == null) {
            connectorConnection = new ConnectorConnection(serviceName, additionalTags, collectorEndpoint);
        }
        return connectorConnection;
    }

    public SpanBuilder spanBuilder(String spanName) {
        return tracer.spanBuilder(spanName);
    }

    public <T> Context getTraceContext(T carrier, TextMapGetter<T> textMapGetter) {
        return openTelemetry.getPropagators().getTextMapPropagator().extract(Context.current(), carrier, textMapGetter);
    }

    /** Creates a transaction context for transactionId */
    public Map<String, String> getTraceContext(String transactionId) {
        Context transactionContext = getTraceVault().getContext(transactionId);
        Map<String, String> traceContext = new HashMap<>();
        traceContext.put(Constants.TRACE_TRANSACTION_ID, transactionId);
        traceContext.put(Constants.TRACE_ID, getTraceVault().getTraceIdForTransaction(transactionId));
        try (Scope scope = transactionContext.makeCurrent()) {
            injectTraceContext(traceContext, HashMapTextMapSetter.INSTANCE);
        }
        log.debug("Create transaction context: {}", traceContext);
        return Collections.unmodifiableMap(traceContext);
    }

    public <T> void injectTraceContext(T carrier, TextMapSetter<T> textMapSetter) {
        openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), carrier, textMapSetter);
    }

    public TraceVault getTraceVault() {
        return traceVault;
    }

    public void invalidate() {
    }

    public enum HashMapTextMapSetter implements TextMapSetter<Map<String, String>> {
        INSTANCE;

        @Override
        public void set(@Nullable Map<String, String> carrier, String key, String value) {
            if (carrier != null)
                carrier.put(key, value);
        }
    }
}
