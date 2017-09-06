package org.jboss.snowdrop.stream.binder.artemis.handlers;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.messaging.support.GenericMessage;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.stream.binder.BinderHeaders.PARTITION_HEADER;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisMessageHandlerTest {

    private final String address = "test-address";

    @Mock
    private ProducerDestination mockDestination;

    @Mock
    private ConnectionFactory mockConnectionFactory;

    @Mock
    private Connection mockConnection;

    @Mock
    private Session mockSession;

    @Mock
    private Topic mockTopic;

    @Mock
    private Message mockMessage;

    @Mock
    private MessageProducer mockMessageProducer;

    @Mock
    private MessageConverter mockMessageConverter;

    private ArtemisMessageHandler handler;

    @Before
    public void before() throws JMSException {
        MockitoAnnotations.initMocks(this);

        when(mockConnectionFactory.createConnection()).thenReturn(mockConnection);
        when(mockConnection.createSession()).thenReturn(mockSession);
        when(mockSession.createTopic(anyString())).thenReturn(mockTopic);
        when(mockSession.createProducer(any())).thenReturn(mockMessageProducer);
        when(mockMessageConverter.toMessage(any(), eq(mockSession))).thenReturn(mockMessage);

        handler = new ArtemisMessageHandler(mockDestination, mockConnectionFactory, mockMessageConverter);
    }

    @Test
    public void shouldHandleUnpartitionedMessage() throws Exception {
        when(mockDestination.getName()).thenReturn(address);

        GenericMessage<String> message = new GenericMessage<>("test");

        handler.handleMessageInternal(message);

        verify(mockDestination, times(1)).getName();
        verify(mockDestination, times(0)).getNameForPartition(anyInt());
        verify(mockConnectionFactory, times(1)).createConnection();
        verify(mockConnection, times(1)).createSession();
        verify(mockSession, times(1)).createTopic(address);
        verify(mockMessageConverter, times(1)).toMessage(message, mockSession);
        verify(mockSession, times(1)).createProducer(mockTopic);
        verify(mockMessageProducer, times(1)).send(mockMessage);
        verify(mockSession, times(1)).close();
        verify(mockConnection, times(1)).close();
    }

    @Test
    public void shouldHandlePartitionedMessageWithIntegerHeader() throws Exception {
        when(mockDestination.getNameForPartition(0)).thenReturn(address);

        GenericMessage<String> message = new GenericMessage<>("test", singletonMap(PARTITION_HEADER, 0));

        handler.handleMessageInternal(message);
        verify(mockDestination, times(0)).getName();
        verify(mockDestination, times(1)).getNameForPartition(0);
        verify(mockConnectionFactory, times(1)).createConnection();
        verify(mockConnection, times(1)).createSession();
        verify(mockSession, times(1)).createTopic(address);
        verify(mockMessageConverter, times(1)).toMessage(message, mockSession);
        verify(mockSession, times(1)).createProducer(mockTopic);
        verify(mockMessageProducer, times(1)).send(mockMessage);
        verify(mockSession, times(1)).close();
        verify(mockConnection, times(1)).close();
    }

    @Test
    public void shouldHandlePartitionedMessageWithStringHeader() throws Exception {
        when(mockDestination.getNameForPartition(0)).thenReturn(address);

        GenericMessage<String> message = new GenericMessage<>("test", singletonMap(PARTITION_HEADER, "0"));

        handler.handleMessageInternal(message);
        verify(mockDestination, times(0)).getName();
        verify(mockDestination, times(1)).getNameForPartition(0);
        verify(mockConnectionFactory, times(1)).createConnection();
        verify(mockConnection, times(1)).createSession();
        verify(mockSession, times(1)).createTopic(address);
        verify(mockMessageConverter, times(1)).toMessage(message, mockSession);
        verify(mockSession, times(1)).createProducer(mockTopic);
        verify(mockMessageProducer, times(1)).send(mockMessage);
        verify(mockSession, times(1)).close();
        verify(mockConnection, times(1)).close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToGetAddressForWrongPartition() throws Exception {
        GenericMessage<String> message = new GenericMessage<>("test", singletonMap(PARTITION_HEADER, new Object()));
        handler.handleMessageInternal(message);
    }

    @Test
    public void shouldStartAndStop() {
        assertThat(handler.isRunning()).isFalse();

        handler.start();
        assertThat(handler.isRunning()).isTrue();

        handler.stop();
        assertThat(handler.isRunning()).isFalse();
    }

}