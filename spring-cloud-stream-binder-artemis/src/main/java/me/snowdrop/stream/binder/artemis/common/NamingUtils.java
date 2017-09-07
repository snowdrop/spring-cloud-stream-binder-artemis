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

package me.snowdrop.stream.binder.artemis.common;

import org.springframework.util.Base64Utils;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public final class NamingUtils {

    public static String getPartitionAddress(String address, int partition) {
        Objects.requireNonNull(address);

        return String.format("%s-%d", address, partition);
    }

    public static String getQueueName(String address, String group) {
        Objects.requireNonNull(address);
        Objects.requireNonNull(group);

        return String.format("%s-%s", address, group);
    }

    public static String getAnonymousQueueName(String address) {
        Objects.requireNonNull(address);

        UUID uuid = UUID.randomUUID();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits());
        String suffix = Base64Utils.encodeToUrlSafeString(buffer.array())
                .replaceAll("=", "")
                .replaceAll("-", "\\$");

        return String.format("%s-%s", address, suffix);
    }

}
