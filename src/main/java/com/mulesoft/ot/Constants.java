package com.mulesoft.ot;

public class Constants {

    // **********************************
    // Connector Library version
    // **********************************
    public static final String LIBRARY_VERSION = "1.0.52";
    public static final String LIBRARY_NAME = "mule-otel-connector";

    // **********************************
    // Variables exposed to the service
    // **********************************

    // Variable with OpenTelemetry data that can be used to forward it to other
    // services
    public static final String VARIABLE_TRACE_DATA = "openTelemetryTrace";

    // Variable inside of VARIABLE_TRACE_DATA
    public static final String TRACE_ID = "id";

    // Variable inside of VARIABLE_TRACE_DATA
    public static final String TRACE_CORRELATION_ID = "correlationId";

    // Using this variable the programmer can add custom tags to the current span
    public static final String VARIABLE_RUNTIME_TAGS = "openTelemetryTags";

    // **********************************
    // Namespaces
    // **********************************
    public static final String NAMESPACE_MULE = "mule";
    public static final String NAMESPACE_DATABASE = "db";
    public static final String FLOW_EVENT = "flow";
    public static final String HTTP_NAMESPACE = "http";

    // **********************************
    // Connector configuration
    // **********************************
    public static final String OTEL_TRACES_EXPORTER = "otel.traces.exporter";
    public static final String OTEL_EXPORTER_OTLP_ENDPOINT = "otel.exporter.otlp.endpoint";
    public static final String OTEL_METRICS_EXPORTER = "otel.metrics.exporter";
    public static final String OTEL_RESOURCE_ATTRIBUTES = "otel.resource.attributes";
    public static final String OTEL_SERVICE_NAME = "otel.service.name";
    public static final String OTLP = "otlp";
    public static final String NONE = "none";

    // **********************************
    // Tags for the traces
    // **********************************
    public static final String SERVER_ID = "mule.serverId";
    public static final String SERVICE_FLOW_NAME = "mule.service.flow.name";
    public static final String SERVICE_FLOW_SOURCE_CONFIGREF = "mule.service.flow.source.configRef";
    public static final String SERVICE_FLOW_SOURCE_NAMESPACE = "mule.service.flow.source.namespace";
    public static final String SERVICE_FLOW_SOURCE_NAME = "mule.service.flow.source.name";
    public static final String SERVICE_PROCESSOR_NAME = "mule.service.processor.name";
    public static final String SERVICE_PROCESSOR_NAMESPACE = "mule.service.processor.namespace";
    public static final String SERVICE_PROCESSOR_DOCNAME = "mule.service.processor.docName";
    public static final String PROCESSOR_CONFIGREF = "mule.service.processor.configRef";
    public static final String HTTP_CONTENT_TYPE = "http.content_type";

    // **********************************
    // General
    // **********************************
    public static final String CORRELATION_ID = "correlationId";
}
