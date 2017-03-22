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
import com.appunite.websocket.rx.messages.RxEventDisconnected;

import java.io.IOException;

import javax.annotation.Nonnull;

/**
 * Event indicate that client was disconnected to the server
 *
 * since then all execution on previosly returned {@link ObjectWebSocketSender} will cause throwing
 * {@link java.io.IOException}
 *
 * See: {@link RxEventDisconnected}
 */
public class RxObjectEventDisconnected extends RxObjectEvent {
    @Nonnull
    private final Throwable exception;

    public RxObjectEventDisconnected(@Nonnull Throwable exception) {
        super();
        this.exception = exception;
    }

    /**
     * See: {@link RxEventDisconnected#exception()}
     *
     * @return exception that caused disconnection
     */
    @Nonnull
    public Throwable exception() {
        return exception;
    }

    @Override
    public String toString() {
        return "RxJsonEventDisconnected{" + "exception=" + exception + '}';
    }
}
