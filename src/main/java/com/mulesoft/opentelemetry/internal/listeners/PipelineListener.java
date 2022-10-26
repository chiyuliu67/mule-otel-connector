package com.mulesoft.opentelemetry.internal.listeners;

import com.mulesoft.opentelemetry.internal.processor.MuleNotificationProcessor;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.runtime.api.notification.PipelineMessageNotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipelineListener implements PipelineMessageNotificationListener<PipelineMessageNotification> {

    private final MuleNotificationProcessor muleNotificationProcessor;
    private final Logger log = LoggerFactory.getLogger(PipelineListener.class);

    public PipelineListener(MuleNotificationProcessor muleNotificationProcessor) {
        this.muleNotificationProcessor = muleNotificationProcessor;
    }

    @Override
    public void onNotification(PipelineMessageNotification notification) {
        log.debug("PipelineListener Resource:" + notification.getResourceIdentifier() + " Action:"
                + notification.getActionName());

        switch (Integer.parseInt(notification.getAction().getIdentifier())) {
            case PipelineMessageNotification.PROCESS_START :
                log.debug("PROCESS_START");
                muleNotificationProcessor.handleFlowStartEvent(notification);
                break;
            case PipelineMessageNotification.PROCESS_END :
                log.debug("PROCESS_END");
                break;
            case PipelineMessageNotification.PROCESS_COMPLETE :
                log.debug("PROCESS_COMPLETE");
                muleNotificationProcessor.handleFlowEndEvent(notification);
                break;
        }
    }
}
