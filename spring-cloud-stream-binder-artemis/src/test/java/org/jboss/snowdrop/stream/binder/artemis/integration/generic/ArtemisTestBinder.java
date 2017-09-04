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

package org.jboss.snowdrop.stream.binder.artemis.integration.generic;

import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.jboss.snowdrop.stream.binder.artemis.ArtemisMessageChannelBinder;
import org.jboss.snowdrop.stream.binder.artemis.properties.ArtemisConsumerProperties;
import org.jboss.snowdrop.stream.binder.artemis.properties.ArtemisProducerProperties;
import org.springframework.cloud.stream.binder.AbstractTestBinder;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisTestBinder extends
        AbstractTestBinder<ArtemisMessageChannelBinder, ExtendedConsumerProperties<ArtemisConsumerProperties>,
                ExtendedProducerProperties<ArtemisProducerProperties>> {

    private final List<String> queues = new ArrayList<>();

    private final ServerLocator serverLocator;

    public ArtemisTestBinder(ArtemisMessageChannelBinder binder, ServerLocator serverLocator) {
        this.serverLocator = serverLocator;
        this.setBinder(binder);
    }

//    @Override
//    public Binding<MessageChannel> bindConsumer(String name, String group, MessageChannel moduleInputChannel,
//            ExtendedConsumerProperties<ArtemisConsumerProperties> properties) {
//        queues.add(String.format("%s::%s", name, group));
//        return getBinder().bindConsumer(name, group, moduleInputChannel, properties);
//    }

//    @Override
//    public Binding<MessageChannel> bindProducer(String name, MessageChannel moduleOutputChannel,
//            ProducerProperties properties) {
//        queues.add(String.format("%s::%s", name, group));
//        return binder.bindProducer(name, moduleOutputChannel, properties);
//    }

    @Override
    public void cleanup() {
//        try (ClientSessionFactory sessionFactory = serverLocator.createSessionFactory();
//             ClientSession session = sessionFactory.createSession()) {
//
//            for (String queue : queues) {
//                session.deleteQueue(queue);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
