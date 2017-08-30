package org.jboss.snowdrop.stream.binder.artemis.provisioning;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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
    public void shouldFailToGetNameForPartition() {
        String name = "test-name";
        ArtemisProducerDestination destination = new ArtemisProducerDestination(name);
        try {
            destination.getNameForPartition(0);
            fail("Exception was expected");
        } catch (UnsupportedOperationException e) {
            assertThat(e.getMessage()).isEqualTo("This destination is not partitioned");
        }
    }

}