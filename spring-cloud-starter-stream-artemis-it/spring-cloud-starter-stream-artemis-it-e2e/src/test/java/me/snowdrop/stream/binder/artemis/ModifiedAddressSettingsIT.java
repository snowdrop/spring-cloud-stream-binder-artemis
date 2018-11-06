/*
 * Copyright 2016-2018 Red Hat, Inc, and individual contributors.
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

import me.snowdrop.stream.binder.artemis.application.StreamApplication;
import me.snowdrop.stream.binder.artemis.listeners.IntegerStreamListener;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientRequestor;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.api.core.management.AddressSettingsInfo;
import org.apache.activemq.artemis.api.core.management.ManagementHelper;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import static org.apache.activemq.artemis.api.core.management.ResourceNames.BROKER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = StreamApplication.class,
        properties = {
                "spring.cloud.stream.bindings.input.destination=custom-destination",
                "spring.cloud.stream.artemis.bindings.input.consumer.modifyAddressSettings=true",
                "spring.cloud.stream.artemis.bindings.input.consumer.autoBindDeadLetterAddress=true",
                "spring.cloud.stream.artemis.bindings.input.consumer.autoBindExpiryAddress=true",
                "spring.cloud.stream.artemis.bindings.input.consumer.brokerRedeliveryDelay=1",
                "spring.cloud.stream.artemis.bindings.input.consumer.brokerMaxRedeliveryDelay=2",
                "spring.cloud.stream.artemis.bindings.input.consumer.brokerRedeliveryDelayMultiplier=3.0",
                "spring.cloud.stream.artemis.bindings.input.consumer.brokerMaxDeliveryAttempts=4",
                "spring.cloud.stream.artemis.bindings.input.consumer.brokerSendToDlaOnNoRoute=true",
        }
)
@Import({ IntegerStreamListener.class })
public class ModifiedAddressSettingsIT {

    @Autowired
    private ActiveMQConnectionFactory connectionFactory;

    @Test
    public void shouldGetModifiedAddressSettings() throws Exception {
        ServerLocator serverLocator = connectionFactory.getServerLocator();
        try (ClientSessionFactory sessionFactory = serverLocator.createSessionFactory();
             ClientSession session = sessionFactory.createSession();
             ClientRequestor requestor = new ClientRequestor(session, "activemq.management")) {
            session.start();
            ClientMessage request = session.createMessage(false);
            ManagementHelper.putOperationInvocation(request, BROKER, "getAddressSettingsAsJSON", "custom-destination");
            ClientMessage reply = requestor.request(request);
            session.stop();

            String result = (String) ManagementHelper.getResult(reply, String.class);
            AddressSettingsInfo addressSettings = AddressSettingsInfo.from(result);

            assertThat(addressSettings.getDeadLetterAddress()).isEqualTo("custom-destination.dlq");
            assertThat(addressSettings.getExpiryAddress()).isEqualTo("custom-destination.exp");
            assertThat(addressSettings.getRedeliveryDelay()).isEqualTo(1);
            assertThat(addressSettings.getMaxRedeliveryDelay()).isEqualTo(2);
            assertThat(addressSettings.getRedeliveryMultiplier()).isEqualTo(3.0);
            assertThat(addressSettings.getMaxDeliveryAttempts()).isEqualTo(4);
            assertThat(addressSettings.isSendToDLAOnNoRoute()).isTrue();
        }
    }

}
