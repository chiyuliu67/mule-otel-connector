package com.mulesoft.ot.listeners;

import com.mulesoft.ot.processor.MuleNotificationProcessor;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.api.notification.MessageProcessorNotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessorListener implements MessageProcessorNotificationListener<MessageProcessorNotification> {

    private final MuleNotificationProcessor muleNotificationProcessor;
    private final Logger log = LoggerFactory.getLogger(ProcessorListener.class);

    public ProcessorListener(MuleNotificationProcessor muleNotificationProcessor) {
        log.debug("ProcessorListener registered");
        this.muleNotificationProcessor = muleNotificationProcessor;
    }

    @Override
    public void onNotification(MessageProcessorNotification notification) {
        if (log.isDebugEnabled()) {
            String actionName = "Other type";
            switch (Integer.parseInt(notification.getAction().getIdentifier())) {
                case MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE :
                    actionName = "Pre invoke";
                    break;

                case MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE :
                    actionName = "Post invoke";
                    break;
            }
            log.debug("Resource: {}, Action: {}", notification.getResourceIdentifier(), actionName);
        }

        switch (Integer.parseInt(notification.getAction().getIdentifier())) {
            case MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE :
                muleNotificationProcessor.handleProcessorStartEvent(notification);
                break;

            case MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE :
                muleNotificationProcessor.handleProcessorEndEvent(notification);
                break;
        }
    }
}
