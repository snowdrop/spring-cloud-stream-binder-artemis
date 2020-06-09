package me.snowdrop.stream.binder.artemis.listeners;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;

@EnableBinding(Sink.class)
public class FailingStreamListener {

    private final Logger logger = Logger.getLogger(FailingStreamListener.class);

    private final List<String> receivedMessages = new LinkedList<>();

    private final AtomicInteger errorsCounter = new AtomicInteger();

    @StreamListener(Sink.INPUT)
    public void streamListener(String payload) {
        logger.debug("received: " + payload);
        receivedMessages.add(payload);
        throw new RuntimeException("test");
    }

    @ServiceActivator(inputChannel = "failing-destination-failing-group.errors")
    public void errorListener(Message<?> message) {
        logger.info("received failed: " + message.getPayload());
        errorsCounter.incrementAndGet();
    }

    public List<String> getReceivedMessages() {
        return receivedMessages;
    }

    public int getErrorsCounter() {
        return errorsCounter.get();
    }

}
