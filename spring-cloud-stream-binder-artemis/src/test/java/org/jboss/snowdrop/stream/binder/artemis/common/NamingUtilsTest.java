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

    @Test(expected = NullPointerException.class)
    public void shouldNotAcceptNullAddressToGetPartitionAddress() {
        NamingUtils.getPartitionAddress(null, 0);
    }

    @Test
    public void shouldGetQueueName() {
        assertThat(NamingUtils.getQueueName("testAddress", "testGroup")).isEqualTo("testAddress-testGroup");
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotAcceptNullAddressToGetQueueName() {
        NamingUtils.getQueueName(null, "testGroup");
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotAcceptNullGroupToGetQueueName() {
        NamingUtils.getQueueName("testAddress", null);
    }

    @Test
    public void shouldGetAnonymousQueueName() {
        assertThat(NamingUtils.getAnonymousQueueName("testAddress")).hasSize(34)
                .startsWith("testAddress-");
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotAcceptNullAddressToGetAnonymousQueueName() {
        NamingUtils.getAnonymousQueueName(null);
    }

}