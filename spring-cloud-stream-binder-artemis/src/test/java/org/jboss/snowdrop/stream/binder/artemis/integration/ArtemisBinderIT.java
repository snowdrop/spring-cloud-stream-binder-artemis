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

package org.jboss.snowdrop.stream.binder.artemis.integration;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.jms.JMSException;
import javax.jms.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ArtemisBinderIT {

    @Autowired
    private JmsTemplate template;

    @Value("${spring.cloud.stream.bindings.input.group}")
    private String inputGroup;

    @Value("${spring.cloud.stream.bindings.input.destination}")
    private String inputDestination;

    @Value("${spring.cloud.stream.bindings.output.destination}")
    private String outputDestination;

    @BeforeClass
    public static void setup() {

    }

    @Test
    public void testSendAndReceive() throws JMSException {
        String originalMessageText = "test message";

        String inputAddress = String.format("%s::%s", inputDestination, inputGroup);
        template.send(inputAddress, s -> s.createTextMessage(originalMessageText));

        Message resultMessage = template.receive(outputDestination);
        assertThat(resultMessage).isNotNull();

        String resultMessageText = new String(resultMessage.getBody(byte[].class));
        assertThat(resultMessageText).isEqualTo(originalMessageText.toUpperCase());
    }

}