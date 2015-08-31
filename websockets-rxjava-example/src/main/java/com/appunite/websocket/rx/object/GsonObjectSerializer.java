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

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import javax.annotation.Nonnull;

public class GsonObjectSerializer implements ObjectSerializer {

    @Nonnull
    private final Gson gson;
    @Nonnull
    private final Type typeOfT;

    public GsonObjectSerializer(@Nonnull Gson gson, @Nonnull Type typeOfT) {
        this.gson = gson;
        this.typeOfT = typeOfT;
    }

    @Nonnull
    @Override
    public Object serialize(@Nonnull String message) throws ObjectParseException {
        try {
            return gson.fromJson(message, typeOfT);
        } catch (JsonParseException e) {
            throw new ObjectParseException("Could not parse", e);
        }
    }

    @Nonnull
    @Override
    public Object serialize(@Nonnull byte[] message) throws ObjectParseException {
        throw new ObjectParseException("Could not parse binary messages");
    }

    @Nonnull
    @Override
    public byte[] deserializeBinary(@Nonnull Object message) throws ObjectParseException {
        throw new IllegalStateException("Only serialization to string is available");
    }

    @Nonnull
    @Override
    public String deserializeString(@Nonnull Object message) throws ObjectParseException {
        try {
            return gson.toJson(message);
        } catch (JsonParseException e) {
            throw new ObjectParseException("Could not parse", e);
        }
    }

    @Override
    public boolean isBinary(@Nonnull Object message) {
        return false;
    }
}
