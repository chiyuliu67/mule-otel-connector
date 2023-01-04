package com.mulesoft.ot.processor;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ComponentWrapper {

    public static final String COMPONENT_NAMESPACE_KEY = "component:namespace";
    public static final String COMPONENT_NAME_KEY = "component:name";
    private final Component component;
    private final Map<String, String> parameters;
    private final ConfigurationComponentLocator configurationComponentLocator;
    private static final Logger log = LoggerFactory.getLogger(ComponentWrapper.class);

    public ComponentWrapper(Component component, ConfigurationComponentLocator configurationComponentLocator) {
        this.component = component;
        this.configurationComponentLocator = configurationComponentLocator;
        parameters = Collections.unmodifiableMap(getComponentAnnotation("{config}componentParameters"));
    }

    public Component getComponent() {
        return component;
    }

    private <T> T getComponentAnnotation(String annotationName) {
        return (T) component.getAnnotation(QName.valueOf(annotationName));
    }

    /**
     * Get Component Parameters Map
     *
     * @return {@link Map}
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getConfigRef() {
        return parameters.get("config-ref");
    }

    public String getDocName() {
        return parameters.get("doc:name");
    }

    public Map<String, String> getConfigConnectionParameters() {
        String componentConfigRef = getConfigRef();
        try {
            return configurationComponentLocator
                    .find(Location.builder().globalName(componentConfigRef).addConnectionPart().build())
                    .map(component1 -> new ComponentWrapper(component1, configurationComponentLocator))
                    .map(this::toExtendedParameters).orElse(Collections.emptyMap());
        } catch (Exception ex) {
            log.trace("Failed to extract connection parameters for {}. Ignoring this failure - {}", componentConfigRef,
                    ex.getMessage());
            return Collections.emptyMap();
        }
    }

    public Map<String, String> getConfigParameters() {
        String componentConfigRef = getConfigRef();
        try {
            return configurationComponentLocator.find(Location.builder().globalName(componentConfigRef).build())
                    .map(componentVar -> new ComponentWrapper(componentVar, configurationComponentLocator))
                    .map(this::toExtendedParameters).orElse(Collections.emptyMap());
        } catch (Exception ex) {
            log.trace("Failed to extract connection parameters for {}. Ignoring this failure - {}", componentConfigRef,
                    ex.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<String, String> toExtendedParameters(ComponentWrapper componentWrapper) {
        Map<String, String> map = new HashMap<>(componentWrapper.getParameters());
        map.put(COMPONENT_NAMESPACE_KEY, componentWrapper.getComponent().getIdentifier().getNamespace());
        map.put(COMPONENT_NAME_KEY, componentWrapper.getComponent().getIdentifier().getName());
        return map;
    }
}
