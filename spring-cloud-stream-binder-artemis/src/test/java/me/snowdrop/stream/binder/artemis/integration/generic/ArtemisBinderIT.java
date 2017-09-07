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

package me.snowdrop.stream.binder.artemis.integration.generic;

import me.snowdrop.stream.binder.artemis.ArtemisMessageChannelBinder;
import me.snowdrop.stream.binder.artemis.properties.ArtemisConsumerProperties;
import me.snowdrop.stream.binder.artemis.properties.ArtemisProducerProperties;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.PartitionCapableBinderTests;
import org.springframework.cloud.stream.binder.Spy;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ArtemisBinderIT extends
        PartitionCapableBinderTests<ArtemisTestBinder, ExtendedConsumerProperties<ArtemisConsumerProperties>,
                ExtendedProducerProperties<ArtemisProducerProperties>> {

    @Autowired
    private ArtemisMessageChannelBinder binder;

    @Autowired
    private ServerLocator serverLocator;

    @Override
    public Spy spyOn(String name) {
        return null;
    }

    @Override
    protected ArtemisTestBinder getBinder() throws Exception {
        if (testBinder == null) {
            testBinder = new ArtemisTestBinder(binder, serverLocator);
        }

        return testBinder;
    }

    @Override
    protected ExtendedConsumerProperties<ArtemisConsumerProperties> createConsumerProperties() {
        return new ExtendedConsumerProperties<>(new ArtemisConsumerProperties());
    }

    @Override
    protected ExtendedProducerProperties<ArtemisProducerProperties> createProducerProperties() {
        ExtendedProducerProperties<ArtemisProducerProperties> properties =
                new ExtendedProducerProperties<>(new ArtemisProducerProperties());
        return properties;
    }

    @Override
    protected boolean usesExplicitRouting() {
        return false;
    }

    @Override
    protected String getClassUnderTestName() {
        return ArtemisMessageChannelBinder.class.getSimpleName();
    }
}
