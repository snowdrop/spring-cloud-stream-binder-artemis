package me.snowdrop.stream.binder.artemis.provisioning;

import me.snowdrop.stream.binder.artemis.properties.ArtemisConsumerProperties;
import me.snowdrop.stream.binder.artemis.properties.ArtemisProducerProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;

import static me.snowdrop.stream.binder.artemis.common.NamingUtils.getQueueName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class ArtemisProvisioningProviderTest {

    private final String address = "test-address";

    private final String[] groups = new String[]{ "test-group-0", "test-group-1" };

    @Mock
    private ArtemisBrokerManager mockArtemisBrokerManager;

    @Mock
    private ExtendedProducerProperties<ArtemisProducerProperties> mockProducerProperties;

    @Mock
    private ArtemisProducerProperties mockArtemisProducerProperties;

    @Mock
    private ExtendedConsumerProperties<ArtemisConsumerProperties> mockConsumerProperties;

    @Mock
    private ArtemisConsumerProperties mockArtemisConsumerProperties;

    private ArtemisProvisioningProvider provider;

    @Before
    public void before() {
        given(mockProducerProperties.getRequiredGroups()).willReturn(new String[]{});
        given(mockProducerProperties.getExtension()).willReturn(mockArtemisProducerProperties);
        given(mockConsumerProperties.getExtension()).willReturn(mockArtemisConsumerProperties);
        provider = new ArtemisProvisioningProvider(mockArtemisBrokerManager);
    }

    @Test
    public void shouldProvisionUnpartitionedProducer() {
        ProducerDestination destination = provider.provisionProducerDestination(address, mockProducerProperties);

        assertThat(destination).isInstanceOf(ArtemisProducerDestination.class);
        assertThat(destination.getName()).isEqualTo(address);
        verify(mockArtemisBrokerManager).createAddress(address, mockArtemisProducerProperties);
        verify(mockArtemisBrokerManager, times(0)).createQueue(anyString(), anyString());
    }

    @Test
    public void shouldProvisionUnpartitionedProducerWithRequiredGroups() {
        given(mockProducerProperties.getRequiredGroups()).willReturn(groups);

        ProducerDestination destination = provider.provisionProducerDestination(address, mockProducerProperties);

        assertThat(destination).isInstanceOf(ArtemisProducerDestination.class);
        assertThat(destination.getName()).isEqualTo(address);
        verify(mockArtemisBrokerManager).createAddress(address, mockArtemisProducerProperties);
        verify(mockArtemisBrokerManager).createQueue(address, getQueueName(address, groups[0]));
        verify(mockArtemisBrokerManager).createQueue(address, getQueueName(address, groups[1]));
    }

    @Test
    public void shouldProvisionPartitionedProducer() {
        String partitionedAddress0 = String.format("%s-0", address);
        String partitionedAddress1 = String.format("%s-1", address);

        given(mockProducerProperties.isPartitioned()).willReturn(true);
        given(mockProducerProperties.getPartitionCount()).willReturn(2);

        ProducerDestination destination = provider.provisionProducerDestination(address, mockProducerProperties);

        assertThat(destination).isInstanceOf(ArtemisProducerDestination.class);
        assertThat(destination.getNameForPartition(0)).isEqualTo(partitionedAddress0);
        assertThat(destination.getNameForPartition(1)).isEqualTo(partitionedAddress1);
        verify(mockArtemisBrokerManager).createAddress(partitionedAddress0, mockArtemisProducerProperties);
        verify(mockArtemisBrokerManager).createAddress(partitionedAddress1, mockArtemisProducerProperties);
        verify(mockArtemisBrokerManager, times(0)).createQueue(anyString(), anyString());
    }

    @Test
    public void shouldProvisionPartitionedProducerWithRequiredGroups() {
        String partitionedAddress0 = String.format("%s-0", address);
        String partitionedAddress1 = String.format("%s-1", address);

        given(mockProducerProperties.getRequiredGroups()).willReturn(groups);
        given(mockProducerProperties.isPartitioned()).willReturn(true);
        given(mockProducerProperties.getPartitionCount()).willReturn(2);

        ProducerDestination destination = provider.provisionProducerDestination(address, mockProducerProperties);

        assertThat(destination).isInstanceOf(ArtemisProducerDestination.class);
        assertThat(destination.getNameForPartition(0)).isEqualTo(partitionedAddress0);
        assertThat(destination.getNameForPartition(1)).isEqualTo(partitionedAddress1);
        verify(mockArtemisBrokerManager).createAddress(partitionedAddress0, mockArtemisProducerProperties);
        verify(mockArtemisBrokerManager).createAddress(partitionedAddress1, mockArtemisProducerProperties);
        verify(mockArtemisBrokerManager).createQueue(partitionedAddress0, getQueueName(partitionedAddress0, groups[0]));
        verify(mockArtemisBrokerManager).createQueue(partitionedAddress0, getQueueName(partitionedAddress0, groups[1]));
        verify(mockArtemisBrokerManager).createQueue(partitionedAddress1, getQueueName(partitionedAddress1, groups[0]));
        verify(mockArtemisBrokerManager).createQueue(partitionedAddress1, getQueueName(partitionedAddress1, groups[1]));
    }

    @Test
    public void shouldProvisionUnpartitionedConsumer() {
        ConsumerDestination destination =
                provider.provisionConsumerDestination(address, null, mockConsumerProperties);

        assertThat(destination).isInstanceOf(ArtemisConsumerDestination.class);
        assertThat(destination.getName()).isEqualTo(address);
        verify(mockArtemisBrokerManager).createAddress(address, mockArtemisConsumerProperties);
    }

    @Test
    public void shouldProvisionPartitionedConsumer() {
        given(mockConsumerProperties.isPartitioned()).willReturn(true);
        given(mockConsumerProperties.getInstanceIndex()).willReturn(0);

        ConsumerDestination destination =
                provider.provisionConsumerDestination(address, null, mockConsumerProperties);

        String partitionedAddress = String.format("%s-0", address);
        assertThat(destination).isInstanceOf(ArtemisConsumerDestination.class);
        assertThat(destination.getName()).isEqualTo(partitionedAddress);
        verify(mockArtemisBrokerManager).createAddress(partitionedAddress, mockArtemisConsumerProperties);
    }

}
