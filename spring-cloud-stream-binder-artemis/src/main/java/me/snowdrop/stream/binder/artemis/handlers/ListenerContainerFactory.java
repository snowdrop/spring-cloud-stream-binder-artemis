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

package me.snowdrop.stream.binder.artemis.handlers;

import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import javax.jms.ConnectionFactory;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ListenerContainerFactory {

    private final ConnectionFactory connectionFactory;

    public ListenerContainerFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public AbstractMessageListenerContainer getListenerContainer(String topic, String subscriptionName) {
        DefaultMessageListenerContainer listenerContainer = new DefaultMessageListenerContainer();
        listenerContainer.setConnectionFactory(connectionFactory);
        listenerContainer.setPubSubDomain(true);
        listenerContainer.setDestinationName(topic);
        listenerContainer.setSubscriptionName(subscriptionName);
        listenerContainer.setSessionTransacted(true);
        listenerContainer.setSubscriptionDurable(true);
        listenerContainer.setSubscriptionShared(true);
        return listenerContainer;
    }

}
