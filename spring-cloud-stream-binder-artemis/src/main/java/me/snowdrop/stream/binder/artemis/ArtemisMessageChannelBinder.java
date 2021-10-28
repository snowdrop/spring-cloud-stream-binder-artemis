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

import javax.jms.ConnectionFactory;

import me.snowdrop.stream.binder.artemis.listener.ListenerContainerFactory;
import me.snowdrop.stream.binder.artemis.listener.RetryableChannelPublishingJmsMessageListener;
import me.snowdrop.stream.binder.artemis.properties.ArtemisConsumerProperties;
import me.snowdrop.stream.binder.artemis.properties.ArtemisExtendedBindingProperties;
import me.snowdrop.stream.binder.artemis.properties.ArtemisProducerProperties;
import me.snowdrop.stream.binder.artemis.provisioning.ArtemisProvisioningProvider;
import org.springframework.cloud.stream.binder.AbstractMessageChannelBinder;
import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.ExtendedPropertiesBinder;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.integration.jms.JmsSendingMessageHandler;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StringUtils;

import static me.snowdrop.stream.binder.artemis.common.NamingUtils.getAnonymousGroupName;
import static me.snowdrop.stream.binder.artemis.common.NamingUtils.getQueueName;
import static org.springframework.cloud.stream.binder.BinderHeaders.PARTITION_HEADER;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisMessageChannelBinder extends
        AbstractMessageChannelBinder<ExtendedConsumerProperties<ArtemisConsumerProperties>,
                ExtendedProducerProperties<ArtemisProducerProperties>, ArtemisProvisioningProvider>
        implements ExtendedPropertiesBinder<MessageChannel, ArtemisConsumerProperties, ArtemisProducerProperties> {

    private static final String[] DEFAULT_HEADERS = new String[0];

    private final ConnectionFactory connectionFactory;

    private final ArtemisExtendedBindingProperties bindingProperties;

    public ArtemisMessageChannelBinder(ArtemisProvisioningProvider provisioningProvider,
            ConnectionFactory connectionFactory, ArtemisExtendedBindingProperties bindingProperties) {
        super(DEFAULT_HEADERS, provisioningProvider);
        this.connectionFactory = connectionFactory;
        this.bindingProperties = bindingProperties;
    }

    @Override
    protected MessageHandler createProducerMessageHandler(ProducerDestination destination,
            ExtendedProducerProperties<ArtemisProducerProperties> properties, MessageChannel errorChannel) {
        logger.debug("Creating producer message handler for '" + destination + "'");

        JmsSendingMessageHandler handler = Jms.outboundAdapter(connectionFactory)
                .destination(message -> getMessageDestination(message, destination))
                .configureJmsTemplate(templateSpec -> templateSpec.pubSubDomain(true))
                .get();
        handler.setApplicationContext(getApplicationContext());
        handler.setBeanFactory(getBeanFactory());

        return handler;
    }

    @Override
    protected MessageProducer createConsumerEndpoint(ConsumerDestination destination, String group,
            ExtendedConsumerProperties<ArtemisConsumerProperties> properties) {
        logger.debug("Creating consumer endpoint for '{" + destination + "}' with a group '{" + group + "}'");

        if (!StringUtils.hasText(group)) {
            group = getAnonymousGroupName();
        }

        String subscriptionName = getQueueName(destination.getName(), group);
        ListenerContainerFactory listenerContainerFactory = new ListenerContainerFactory(connectionFactory);
        AbstractMessageListenerContainer listenerContainer = listenerContainerFactory
                .getListenerContainer(destination.getName(), subscriptionName, properties);

        if (properties.getMaxAttempts() == 1) {
            return Jms.messageDrivenChannelAdapter(listenerContainer).get();
        }

        RetryTemplate retryTemplate = buildRetryTemplate(properties);
        ErrorInfrastructure errorInfrastructure = registerErrorInfrastructure(destination, group, properties);
        RetryableChannelPublishingJmsMessageListener listener =
                new RetryableChannelPublishingJmsMessageListener(retryTemplate, errorInfrastructure.getRecoverer());
        listener.setExpectReply(false);
        return new JmsMessageDrivenEndpoint(listenerContainer, listener);
    }

    @Override
    public ArtemisConsumerProperties getExtendedConsumerProperties(String channelName) {
        return bindingProperties.getExtendedConsumerProperties(channelName);
    }

    @Override
    public ArtemisProducerProperties getExtendedProducerProperties(String channelName) {
        return bindingProperties.getExtendedProducerProperties(channelName);
    }

    @Override
    public String getDefaultsPrefix() {
        return this.bindingProperties.getDefaultsPrefix();
    }

    @Override
    public Class<? extends BinderSpecificPropertiesProvider> getExtendedPropertiesEntryClass() {
        return this.bindingProperties.getExtendedPropertiesEntryClass();
    }

    @Override
    protected String errorsBaseName(ConsumerDestination destination, String group,
            ExtendedConsumerProperties<ArtemisConsumerProperties> properties) {
        if (group == null) {
            /*
            This is a workaround to not get NPE when calling getQueueName.
            Such situation occurs with anonymous groups during the destroyErrorInfrastructure execution.
            Differently from registerErrorInfrastructure, destroyErrorInfrastructure doesn't know generated group name and uses null instead.
            However, this means that the actual channel, recoverer, message handler and bridge handler beans aren't destroyed.
            See https://github.com/snowdrop/spring-cloud-stream-binder-artemis/issues/22
             */
            return destination.getName() + ".errors";
        }
        return getQueueName(destination.getName(), group) + ".errors";
    }

    private String getMessageDestination(Message<?> message, ProducerDestination destination) {
        Object partition = message.getHeaders()
                .get(PARTITION_HEADER);

        if (partition == null) {
            return destination.getName();
        }
        if (partition instanceof Integer) {
            return destination.getNameForPartition((Integer) partition);
        }
        if (partition instanceof String) {
            return destination.getNameForPartition(Integer.parseInt((String) partition));
        }
        throw new IllegalArgumentException(
                String.format("The provided partition '%s' is not a valid format", partition));
    }
}
