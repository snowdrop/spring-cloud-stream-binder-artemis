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

package me.snowdrop.stream.binder.artemis.utils;

import org.springframework.messaging.Message;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public final class Assertions {

    public static void assertPayload(List<Message> actualMessages, String... expectedPayloads) {
        List<String> actualPayloads = actualMessages.stream()
                .map(Message::getPayload)
                .map(Object::toString)
                .collect(Collectors.toList());

        assertThat(actualPayloads).containsExactlyInAnyOrder(expectedPayloads);
    }

    public static void assertHeaders(List<Message> actualMessages, Map<String, Object> headers) {
        for (Message handledMessage : actualMessages) {
            assertThat(handledMessage.getHeaders()).containsAllEntriesOf(headers);
        }
    }

}
