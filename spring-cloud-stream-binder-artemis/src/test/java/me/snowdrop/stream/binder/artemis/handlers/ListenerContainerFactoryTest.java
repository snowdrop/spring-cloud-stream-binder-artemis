package me.snowdrop.stream.binder.artemis.handlers;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jms.listener.AbstractMessageListenerContainer;

import javax.jms.ConnectionFactory;
import javax.jms.Topic;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ListenerContainerFactoryTest {

    @Mock
    private ConnectionFactory mockConnectionFactory;

    @Mock
    private Topic mockTopic;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldGetListenerContainer() {
        ListenerContainerFactory factory = new ListenerContainerFactory(mockConnectionFactory);
        AbstractMessageListenerContainer container = factory.getListenerContainer(mockTopic, "testSubscription");
        assertThat(container.getConnectionFactory()).isEqualTo(mockConnectionFactory);
        assertThat(container.getDestination()).isEqualTo(mockTopic);
        assertThat(container.getSubscriptionName()).isEqualTo("testSubscription");
        assertThat(container.isSessionTransacted()).isTrue();
        assertThat(container.isSubscriptionDurable()).isTrue();
        assertThat(container.isSubscriptionShared()).isTrue();
    }

}