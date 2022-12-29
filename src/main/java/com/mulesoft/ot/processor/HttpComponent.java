package com.mulesoft.ot.processor;

import com.mulesoft.ot.Constants;
import com.mulesoft.ot.ContextHandler;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.notification.EnrichedServerNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.*;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

public class HttpComponent extends AbstractProcessorComponent {

    private final Logger log = LoggerFactory.getLogger(HttpComponent.class);

    @Override
    protected String getNamespace() {
        return Constants.HTTP_NAMESPACE;
    }

    @Override
    protected List<String> getOperations() {
        return singletonList("request");
    }

    @Override
    protected List<String> getSources() {
        return singletonList("listener");
    }

    @Override
    protected SpanKind getSpanKind() {
        return SpanKind.CLIENT;
    }

    @Override
    public TraceMetadata getEndTraceComponent(EnrichedServerNotification notification) {
        TraceMetadata endTraceMetadata = super.getEndTraceComponent(notification);

        // When error is thrown by http:request, the response msg will be on error obj
        Message responseMessage = notification.getEvent().getError().map(Error::getErrorMessage)
                .orElse(notification.getEvent().getMessage());
        TypedValue<HttpResponseAttributes> responseAttributes = responseMessage.getAttributes();

        if (responseAttributes.getValue() == null
                || !(responseAttributes.getValue() instanceof HttpResponseAttributes)) {
            return endTraceMetadata;
        }

        // If request generate error, then error message includes the HTTP Response
        // attributes.
        HttpResponseAttributes attributes = responseAttributes.getValue();
        Map<String, String> tags = new HashMap<>();
        tags.put(HTTP_STATUS_CODE.getKey(), Integer.toString(attributes.getStatusCode()));
        endTraceMetadata.setStatusCode(getSpanStatus(false, attributes.getStatusCode()));
        tags.put(HTTP_RESPONSE_CONTENT_LENGTH.getKey(), attributes.getHeaders().get("content-length"));
        if (endTraceMetadata.getTags() != null) {
            tags.putAll(endTraceMetadata.getTags());
        }
        endTraceMetadata.setTags(tags);
        return endTraceMetadata;
    }

    @Override
    public TraceMetadata getStartTraceComponent(EnrichedServerNotification notification) {

        TraceMetadata traceMetadata = super.getStartTraceComponent(notification);
        Map<String, String> requestTags = getAttributes(notification.getInfo().getComponent(),
                notification.getEvent().getMessage().getAttributes());
        requestTags.putAll(traceMetadata.getTags());

        traceMetadata.setName(notification.getResourceIdentifier());
        traceMetadata.setTags(requestTags);
        traceMetadata.setLocation(notification.getComponent().getLocation().getLocation());
        traceMetadata.setSpanName(requestTags.get(HTTP_ROUTE.getKey()));
        traceMetadata.setCorrelationId(traceMetadata.getCorrelationId());
        traceMetadata.setSpanKind(getSpanKind());
        return traceMetadata;
    }

    @Override
    protected <A> Map<String, String> getAttributes(Component component, TypedValue<A> attributes) {
        ComponentWrapper componentWrapper = new ComponentWrapper(component, configurationComponentLocator);
        Map<String, String> tags = new HashMap<>();
        if (isOutputRequest(component.getIdentifier())) {
            tags.putAll(getOutputMetadata(componentWrapper));
        } else {
            tags.putAll(requestHeaders((HttpRequestAttributes) attributes.getValue()));
        }
        return tags;
    }

    @Override
    public Optional<TraceMetadata> getSourceStartTraceComponent(EnrichedServerNotification notification,
            ContextHandler contextHandler) {
        if (!isListenerFlowEvent(notification)) {
            return Optional.empty();
        }
        TypedValue<HttpRequestAttributes> attributesTypedValue = notification.getEvent().getMessage().getAttributes();
        HttpRequestAttributes attributes = attributesTypedValue.getValue();
        Map<String, String> tags = requestHeaders(attributes);
        TraceMetadata traceMetadata = new TraceMetadata();
        traceMetadata.setName(notification.getResourceIdentifier());
        traceMetadata.setTags(tags);
        traceMetadata.setCorrelationId(getTransactionId(notification));
        traceMetadata.setSpanName(attributes.getListenerPath());
        traceMetadata.setContext(contextHandler.getTraceContext(attributes.getHeaders(), ContextMapGetter.INSTANCE));
        return Optional.of(traceMetadata);
    }

