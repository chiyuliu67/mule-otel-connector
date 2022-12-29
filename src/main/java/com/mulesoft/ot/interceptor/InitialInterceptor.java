package com.mulesoft.ot.interceptor;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.springframework.stereotype.Component;

/**
 * Will intercept the root processor flow. This class must be registered in
 * registry-bootstrap.properties
 *
 * <p>
 * Disable tracing by setting "otel.mule.enabletracing" to `false`.
 *
 * <p>
 * See registry-bootstrap.properties.
 */
@Component
public class InitialInterceptor implements ProcessorInterceptorFactory {

    public static final String OTEL_MULE_ENABLETRACING = "otel.mule.enabletracing";
    private final boolean enableTracing = Boolean.parseBoolean(System.getProperty(OTEL_MULE_ENABLETRACING, "true"));

    @Override
    public org.mule.runtime.api.interception.ProcessorInterceptor get() {
        return new ProcessorEvent();
    }

    /**
     * This intercepts the first processor of root container
     *
     * @param location
     *            {@link ComponentLocation}
     * @return true if intercept
     */
    @Override
    public boolean intercept(ComponentLocation location) {
        return enableTracing && location.getLocation().endsWith("/0");
    }
}
