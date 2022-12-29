package com.mulesoft.ot.interceptor;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.springframework.stereotype.Component;

/**
 * Will intercept the root processor flow. This class must be registered in registry-bootstrap.properties
 */
@Component
public class InitialInterceptor implements ProcessorInterceptorFactory {

    @Override
    public org.mule.runtime.api.interception.ProcessorInterceptor get() {
        return new ProcessorEvent();
    }

    /**
     * This intercepts the first processor of root container
     */
    @Override
    public boolean intercept(ComponentLocation location) {
        return true;
    }
}
