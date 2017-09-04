package org.jboss.snowdrop.stream.binder.artemis.common;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class NamingUtilsTest {

    @Test
    public void shouldGetPartitionAddress() {
        assertThat(NamingUtils.getPartitionAddress("testAddress", 0)).isEqualTo("testAddress-0");
    }

    @Test
    public void shouldGetGroupName() {
        assertThat(NamingUtils.getQueueName("testAddress", "testGroup")).isEqualTo("testAddress-testGroup");
    }

}