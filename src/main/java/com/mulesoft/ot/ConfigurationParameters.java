package com.mulesoft.ot;

public class ConfigurationParameters {
    private final ServiceAttributes serviceAttributes;

    public ConfigurationParameters(ServiceAttributes serviceAttributes) {
        this.serviceAttributes = serviceAttributes;
    }

    public ServiceAttributes getServiceAttributes() {
        return serviceAttributes;
    }
}
