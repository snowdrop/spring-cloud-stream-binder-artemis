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

package me.snowdrop.stream.binder.artemis.listener;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.support.RetryTemplate;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class RetryableChannelPublishingJmsMessageListener extends ChannelPublishingJmsMessageListener {

    private final RetryTemplate retryTemplate;

    private final RecoveryCallback<?> recoveryCallback;

    public RetryableChannelPublishingJmsMessageListener(RetryTemplate retryTemplate,
            RecoveryCallback<?> recoveryCallback) {
        this.retryTemplate = retryTemplate;
        this.recoveryCallback = recoveryCallback;
    }

    @Override
    public void onMessage(Message message, Session session) throws JMSException {
        retryTemplate.execute(c -> {
            if (message instanceof BytesMessage) {
                // Make sure byte message is readable in every iteration.
                ((BytesMessage) message).reset();
            }
            super.onMessage(message, session);
            return null;
        }, recoveryCallback);
    }

}
