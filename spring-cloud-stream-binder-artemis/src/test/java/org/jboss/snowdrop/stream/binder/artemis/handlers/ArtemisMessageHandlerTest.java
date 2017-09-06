package org.jboss.snowdrop.stream.binder.artemis.handlers;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.messaging.support.GenericMessage;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisMessageHandlerTest {

    private final String address = "test-address";

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
        handler = new ArtemisMessageHandler(address, mockConnectionFactory, mockMessageConverter);
    }

    @Test
    public void shouldHandleMessage() throws Exception {
        GenericMessage<String> message = new GenericMessage<>("test");
        handler.handleMessageInternal(message);
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
    public void shouldStartAndStop() {
        assertThat(handler.isRunning()).isFalse();
        handler.start();
        assertThat(handler.isRunning()).isTrue();
        handler.stop();
        assertThat(handler.isRunning()).isFalse();
    }

}