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

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.jboss.snowdrop.stream.binder.artemis.properties.ArtemisBinderConfigurationProperties;
import org.jboss.snowdrop.stream.binder.artemis.properties.ArtemisExtendedBindingProperties;
import org.jboss.snowdrop.stream.binder.artemis.provisioning.ArtemisProvisioningProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.JndiConnectionFactoryAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.StringUtils;

import javax.jms.ConnectionFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO rename
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@Configuration
@AutoConfigureAfter(JndiConnectionFactoryAutoConfiguration.class)
@ConditionalOnClass(ServerLocator.class)
@EnableConfigurationProperties({ArtemisBinderConfigurationProperties.class, ArtemisExtendedBindingProperties.class})
public class ArtemisJmsConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ArtemisMessageChannelBinder artemisMessageChannelBinder(ArtemisProvisioningProvider provisioningProvider,
            ConnectionFactory connectionFactory, JmsTemplate jmsTemplate,
            ArtemisExtendedBindingProperties bindingProperties) {
        return new ArtemisMessageChannelBinder(provisioningProvider, connectionFactory,
                jmsTemplate.getMessageConverter(), bindingProperties);
    }

    @Bean
    @ConditionalOnMissingBean(TransportConfiguration.class)
    TransportConfiguration transportConfiguration(ArtemisBinderConfigurationProperties properties) {
        Map<String, Object> config = new HashMap<>();
        if (!StringUtils.isEmpty(properties.getHost())) {
            config.put("host", properties.getHost());
        }
        if (!StringUtils.isEmpty(properties.getPort())) {
            config.put("port", properties.getPort());
        }
        return new TransportConfiguration(properties.getTransport(), config);
    }

    @Bean
    @ConditionalOnMissingBean(ServerLocator.class)
    ServerLocator serverLocator(TransportConfiguration transportConfiguration) {
        return ActiveMQClient.createServerLocatorWithoutHA(transportConfiguration);
    }

    @Bean
    @ConditionalOnMissingBean(ProvisioningProvider.class)
    ArtemisProvisioningProvider provisioningProvider(ServerLocator serverLocator) {
        return new ArtemisProvisioningProvider(serverLocator);
    }

}