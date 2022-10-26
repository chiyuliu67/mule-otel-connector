package com.mulesoft.opentelemetry.internal;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

@Alias("Service")
@SuppressWarnings("unused")
public class ServiceAttributes {

    @Parameter
    @Summary("Service name for this application.")
    @Expression(ExpressionSupport.NOT_SUPPORTED)
    private String serviceName;

    public String getServiceName() {
        return serviceName;
    }

    @Parameter
    @Summary("Additional optional service tags, key=value separated by commas")
    @Expression(ExpressionSupport.NOT_SUPPORTED)
    @Optional
    @Example(value = "environment=${env}, layer=papi")
    private String additionalTags;

    public String getAdditionalTags() {
        return additionalTags;
    }

    @Parameter
    @Optional
    @Summary(value = "The endpoint to send the Open Telemetry traces, by default http://localhost:4317")
    @Example(value = "http://localhost:4317")
    private String collectorEndpoint;

    public String getCollectorEndpoint() {
        return collectorEndpoint;
    }
}
