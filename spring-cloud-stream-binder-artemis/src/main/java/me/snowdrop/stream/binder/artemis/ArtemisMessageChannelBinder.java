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

import me.snowdrop.stream.binder.artemis.handlers.ArtemisMessageHandler;
import me.snowdrop.stream.binder.artemis.handlers.ListenerContainerFactory;
import me.snowdrop.stream.binder.artemis.properties.ArtemisConsumerProperties;
import me.snowdrop.stream.binder.artemis.properties.ArtemisExtendedBindingProperties;
import me.snowdrop.stream.binder.artemis.properties.ArtemisProducerProperties;
import me.snowdrop.stream.binder.artemis.provisioning.ArtemisProvisioningProvider;
import org.springframework.cloud.stream.binder.AbstractMessageChannelBinder;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.ExtendedPropertiesBinder;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.dsl.jms.JmsMessageDrivenChannelAdapter;
import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.StringUtils;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.Topic;

import static me.snowdrop.stream.binder.artemis.common.NamingUtils.getAnonymousQueueName;
import static me.snowdrop.stream.binder.artemis.common.NamingUtils.getQueueName;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisMessageChannelBinder extends
        AbstractMessageChannelBinder<ExtendedConsumerProperties<ArtemisConsumerProperties>,
                ExtendedProducerProperties<ArtemisProducerProperties>, ArtemisProvisioningProvider>
        implements ExtendedPropertiesBinder<MessageChannel, ArtemisConsumerProperties, ArtemisProducerProperties> {

    private static final String[] DEFAULT_HEADERS = new String[0];

    private final ConnectionFactory connectionFactory;

    private final ListenerContainerFactory listenerContainerFactory;

    private final MessageConverter messageConverter;

    private final ArtemisExtendedBindingProperties bindingProperties;

    public ArtemisMessageChannelBinder(ArtemisProvisioningProvider provisioningProvider,
            ConnectionFactory connectionFactory, ListenerContainerFactory listenerContainerFactory,
            MessageConverter messageConverter, ArtemisExtendedBindingProperties bindingProperties) {
        super(true, DEFAULT_HEADERS, provisioningProvider);
        this.connectionFactory = connectionFactory;
        this.listenerContainerFactory = listenerContainerFactory;
        this.messageConverter = messageConverter;
        this.bindingProperties = bindingProperties;
    }

    @Override
    protected MessageHandler createProducerMessageHandler(ProducerDestination destination,
            ExtendedProducerProperties<ArtemisProducerProperties> properties) {
        return new ArtemisMessageHandler(destination, connectionFactory, messageConverter);
    }

    @Override
    protected MessageProducer createConsumerEndpoint(ConsumerDestination destination, String group,
            ExtendedConsumerProperties<ArtemisConsumerProperties> properties) {
        String subscriptionName = getSubscriptionName(destination.getName(), group);

        try (JMSContext context = connectionFactory.createContext()) {
            Topic topic = context.createTopic(destination.getName());
            AbstractMessageListenerContainer listenerContainer =
                    listenerContainerFactory.getListenerContainer(topic, subscriptionName);
            return new JmsMessageDrivenChannelAdapter(listenerContainer, new ChannelPublishingJmsMessageListener());
        }
    }

    @Override
    public ArtemisConsumerProperties getExtendedConsumerProperties(String channelName) {
        return bindingProperties.getExtendedConsumerProperties(channelName);
    }

    @Override
    public ArtemisProducerProperties getExtendedProducerProperties(String channelName) {
        return bindingProperties.getExtendedProducerProperties(channelName);
    }

    private String getSubscriptionName(String address, String group) {
        if (StringUtils.hasText(group)) {
            return getQueueName(address, group);
        } else {
            return getAnonymousQueueName(address);
        }
    }
}
