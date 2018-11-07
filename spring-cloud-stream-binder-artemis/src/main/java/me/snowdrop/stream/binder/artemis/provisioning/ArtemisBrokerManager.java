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

import me.snowdrop.stream.binder.artemis.properties.ArtemisCommonProperties;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientRequestor;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.api.core.management.AddressSettingsInfo;
import org.apache.activemq.artemis.api.core.management.ManagementHelper;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.provisioning.ProvisioningException;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisBrokerManager {

    private static final String DLQ_SUFFIX = "dlq";

    private static final String EXP_SUFFIX = "exp";

    private static final String ADD_SETTINGS_OPERATION = "addAddressSettings";

    private static final String GET_SETTINGS_OPERATION = "getAddressSettingsAsJSON";

    private final Logger logger = LoggerFactory.getLogger(ArtemisProvisioningProvider.class);

    private final ServerLocator serverLocator;

    private final String username;

    private final String password;

    public ArtemisBrokerManager(ServerLocator serverLocator, String username, String password) {
        this.serverLocator = serverLocator;
        this.username = username;
        this.password = password;
    }

    /**
     * Create Artemis broker address with the provided name and multicast routing type.
     * If address with provided name already exists method does nothing.
     *
     * @param name       Name of a new address.
     * @param properties Properties to be used to configure address if needed.
     * @throws ProvisioningException if address creation fails for some reason.
     */
    public void createAddress(String name, ArtemisCommonProperties properties) {
        logger.debug("Creating address '{}'", name);

        SimpleString nameString = SimpleString.toSimpleString(name);

        try (ClientSessionFactory sessionFactory = serverLocator.createSessionFactory();
             ClientSession session = getClientSession(sessionFactory)) {
            session.start();
            if (!session.addressQuery(nameString).isExists()) {
                session.createAddress(nameString, RoutingType.MULTICAST, true);
                if (properties.isModifyAddressSettings()) {
                    configureAddress(session, name, properties);
                }
            } else {
                logger.debug("Address '{}' already exists, ignoring", name);
            }
            session.stop();
        } catch (Exception e) {
            throw new ProvisioningException(String.format("Failed to create address '%s'", name), e);
        }
    }

    /**
     * Create Artemis broker queue with provided address, name and multicast routing type.
     * If queue with provided name already exists under provided address method does nothing.
     *
     * @param address Name of an address with which queue should be associated.
     * @param name    Name of a new queue.
     * @throws ProvisioningException if queue with provided name exists under another address or queue creation fails
     *                               for some other reason.
     */
    public void createQueue(String address, String name) {
        try (ClientSessionFactory sessionFactory = serverLocator.createSessionFactory();
             ClientSession session = getClientSession(sessionFactory)) {
            createQueueInternal(session, address, name);
        } catch (ProvisioningException e) {
            throw e;
        } catch (Exception e) {
            throw new ProvisioningException(
                    String.format("Failed to create queue '%s' with address '%s'", name, address), e);
        }
    }

    private void createQueueInternal(ClientSession session, String address, String name) throws ActiveMQException {
        logger.debug("Creating queue '{}' with address '{}", name, address);

        SimpleString addressString = SimpleString.toSimpleString(address);
        SimpleString nameString = SimpleString.toSimpleString(name);
        ClientSession.QueueQuery queueQuery = session.queueQuery(nameString);
        if (!queueQuery.isExists()) {
            session.createSharedQueue(addressString, RoutingType.MULTICAST, nameString, true);
        } else if (!addressString.equals(queueQuery.getAddress())) {
            logger.debug("Queue '{}' already exists under another address '{}', failing", name, queueQuery.getAddress());
            throw new ProvisioningException(String.format(
                    "Failed to create queue '%s' with address '%s'. Queue already exists under another address '%s'",
                    name, address, queueQuery.getAddress()));
        } else {
            logger.debug("Queue '{}' already exists, ignoring");
        }
    }

    private void configureAddress(ClientSession session, String address, ArtemisCommonProperties properties)
            throws Exception {
        updateAddressSettings(session, address, properties);

        if (properties.isAutoBindDeadLetterAddress()) {
            String dlqAddress = String.format("%s.%s", address, DLQ_SUFFIX);
            createQueueInternal(session, dlqAddress, dlqAddress);
        }

        if (properties.isAutoBindExpiryAddress()) {
            String expAddress = String.format("%s.%s", address, EXP_SUFFIX);
            createQueueInternal(session, expAddress, expAddress);
        }
    }

    private void updateAddressSettings(ClientSession session, String address, ArtemisCommonProperties properties)
            throws Exception {
        logger.debug("Updating address '{}' settings", address);

        try (ClientRequestor requestor = new ClientRequestor(session, properties.getManagementAddress())) {
            AddressSettingsInfo currentSettings = getCurrentAddressSettings(session, requestor, address);
            ClientMessage request = session.createMessage(false);
            Object[] updateParameters = new Object[]{
                    address,
                    properties.isAutoBindDeadLetterAddress()
                            ? String.format("%s.%s", address, DLQ_SUFFIX)
                            : currentSettings.getDeadLetterAddress(),
                    properties.isAutoBindExpiryAddress()
                            ? String.format("%s.%s", address, EXP_SUFFIX)
                            : currentSettings.getExpiryAddress(),
                    properties.getBrokerExpiryDelay(),
                    currentSettings.isLastValueQueue(),
                    properties.getBrokerMaxDeliveryAttempts(),
                    currentSettings.getMaxSizeBytes(),
                    currentSettings.getPageSizeBytes(),
                    currentSettings.getPageCacheMaxSize(),
                    properties.getBrokerRedeliveryDelay(),
                    properties.getBrokerRedeliveryDelayMultiplier(),
                    properties.getBrokerMaxRedeliveryDelay(),
                    currentSettings.getRedistributionDelay(),
                    properties.isBrokerSendToDlaOnNoRoute(),
                    currentSettings.getAddressFullMessagePolicy(),
                    currentSettings.getSlowConsumerThreshold(),
                    currentSettings.getSlowConsumerCheckPeriod(),
                    currentSettings.getSlowConsumerPolicy(),
                    currentSettings.isAutoCreateJmsQueues(),
                    currentSettings.isAutoDeleteJmsQueues(),
                    currentSettings.isAutoCreateJmsTopics(),
                    currentSettings.isAutoDeleteJmsTopics()
            };

            ManagementHelper.putOperationInvocation(request, ResourceNames.BROKER, ADD_SETTINGS_OPERATION,
                    updateParameters);
            requestor.request(request);
        }
    }

    private AddressSettingsInfo getCurrentAddressSettings(ClientSession session, ClientRequestor requestor,
            String address) throws Exception {
        ClientMessage request = session.createMessage(false);
        ManagementHelper.putOperationInvocation(request, ResourceNames.BROKER, GET_SETTINGS_OPERATION, address);
        ClientMessage reply = requestor.request(request);
        String result = (String) ManagementHelper.getResult(reply, String.class);

        return AddressSettingsInfo.from(result);
    }

    private ClientSession getClientSession(ClientSessionFactory clientSessionFactory) throws Exception {
        if (username == null) {
            return clientSessionFactory.createSession();
        }

        return clientSessionFactory.createSession(username, password, true, false, false,
                serverLocator.isPreAcknowledge(), serverLocator.getAckBatchSize());
    }

}
