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

package org.jboss.snowdrop.stream.binder.artemis.properties;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ArtemisBindingProperties {

    private ArtemisConsumerProperties consumer = new ArtemisConsumerProperties();

    private ArtemisProducerProperties producer = new ArtemisProducerProperties();

    public ArtemisConsumerProperties getConsumer() {
        return consumer;
    }

    public void setConsumer(ArtemisConsumerProperties consumer) {
        this.consumer = consumer;
    }

    public ArtemisProducerProperties getProducer() {
        return producer;
    }

    public void setProducer(ArtemisProducerProperties producer) {
        this.producer = producer;
    }

}
