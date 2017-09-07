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

package me.snowdrop.stream.binder.artemis.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.stream.binder.ExtendedBindingProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@ConfigurationProperties("spring.cloud.stream.artemis")
public class ArtemisExtendedBindingProperties
        implements ExtendedBindingProperties<ArtemisConsumerProperties, ArtemisProducerProperties> {

    private Map<String, ArtemisBindingProperties> bindings = new HashMap<>();

    public Map<String, ArtemisBindingProperties> getBindings() {
        return bindings;
    }

    public void setBindings(Map<String, ArtemisBindingProperties> bindings) {
        this.bindings = bindings;
    }

    @Override
    public ArtemisConsumerProperties getExtendedConsumerProperties(String channelName) {
        if (bindings.containsKey(channelName) && bindings.get(channelName)
                .getConsumer() != null) {
            return bindings.get(channelName)
                    .getConsumer();
        } else {
            return new ArtemisConsumerProperties();
        }
    }

    @Override
    public ArtemisProducerProperties getExtendedProducerProperties(String channelName) {
        if (bindings.containsKey(channelName) && bindings.get(channelName)
                .getProducer() != null) {
            return bindings.get(channelName)
                    .getProducer();
        } else {
            return new ArtemisProducerProperties();
        }
    }
}
