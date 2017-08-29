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
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;

import java.util.Arrays;

import static org.apache.activemq.artemis.api.core.RoutingType.MULTICAST;
import static org.apache.activemq.artemis.api.core.SimpleString.toSimpleString;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisProvisioningProvider implements ProvisioningProvider<ConsumerProperties, ProducerProperties> {

    private final ServerLocator serverLocator;

    public ArtemisProvisioningProvider(ServerLocator serverLocator) {
        this.serverLocator = serverLocator;
    }

    @Override
    public ProducerDestination provisionProducerDestination(String name, ProducerProperties properties)
            throws ProvisioningException {
//        if (properties.isPartitioned()) {
//            return provisionPartitionedDestination(name, properties.getPartitionCount());
//        }

        return provisionUnpartitionedDestination(name, properties);
    }

    @Override
    public ConsumerDestination provisionConsumerDestination(String name, String group, ConsumerProperties properties)
            throws ProvisioningException {
//        if (properties.isPartitioned()) {
//            return new ArtemisConsumerDestination(getPartitionedAddress(name, properties.getInstanceCount()));
//        }

        return new ArtemisConsumerDestination(String.format("%s::%s", name, group));
    }

    private ArtemisProducerDestination provisionUnpartitionedDestination(String address,
            ProducerProperties properties) {
        // Create address to send messages to
        createAddress(address);
        // Create queues for each group so that messages could be persisted until consumer register
        Arrays.stream(properties.getRequiredGroups())
                .forEach(group -> createQueue(address, group));
        return new ArtemisProducerDestination(address);
    }

//    private ArtemisPartitionedProducerDestination provisionPartitionedDestination(String name, int partitionsCount) {
//        List<String> addresses = IntStream.range(0, partitionsCount)
//                .mapToObj(i -> getPartitionedAddress(name, i))
//                .peek(this::createAddress)
//                .collect(Collectors.toList());
//        return new ArtemisPartitionedProducerDestination(addresses);
//    }

    private void createAddress(String name) {
        try (ClientSessionFactory sessionFactory = serverLocator.createSessionFactory();
             ClientSession session = sessionFactory.createSession()) {
            session.createAddress(toSimpleString(name), MULTICAST, true);
        } catch (Exception e) {
            throw new ProvisioningException(String.format("Failed to create address '%s'", name));
        }
    }

    private void createQueue(String address, String name) {
        try (ClientSessionFactory sessionFactory = serverLocator.createSessionFactory();
             ClientSession session = sessionFactory.createSession()) {
            session.createSharedQueue(toSimpleString(address), MULTICAST, toSimpleString(name), true);
        } catch (Exception e) {
            throw new ProvisioningException(
                    String.format("Failed to create queue '%s' with address '%s'", name, address));
        }
    }


//    private String getPartitionedAddress(String address, int partition) {
//        return String.format("%s-%d", address, partition);
//    }

}
