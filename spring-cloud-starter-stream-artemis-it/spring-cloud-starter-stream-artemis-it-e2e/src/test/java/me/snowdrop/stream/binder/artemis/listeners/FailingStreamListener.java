package me.snowdrop.stream.binder.artemis.listeners;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;

@EnableBinding(Sink.class)
public class FailingStreamListener {

    private AtomicInteger invocationsCounter = new AtomicInteger();

    private AtomicInteger errorsCounter = new AtomicInteger();

    @StreamListener(Sink.INPUT)
    public void streamListener(String payload) {
        invocationsCounter.incrementAndGet();
        throw new RuntimeException("test");
    }

    @ServiceActivator(inputChannel = "failing-destination-failing-group.errors")
    public void error(Message<?> message) {
        errorsCounter.incrementAndGet();
    }

    public int getInvocationsCounter() {
        return invocationsCounter.get();
    }

    public int getErrorsCounter() {
        return errorsCounter.get();
    }

}
