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

package me.snowdrop.stream.binder.artemis.provisioning;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import me.snowdrop.stream.binder.artemis.properties.ArtemisConsumerProperties;
import me.snowdrop.stream.binder.artemis.properties.ArtemisProducerProperties;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;

import static me.snowdrop.stream.binder.artemis.common.NamingUtils.getPartitionAddress;
import static me.snowdrop.stream.binder.artemis.common.NamingUtils.getQueueName;
import static org.apache.activemq.artemis.api.core.RoutingType.MULTICAST;
import static org.apache.activemq.artemis.api.core.SimpleString.toSimpleString;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisProvisioningProvider implements ProvisioningProvider<
        ExtendedConsumerProperties<ArtemisConsumerProperties>, ExtendedProducerProperties<ArtemisProducerProperties>> {

    private final Logger logger = Logger.getLogger(ArtemisProvisioningProvider.class.getName());

    private final ServerLocator serverLocator;

    private final String username;

    private final String password;

    public ArtemisProvisioningProvider(ServerLocator serverLocator, String username, String password) {
        this.serverLocator = serverLocator;
        this.username = username;
        this.password = password;
    }

    /**
     * Provision all addresses and queues required for the producer.
     * If destination is unpartitioned, Artemis address is created with a value provided in an address argument. For
     * the partitioned destination, Artemis address is created for each partition using the following naming scheme:
     * {address}-{partitionIndex}.
     * <p>
     * For each address and required group pair a shared queue is created using the following naming scheme:
     * {address}[-partitionIndex]-{groupName}
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
            return provisionPartitionedProducerDestination(address, properties);
        }

        return provisionUnpartitionedProducerDestination(address, properties);
    }

    /**
     * Provision address required for the consumer. Queue will be created later when registering consumer listener.
     * If destination is unpartitioned, Artemis address is created with a value provided in an address argument. If
     * destination is partitioned, Artemis address is created using the following naming scheme:
     * {address}-{instanceIndex}.
     *
     * @param address
     * @param group
     * @param properties
     * @return
     * @throws ProvisioningException
     */
    @Override
    public ConsumerDestination provisionConsumerDestination(String address, String group,
            ExtendedConsumerProperties<ArtemisConsumerProperties> properties) throws ProvisioningException {
        ArtemisConsumerDestination destination;

        if (properties.isPartitioned()) {
            destination = new ArtemisConsumerDestination(getPartitionAddress(address, properties.getInstanceIndex()));
        } else {
            destination = new ArtemisConsumerDestination(address);
        }

        createAddress(destination.getName());

        return destination;
    }

    private ArtemisProducerDestination provisionUnpartitionedProducerDestination(String address,
            ProducerProperties properties) {
        // Create address to send messages to
        createAddress(address);
        // Create queues for each group so that messages could be persisted until consumer register
        provisionGroups(address, properties.getRequiredGroups());
        return new ArtemisProducerDestination(address);
    }

    private ArtemisPartitionedProducerDestination provisionPartitionedProducerDestination(String address,
            ProducerProperties properties) {
        List<String> addresses = IntStream.range(0, properties.getPartitionCount())
                .mapToObj(i -> getPartitionAddress(address, i))
                .peek(this::createAddress)
                .peek(partitionAddress -> provisionGroups(partitionAddress, properties.getRequiredGroups()))
                .collect(Collectors.toList());
        return new ArtemisPartitionedProducerDestination(addresses);
    }

    private void provisionGroups(String address, String[] groups) {
        Arrays.stream(groups)
                .map(group -> getQueueName(address, group))
                .forEach(queueName -> createQueue(address, queueName));
    }

    private void createAddress(String name) {
        logger.fine(String.format("Creating address='%s'", name));

        try (ClientSessionFactory sessionFactory = serverLocator.createSessionFactory();
             ClientSession session = getClientSession(sessionFactory)) {
            session.createAddress(toSimpleString(name), MULTICAST, true);
        } catch (Exception e) {
            throw new ProvisioningException(String.format("Failed to create address '%s'", name), e);
        }
    }

    private void createQueue(String address, String name) {
        logger.fine(String.format("Creating queue='%s' with address='%s'", name, address));

        try (ClientSessionFactory sessionFactory = serverLocator.createSessionFactory();
             ClientSession session = getClientSession(sessionFactory)) {
            session.createSharedQueue(toSimpleString(address), MULTICAST, toSimpleString(name), true);
        } catch (Exception e) {
            throw new ProvisioningException(
                    String.format("Failed to create queue '%s' with address '%s'", name, address), e);
        }
    }

    private ClientSession getClientSession(ClientSessionFactory clientSessionFactory) throws Exception {
        if (username == null) {
            return clientSessionFactory.createSession();
        }

        return clientSessionFactory.createSession(username, password, true, false, false,
                serverLocator.isPreAcknowledge(), serverLocator.getAckBatchSize());
    }

}
