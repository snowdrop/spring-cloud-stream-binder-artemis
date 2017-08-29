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

package org.jboss.snowdrop.stream.binder.artemis;

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;

import java.util.Objects;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisMessageHandler extends AbstractMessageHandler {

    private final String destination;

    private final JmsTemplate jmsTemplate;

    public ArtemisMessageHandler(String destination, JmsTemplate jmsTemplate) {
        this.destination = destination;
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    protected void handleMessageInternal(Message<?> message) throws Exception {
        Objects.requireNonNull(message);
        // TODO handle partitions
        // TODO handle headers

        jmsTemplate.convertAndSend(destination, message.getPayload());
    }

}
