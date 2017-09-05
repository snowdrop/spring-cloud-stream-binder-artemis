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

package org.jboss.snowdrop.stream.binder.artemis.integration.sink;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Topic;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
        properties = {
                "spring.cloud.stream.bindings.input.destination=testIn",
                "spring.cloud.stream.bindings.input.group=streamApplication"
        }
)
public class ArtemisBinderWithStreamSinkIT {

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private TestSink sink;

    @Value("${spring.cloud.stream.bindings.input.destination}")
    private String inputDestination;

    @Test
    public void testReceive() throws JMSException {
        String message = "test message";

        try (JMSContext context = connectionFactory.createContext()) {
            Topic inputTopic = context.createTopic(inputDestination);
            JMSProducer producer = context.createProducer();
            producer.send(inputTopic, message);
        }

        await().atMost(5, SECONDS)
                .untilAsserted(() -> assertThat(sink.getMessages()).containsExactly(message));
    }

}
