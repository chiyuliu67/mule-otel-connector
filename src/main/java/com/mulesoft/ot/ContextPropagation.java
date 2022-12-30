package com.mulesoft.ot;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;

/*
 * Operations to retrieve or inject the Trace Context
 * github.com/open-telemetry/opentelemetry-java/blob/main/context/src/main/java/io/opentelemetry/context/Context.java
 */
public interface ContextPropagation {

    <T> Context get(T carrier, TextMapGetter<T> textMapGetter);

    <T> void set(T carrier, TextMapSetter<T> textMapSetter);
}
