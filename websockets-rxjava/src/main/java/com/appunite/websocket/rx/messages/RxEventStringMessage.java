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
 * Event indicating that server sent a string message
 */
public class RxEventStringMessage extends RxEventConn {

    @Nonnull
    private final String message;

    public RxEventStringMessage(@Nonnull WebSocket sender, @Nonnull String message) {
        super(sender);
        this.message = message;
    }

    /**
     * String message that was returned by server
     * @return string message
     */
    @Nonnull
    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return "StringMessageRxEvent{" +
                "message='" + message + '\'' +
                '}';
    }
}
