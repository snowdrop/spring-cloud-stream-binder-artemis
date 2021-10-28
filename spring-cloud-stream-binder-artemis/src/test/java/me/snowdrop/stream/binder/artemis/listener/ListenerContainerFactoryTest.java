package me.snowdrop.stream.binder.artemis.listener;

import javax.jms.ConnectionFactory;

import me.snowdrop.stream.binder.artemis.properties.ArtemisConsumerProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class ListenerContainerFactoryTest {

    @Mock
    private ConnectionFactory mockConnectionFactory;

    @Mock
    private ArtemisConsumerProperties artemisConsumerProperties;

    @Test
    public void shouldGetListenerContainer() {
        ExtendedConsumerProperties<ArtemisConsumerProperties> properties = new ExtendedConsumerProperties<>(artemisConsumerProperties);
        properties.setAutoStartup(true);
        properties.setConcurrency(10);

        ListenerContainerFactory factory = new ListenerContainerFactory(mockConnectionFactory);
        AbstractMessageListenerContainer container = factory.getListenerContainer("testTopic", "testSubscription", properties);
        assertThat(container.getConnectionFactory()).isEqualTo(mockConnectionFactory);
        assertThat(container.getDestinationName()).isEqualTo("testTopic");
        assertThat(container.isPubSubDomain()).isTrue();
        assertThat(container.getSubscriptionName()).isEqualTo("testSubscription");
        assertThat(container.isSessionTransacted()).isTrue();
        assertThat(container.isSubscriptionDurable()).isTrue();
        assertThat(container.isSubscriptionShared()).isTrue();
        assertThat(container.isAutoStartup()).isTrue();

        DefaultMessageListenerContainer defaultMessageListenerContainer = (DefaultMessageListenerContainer) container;
        assertThat(defaultMessageListenerContainer.getConcurrentConsumers()).isEqualTo(10);
    }
}
