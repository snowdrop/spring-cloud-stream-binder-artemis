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

package me.snowdrop.stream.binder.artemis.application;

import org.springframework.messaging.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public abstract class AbstractReceiver {

    public static final String EXCEPTION_REQUEST = "Exception request";

    public static final String REQUESTED_EXCEPTION = "Receiver exception";

    private final List<Message> handledMessages = new ArrayList<>();

    private final List<Message> receivedMessages = new ArrayList<>();

    private CountDownLatch latch;

    public void receive(Message message) {
        receivedMessages.add(message);

        if (EXCEPTION_REQUEST.equals(message.getPayload())) {
            throw new RuntimeException(REQUESTED_EXCEPTION);
        }

        handledMessages.add(message);
        if (latch != null) {
            latch.countDown();
        }
    }

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    public List<Message> getHandledMessages() {
        return handledMessages;
    }

    public List<Message> getReceivedMessages() {
        return receivedMessages;
    }

    public void clear() {
        handledMessages.clear();
        receivedMessages.clear();
    }

}
