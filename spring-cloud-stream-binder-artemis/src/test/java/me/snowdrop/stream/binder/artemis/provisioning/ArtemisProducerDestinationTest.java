package me.snowdrop.stream.binder.artemis.provisioning;

import org.junit.Test;

import static me.snowdrop.stream.binder.artemis.common.NamingUtils.getPartitionAddress;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisProducerDestinationTest {

    @Test
    public void shouldGetName() {
        String name = "test-name";
        ArtemisProducerDestination destination = new ArtemisProducerDestination(name);
        assertThat(destination.getName()).isEqualTo(name);
    }

    @Test
    public void shouldGetNameForPartition() {
        String name = "test-name";
        ArtemisProducerDestination destination = new ArtemisProducerDestination(name);
        assertThat(destination.getNameForPartition(0)).isEqualTo(getPartitionAddress(name, 0));
    }

}
