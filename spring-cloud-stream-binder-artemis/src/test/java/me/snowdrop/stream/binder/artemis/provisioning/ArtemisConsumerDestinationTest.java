package me.snowdrop.stream.binder.artemis.provisioning;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisConsumerDestinationTest {

    @Test
    public void shouldGetName() {
        String name = "test-name";
        ArtemisConsumerDestination destination = new ArtemisConsumerDestination(name);
        assertThat(destination.getName()).isEqualTo(name);
    }

}