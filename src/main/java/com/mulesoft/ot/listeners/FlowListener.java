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
        log.debug("FlowListener registered");
        this.muleNotificationProcessor = muleNotificationProcessor;
    }

    @Override
    public void onNotification(PipelineMessageNotification notification) {
        if (log.isDebugEnabled()) {
            String actionName = "Other type";
            switch (Integer.parseInt(notification.getAction().getIdentifier())) {
                case PipelineMessageNotification.PROCESS_START :
                    actionName = "Start";
                    break;
                case PipelineMessageNotification.PROCESS_COMPLETE :
                    actionName = "Complete";
                    break;
            }
            log.debug("Flow: {}, Action: {}", notification.getResourceIdentifier(), actionName);
        }
        switch (Integer.parseInt(notification.getAction().getIdentifier())) {
            case PipelineMessageNotification.PROCESS_START :
                muleNotificationProcessor.handleFlowStartEvent(notification);
                break;
            case PipelineMessageNotification.PROCESS_COMPLETE :
                muleNotificationProcessor.handleFlowEndEvent(notification);
                break;
        }
    }
}
