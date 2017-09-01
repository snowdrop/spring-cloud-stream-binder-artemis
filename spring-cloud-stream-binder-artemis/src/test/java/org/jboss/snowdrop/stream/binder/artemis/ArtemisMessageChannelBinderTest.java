package org.jboss.snowdrop.stream.binder.artemis;

import org.jboss.snowdrop.stream.binder.artemis.properties.ArtemisConsumerProperties;
import org.jboss.snowdrop.stream.binder.artemis.properties.ArtemisExtendedBindingProperties;
import org.jboss.snowdrop.stream.binder.artemis.properties.ArtemisProducerProperties;
import org.jboss.snowdrop.stream.binder.artemis.provisioning.ArtemisProvisioningProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.dsl.jms.JmsMessageDrivenChannelAdapter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.messaging.MessageHandler;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.Topic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisMessageChannelBinderTest {

    private final String address = "test-address";

    private final String group = "test-group";

    @Mock
    private ArtemisProvisioningProvider mockProvisioningProvider;

    @Mock
    private ConnectionFactory mockConnectionFactory;

    @Mock
    private JMSContext mockJmsContext;

    @Mock
    private Topic mockTopic;

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
        binder = new ArtemisMessageChannelBinder(mockProvisioningProvider, mockConnectionFactory, mockMessageConverter,
                mockBindingProperties);
    }

    @Test
    public void shouldCreateUnpartitionedProducerMessageHandler() throws Exception {
        when(mockProducerDestination.getName()).thenReturn(address);
        when(mockProducerProperties.isPartitioned()).thenReturn(false);
        MessageHandler handler = binder.createProducerMessageHandler(mockProducerDestination, mockProducerProperties);
        assertThat(handler).isInstanceOf(ArtemisMessageHandler.class);
        verify(mockProducerProperties, times(1)).isPartitioned();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldCreatePartitionedProducerMessageHandler() throws Exception {
        when(mockProducerDestination.getName()).thenReturn(address);
        when(mockProducerProperties.isPartitioned()).thenReturn(true);
        binder.createProducerMessageHandler(mockProducerDestination, mockProducerProperties);
    }

    @Test
    public void shouldCreateConsumerEndpoint() throws Exception {
        when(mockConsumerDestination.getName()).thenReturn(address);
        MessageProducer producer =
                binder.createConsumerEndpoint(mockConsumerDestination, group, mockConsumerProperties);
        assertThat(producer).isInstanceOf(JmsMessageDrivenChannelAdapter.class);
        verify(mockConnectionFactory, times(1)).createContext();
        verify(mockJmsContext, times(1)).createTopic(address);
    }

}