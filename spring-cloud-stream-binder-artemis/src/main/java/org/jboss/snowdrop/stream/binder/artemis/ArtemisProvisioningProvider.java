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

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.jboss.snowdrop.stream.binder.artemis.properties.ArtemisConsumerProperties;
import org.jboss.snowdrop.stream.binder.artemis.properties.ArtemisProducerProperties;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.activemq.artemis.api.core.RoutingType.MULTICAST;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisProvisioningProvider implements
        ProvisioningProvider<ExtendedConsumerProperties<ArtemisConsumerProperties>,
                ExtendedProducerProperties<ArtemisProducerProperties>> {

    private final ServerLocator serverLocator;

    ArtemisProvisioningProvider(ServerLocator serverLocator) {
        this.serverLocator = serverLocator;
    }

    @Override
    public ProducerDestination provisionProducerDestination(final String name,
            ExtendedProducerProperties<ArtemisProducerProperties> properties) throws ProvisioningException {
        if (properties.isPartitioned()) {
            return provisionPartitionedDestination(name, properties.getPartitionCount());
        }

        return provisionUnpartitionedDestination(name);
    }

    @Override
    public ConsumerDestination provisionConsumerDestination(String name, String group,
            ExtendedConsumerProperties<ArtemisConsumerProperties> properties) throws ProvisioningException {
        if (properties.isPartitioned()) {
            return new ArtemisConsumerDestination(getPartitionedAddress(name, properties.getInstanceCount()));
        }

        return new ArtemisConsumerDestination(name);
    }

    private ArtemisProducerDestination provisionUnpartitionedDestination(String name) {
        createAddress(name);
        return new ArtemisProducerDestination(name);
    }

    private ArtemisPartitionedProducerDestination provisionPartitionedDestination(String name, int partitionsCount) {
        List<String> addresses = IntStream.range(0, partitionsCount)
                .mapToObj(i -> getPartitionedAddress(name, i))
                .peek(this::createAddress)
                .collect(Collectors.toList());
        return new ArtemisPartitionedProducerDestination(addresses);
    }

    private void createAddress(String name) {
        try (ClientSessionFactory sessionFactory = serverLocator.createSessionFactory();
             ClientSession session = sessionFactory.createSession()) {
            session.createAddress(SimpleString.toSimpleString(name), MULTICAST, false);
        } catch (Exception e) {
            throw new ProvisioningException(String.format("Failed to create address '%s'", name));
        }
    }

    private String getPartitionedAddress(String address, int partition) {
        return String.format("%s-%d", address, partition);
    }

}
