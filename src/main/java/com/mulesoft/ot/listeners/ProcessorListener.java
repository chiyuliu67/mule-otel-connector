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
        log.debug("ProcessorListener Resource:" + notification.getResourceIdentifier() + " Action:"
                + notification.getActionName());

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
