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

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.stream.binder.AbstractExtendedBindingProperties;
import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@ConfigurationProperties("spring.cloud.stream.artemis")
public class ArtemisExtendedBindingProperties extends
        AbstractExtendedBindingProperties<ArtemisConsumerProperties, ArtemisProducerProperties, ArtemisBindingProperties> {

    public static final String DEFAULT_PREFIX = "spring.cloud.stream.artemis.default";

    @Override
    public String getDefaultsPrefix() {
        return DEFAULT_PREFIX;
    }

    @Override
    public Class<? extends BinderSpecificPropertiesProvider> getExtendedPropertiesEntryClass() {
        return ArtemisBindingProperties.class;
    }

    @Override
    public Map<String, ArtemisBindingProperties> getBindings() {
        return this.doGetBindings();
    }
}
