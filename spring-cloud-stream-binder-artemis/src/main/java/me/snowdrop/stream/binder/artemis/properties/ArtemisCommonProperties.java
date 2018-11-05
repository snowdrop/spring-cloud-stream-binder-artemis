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

package me.snowdrop.stream.binder.artemis.properties;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisCommonProperties {

    private String managementAddress = "activemq.management";

    private boolean modifyAddressSettings = false;

    private boolean autoBindDeadLetterAddress = false;

    private boolean autoBindExpiryAddress = false;

    private long brokerExpiryDelay = -1;

    private long brokerRedeliveryDelay = 0;

    private Long brokerMaxRedeliveryDelay;

    private double brokerRedeliveryDelayMultiplier = 1.0;

    private int brokerMaxDeliveryAttempts = 10;

    private boolean brokerSendToDlaOnNoRoute = false;

    public String getManagementAddress() {
        return managementAddress;
    }

    public void setManagementAddress(String managementAddress) {
        this.managementAddress = managementAddress;
    }

    public boolean isModifyAddressSettings() {
        return modifyAddressSettings;
    }

    public void setModifyAddressSettings(boolean modifyAddressSettings) {
        this.modifyAddressSettings = modifyAddressSettings;
    }

    public boolean isAutoBindDeadLetterAddress() {
        return autoBindDeadLetterAddress;
    }

    public void setAutoBindDeadLetterAddress(boolean autoBindDeadLetterAddress) {
        this.autoBindDeadLetterAddress = autoBindDeadLetterAddress;
    }

    public boolean isAutoBindExpiryAddress() {
        return autoBindExpiryAddress;
    }

    public void setAutoBindExpiryAddress(boolean autoBindExpiryAddress) {
        this.autoBindExpiryAddress = autoBindExpiryAddress;
    }

    public long getBrokerExpiryDelay() {
        return brokerExpiryDelay;
    }

    public void setBrokerExpiryDelay(long brokerExpiryDelay) {
        this.brokerExpiryDelay = brokerExpiryDelay;
    }

    public long getBrokerRedeliveryDelay() {
        return brokerRedeliveryDelay;
    }

    public void setBrokerRedeliveryDelay(long brokerRedeliveryDelay) {
        this.brokerRedeliveryDelay = brokerRedeliveryDelay;
    }

    public long getBrokerMaxRedeliveryDelay() {
        if (brokerMaxRedeliveryDelay == null) {
            return getBrokerRedeliveryDelay() * 10;
        }
        return brokerMaxRedeliveryDelay;
    }

    public void setBrokerMaxRedeliveryDelay(long brokerMaxRedeliveryDelay) {
        this.brokerMaxRedeliveryDelay = brokerMaxRedeliveryDelay;
    }

    public double getBrokerRedeliveryDelayMultiplier() {
        return brokerRedeliveryDelayMultiplier;
    }

    public void setBrokerRedeliveryDelayMultiplier(double brokerRedeliveryDelayMultiplier) {
        this.brokerRedeliveryDelayMultiplier = brokerRedeliveryDelayMultiplier;
    }

    public int getBrokerMaxDeliveryAttempts() {
        return brokerMaxDeliveryAttempts;
    }

    public void setBrokerMaxDeliveryAttempts(int brokerMaxDeliveryAttempts) {
        this.brokerMaxDeliveryAttempts = brokerMaxDeliveryAttempts;
    }

    public boolean isBrokerSendToDlaOnNoRoute() {
        return brokerSendToDlaOnNoRoute;
    }

    public void setBrokerSendToDlaOnNoRoute(boolean brokerSendToDlaOnNoRoute) {
        this.brokerSendToDlaOnNoRoute = brokerSendToDlaOnNoRoute;
    }

}
