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

package me.snowdrop.stream.binder.artemis.integration.e2e.common;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public final class AwaitUtils {

    public static void awaitForReceivedMessages(AbstractReceiver receiver, int expectedMessagesCount) {
        await().atMost(5, SECONDS)
                .until(() -> receiver.getReceivedMessages()
                        .size() == expectedMessagesCount);
    }

    public static void awaitForHandledMessages(AbstractReceiver receiver, int expectedMessagesCount) {
        await().atMost(5, SECONDS)
                .until(() -> receiver.getHandledMessages()
                        .size() == expectedMessagesCount);
    }

}
