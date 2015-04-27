/*
 * Copyright (C) 2015 Jacek Marchwicki <jacek.marchwicki@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.appunite.websocket.internal;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;

import javax.annotation.Nonnull;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import static com.appunite.websocket.tools.Preconditions.checkNotNull;

/**
 * Implementation of {@link SocketProvider}
 */
public class SocketProviderImpl implements SocketProvider {

    @Nonnull
    @Override
    public Socket getSocket(@Nonnull URI uri) throws IOException {
        if (isSsl(uri)) {
            return SSLSocketFactory.getDefault().createSocket();
        } else {
            return SocketFactory.getDefault().createSocket();
        }
    }

    /**
     * Return if given uri is ssl encrypted (thread safe)
     *
     * @param uri uri to check
     * @return true if uri is wss
     * @throws IllegalArgumentException
     *             if unkonwo schema
     */
    private static boolean isSsl(@Nonnull URI uri) {
        checkNotNull(uri);
        final String scheme = uri.getScheme();
        if ("wss".equals(scheme)) {
            return true;
        } else if ("ws".equals(scheme)) {
            return false;
        } else {
            throw new IllegalArgumentException("Unknown schema");
        }
    }
}
