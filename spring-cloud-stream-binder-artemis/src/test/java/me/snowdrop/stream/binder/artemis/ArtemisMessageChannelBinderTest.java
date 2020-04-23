package me.snowdrop.stream.binder.artemis;

import me.snowdrop.stream.binder.artemis.listener.RetryableChannelPublishingJmsMessageListener;
import me.snowdrop.stream.binder.artemis.properties.ArtemisConsumerProperties;
import me.snowdrop.stream.binder.artemis.provisioning.ArtemisConsumerDestination;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.integration.jms.JmsSendingMessageHandler;
import org.springframework.messaging.MessageHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class ArtemisMessageChannelBinderTest {

    @Mock
    private ExtendedConsumerProperties<ArtemisConsumerProperties> mockConsumerProperties;

    @Mock
    private DefaultListableBeanFactory mockBeanFactory;

    private ArtemisMessageChannelBinder binder;

    @Before
    public void before() {
        binder = new ArtemisMessageChannelBinder(null, null, null);
        binder.setApplicationContext(new GenericApplicationContext(mockBeanFactory));
    }

    @Test
    public void shouldCreateProducerMessageHandler() {
        MessageHandler handler = binder.createProducerMessageHandler(null, null, null);

        assertThat(handler).isInstanceOf(JmsSendingMessageHandler.class);
    }

    @Test
    public void shouldCreateRegularConsumerEndpoint() {
        given(mockConsumerProperties.getMaxAttempts()).willReturn(1);

        ArtemisConsumerDestination destination = new ArtemisConsumerDestination("test-destination");
        MessageProducer producer = binder.createConsumerEndpoint(destination, "test-group", mockConsumerProperties);

        assertThat(producer).isInstanceOf(JmsMessageDrivenEndpoint.class);

        JmsMessageDrivenEndpoint endpoint = (JmsMessageDrivenEndpoint) producer;
        assertThat(endpoint.getListener()).isNotInstanceOf(RetryableChannelPublishingJmsMessageListener.class);
    }

    @Test
    public void shouldCreateRetryableConsumerEndpoint() {
        given(mockConsumerProperties.getMaxAttempts()).willReturn(2);

        ArtemisConsumerDestination destination = new ArtemisConsumerDestination("test-destination");
        MessageProducer producer = binder.createConsumerEndpoint(destination, "test-group", mockConsumerProperties);

        assertThat(producer).isInstanceOf(JmsMessageDrivenEndpoint.class);

        ChannelPublishingJmsMessageListener listener = ((JmsMessageDrivenEndpoint) producer).getListener();
        assertThat(listener).isInstanceOf(RetryableChannelPublishingJmsMessageListener.class);
        assertThat(listener.getComponentType()).isEqualTo("jms:message-driven-channel-adapter");
    }
}
