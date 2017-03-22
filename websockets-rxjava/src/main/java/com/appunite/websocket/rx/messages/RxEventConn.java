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

package com.appunite.websocket.rx.messages;

import okhttp3.WebSocket;

import javax.annotation.Nonnull;

/**
 * Abstract class for {@link RxEvent} that allows sending messages to the server using
 * {@link #sender()} method
 */
public abstract class RxEventConn extends RxEvent {
    @Nonnull
    private final WebSocket sender;

    public RxEventConn(@Nonnull WebSocket sender) {
        this.sender = sender;
    }

    /**
     * Get sender
     *
     * Sender is valid until disconnection from server ({@link RxEventDisconnected} event)
     *
     * @return instance of {@link WebSocket} that allows you to send back messages to server
     */
    @Nonnull
    public WebSocket sender() {
        return sender;
    }

}
