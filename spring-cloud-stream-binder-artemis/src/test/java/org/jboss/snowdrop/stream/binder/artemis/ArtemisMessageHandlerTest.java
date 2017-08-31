package org.jboss.snowdrop.stream.binder.artemis;

import org.junit.Before;
import org.junit.Ignore;
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
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

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
    public void shouldHandleTextMessage() throws Exception {
        GenericMessage<String> message = new GenericMessage<>("test");
        handler.handleMessageInternal(message);
        verify(mockConnectionFactory, times(1)).createConnection();
        verify(mockConnection, times(1)).createSession();
        verify(mockSession, times(1)).createTopic(address);
        verify(mockMessageConverter, times(1)).toMessage("test", mockSession);
        verify(mockSession, times(1)).createProducer(mockTopic);
        verify(mockMessageProducer, times(1)).send(mockMessage);
        verify(mockSession, times(1)).close();
        verify(mockConnection, times(1)).close();
    }

    @Test
    @Ignore
    public void shouldHandleTextMessageWithHeaders() {

    }

    @Test
    public void shouldHandleBytesMessage() throws Exception {
        byte[] bytes = "test".getBytes();
        GenericMessage<byte[]> message = new GenericMessage<>(bytes);
        handler.handleMessageInternal(message);
        verify(mockConnectionFactory, times(1)).createConnection();
        verify(mockConnection, times(1)).createSession();
        verify(mockSession, times(1)).createTopic(address);
        verify(mockMessageConverter, times(1)).toMessage(bytes, mockSession);
        verify(mockSession, times(1)).createProducer(mockTopic);
        verify(mockMessageProducer, times(1)).send(mockMessage);
        verify(mockSession, times(1)).close();
        verify(mockConnection, times(1)).close();
    }

    @Test
    @Ignore
    public void shouldHandleBytesMessageWithHeaders() {

    }

    @Test
    public void shouldHandleObjectMessage() throws Exception {
        Object object = new Object();
        GenericMessage<Object> message = new GenericMessage<>(object);
        handler.handleMessageInternal(message);
        verify(mockConnectionFactory, times(1)).createConnection();
        verify(mockConnection, times(1)).createSession();
        verify(mockSession, times(1)).createTopic(address);
        verify(mockMessageConverter, times(1)).toMessage(object, mockSession);
        verify(mockSession, times(1)).createProducer(mockTopic);
        verify(mockMessageProducer, times(1)).send(mockMessage);
        verify(mockSession, times(1)).close();
        verify(mockConnection, times(1)).close();
    }

    @Test
    @Ignore
    public void shouldHandleObjectMessageWithHeaders() {

    }

    @Test
    public void shouldHandleMapMessage() throws Exception {
        Map<String, String> map = Collections.singletonMap("testKey", "testValue");
        GenericMessage<Object> message = new GenericMessage<>(map);
        handler.handleMessageInternal(message);
        verify(mockConnectionFactory, times(1)).createConnection();
        verify(mockConnection, times(1)).createSession();
        verify(mockSession, times(1)).createTopic(address);
        verify(mockMessageConverter, times(1)).toMessage(map, mockSession);
        verify(mockSession, times(1)).createProducer(mockTopic);
        verify(mockMessageProducer, times(1)).send(mockMessage);
        verify(mockSession, times(1)).close();
        verify(mockConnection, times(1)).close();
    }

    @Test
    @Ignore
    public void shouldHandleMapMessageWithHeaders() {

    }

    @Test
    public void shouldHandleStreamMessage() throws Exception {
        Stream<Object> stream = Stream.empty();
        GenericMessage<Object> message = new GenericMessage<>(stream);
        handler.handleMessageInternal(message);
        verify(mockConnectionFactory, times(1)).createConnection();
        verify(mockConnection, times(1)).createSession();
        verify(mockSession, times(1)).createTopic(address);
        verify(mockMessageConverter, times(1)).toMessage(stream, mockSession);
        verify(mockSession, times(1)).createProducer(mockTopic);
        verify(mockMessageProducer, times(1)).send(mockMessage);
        verify(mockSession, times(1)).close();
        verify(mockConnection, times(1)).close();
    }

    @Test
    @Ignore
    public void shouldHandleStreamMessageWithHeaders() {

    }

}