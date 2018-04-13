package me.snowdrop.stream.binder.artemis;

import me.snowdrop.stream.binder.artemis.handlers.ArtemisMessageHandler;
import me.snowdrop.stream.binder.artemis.handlers.ListenerContainerFactory;
import me.snowdrop.stream.binder.artemis.properties.ArtemisConsumerProperties;
import me.snowdrop.stream.binder.artemis.properties.ArtemisExtendedBindingProperties;
import me.snowdrop.stream.binder.artemis.properties.ArtemisProducerProperties;
import me.snowdrop.stream.binder.artemis.provisioning.ArtemisProvisioningProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.dsl.jms.JmsMessageDrivenChannelAdapter;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.messaging.MessageHandler;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.Topic;

import static me.snowdrop.stream.binder.artemis.common.NamingUtils.getQueueName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisMessageChannelBinderTest {

    private final String address = "testAddress";

    @Mock
    private ArtemisProvisioningProvider mockProvisioningProvider;

    @Mock
    private ConnectionFactory mockConnectionFactory;

    @Mock
    private JMSContext mockJmsContext;

    @Mock
    private Topic mockTopic;

    @Mock
    private ListenerContainerFactory mockListenerContainerFactory;

    @Mock
    private AbstractMessageListenerContainer mockListenerContainer;

    @Mock
    private MessageConverter mockMessageConverter;

    @Mock
    private ProducerDestination mockProducerDestination;

    @Mock
    private ConsumerDestination mockConsumerDestination;

    @Mock
    private ArtemisExtendedBindingProperties mockBindingProperties;

    @Mock
    private ExtendedProducerProperties<ArtemisProducerProperties> mockProducerProperties;

    @Mock
    private ExtendedConsumerProperties<ArtemisConsumerProperties> mockConsumerProperties;

    private ArtemisMessageChannelBinder binder;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        when(mockConnectionFactory.createContext()).thenReturn(mockJmsContext);
        when(mockJmsContext.createTopic(eq(address))).thenReturn(mockTopic);
        when(mockListenerContainerFactory.getListenerContainer(eq(mockTopic), any())).thenReturn(mockListenerContainer);
        when(mockProducerDestination.getName()).thenReturn(address);
        when(mockConsumerDestination.getName()).thenReturn(address);

        binder = new ArtemisMessageChannelBinder(mockProvisioningProvider, mockConnectionFactory,
                mockListenerContainerFactory, mockMessageConverter, mockBindingProperties);
    }

    @Test
    public void shouldCreateProducerMessageHandler() throws Exception {
        when(mockProducerProperties.isPartitioned()).thenReturn(false);

        MessageHandler handler =
                binder.createProducerMessageHandler(mockProducerDestination, mockProducerProperties, null);

        assertThat(handler).isInstanceOf(ArtemisMessageHandler.class);
    }

    @Test
    public void shouldCreateConsumerEndpoint() throws Exception {
        String group = "testGroup";
        MessageProducer producer =
                binder.createConsumerEndpoint(mockConsumerDestination, group, mockConsumerProperties);

        assertThat(producer).isInstanceOf(JmsMessageDrivenChannelAdapter.class);

        verify(mockConnectionFactory, times(1)).createContext();
        verify(mockJmsContext, times(1)).createTopic(address);
        verify(mockListenerContainerFactory, times(1)).getListenerContainer(mockTopic, getQueueName(address, group));
    }

    @Test
    public void shouldCreateConsumerEndpointWithAnonymousGroup() throws Exception {
        MessageProducer producer =
                binder.createConsumerEndpoint(mockConsumerDestination, null, mockConsumerProperties);

        assertThat(producer).isInstanceOf(JmsMessageDrivenChannelAdapter.class);

        verify(mockConnectionFactory, times(1)).createContext();
        verify(mockJmsContext, times(1)).createTopic(address);

        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockListenerContainerFactory).getListenerContainer(eq(mockTopic), stringCaptor.capture());
        assertThat(stringCaptor.getValue()).hasSize(34).startsWith(String.format("%s-", address));
    }

}
