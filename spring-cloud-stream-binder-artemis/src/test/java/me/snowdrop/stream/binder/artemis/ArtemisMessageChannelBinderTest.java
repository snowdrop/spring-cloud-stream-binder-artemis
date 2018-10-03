package me.snowdrop.stream.binder.artemis;

import me.snowdrop.stream.binder.artemis.handlers.ArtemisMessageHandler;
import me.snowdrop.stream.binder.artemis.provisioning.ArtemisConsumerDestination;
import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.messaging.MessageHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisMessageChannelBinderTest {

    private ArtemisMessageChannelBinder binder;

    @Before
    public void before() {
        binder = new ArtemisMessageChannelBinder(null, null, null, null);
    }

    @Test
    public void shouldCreateProducerMessageHandler() {
        MessageHandler handler = binder.createProducerMessageHandler(null, null, null);

        assertThat(handler).isInstanceOf(ArtemisMessageHandler.class);
    }

    @Test
    public void shouldCreateConsumerEndpoint() {
        ArtemisConsumerDestination destination = new ArtemisConsumerDestination("");
        MessageProducer producer = binder.createConsumerEndpoint(destination, null, null);

        assertThat(producer).isInstanceOf(JmsMessageDrivenEndpoint.class);
    }
}
