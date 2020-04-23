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
import me.snowdrop.stream.binder.artemis.properties.ArtemisExtendedBindingProperties;
import me.snowdrop.stream.binder.artemis.provisioning.ArtemisBrokerManager;
import me.snowdrop.stream.binder.artemis.provisioning.ArtemisProvisioningProvider;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.SingleConnectionFactory;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@Configuration
@AutoConfigureAfter(ArtemisAutoConfiguration.class)
@ConditionalOnClass(ActiveMQConnectionFactory.class)
@ConditionalOnBean(ConnectionFactory.class)
@EnableConfigurationProperties(ArtemisExtendedBindingProperties.class)
public class ArtemisBinderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ListenerContainerFactory listenerContainerFactory(ConnectionFactory connectionFactory) {
        return new ListenerContainerFactory(connectionFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    ArtemisMessageChannelBinder artemisMessageChannelBinder(ArtemisProvisioningProvider provisioningProvider,
            ConnectionFactory connectionFactory, ArtemisExtendedBindingProperties bindingProperties) {
        return new ArtemisMessageChannelBinder(provisioningProvider, connectionFactory, bindingProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    ArtemisBrokerManager artemisBrokerManager(ConnectionFactory connectionFactory, ArtemisProperties properties) {
        return new ArtemisBrokerManager(getServerLocator(connectionFactory), properties.getUser(),
                properties.getPassword());
    }

    private ServerLocator getServerLocator(ConnectionFactory connectionFactory) {
        if (connectionFactory instanceof ActiveMQConnectionFactory) {
            return ((ActiveMQConnectionFactory) connectionFactory).getServerLocator();
        }
        if (connectionFactory instanceof SingleConnectionFactory) {
            return getServerLocator(((SingleConnectionFactory) connectionFactory).getTargetConnectionFactory());
        }
        throw new RuntimeException("Unsupported connection factory " + connectionFactory.getClass().getName());
    }

    @Bean
    @ConditionalOnMissingBean(ProvisioningProvider.class)
    ArtemisProvisioningProvider provisioningProvider(ArtemisBrokerManager artemisBrokerManager) {
        return new ArtemisProvisioningProvider(artemisBrokerManager);
    }

}
