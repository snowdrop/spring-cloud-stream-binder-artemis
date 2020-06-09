/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.snowdrop.stream.binder.artemis;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import me.snowdrop.stream.binder.artemis.application.StreamApplication;
import me.snowdrop.stream.binder.artemis.properties.ArtemisConsumerProperties;
import me.snowdrop.stream.binder.artemis.properties.ArtemisProducerProperties;
import me.snowdrop.stream.binder.artemis.utils.ArtemisTestBinder;
import org.assertj.core.api.Condition;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.PartitionCapableBinderTests;
import org.springframework.cloud.stream.binder.Spy;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.context.annotation.Import;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = StreamApplication.class)
@Import({ ArtemisAutoConfiguration.class, ArtemisBinderAutoConfiguration.class })
public class PartitionCapableBinderIT extends
        PartitionCapableBinderTests<ArtemisTestBinder, ExtendedConsumerProperties<ArtemisConsumerProperties>,
                ExtendedProducerProperties<ArtemisProducerProperties>> {

    @Autowired
    private ArtemisMessageChannelBinder binder;

    /**
     * Overriding because parent is checking for text/plain type, but default type is application/json in SCS v2
     */
    @Test
    @Override
    public void testSendAndReceiveNoOriginalContentType() throws Exception {
        Binder binder = getBinder();

        BindingProperties producerBindingProperties = createProducerBindingProperties(
                createProducerProperties());
        DirectChannel moduleOutputChannel = createBindableChannel("output",
                producerBindingProperties);
        BindingProperties inputBindingProperties = createConsumerBindingProperties(createConsumerProperties());
        DirectChannel moduleInputChannel = createBindableChannel("input", inputBindingProperties);
        Binding<MessageChannel> producerBinding = binder.bindProducer(String.format("bar%s0",
                getDestinationNameDelimiter()), moduleOutputChannel, producerBindingProperties.getProducer());
        Binding<MessageChannel> consumerBinding = binder.bindConsumer(String.format("bar%s0",
                getDestinationNameDelimiter()), "testSendAndReceiveNoOriginalContentType", moduleInputChannel,
                createConsumerProperties());
        binderBindUnbindLatency();

        Message<?> message = MessageBuilder.withPayload("foo")
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN).build();
        moduleOutputChannel.send(message);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Message<byte[]>> inboundMessageRef = new AtomicReference<Message<byte[]>>();
        moduleInputChannel.subscribe(message1 -> {
            try {
                inboundMessageRef.set((Message<byte[]>) message1);
            } finally {
                latch.countDown();
            }
        });

        moduleOutputChannel.send(message);
        Assert.isTrue(latch.await(5, TimeUnit.SECONDS), "Failed to receive message");
        assertThat(inboundMessageRef.get()).isNotNull();
        assertThat(new String(inboundMessageRef.get().getPayload(), StandardCharsets.UTF_8)).isEqualTo("foo");
        // Here parent looks for TEXT_PLAIN which is not correct
        assertThat(inboundMessageRef.get().getHeaders().get(MessageHeaders.CONTENT_TYPE).toString())
                .isEqualTo(MimeTypeUtils.APPLICATION_JSON_VALUE);
    }

    /**
     * Overriding because SimpleJmsHeaderMapper doesn't accept MimeType as a header value which is used by the parent
     */
    @Test(expected = MessageDeliveryException.class)
    public void testStreamListenerJavaSerializationNonSerializable() throws Exception {
        Binder binder = getBinder();

        BindingProperties producerBindingProperties = createProducerBindingProperties(createProducerProperties());

        DirectChannel moduleOutputChannel = createBindableChannel("output", producerBindingProperties);

        BindingProperties consumerBindingProperties = createConsumerBindingProperties(createConsumerProperties());

        DirectChannel moduleInputChannel = createBindableChannel("input", consumerBindingProperties);

        Binding<MessageChannel> producerBinding = binder.bindProducer(String.format("bad%s0c",
                getDestinationNameDelimiter()), moduleOutputChannel, producerBindingProperties.getProducer());

        Binding<MessageChannel> consumerBinding = binder.bindConsumer(String.format("bad%s0c",
                getDestinationNameDelimiter()), "test-3", moduleInputChannel, consumerBindingProperties.getConsumer());
        try {
            Station station = new Station();
            Message<?> message = MessageBuilder.withPayload(station)
                    // Here parent uses MessageConverterUtils.X_JAVA_SERIALIZED_OBJECT which type is not supported by the mapper
                    .setHeader(MessageHeaders.CONTENT_TYPE, "application/x-java-serialized-object")
                    .build();
            moduleOutputChannel.send(message);
        } finally {
            producerBinding.unbind();
            consumerBinding.unbind();
        }
    }

    /**
     * Overriding because parent compares arrays with Object.equals rather than with Arrays.equals
     */
    @Test
    @Override
    public void testPartitionedModuleSpEL() throws Exception {
        ArtemisTestBinder binder = getBinder();

        ExtendedConsumerProperties<ArtemisConsumerProperties> consumerProperties = createConsumerProperties();
        consumerProperties.setConcurrency(2);
        consumerProperties.setInstanceIndex(0);
        consumerProperties.setInstanceCount(3);
        consumerProperties.setPartitioned(true);
        QueueChannel input0 = new QueueChannel();
        input0.setBeanName("test.input0S");
        Binding<MessageChannel> input0Binding = binder.bindConsumer(String.format("part%s0",
                getDestinationNameDelimiter()), "testPartitionedModuleSpEL", input0, consumerProperties);
        consumerProperties.setInstanceIndex(1);
        QueueChannel input1 = new QueueChannel();
        input1.setBeanName("test.input1S");
        Binding<MessageChannel> input1Binding = binder.bindConsumer(String.format("part%s0",
                getDestinationNameDelimiter()), "testPartitionedModuleSpEL", input1, consumerProperties);
        consumerProperties.setInstanceIndex(2);
        QueueChannel input2 = new QueueChannel();
        input2.setBeanName("test.input2S");
        Binding<MessageChannel> input2Binding = binder.bindConsumer(String.format("part%s0",
                getDestinationNameDelimiter()), "testPartitionedModuleSpEL", input2, consumerProperties);

        ExtendedProducerProperties<ArtemisProducerProperties> producerProperties = createProducerProperties();
        producerProperties.setPartitionKeyExpression(spelExpressionParser.parseExpression("payload"));
        producerProperties.setPartitionSelectorExpression(spelExpressionParser.parseExpression("hashCode()"));
        producerProperties.setPartitionCount(3);

        DirectChannel output = createBindableChannel("output", createProducerBindingProperties(producerProperties));
        output.setBeanName("test.output");
        Binding<MessageChannel> outputBinding = binder.bindProducer(String.format("part%s0",
                getDestinationNameDelimiter()), output, producerProperties);
        try {
            Object endpoint = extractEndpoint(outputBinding);
            checkRkExpressionForPartitionedModuleSpEL(endpoint);
        } catch (UnsupportedOperationException ignored) {
        }

        Message<String> message2 = MessageBuilder.withPayload("2")
                .setHeader(IntegrationMessageHeaderAccessor.CORRELATION_ID, "foo")
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN)
                .setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, 42)
                .setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, 43).build();
        output.send(message2);
        output.send(MessageBuilder.withPayload("1")
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN)
                .build());
        output.send(MessageBuilder.withPayload("0")
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN)
                .build());

        Message<?> receive0 = receive(input0);
        assertThat(receive0).isNotNull();
        Message<?> receive1 = receive(input1);
        assertThat(receive1).isNotNull();
        Message<?> receive2 = receive(input2);
        assertThat(receive2).isNotNull();

        Condition<Message<?>> correlationHeadersForPayload2 = new Condition<Message<?>>() {
            @Override
            public boolean matches(Message<?> value) {
                IntegrationMessageHeaderAccessor accessor = new IntegrationMessageHeaderAccessor(value);
                return "foo".equals(accessor.getCorrelationId()) && 42 == accessor.getSequenceNumber()
                        && 43 == accessor.getSequenceSize();
            }
        };

        if (usesExplicitRouting()) {
            assertThat(receive0.getPayload()).isEqualTo("0".getBytes());
            assertThat(receive1.getPayload()).isEqualTo("1".getBytes());
            assertThat(receive2.getPayload()).isEqualTo("2".getBytes());
            assertThat(receive2).has(correlationHeadersForPayload2);
        } else {
            List<Message<?>> receivedMessages = Arrays.asList(receive0, receive1, receive2);
            assertThat(receivedMessages).extracting("payload")
                    .containsExactlyInAnyOrder("0".getBytes(), "1".getBytes(), "2".getBytes());
            Condition<Message<?>> payloadIs2 = new Condition<Message<?>>() {

                @Override
                public boolean matches(Message<?> value) {
                    // Here parent has value.getPayload().equals("2".getBytes())
                    return Arrays.equals((byte[]) value.getPayload(), "2".getBytes());
                }
            };
            assertThat(receivedMessages).filteredOn(payloadIs2).areExactly(1, correlationHeadersForPayload2);

        }
        input0Binding.unbind();
        input1Binding.unbind();
        input2Binding.unbind();
        outputBinding.unbind();
    }

    /**
     * {@link org.springframework.integration.jms.JmsSendingMessageHandler} doesn't support lifecycle
     */
    @Ignore
    @Test
    @Override
    public void testClean() {

    }

    @Override
    public Spy spyOn(String name) {
        return null;
    }

    @Override
    protected ArtemisTestBinder getBinder() throws Exception {
        if (testBinder == null) {
            testBinder = new ArtemisTestBinder(binder);
        }

        return testBinder;
    }

    @Override
    protected ExtendedConsumerProperties<ArtemisConsumerProperties> createConsumerProperties() {
        return new ExtendedConsumerProperties<>(new ArtemisConsumerProperties());
    }

    @Override
    protected ExtendedProducerProperties<ArtemisProducerProperties> createProducerProperties() {
        ExtendedProducerProperties<ArtemisProducerProperties> properties =
                new ExtendedProducerProperties<>(new ArtemisProducerProperties());
        return properties;
    }

    @Override
    protected boolean usesExplicitRouting() {
        return false;
    }

    @Override
    protected String getClassUnderTestName() {
        return ArtemisMessageChannelBinder.class.getSimpleName();
    }
}
