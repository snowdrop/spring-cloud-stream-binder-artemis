package me.snowdrop.stream.binder.artemis.provisioning;

import me.snowdrop.stream.binder.artemis.properties.ArtemisConsumerProperties;
import me.snowdrop.stream.binder.artemis.properties.ArtemisProducerProperties;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;

import static me.snowdrop.stream.binder.artemis.common.NamingUtils.getPartitionAddress;
import static me.snowdrop.stream.binder.artemis.common.NamingUtils.getQueueName;
import static org.apache.activemq.artemis.api.core.SimpleString.toSimpleString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisProvisioningProviderTest {

    private final String address = "test-address";

    private final String[] groups = new String[]{"test-group-0", "test-group-1"};

    private final String username = "test-user";

    private final String password = "test-password";

    @Mock
    private ServerLocator mockServerLocator;

    @Mock
    private ClientSessionFactory mockClientSessionFactory;

    @Mock
    private ClientSession mockClientSession;

    @Mock
    private ExtendedProducerProperties<ArtemisProducerProperties> mockProducerProperties;

    @Mock
    private ExtendedConsumerProperties<ArtemisConsumerProperties> mockConsumerProperties;

    private ArtemisProvisioningProvider provider;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mockServerLocator.createSessionFactory()).thenReturn(mockClientSessionFactory);
        when(mockClientSessionFactory.createSession()).thenReturn(mockClientSession);
        when(mockClientSessionFactory.createSession(eq(username), eq(password), anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(), anyInt())).thenReturn(mockClientSession);
        when(mockProducerProperties.getRequiredGroups()).thenReturn(new String[]{});
        provider = new ArtemisProvisioningProvider(mockServerLocator, null, null);
    }

    @Test
    public void shouldProvisionUnpartitionedProducer() throws ActiveMQException {
        ProducerDestination destination = provider.provisionProducerDestination(address, mockProducerProperties);

        assertThat(destination).isInstanceOf(ArtemisProducerDestination.class);
        assertThat(destination.getName()).isEqualTo(address);

        SimpleString actualAddress = toSimpleString("jms.topic." + address);
        SimpleString actualAddressQueueName = toSimpleString(address);
        // verify queue with address=jms.topic.test-address and name=test-address
        verify(mockClientSession).createSharedQueue(actualAddress, actualAddressQueueName, true);
    }

    @Test
    public void shouldProvisionUnpartitionedProducerWithRequiredGroups() throws ActiveMQException {
        when(mockProducerProperties.getRequiredGroups()).thenReturn(groups);

        ProducerDestination destination = provider.provisionProducerDestination(address, mockProducerProperties);

        assertThat(destination).isInstanceOf(ArtemisProducerDestination.class);
        assertThat(destination.getName()).isEqualTo(address);

        SimpleString actualAddress = toSimpleString("jms.topic." + address);
        SimpleString actualAddressQueueName = toSimpleString(address);
        // verify queue with address=jms.topic.test-address and name=test-address
        verify(mockClientSession).createSharedQueue(actualAddress, actualAddressQueueName, true);

        SimpleString actualGroup0QueueName = toSimpleString(getQueueName(address, groups[0]));
        // verify queue with address=jms.topic.test-address and name=test-address-test-group-0
        verify(mockClientSession).createSharedQueue(actualAddress, actualGroup0QueueName, true);

        SimpleString actualGroup1QueueName = toSimpleString(getQueueName(address, groups[1]));
        // verify queue with address=jms.topic.test-address and name=test-address-test-group-1
        verify(mockClientSession).createSharedQueue(actualAddress, actualGroup1QueueName, true);
    }

    @Test
    public void shouldProvisionPartitionedProducer() throws ActiveMQException {
        String partitionedAddress0 = getPartitionAddress(address, 0);
        String partitionedAddress1 = getPartitionAddress(address, 1);

        when(mockProducerProperties.isPartitioned()).thenReturn(true);
        when(mockProducerProperties.getPartitionCount()).thenReturn(2);

        ProducerDestination destination = provider.provisionProducerDestination(address, mockProducerProperties);

        assertThat(destination).isInstanceOf(ArtemisPartitionedProducerDestination.class);
        assertThat(destination.getNameForPartition(0)).isEqualTo(partitionedAddress0);
        assertThat(destination.getNameForPartition(1)).isEqualTo(partitionedAddress1);

        SimpleString actualAddress0 = toSimpleString("jms.topic." + partitionedAddress0);
        SimpleString actualAddress0QueueName = toSimpleString(partitionedAddress0);
        // verify queue with address=jms.topic.test-address-0 and name=test-address-0
        verify(mockClientSession).createSharedQueue(actualAddress0, actualAddress0QueueName, true);

        SimpleString actualAddress1 = toSimpleString("jms.topic." + partitionedAddress1);
        SimpleString actualAddress1QueueName = toSimpleString(partitionedAddress1);
        // verify queue with address=jms.topic.test-address-1 and name=test-address-1
        verify(mockClientSession).createSharedQueue(actualAddress1, actualAddress1QueueName, true);
    }

    @Test
    public void shouldProvisionPartitionedProducerWithRequiredGroups() throws ActiveMQException {
        String partitionedAddress0 = getPartitionAddress(address, 0);
        String partitionedAddress1 = getPartitionAddress(address, 1);

        when(mockProducerProperties.getRequiredGroups()).thenReturn(groups);
        when(mockProducerProperties.isPartitioned()).thenReturn(true);
        when(mockProducerProperties.getPartitionCount()).thenReturn(2);

        ProducerDestination destination = provider.provisionProducerDestination(address, mockProducerProperties);

        assertThat(destination).isInstanceOf(ArtemisPartitionedProducerDestination.class);
        assertThat(destination.getNameForPartition(0)).isEqualTo(partitionedAddress0);
        assertThat(destination.getNameForPartition(1)).isEqualTo(partitionedAddress1);

        SimpleString actualAddress0 = toSimpleString("jms.topic." + partitionedAddress0);
        SimpleString actualAddress0QueueName = toSimpleString(partitionedAddress0);
        // verify queue with address=jms.topic.test-address-0 and name=test-address-0
        verify(mockClientSession).createSharedQueue(actualAddress0, actualAddress0QueueName, true);

        SimpleString actualAddress1 = toSimpleString("jms.topic." + partitionedAddress1);
        SimpleString actualAddress1QueueName = toSimpleString(partitionedAddress1);
        // verify queue with address=jms.topic.test-address-1 and name=test-address-1
        verify(mockClientSession).createSharedQueue(actualAddress1, actualAddress1QueueName, true);

        SimpleString actualGroup00QueueName = toSimpleString(getQueueName(partitionedAddress0, groups[0]));
        // verify queue with address=jms.topic.test-address-0 and name=test-address-test-group-0
        verify(mockClientSession).createSharedQueue(actualAddress0, actualGroup00QueueName, true);

        SimpleString actualGroup01QueueName = toSimpleString(getQueueName(partitionedAddress0, groups[1]));
        // verify queue with address=jms.topic.test-address-0 and name=test-address-test-group-1
        verify(mockClientSession).createSharedQueue(actualAddress0, actualGroup01QueueName, true);

        SimpleString actualGroup10QueueName = toSimpleString(getQueueName(partitionedAddress1, groups[0]));
        // verify queue with address=jms.topic.test-address-1 and name=test-address-test-group-0
        verify(mockClientSession).createSharedQueue(actualAddress1, actualGroup10QueueName, true);

        SimpleString actualGroup11QueueName = toSimpleString(getQueueName(partitionedAddress1, groups[1]));
        // verify queue with address=jms.topic.test-address-1 and name=test-address-test-group-1
        verify(mockClientSession).createSharedQueue(actualAddress1, actualGroup11QueueName, true);
    }

    @Test
    public void shouldFailToCreateAddressForProducer() throws ActiveMQException {
        SimpleString actualAddress = toSimpleString("jms.topic." + address);
        SimpleString actualAddressQueueName = toSimpleString(address);

        doThrow(new ActiveMQException("Test exception")).when(mockClientSession)
                .createSharedQueue(actualAddress, actualAddressQueueName, true);
        try {
            provider.provisionProducerDestination(address, mockProducerProperties);
            fail("Exception was expected");
        } catch (ProvisioningException e) {
            assertThat(e.getMessage()).contains(
                    String.format("Failed to create queue '%s' with address '%s'", actualAddressQueueName,
                            actualAddress));
        }
    }

    @Test
    public void shouldFailToCreateQueueForProducer() throws ActiveMQException {
        when(mockProducerProperties.getRequiredGroups()).thenReturn(groups);

        SimpleString actualAddress = toSimpleString("jms.topic." + address);
        SimpleString actualGroupQueueName = toSimpleString(getQueueName(address, groups[0]));
        doThrow(new ActiveMQException("Test exception")).when(mockClientSession)
                .createSharedQueue(actualAddress, actualGroupQueueName, true);
        try {
            provider.provisionProducerDestination(address, mockProducerProperties);
            fail("Exception was expected");
        } catch (ProvisioningException e) {
            assertThat(e.getMessage()).contains(
                    String.format("Failed to create queue '%s' with address '%s'", actualGroupQueueName,
                            actualAddress));
        }
    }

    @Test
    public void shouldProvisionUnpartitionedConsumer() throws ActiveMQException {
        ConsumerDestination destination =
                provider.provisionConsumerDestination(address, null, mockConsumerProperties);

        assertThat(destination).isInstanceOf(ArtemisConsumerDestination.class);
        assertThat(destination.getName()).isEqualTo(address);

        SimpleString actualAddress = toSimpleString("jms.topic." + address);
        SimpleString actualAddressQueueName = toSimpleString(address);
        verify(mockClientSession).createSharedQueue(actualAddress, actualAddressQueueName, true);
    }

    @Test
    public void shouldProvisionPartitionedConsumer() throws ActiveMQException {
        String partitionedAddress = getPartitionAddress(address, 0);

        when(mockConsumerProperties.isPartitioned()).thenReturn(true);
        when(mockConsumerProperties.getInstanceIndex()).thenReturn(0);

        ConsumerDestination destination =
                provider.provisionConsumerDestination(address, null, mockConsumerProperties);

        assertThat(destination).isInstanceOf(ArtemisConsumerDestination.class);
        assertThat(destination.getName()).isEqualTo(partitionedAddress);

        SimpleString actualAddress = toSimpleString("jms.topic." + partitionedAddress);
        SimpleString actualAddressQueueName = toSimpleString(partitionedAddress);
        verify(mockClientSession).createSharedQueue(actualAddress, actualAddressQueueName, true);
    }

    @Test
    public void shouldFailToCreateAddressForConsumer() throws ActiveMQException {
        SimpleString actualAddress = toSimpleString("jms.topic." + address);
        SimpleString actualAddressQueueName = toSimpleString(address);
        doThrow(new ActiveMQException("Test exception")).when(mockClientSession)
                .createSharedQueue(actualAddress, actualAddressQueueName, true);
        try {
            provider.provisionConsumerDestination(address, groups[0], mockConsumerProperties);
            fail("Exception was expected");
        } catch (ProvisioningException e) {
            assertThat(e.getMessage()).contains(
                    String.format("Failed to create queue '%s' with address '%s'", actualAddressQueueName,
                            actualAddress));
        }
    }

    @Test
    public void shouldNotAuthenticateWhenProvisioning() throws ActiveMQException {
        provider.provisionConsumerDestination(address, groups[0], mockConsumerProperties);

        verify(mockClientSessionFactory).createSession();
    }

    @Test
    public void shouldAuthenticateWhenProvisioning() throws ActiveMQException {
        when(mockServerLocator.isPreAcknowledge()).thenReturn(true);
        when(mockServerLocator.getAckBatchSize()).thenReturn(10);

        provider = new ArtemisProvisioningProvider(mockServerLocator, username, password);
        provider.provisionConsumerDestination(address, groups[0], mockConsumerProperties);

        verify(mockClientSessionFactory, times(0)).createSession();
        verify(mockClientSessionFactory).createSession(username, password, true, false, false, true, 10);
    }

}
