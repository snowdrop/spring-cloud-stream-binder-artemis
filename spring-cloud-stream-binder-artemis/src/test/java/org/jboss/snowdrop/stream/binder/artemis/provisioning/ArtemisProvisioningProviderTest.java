package org.jboss.snowdrop.stream.binder.artemis.provisioning;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;

import static org.apache.activemq.artemis.api.core.RoutingType.MULTICAST;
import static org.apache.activemq.artemis.api.core.SimpleString.toSimpleString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisProvisioningProviderTest {

    private final String address = "test-address";

    private final String[] groups = new String[]{"test-group-1", "test-group-2"};

    @Mock
    private ServerLocator mockServerLocator;

    @Mock
    private ClientSessionFactory mockClientSessionFactory;

    @Mock
    private ClientSession mockClientSession;

    @Mock
    private ProducerProperties mockProducerProperties;

    @Mock
    private ConsumerProperties mockConsumerProperties;

    private ArtemisProvisioningProvider provider;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mockServerLocator.createSessionFactory()).thenReturn(mockClientSessionFactory);
        when(mockClientSessionFactory.createSession()).thenReturn(mockClientSession);
        when(mockProducerProperties.getRequiredGroups()).thenReturn(new String[]{});
        provider = new ArtemisProvisioningProvider(mockServerLocator);
    }

    @Test
    public void shouldProvisionUnpartitionedProducer() throws ActiveMQException {
        ProducerDestination destination = provider.provisionProducerDestination(address, mockProducerProperties);
        assertThat(destination).isInstanceOf(ArtemisProducerDestination.class);
        assertThat(destination.getName()).isEqualTo(address);
        verify(mockClientSession, times(1)).createAddress(toSimpleString(address), MULTICAST, true);
        verify(mockClientSession, times(0)).createSharedQueue(any(SimpleString.class), any(RoutingType.class),
                any(SimpleString.class), anyBoolean());
    }

    @Test
    public void shouldProvisionUnpartitionedProducerWithRequiredGroups() throws ActiveMQException {
        when(mockProducerProperties.getRequiredGroups()).thenReturn(groups);
        ProducerDestination destination = provider.provisionProducerDestination(address, mockProducerProperties);
        assertThat(destination).isInstanceOf(ArtemisProducerDestination.class);
        assertThat(destination.getName()).isEqualTo(address);
        verify(mockClientSession, times(1)).createAddress(toSimpleString(address), MULTICAST, true);
        verify(mockClientSession, times(1)).createSharedQueue(toSimpleString(address), MULTICAST,
                toSimpleString(groups[0]), true);
        verify(mockClientSession, times(1)).createSharedQueue(toSimpleString(address), MULTICAST,
                toSimpleString(groups[1]), true);
    }

    @Test
    public void shouldProvisionPartitionedProducer() throws ActiveMQException {
        String partitionedAddress0 = String.format("%s-%d", address, 0);
        String partitionedAddress1 = String.format("%s-%d", address, 1);

        when(mockProducerProperties.isPartitioned()).thenReturn(true);
        when(mockProducerProperties.getPartitionCount()).thenReturn(2);

        ProducerDestination destination = provider.provisionProducerDestination(address, mockProducerProperties);

        assertThat(destination).isInstanceOf(ArtemisPartitionedProducerDestination.class);
        assertThat(destination.getNameForPartition(0)).isEqualTo(partitionedAddress0);
        assertThat(destination.getNameForPartition(1)).isEqualTo(partitionedAddress1);

        verify(mockClientSession, times(1)).createAddress(toSimpleString(partitionedAddress0), MULTICAST, true);
        verify(mockClientSession, times(1)).createAddress(toSimpleString(partitionedAddress1), MULTICAST, true);
        verify(mockClientSession, times(0)).createSharedQueue(any(SimpleString.class), any(RoutingType.class),
                any(SimpleString.class), anyBoolean());
    }

    @Test
    public void shouldProvisionPartitionedProducerWithRequiredGroups() throws ActiveMQException {
        String partitionedAddress0 = String.format("%s-%d", address, 0);
        String partitionedAddress1 = String.format("%s-%d", address, 1);

        when(mockProducerProperties.getRequiredGroups()).thenReturn(groups);
        when(mockProducerProperties.isPartitioned()).thenReturn(true);
        when(mockProducerProperties.getPartitionCount()).thenReturn(2);

        ProducerDestination destination = provider.provisionProducerDestination(address, mockProducerProperties);

        assertThat(destination).isInstanceOf(ArtemisPartitionedProducerDestination.class);
        assertThat(destination.getNameForPartition(0)).isEqualTo(partitionedAddress0);
        assertThat(destination.getNameForPartition(1)).isEqualTo(partitionedAddress1);

        verify(mockClientSession, times(1)).createAddress(toSimpleString(partitionedAddress0), MULTICAST, true);
        verify(mockClientSession, times(1)).createAddress(toSimpleString(partitionedAddress1), MULTICAST, true);
        verify(mockClientSession, times(1)).createSharedQueue(toSimpleString(partitionedAddress0), MULTICAST,
                toSimpleString(groups[0]), true);
        verify(mockClientSession, times(1)).createSharedQueue(toSimpleString(partitionedAddress0), MULTICAST,
                toSimpleString(groups[1]), true);
        verify(mockClientSession, times(1)).createSharedQueue(toSimpleString(partitionedAddress1), MULTICAST,
                toSimpleString(groups[0]), true);
        verify(mockClientSession, times(1)).createSharedQueue(toSimpleString(partitionedAddress1), MULTICAST,
                toSimpleString(groups[1]), true);
    }

    @Test
    public void shouldFailToCreateAddressForProducer() throws ActiveMQException {
        doThrow(new ActiveMQException("Test exception")).when(mockClientSession)
                .createAddress(toSimpleString(address), MULTICAST, true);
        try {
            provider.provisionProducerDestination(address, mockProducerProperties);
            fail("Exception was expected");
        } catch (ProvisioningException e) {
            assertThat(e.getMessage()).isEqualTo(String.format("Failed to create address '%s'", address));
        }
    }

    @Test
    public void shouldFailToCreateQueueForProducer() throws ActiveMQException {
        when(mockProducerProperties.getRequiredGroups()).thenReturn(groups);
        doThrow(new ActiveMQException("Test exception")).when(mockClientSession)
                .createSharedQueue(toSimpleString(address), MULTICAST, toSimpleString(groups[0]), true);
        try {
            provider.provisionProducerDestination(address, mockProducerProperties);
            fail("Exception was expected");
        } catch (ProvisioningException e) {
            assertThat(e.getMessage()).isEqualTo(
                    String.format("Failed to create queue '%s' with address '%s'", groups[0], address));
        }
    }

    @Test
    public void shouldProvisionUnpartitionedConsumer() throws ActiveMQException {
        ConsumerDestination destination =
                provider.provisionConsumerDestination(address, groups[0], mockConsumerProperties);
        assertThat(destination).isInstanceOf(ArtemisConsumerDestination.class);
        assertThat(destination.getName()).isEqualTo(address);
        verify(mockClientSession, times(1)).createAddress(toSimpleString(address), MULTICAST, true);
        verify(mockClientSession, times(1)).createSharedQueue(toSimpleString(address), MULTICAST,
                toSimpleString(groups[0]), true);
    }

    @Test
    @Ignore
    public void shouldProvisionPartitionedConsumer() {

    }

    @Test
    public void shouldFailToCreateAddressForConsumer() throws ActiveMQException {
        doThrow(new ActiveMQException("Test exception")).when(mockClientSession)
                .createAddress(toSimpleString(address), MULTICAST, true);
        try {
            provider.provisionConsumerDestination(address, groups[0], mockConsumerProperties);
            fail("Exception was expected");
        } catch (ProvisioningException e) {
            assertThat(e.getMessage()).isEqualTo(String.format("Failed to create address '%s'", address));
        }
    }

    @Test
    public void shouldFailToCreateQueueForConsumer() throws ActiveMQException {
        doThrow(new ActiveMQException("Test exception")).when(mockClientSession)
                .createSharedQueue(toSimpleString(address), MULTICAST, toSimpleString(groups[0]), true);
        try {
            provider.provisionConsumerDestination(address, groups[0], mockConsumerProperties);
            fail("Exception was expected");
        } catch (ProvisioningException e) {
            assertThat(e.getMessage()).isEqualTo(
                    String.format("Failed to create queue '%s' with address '%s'", groups[0], address));
        }
    }

}