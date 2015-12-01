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

import com.appunite.websocket.rx.object.ObjectParseException;
import com.appunite.websocket.rx.object.ObjectSerializer;
import com.appunite.websocket.rx.object.ObjectWebSocketSender;

import java.util.Arrays;

import javax.annotation.Nonnull;

/**
 * Event indicating that binary data returned by server was not correctly parsed
 *
 * This means {@link ObjectParseException} was returned via
 * {@link ObjectSerializer#deserializeBinary(Object)}
 */
public class RxObjectEventWrongBinaryMessageFormat extends RxObjectEventWrongMessageFormat {
    @Nonnull
    private final byte[] message;

    public RxObjectEventWrongBinaryMessageFormat(@Nonnull ObjectWebSocketSender sender,
                                                 @Nonnull byte[] message,
                                                 @Nonnull ObjectParseException exception) {
        super(sender, exception);
        this.message = message;
    }

    @Nonnull
    public byte[] message() {
        return message;
    }

    @Override
    public String toString() {
        return "RxJsonEventWrongBinaryMessageFormat{" +
                "message='" + Arrays.toString(message) + '\'' +
                '}';
    }
}
