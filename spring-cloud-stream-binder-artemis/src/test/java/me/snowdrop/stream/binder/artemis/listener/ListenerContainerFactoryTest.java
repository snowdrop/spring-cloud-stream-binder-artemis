package me.snowdrop.stream.binder.artemis.listener;

import javax.jms.ConnectionFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.jms.listener.AbstractMessageListenerContainer;

import static org.assertj.core.api.Assertions.assertThat;

import me.snowdrop.stream.binder.artemis.properties.ArtemisConsumerProperties;
import me.snowdrop.stream.binder.artemis.properties.ArtemisExtendedBindingProperties;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ListenerContainerFactoryTest {

    private final static String TEST_TOPIC = "testTopic";

    @Mock
    private ConnectionFactory mockConnectionFactory;

    @Mock
    private ExtendedConsumerProperties<ArtemisConsumerProperties> extendedConsumerProperties;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(extendedConsumerProperties.getExtension()).thenReturn(new ArtemisConsumerProperties());
    }

    @Test
    public void shouldGetListenerContainer() {
        ListenerContainerFactory factory = new ListenerContainerFactory(mockConnectionFactory);
        AbstractMessageListenerContainer container = factory.getListenerContainer("testTopic", "testSubscription", extendedConsumerProperties);
        assertThat(container.getConnectionFactory()).isEqualTo(mockConnectionFactory);
        assertThat(container.getDestinationName()).isEqualTo(TEST_TOPIC);
        assertThat(container.isPubSubDomain()).isTrue();
        assertThat(container.getSubscriptionName()).isEqualTo("testSubscription");
        assertThat(container.isSessionTransacted()).isTrue();
        assertThat(container.isSubscriptionDurable()).isTrue();
        assertThat(container.isSubscriptionShared()).isTrue();
    }

}
