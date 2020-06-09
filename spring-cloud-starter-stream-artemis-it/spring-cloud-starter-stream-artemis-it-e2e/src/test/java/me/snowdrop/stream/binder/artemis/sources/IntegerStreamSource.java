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
package me.snowdrop.stream.binder.artemis.sources;

import org.jboss.logging.Logger;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@EnableBinding(Source.class)
public class IntegerStreamSource {

    private final Logger logger = Logger.getLogger(IntegerStreamSource.class);

    private final Source source;

    public IntegerStreamSource(Source source) {
        this.source = source;
    }

    public void send(int payload) {
        logger.debug("sending " + payload);
        Message<Integer> message = MessageBuilder
                .withPayload(payload)
                .build();
        source.output()
                .send(message);
    }
}
