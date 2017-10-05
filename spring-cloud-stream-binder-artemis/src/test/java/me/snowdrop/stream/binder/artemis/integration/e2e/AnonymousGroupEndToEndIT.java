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

package me.snowdrop.stream.binder.artemis.integration.e2e;

import me.snowdrop.stream.binder.artemis.integration.e2e.common.FirstReceiver;
import me.snowdrop.stream.binder.artemis.integration.e2e.common.SecondReceiver;
import me.snowdrop.stream.binder.artemis.integration.e2e.common.Sender;
import me.snowdrop.stream.binder.artemis.integration.e2e.common.SerializablePayload;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.Map;

import static me.snowdrop.stream.binder.artemis.integration.e2e.common.AbstractReceiver.EXCEPTION_REQUEST;
import static me.snowdrop.stream.binder.artemis.integration.e2e.common.Assertions.assertHeaders;
import static me.snowdrop.stream.binder.artemis.integration.e2e.common.Assertions.assertPayload;
import static me.snowdrop.stream.binder.artemis.integration.e2e.common.AwaitUtils.awaitForHandledMessages;
import static me.snowdrop.stream.binder.artemis.integration.e2e.common.AwaitUtils.awaitForReceivedMessages;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {Sender.class, FirstReceiver.class, SecondReceiver.class},
        properties = {
                "spring.cloud.stream.bindings.output.destination=testDestination",
                "spring.cloud.stream.bindings.input.destination=testDestination",
                "spring.cloud.stream.bindings.input.consumer.backOffMultiplier=1",
                "spring.cloud.stream.bindings.input.consumer.backOffInitialInterval=50",
                "spring.cloud.stream.bindings.alternativeInput.destination=testDestination",
                "spring.cloud.stream.artemis.binder.transport=org.apache.activemq.artemis.core.remoting.impl.invm"
                        + ".InVMConnectorFactory"
        }
)
@EnableAutoConfiguration
public class AnonymousGroupEndToEndIT extends MultipleGroupsEndToEndIT {

    private static final String[] MESSAGES = {
            "Test message 1", "Test message 2", "Test message 3", "Test message 4"
    };

    @Autowired
    private Sender sender;

    @Autowired
    private FirstReceiver firstReceiver;

    @Autowired
    private SecondReceiver secondReceiver;

    @Test
    public void shouldReceiveMessageWithAllReceiversAndRetainHeaders() {
        Map<String, Object> headers = Collections.singletonMap("headerKey", "headerValue");
        for (String message : MESSAGES) {
            sender.send(message, headers);
        }

        awaitForHandledMessages(firstReceiver, MESSAGES.length);
        awaitForHandledMessages(secondReceiver, MESSAGES.length);

        assertPayload(firstReceiver.getHandledMessages(), MESSAGES);
        assertPayload(secondReceiver.getHandledMessages(), MESSAGES);

        assertHeaders(firstReceiver.getHandledMessages(), headers);
        assertHeaders(secondReceiver.getHandledMessages(), headers);
    }

    @Ignore // https://issues.jboss.org/browse/SB-254
    @Test
    public void shouldReceiveMessageWithAllReceiversAndFixedHeaders() {
        Map<String, Object> headers = Collections.singletonMap("header-key", "header-value");
        for (String message : MESSAGES) {
            sender.send(message, headers);
        }

        awaitForHandledMessages(firstReceiver, MESSAGES.length);
        awaitForHandledMessages(secondReceiver, MESSAGES.length);

        assertPayload(firstReceiver.getHandledMessages(), MESSAGES);
        assertPayload(secondReceiver.getHandledMessages(), MESSAGES);

        assertHeaders(firstReceiver.getHandledMessages(), headers);
        assertHeaders(secondReceiver.getHandledMessages(), headers);
    }


    @Ignore // https://issues.jboss.org/browse/SB-255
    @Test
    public void shouldRetryMessagesForFailingReceivers() {
        sender.send(EXCEPTION_REQUEST);

        awaitForReceivedMessages(firstReceiver, 3);
        awaitForReceivedMessages(secondReceiver, 3);

        assertThat(firstReceiver.getHandledMessages()).hasSize(0);
        assertThat(secondReceiver.getHandledMessages()).hasSize(0);
    }

    @Test
    public void shouldSupportSerializablePayload() {
        SerializablePayload payload = new SerializablePayload("testPayload");
        sender.send(payload);

        awaitForHandledMessages(firstReceiver, 1);
        assertThat(firstReceiver.getHandledMessages()
                .get(0)
                .getPayload()).isEqualTo(payload);
    }

    @Test
    public void shouldSupportPrimitivePayload() {
        sender.send(1);

        awaitForHandledMessages(firstReceiver, 1);
        assertThat(firstReceiver.getHandledMessages()
                .get(0)
                .getPayload()).isEqualTo(1);
    }

}
