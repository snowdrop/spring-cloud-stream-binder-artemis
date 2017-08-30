package org.jboss.snowdrop.stream.binder.artemis.provisioning;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisProducerDestinationTest {

    @Test
    public void getName() {
        String address = "test-address";
        ArtemisProducerDestination destination = new ArtemisProducerDestination(address);
        assertThat(destination.getName()).isEqualTo(address);
    }

    @Test
    public void getNameForPartition() {
        String address = "test-address";
        ArtemisProducerDestination destination = new ArtemisProducerDestination(address);
        try {
            destination.getNameForPartition(0);
            fail("Exception was expected");
        } catch (UnsupportedOperationException e) {
            assertThat(e.getMessage()).isEqualTo("This destination is not partitioned");
        }
    }

}