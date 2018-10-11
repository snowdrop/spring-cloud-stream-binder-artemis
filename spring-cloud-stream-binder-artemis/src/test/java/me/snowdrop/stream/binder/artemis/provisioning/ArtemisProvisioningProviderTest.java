package me.snowdrop.stream.binder.artemis.provisioning;

import me.snowdrop.stream.binder.artemis.properties.ArtemisConsumerProperties;
import me.snowdrop.stream.binder.artemis.properties.ArtemisProducerProperties;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;

import static me.snowdrop.stream.binder.artemis.common.NamingUtils.getQueueName;
import static org.apache.activemq.artemis.api.core.RoutingType.MULTICAST;
import static org.apache.activemq.artemis.api.core.SimpleString.toSimpleString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class ArtemisProvisioningProviderTest {

    private final String address = "test-address";

    private final String[] groups = new String[]{ "test-group-0", "test-group-1" };

    private final String username = "test-user";

    private final String password = "test-password";

    @Mock
    private ServerLocator mockServerLocator;

    @Mock
    private ClientSessionFactory mockClientSessionFactory;

    @Mock
    private ClientSession mockClientSession;

    @Mock
    private ClientSession.AddressQuery mockAddressQuery;

    @Mock
    private ClientSession.QueueQuery mockQueueQuery;

    @Mock
    private ExtendedProducerProperties<ArtemisProducerProperties> mockProducerProperties;

    @Mock
    private ExtendedConsumerProperties<ArtemisConsumerProperties> mockConsumerProperties;

    private ArtemisProvisioningProvider provider;

    @Before
    public void before() throws Exception {
        given(mockServerLocator.createSessionFactory()).willReturn(mockClientSessionFactory);
        given(mockClientSessionFactory.createSession()).willReturn(mockClientSession);
        given(mockClientSessionFactory.createSession(eq(username), eq(password), anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(), anyInt())).willReturn(mockClientSession);
        given(mockClientSession.addressQuery(any())).willReturn(mockAddressQuery);
        given(mockClientSession.queueQuery(any())).willReturn(mockQueueQuery);
        given(mockProducerProperties.getRequiredGroups()).willReturn(new String[]{});
        provider = new ArtemisProvisioningProvider(mockServerLocator, null, null);
    }

    @Test
    public void shouldProvisionUnpartitionedProducer() throws ActiveMQException {
        ProducerDestination destination = provider.provisionProducerDestination(address, mockProducerProperties);

        assertThat(destination).isInstanceOf(ArtemisProducerDestination.class);
        assertThat(destination.getName()).isEqualTo(address);
        verify(mockClientSession).createAddress(toSimpleString(address), MULTICAST, true);
    }

    @Test
    public void shouldProvisionUnpartitionedProducerWithRequiredGroups() throws ActiveMQException {
        given(mockProducerProperties.getRequiredGroups()).willReturn(groups);

        ProducerDestination destination = provider.provisionProducerDestination(address, mockProducerProperties);

        assertThat(destination).isInstanceOf(ArtemisProducerDestination.class);
        assertThat(destination.getName()).isEqualTo(address);
        verify(mockClientSession).createAddress(toSimpleString(address), MULTICAST, true);
        verify(mockClientSession).createSharedQueue(toSimpleString(address), MULTICAST,
                toSimpleString(getQueueName(address, groups[0])), true);
        verify(mockClientSession).createSharedQueue(toSimpleString(address), MULTICAST,
                toSimpleString(getQueueName(address, groups[1])), true);
    }

    @Test
    public void shouldProvisionPartitionedProducer() throws ActiveMQException {
        String partitionedAddress0 = String.format("%s-0", address);
        String partitionedAddress1 = String.format("%s-1", address);

        given(mockProducerProperties.isPartitioned()).willReturn(true);
        given(mockProducerProperties.getPartitionCount()).willReturn(2);

        ProducerDestination destination = provider.provisionProducerDestination(address, mockProducerProperties);

        assertThat(destination).isInstanceOf(ArtemisProducerDestination.class);
        assertThat(destination.getNameForPartition(0)).isEqualTo(partitionedAddress0);
        assertThat(destination.getNameForPartition(1)).isEqualTo(partitionedAddress1);
        verify(mockClientSession).createAddress(toSimpleString(partitionedAddress0), MULTICAST, true);
        verify(mockClientSession).createAddress(toSimpleString(partitionedAddress1), MULTICAST, true);
    }

    @Test
    public void shouldProvisionPartitionedProducerWithRequiredGroups() throws ActiveMQException {
        String partitionedAddress0 = String.format("%s-0", address);
        String partitionedAddress1 = String.format("%s-1", address);

        given(mockProducerProperties.getRequiredGroups()).willReturn(groups);
        given(mockProducerProperties.isPartitioned()).willReturn(true);
        given(mockProducerProperties.getPartitionCount()).willReturn(2);

        ProducerDestination destination = provider.provisionProducerDestination(address, mockProducerProperties);

        assertThat(destination).isInstanceOf(ArtemisProducerDestination.class);
        assertThat(destination.getNameForPartition(0)).isEqualTo(partitionedAddress0);
        assertThat(destination.getNameForPartition(1)).isEqualTo(partitionedAddress1);
        verify(mockClientSession).createAddress(toSimpleString(partitionedAddress0), MULTICAST, true);
        verify(mockClientSession).createAddress(toSimpleString(partitionedAddress1), MULTICAST, true);
        verify(mockClientSession).createSharedQueue(toSimpleString(partitionedAddress0), MULTICAST,
                toSimpleString(getQueueName(partitionedAddress0, groups[0])), true);
        verify(mockClientSession).createSharedQueue(toSimpleString(partitionedAddress0), MULTICAST,
                toSimpleString(getQueueName(partitionedAddress0, groups[1])), true);
        verify(mockClientSession).createSharedQueue(toSimpleString(partitionedAddress1), MULTICAST,
                toSimpleString(getQueueName(partitionedAddress1, groups[0])), true);
        verify(mockClientSession).createSharedQueue(toSimpleString(partitionedAddress1), MULTICAST,
                toSimpleString(getQueueName(partitionedAddress1, groups[1])), true);
    }

    @Test
    public void shouldFailToCreateAddressForProducer() throws ActiveMQException {
        doThrow(new ActiveMQException("Test exception"))
                .when(mockClientSession)
                .createAddress(toSimpleString(address), MULTICAST, true);
        try {
            provider.provisionProducerDestination(address, mockProducerProperties);
            fail("Exception was expected");
        } catch (ProvisioningException e) {
            assertThat(e.getMessage()).contains(String.format("Failed to create address '%s'", address));
        }
    }

    @Test
    public void shouldFailToCreateQueueForProducer() throws ActiveMQException {
        given(mockProducerProperties.getRequiredGroups()).willReturn(groups);
        String queueName = getQueueName(address, groups[0]);

        doThrow(new ActiveMQException("Test exception"))
                .when(mockClientSession)
                .createSharedQueue(toSimpleString(address), MULTICAST, toSimpleString(queueName), true);
        try {
            provider.provisionProducerDestination(address, mockProducerProperties);
            fail("Exception was expected");
        } catch (ProvisioningException e) {
            assertThat(e.getMessage()).contains(
                    String.format("Failed to create queue '%s' with address '%s'", queueName, address));
        }
    }

    @Test
    public void shouldProvisionUnpartitionedConsumer() throws ActiveMQException {
        ConsumerDestination destination =
                provider.provisionConsumerDestination(address, null, mockConsumerProperties);

        assertThat(destination).isInstanceOf(ArtemisConsumerDestination.class);
        assertThat(destination.getName()).isEqualTo(address);
        verify(mockClientSession).createAddress(toSimpleString(address), MULTICAST, true);
    }

    @Test
    public void shouldProvisionPartitionedConsumer() throws ActiveMQException {
        given(mockConsumerProperties.isPartitioned()).willReturn(true);
        given(mockConsumerProperties.getInstanceIndex()).willReturn(0);

        ConsumerDestination destination =
                provider.provisionConsumerDestination(address, null, mockConsumerProperties);

        String partitionedAddress = String.format("%s-0", address);
        assertThat(destination).isInstanceOf(ArtemisConsumerDestination.class);
        assertThat(destination.getName()).isEqualTo(partitionedAddress);
        verify(mockClientSession).createAddress(toSimpleString(partitionedAddress), MULTICAST, true);
    }

    @Test
    public void shouldFailToCreateAddressForConsumer() throws ActiveMQException {
        doThrow(new ActiveMQException("Test exception"))
                .when(mockClientSession)
                .createAddress(toSimpleString(address), MULTICAST, true);
        try {
            provider.provisionConsumerDestination(address, groups[0], mockConsumerProperties);
            fail("Exception was expected");
        } catch (ProvisioningException e) {
            assertThat(e.getMessage()).contains(String.format("Failed to create address '%s'", address));
        }
    }

    @Test
    public void shouldNotAuthenticateWhenProvisioning() throws ActiveMQException {
        provider.provisionConsumerDestination(address, groups[0], mockConsumerProperties);

        verify(mockClientSessionFactory).createSession();
    }

    @Test
    public void shouldAuthenticateWhenProvisioning() throws ActiveMQException {
        given(mockServerLocator.isPreAcknowledge()).willReturn(true);
        given(mockServerLocator.getAckBatchSize()).willReturn(10);

        provider = new ArtemisProvisioningProvider(mockServerLocator, username, password);
        provider.provisionConsumerDestination(address, groups[0], mockConsumerProperties);

        verify(mockClientSessionFactory, times(0)).createSession();
        verify(mockClientSessionFactory).createSession(username, password, true, false, false, true, 10);
    }

    @Test
    public void shouldDoNothingIfAddressAlreadyExists() throws ActiveMQException {
        given(mockAddressQuery.isExists()).willReturn(true);

        provider.provisionProducerDestination(address, mockProducerProperties);

        verify(mockClientSession).addressQuery(toSimpleString(address));
        verify(mockClientSession, times(0))
                .createAddress(any(), any(RoutingType.class), anyBoolean());
    }

    @Test
    public void shouldDoNothingIfQueueAlreadyExists() throws ActiveMQException {
        given(mockProducerProperties.getRequiredGroups()).willReturn(groups);
        given(mockQueueQuery.isExists()).willReturn(true);
        given(mockQueueQuery.getAddress()).willReturn(toSimpleString(address));

        provider.provisionProducerDestination(address, mockProducerProperties);

        verify(mockClientSession).queueQuery(toSimpleString(getQueueName(address, groups[0])));
        verify(mockClientSession).queueQuery(toSimpleString(getQueueName(address, groups[1])));
        verify(mockClientSession, times(0))
                .createSharedQueue(any(), any(RoutingType.class), any(), anyBoolean());
    }

    @Test
    public void shouldFailIfQueueAlreadyExistsUnderDifferentAddress() {
        given(mockProducerProperties.getRequiredGroups()).willReturn(groups);
        given(mockQueueQuery.isExists()).willReturn(true);
        given(mockQueueQuery.getAddress()).willReturn(toSimpleString("another-address"));

        try {
            provider.provisionProducerDestination(address, mockProducerProperties);
            fail("Exception was expected");
        } catch (ProvisioningException e) {
            assertThat(e.getMessage()).contains("Queue already exists under another address 'another-address'");
        }
    }

}
