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

package com.appunite.websocket.rx.object;

import javax.annotation.Nonnull;

/**
 * Serialize and deserialize objects from web socket
 */
public interface ObjectSerializer {

    /**
     * Serialize string message
     * @param message string message from socket
     * @return serialized object
     * @throws ObjectParseException if serialization fail
     */
    @Nonnull Object serialize(@Nonnull String message) throws ObjectParseException;

    /**
     * Serialize binary message
     * @param message binary message from socket
     * @return serialized object
     * @throws ObjectParseException if serialization fail
     */
    @Nonnull Object serialize(@Nonnull byte[] message) throws ObjectParseException;

    /**
     * Deserialize to binary message (is called only if {@link #isBinary(Object)} return true)
     * @param message object to deserialize
     * @return de-serialized object
     * @throws ObjectParseException if serialization fail
     */
    @Nonnull byte[] deserializeBinary(@Nonnull Object message) throws ObjectParseException;

    /**
     * Deserialize to string message (is called only if {@link #isBinary(Object)} return false)
     * @param message object to deserialize
     * @return de-serialized object
     * @throws ObjectParseException if serialization fail
     */
    @Nonnull String deserializeString(@Nonnull Object message) throws ObjectParseException;

    /**
     * Determine if message should be serialized to binary
     * @param message object to deserialize
     * @return true if should use binary serialize {@link #deserializeBinary(Object)}, false if
     * should use string serialize {@link #deserializeString(Object)}
     */
    boolean isBinary(@Nonnull Object message);
}
