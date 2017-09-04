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

package org.jboss.snowdrop.stream.binder.artemis.provisioning;

import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.jboss.snowdrop.stream.binder.artemis.properties.ArtemisConsumerProperties;
import org.jboss.snowdrop.stream.binder.artemis.properties.ArtemisProducerProperties;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.activemq.artemis.api.core.RoutingType.MULTICAST;
import static org.apache.activemq.artemis.api.core.SimpleString.toSimpleString;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisProvisioningProvider implements
        ProvisioningProvider<ExtendedConsumerProperties<ArtemisConsumerProperties>,
                ExtendedProducerProperties<ArtemisProducerProperties>> {

    private final ServerLocator serverLocator;

    public ArtemisProvisioningProvider(ServerLocator serverLocator) {
        this.serverLocator = serverLocator;
    }

    /**
     * Provision all addresses and queues required for the producer.
     *
     * @param address    Artemis address to route messages to.
     * @param properties Producer specific properties.
     * @return
     * @throws ProvisioningException
     */
    @Override
    public ProducerDestination provisionProducerDestination(String address,
            ExtendedProducerProperties<ArtemisProducerProperties> properties) throws ProvisioningException {
        if (properties.isPartitioned()) {
            return provisionPartitionedDestination(address, properties);
        }

        return provisionUnpartitionedDestination(address, properties);
    }

    @Override
    public ConsumerDestination provisionConsumerDestination(String address, String group,
            ExtendedConsumerProperties<ArtemisConsumerProperties> properties) throws ProvisioningException {
        ArtemisConsumerDestination destination;

        if (properties.isPartitioned()) {
            destination =
                    new ArtemisConsumerDestination(String.format("%s-%d", address, properties.getInstanceIndex()));
        } else {
            destination = new ArtemisConsumerDestination(address);
        }

        createAddress(destination.getName());
        createQueue(destination.getName(), group);

        return destination;
    }

    private ArtemisProducerDestination provisionUnpartitionedDestination(String address,
            ProducerProperties properties) {
        // Create address to send messages to
        createAddress(address);
        // Create queues for each group so that messages could be persisted until consumer register
        provisionGroups(address, properties.getRequiredGroups());
        return new ArtemisProducerDestination(address);
    }

    private ArtemisPartitionedProducerDestination provisionPartitionedDestination(String address,
            ProducerProperties properties) {
        List<String> addresses = IntStream.range(0, properties.getPartitionCount())
                .mapToObj(i -> String.format("%s-%d", address, i))
                .peek(this::createAddress)
                .peek(partitionAddress -> provisionGroups(partitionAddress, properties.getRequiredGroups()))
                .collect(Collectors.toList());
        return new ArtemisPartitionedProducerDestination(addresses);
    }

    private void provisionGroups(String address, String[] groups) {
        Arrays.stream(groups)
                .forEach(group -> createQueue(address, group));
    }

    private void createAddress(String name) {
        try (ClientSessionFactory sessionFactory = serverLocator.createSessionFactory();
             ClientSession session = sessionFactory.createSession()) {
            session.createAddress(toSimpleString(name), MULTICAST, true);
        } catch (Exception e) {
            throw new ProvisioningException(String.format("Failed to create address '%s'", name), e);
        }
    }

    private void createQueue(String address, String name) {
        try (ClientSessionFactory sessionFactory = serverLocator.createSessionFactory();
             ClientSession session = sessionFactory.createSession()) {
            session.createSharedQueue(toSimpleString(address), MULTICAST, toSimpleString(name), true);
        } catch (Exception e) {
            throw new ProvisioningException(
                    String.format("Failed to create queue '%s' with address '%s'", name, address), e);
        }
    }

}
