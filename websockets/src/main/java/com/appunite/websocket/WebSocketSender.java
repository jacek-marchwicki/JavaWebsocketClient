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

package com.appunite.websocket;

import java.io.IOException;

import javax.annotation.Nonnull;

public interface WebSocketSender {
    /**
     * Send ping request (thread safe). Can be called after onConnect and
     * before onDisconnect by any thread. Thread will be blocked until send
     *
     * @param buffer
     *            buffer to send
     * @throws IOException
     *             when exception occur while sending
     * @throws InterruptedException
     *             when user call disconnect
     * @throws NotConnectedException
     *             when called before onConnect or after onDisconnect
     */
    void sendPingMessage(@Nonnull byte[] buffer) throws IOException,
            InterruptedException, NotConnectedException;

    /**
     * Send binary message (thread safe). Can be called after onConnect and
     * before onDisconnect by any thread. Thread will be blocked until send
     *
     * @param buffer
     *            buffer to send
     * @throws IOException
     *             when exception occur while sending
     * @throws InterruptedException
     *             when user call disconnect
     * @throws NotConnectedException
     *             when called before onConnect or after onDisconnect
     */
    void sendByteMessage(@Nonnull byte[] buffer) throws IOException,
            InterruptedException, NotConnectedException;

    /**
     * Send text message (thread safe). Can be called after onConnect and before
     * onDisconnect by any thread. Thread will be blocked until send
     *
     * @param message
     *            message to send
     * @throws IOException
     *             when exception occur while sending
     * @throws InterruptedException
     *             when user call disconnect
     * @throws NotConnectedException
     *             when called before onConnect or after onDisconnect
     */
    void sendStringMessage(@Nonnull String message) throws IOException,
            InterruptedException, NotConnectedException;
}
