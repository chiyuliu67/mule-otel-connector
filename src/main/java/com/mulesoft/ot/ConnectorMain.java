package com.mulesoft.ot;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.api.meta.Category;

/**
 * This is the main class of an extension, is the entry point from which
 * configurations, connection providers, operations and sources are going to be
 * declared. Link: docs.mulesoft.com/mule-sdk/1.1/module-structure
 */
@Xml(prefix = "open-telemetry")
@Extension(name = "Open Telemetry Connector", category = Category.COMMUNITY)
@Configurations(ConnectorConfiguration.class)
@SuppressWarnings("unused")
public class ConnectorMain {
}
