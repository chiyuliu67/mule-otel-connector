package com.mulesoft.ot.listeners;

import com.mulesoft.ot.processor.MuleNotificationProcessor;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.runtime.api.notification.PipelineMessageNotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowListener implements PipelineMessageNotificationListener<PipelineMessageNotification> {

    private final MuleNotificationProcessor muleNotificationProcessor;
    private final Logger log = LoggerFactory.getLogger(FlowListener.class);

    public FlowListener(MuleNotificationProcessor muleNotificationProcessor) {
        this.muleNotificationProcessor = muleNotificationProcessor;
    }

    @Override
    public void onNotification(PipelineMessageNotification notification) {
        log.debug("Flow: " + notification.getResourceIdentifier() + ", Action: " + notification.getActionName());

        switch (Integer.parseInt(notification.getAction().getIdentifier())) {
            case PipelineMessageNotification.PROCESS_START :
                muleNotificationProcessor.handleFlowStartEvent(notification);
                break;
            case PipelineMessageNotification.PROCESS_END :
                break;
            case PipelineMessageNotification.PROCESS_COMPLETE :
                muleNotificationProcessor.handleFlowEndEvent(notification);
                break;
        }
    }
}
