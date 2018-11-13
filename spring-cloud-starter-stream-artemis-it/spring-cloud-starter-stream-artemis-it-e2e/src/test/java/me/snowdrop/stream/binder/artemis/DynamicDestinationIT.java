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

import javax.jms.JMSException;
import javax.jms.Message;

import me.snowdrop.stream.binder.artemis.application.StreamApplication;
import me.snowdrop.stream.binder.artemis.properties.ArtemisProducerProperties;
import me.snowdrop.stream.binder.artemis.sources.DynamicDestinationSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static me.snowdrop.stream.binder.artemis.common.NamingUtils.getQueueName;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = StreamApplication.class
)
@Import({ DynamicDestinationIT.TestConfiguration.class, DynamicDestinationSource.class })
public class DynamicDestinationIT {

    private static final String ADDRESS = "dynamic-destination";

    private static final String GROUP = "test-group";

    @Autowired
    private DynamicDestinationSource source;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Test
    public void shouldSendToDynamicallyBoundDestination() throws Exception {
        source.send(ADDRESS, "test message");
        String message = receiveStringMessage();
        assertThat(message).isEqualTo("test message");
    }

    private String receiveStringMessage() throws JMSException {
        Message message = jmsTemplate.receive(getQueueName(ADDRESS, GROUP));
        if (message != null) {
            return new String(message.getBody(byte[].class));
        }
        return null;
    }

    @Configuration
    public static class TestConfiguration {

        @Bean
        public BinderAwareChannelResolver.NewDestinationBindingCallback<ArtemisProducerProperties> configurer() {
            // Setup a required group to be able to receive message in a test method
            return (name, channel, properties, extendedProperties) -> properties.setRequiredGroups(GROUP);
        }

    }

}
