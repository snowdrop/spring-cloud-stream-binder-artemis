package me.snowdrop.stream.binder.artemis.provisioning;

import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisPartitionedProducerDestinationTest {

    @Test
    public void shouldFailToGetName() {
        ArtemisPartitionedProducerDestination destination =
                new ArtemisPartitionedProducerDestination(Collections.emptyList());
        try {
            destination.getName();
            fail("Exception was expected");
        } catch (UnsupportedOperationException e) {
            assertThat(e.getMessage()).isEqualTo("This destination is partitioned");
        }
    }

    @Test
    public void shouldGetNameForPartition() {
        String name = "test-name";
        ArtemisPartitionedProducerDestination destination =
                new ArtemisPartitionedProducerDestination(Collections.singletonList(name));
        assertThat(destination.getNameForPartition(0)).isEqualTo(name);
    }

    @Test
    public void shouldFailToGetNameForNonExistingPartition() {
        ArtemisPartitionedProducerDestination destination =
                new ArtemisPartitionedProducerDestination(Collections.emptyList());
        try {
            destination.getNameForPartition(0);
            fail("Exception was expected");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo("Partition '0' doesn't exist");
        }
    }

}