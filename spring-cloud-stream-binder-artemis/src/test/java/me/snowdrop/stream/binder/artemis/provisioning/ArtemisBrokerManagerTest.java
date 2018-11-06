package me.snowdrop.stream.binder.artemis.provisioning;

import me.snowdrop.stream.binder.artemis.properties.ArtemisCommonProperties;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.stream.provisioning.ProvisioningException;

import static org.apache.activemq.artemis.api.core.RoutingType.MULTICAST;
import static org.apache.activemq.artemis.api.core.SimpleString.toSimpleString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// TODO Find a reasonable way to test address settings change
@RunWith(MockitoJUnitRunner.class)
public class ArtemisBrokerManagerTest {

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

    private ArtemisCommonProperties artemisCommonProperties;

    private SimpleString address = toSimpleString("address-name");

    private SimpleString queue = toSimpleString("queue-name");

    @Before
    public void before() throws Exception {
        given(mockServerLocator.createSessionFactory()).willReturn(mockClientSessionFactory);
        given(mockClientSessionFactory.createSession()).willReturn(mockClientSession);
        given(mockClientSessionFactory.createSession(anyString(), anyString(), anyBoolean(), anyBoolean(), anyBoolean(),
                anyBoolean(), anyInt())).willReturn(mockClientSession);
        given(mockClientSession.addressQuery(any(SimpleString.class))).willReturn(mockAddressQuery);
        given(mockClientSession.queueQuery(any(SimpleString.class))).willReturn(mockQueueQuery);
        artemisCommonProperties = new ArtemisCommonProperties();
    }

    @Test
    public void shouldCreateAddress() throws Exception {
        ArtemisBrokerManager manager = new ArtemisBrokerManager(mockServerLocator, null, null);
        manager.createAddress(address.toString(), artemisCommonProperties);

        verify(mockServerLocator).createSessionFactory();
        verify(mockClientSessionFactory).createSession();
        verify(mockClientSession).start();
        verify(mockClientSession).createAddress(address, MULTICAST, true);
        verify(mockClientSession).stop();
        verify(mockClientSession, times(0)).queueQuery(any(SimpleString.class));
        verify(mockClientSession, times(0)).createMessage(anyBoolean());
    }

    @Test
    public void shouldCreateAddressWithCredentials() throws Exception {
        given(mockServerLocator.isPreAcknowledge()).willReturn(true);
        given(mockServerLocator.getAckBatchSize()).willReturn(1);

        ArtemisBrokerManager manager = new ArtemisBrokerManager(mockServerLocator, "user", "pass");
        manager.createAddress(address.toString(), artemisCommonProperties);

        verify(mockServerLocator).createSessionFactory();
        verify(mockClientSessionFactory).createSession("user", "pass", true, false, false, true, 1);
        verify(mockClientSession).start();
        verify(mockClientSession).createAddress(address, MULTICAST, true);
        verify(mockClientSession).stop();
        verify(mockClientSession, times(0)).queueQuery(any(SimpleString.class));
        verify(mockClientSession, times(0)).createMessage(anyBoolean());
    }

    @Test
    public void shouldNotCreateAddressIfOneExists() throws Exception {
        given(mockAddressQuery.isExists()).willReturn(true);

        ArtemisBrokerManager manager = new ArtemisBrokerManager(mockServerLocator, null, null);
        manager.createAddress(address.toString(), artemisCommonProperties);

        verify(mockServerLocator).createSessionFactory();
        verify(mockClientSessionFactory).createSession();
        verify(mockClientSession).start();
        verify(mockClientSession).stop();
        verify(mockClientSession, times(0)).createAddress(any(), any(RoutingType.class), anyBoolean());
        verify(mockClientSession, times(0)).queueQuery(any(SimpleString.class));
        verify(mockClientSession, times(0)).createMessage(anyBoolean());
    }

    @Test
    public void shouldFailToCreateAddress() throws Exception {
        given(mockServerLocator.createSessionFactory()).willThrow(new RuntimeException("test"));

        ArtemisBrokerManager manager = new ArtemisBrokerManager(mockServerLocator, null, null);

        try {
            manager.createAddress(address.toString(), artemisCommonProperties);
            fail("Provisioning exception was expected");
        } catch (ProvisioningException e) {
            String message = String.format("Failed to create address '%s'", address);
            assertThat(e.getMessage()).contains(message);
        }
    }

    @Test
    public void shouldCreateQueue() throws Exception {
        ArtemisBrokerManager manager = new ArtemisBrokerManager(mockServerLocator, null, null);
        manager.createQueue(address.toString(), queue.toString());

        verify(mockServerLocator).createSessionFactory();
        verify(mockClientSessionFactory).createSession();
        verify(mockClientSession).createSharedQueue(address, MULTICAST, queue, true);
    }

    @Test
    public void shouldCreateQueueWithCredentials() throws Exception {
        given(mockServerLocator.isPreAcknowledge()).willReturn(true);
        given(mockServerLocator.getAckBatchSize()).willReturn(1);

        ArtemisBrokerManager manager = new ArtemisBrokerManager(mockServerLocator, "user", "pass");
        manager.createQueue(address.toString(), queue.toString());

        verify(mockServerLocator).createSessionFactory();
        verify(mockClientSessionFactory).createSession("user", "pass", true, false, false, true, 1);
        verify(mockClientSession).createSharedQueue(address, MULTICAST, queue, true);
    }

    @Test
    public void shouldNotCreateQueueIfOneAlreadyExists() throws Exception {
        given(mockQueueQuery.getAddress()).willReturn(address);
        given(mockQueueQuery.isExists()).willReturn(true);

        ArtemisBrokerManager manager = new ArtemisBrokerManager(mockServerLocator, null, null);
        manager.createQueue(address.toString(), queue.toString());

        verify(mockServerLocator).createSessionFactory();
        verify(mockClientSessionFactory).createSession();
        verify(mockClientSession, times(0)).createSharedQueue(any(), any(RoutingType.class), any(), anyBoolean());
    }

    @Test
    public void shouldFailToCreateQueue() throws Exception {
        given(mockServerLocator.createSessionFactory()).willThrow(new RuntimeException("test"));

        ArtemisBrokerManager manager = new ArtemisBrokerManager(mockServerLocator, null, null);

        try {
            manager.createQueue(address.toString(), queue.toString());
            fail("Provisioning exception was expected");
        } catch (ProvisioningException e) {
            String message = String.format("Failed to create queue '%s' with address '%s'", queue, address);
            assertThat(e.getMessage()).contains(message);
        }
    }

    @Test
    public void shouldFailToCreateQueueIfOneAlreadyExistsUnderAnotherAddress() {
        SimpleString anotherAddress = toSimpleString("another-address-name");
        given(mockQueueQuery.getAddress()).willReturn(anotherAddress);
        given(mockQueueQuery.isExists()).willReturn(true);

        ArtemisBrokerManager manager = new ArtemisBrokerManager(mockServerLocator, null, null);

        try {
            manager.createQueue(address.toString(), queue.toString());
            fail("Provisioning exception was expected");
        } catch (ProvisioningException e) {
            String message = String.format(
                    "Failed to create queue '%s' with address '%s'. Queue already exists under another address '%s'",
                    queue, address, anotherAddress);
            assertThat(e.getMessage()).contains(message);
        }
    }

}
