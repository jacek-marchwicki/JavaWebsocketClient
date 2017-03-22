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

package com.appunite.websocket.rx;

import com.appunite.websocket.rx.object.ObjectSerializer;
import com.appunite.websocket.rx.object.ObjectWebSocketSender;
import com.appunite.websocket.rx.object.RxObjectWebSockets;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import okhttp3.WebSocket;
import rx.Single;

public class RxMoreObservables {

    public static final Logger logger = Logger.getLogger("RxWebSockets");

    public RxMoreObservables() {
    }

    /**
     * Enqueue message to send
     *
     * @param sender connection event that is used to send message
     * @param message message to send
     * @return Single that returns true if message was enqueued
     * @see #sendObjectMessage(ObjectWebSocketSender, Object)
     */
    @Nonnull
    public static Single<Boolean> sendMessage(final @Nonnull WebSocket sender, final @Nonnull String message) {
        return Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                logger.log(Level.FINE, "sendStringMessage: {0}", message);
                return sender.send(message);
            }
        });
    }

    /**
     * Send object
     * <p>
     * Object is parsed via {@link ObjectSerializer} given by
     * {@link RxObjectWebSockets#RxObjectWebSockets(RxWebSockets, ObjectSerializer)}
     *
     * @param sender connection event that is used to send message
     * @param message message to serialize and sent
     * @return Single that returns true if message was enqueued or ObjectParseException if couldn't
     * serialize
     * @see #sendMessage(WebSocket, String)
     */
    @Nonnull
    public static Single<Boolean> sendObjectMessage(final @Nonnull ObjectWebSocketSender sender, final @Nonnull Object message) {
        return Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                logger.log(Level.FINE, "sendStringMessage: {0}", message);
                return sender.sendObjectMessage(message);
            }
        });
    }


}
