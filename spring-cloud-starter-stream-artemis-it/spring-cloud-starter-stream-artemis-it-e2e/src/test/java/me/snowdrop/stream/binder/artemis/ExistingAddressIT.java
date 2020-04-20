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

import me.snowdrop.stream.binder.artemis.application.StreamApplication;
import me.snowdrop.stream.binder.artemis.listeners.StringStreamListener;
import me.snowdrop.stream.binder.artemis.sources.StringStreamSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = StreamApplication.class,
        properties = {
                "spring.artemis.embedded.queues=existing-destination",
                "spring.cloud.stream.bindings.output.destination=existing-destination",
                "spring.cloud.stream.bindings.input.destination=existing-destination",
                "spring.jms.cache.enabled=false"
        }
)
@Import({ StringStreamSource.class, StringStreamListener.class })
public class ExistingAddressIT {

    @Autowired
    private StringStreamSource source;

    @Autowired
    private StringStreamListener listener;

    @Test
    public void shouldSendAndReceiveMessageThroughExistingAddress() {
        source.send("test message");

        await().atMost(10, SECONDS)
                .until(() -> listener.getPayloads().size() == 1);

        assertThat(listener.getPayloads())
                .contains("test message");
    }

}