    @Override
    public Optional<TraceMetadata> getSourceEndTraceComponent(EnrichedServerNotification notification,
            ContextHandler contextHandler) {
        TypedValue<?> httpStatus = notification.getEvent().getVariables().get("httpStatus");
        if (httpStatus != null) {
            String statusCode = TypedValue.unwrap(httpStatus).toString();
            TraceMetadata traceMetadata = getTraceComponentBuilderFor(notification);
            traceMetadata.setTags(singletonMap(HTTP_STATUS_CODE.getKey(), statusCode));
            traceMetadata.setStatusCode(getSpanStatus(true, Integer.parseInt(statusCode)));
            return Optional.of(traceMetadata);
        }
        return Optional.empty();
    }

    // ************************************
    // Utility Methods
    // ************************************

    private boolean isOutputRequest(ComponentIdentifier componentIdentifier) {
        return namespaceSupported(componentIdentifier) && operationSupported(componentIdentifier);
    }

    private boolean isHttpListener(ComponentIdentifier componentIdentifier) {
        return namespaceSupported(componentIdentifier) && sourceSupported(componentIdentifier);
    }

    private boolean isListenerFlowEvent(EnrichedServerNotification notification) {
        return Constants.FLOW_EVENT.equals(notification.getComponent().getIdentifier().getName())
                && isHttpListener(getSourceIdentifier(notification));
    }

    private StatusCode getSpanStatus(boolean isServer, int statusCode) {
        StatusCode result;
        int maxStatus = isServer ? 500 : 400;
        if (statusCode >= 100 && statusCode < maxStatus) {
            result = StatusCode.UNSET;
        } else {
            result = StatusCode.ERROR;
        }
        return result;
    }

    private Map<String, String> getOutputMetadata(ComponentWrapper componentWrapper) {
        Map<String, String> tags = new HashMap<>();
        String path = componentWrapper.getParameters().get("path");
        Map<String, String> connectionParameters = componentWrapper.getConfigConnectionParameters();
        if (!connectionParameters.isEmpty()) {
            tags.put(HTTP_SCHEME.getKey(), connectionParameters.getOrDefault("protocol", "").toLowerCase());
            tags.put(HTTP_HOST.getKey(), connectionParameters.getOrDefault("host", "").concat(":")
                    .concat(connectionParameters.getOrDefault("port", "")));
            tags.put(NET_PEER_NAME.getKey(), connectionParameters.getOrDefault("host", ""));
            tags.put(NET_PEER_PORT.getKey(), connectionParameters.getOrDefault("port", ""));
        }
        Map<String, String> configParameters = componentWrapper.getConfigParameters();
        if (!configParameters.isEmpty()) {
            if (configParameters.containsKey("basePath") && !configParameters.get("basePath").equalsIgnoreCase("/")) {
                path = configParameters.get("basePath").concat(path);
            }
        }
        tags.put(HTTP_ROUTE.getKey(), path);
        tags.put(HTTP_METHOD.getKey(), componentWrapper.getParameters().get("method"));
        return tags;
    }

    private Map<String, String> requestHeaders(HttpRequestAttributes attributes) {
        log.trace("Request headers: {}", attributes.getHeaders().toString());
        Map<String, String> tags = new HashMap<>();
        tags.put(HTTP_HOST.getKey(), attributes.getHeaders().get("host"));
        tags.put(HTTP_USER_AGENT.getKey(), attributes.getHeaders().get("user-agent"));
        tags.put(HTTP_REQUEST_CONTENT_LENGTH.getKey(), attributes.getHeaders().get("content-length"));
        tags.put(Constants.HTTP_CONTENT_TYPE, attributes.getHeaders().get("content-type"));
        tags.put(HTTP_METHOD.getKey(), attributes.getMethod());
        tags.put(HTTP_SCHEME.getKey(), attributes.getScheme());
        tags.put(HTTP_ROUTE.getKey(), attributes.getListenerPath());
        tags.put(HTTP_TARGET.getKey(), attributes.getRequestPath());
        tags.put(HTTP_FLAVOR.getKey(), attributes.getVersion());
        return tags;
    }

    protected enum ContextMapGetter implements TextMapGetter<Map<String, String>> {
        INSTANCE;

        @Override
        public Iterable<String> keys(Map<String, String> map) {
            return map.keySet();
        }

        @Nullable
        @Override
        public String get(@Nullable Map<String, String> map, String s) {
            return map == null ? null : map.get(s);
        }
    }
}
