/*
 * Copyright 2016-2018 Red Hat, Inc, and individual contributors.
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
import java.util.stream.IntStream;

import me.snowdrop.stream.binder.artemis.properties.ArtemisConsumerProperties;
import me.snowdrop.stream.binder.artemis.properties.ArtemisProducerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;

import static me.snowdrop.stream.binder.artemis.common.NamingUtils.getPartitionAddress;
import static me.snowdrop.stream.binder.artemis.common.NamingUtils.getQueueName;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisProvisioningProvider implements ProvisioningProvider<
        ExtendedConsumerProperties<ArtemisConsumerProperties>, ExtendedProducerProperties<ArtemisProducerProperties>> {

    private final Logger logger = LoggerFactory.getLogger(ArtemisProvisioningProvider.class);

    private final ArtemisBrokerManager artemisBrokerManager;

    public ArtemisProvisioningProvider(ArtemisBrokerManager artemisBrokerManager) {
        this.artemisBrokerManager = artemisBrokerManager;
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
            logger.debug("Provisioning partitioned consumer destination with address '{}' and instance index '{}'",
                    address, properties.getInstanceIndex());
            destination = new ArtemisConsumerDestination(getPartitionAddress(address, properties.getInstanceIndex()));
        } else {
            logger.debug("Provisioning unpartitioned consumer destination with address '{}'", address);
            destination = new ArtemisConsumerDestination(address);
        }

        artemisBrokerManager.createAddress(destination.getName(), properties.getExtension());

        return destination;
    }

    private ArtemisProducerDestination provisionUnpartitionedProducerDestination(String address,
            ExtendedProducerProperties<ArtemisProducerProperties> properties) {
        logger.debug("Provisioning unpartitioned producer destination with address '{}'", address);

        // Create address to send messages to
        artemisBrokerManager.createAddress(address, properties.getExtension());
        // Create queues for each group so that messages could be persisted until consumer register
        provisionGroups(address, properties.getRequiredGroups());
        return new ArtemisProducerDestination(address);
    }

    private ArtemisProducerDestination provisionPartitionedProducerDestination(String address,
            ExtendedProducerProperties<ArtemisProducerProperties> properties) {
        logger.debug("Provisioning partitioned producer destination with address '{}' and '{}' partitions", address,
                properties.getPartitionCount());

        IntStream.range(0, properties.getPartitionCount())
                .mapToObj(i -> getPartitionAddress(address, i))
                .peek(partitionAddress -> artemisBrokerManager.createAddress(partitionAddress,
                        properties.getExtension()))
                .forEach(partitionAddress -> provisionGroups(partitionAddress, properties.getRequiredGroups()));
        return new ArtemisProducerDestination(address);
    }

    private void provisionGroups(String address, String[] groups) {
        logger.debug("Provisioning required groups '{}' at address '{}'", groups, address);

        Arrays.stream(groups)
                .map(group -> getQueueName(address, group))
                .forEach(queueName -> artemisBrokerManager.createQueue(address, queueName));
    }

}
