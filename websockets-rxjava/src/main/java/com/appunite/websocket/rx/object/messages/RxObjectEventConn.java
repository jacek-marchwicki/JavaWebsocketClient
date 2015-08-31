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

package com.appunite.websocket.rx.object.messages;

import com.appunite.websocket.rx.object.ObjectWebSocketSender;
import com.appunite.websocket.rx.messages.RxEventConn;

import javax.annotation.Nonnull;

/**
 * Abstract class for {@link RxObjectEvent} that allows sending messages to the server using
 * {@link #sender()} method
 *
 * @see RxEventConn
 */
public abstract class RxObjectEventConn extends RxObjectEvent {
    @Nonnull
    private final ObjectWebSocketSender sender;

    public RxObjectEventConn(@Nonnull ObjectWebSocketSender sender) {
        this.sender = sender;
    }

    /**
     * Get sender
     *
     * Sender is valid until disconnection from server ({@link RxObjectEventDisconnected} event)
     *
     * @return instance of {@link ObjectWebSocketSender} that allows you to send back messages to server
     * @see RxEventConn#sender()
     */
    @Nonnull
    public ObjectWebSocketSender sender() {
        return sender;
    }
}
