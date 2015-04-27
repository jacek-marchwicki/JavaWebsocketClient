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

package com.example.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import javax.annotation.Nonnull;

public abstract class Message {
    @Nonnull
    public final MessageType type;

    public Message(@Nonnull MessageType type) {
        this.type = type;
    }

    @Nonnull
    public MessageType type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;

        Message message = (Message) o;

        return type == message.type;

    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return "Response{" +
                "type=" + type +
                '}';
    }

    public static class Deserializer implements JsonDeserializer<Message> {

        @Override
        public Message deserialize(JsonElement jsonElement,
                                    Type type,
                                    JsonDeserializationContext jsonDeserializationContext)
                throws JsonParseException {
            final JsonObject jsonObject = jsonElement.getAsJsonObject();
            final JsonElement typeElement = jsonObject.get("type");
            if (typeElement == null) {
                throw new JsonParseException("No \"type\" field in reference");
            }
            final MessageType messageType = jsonDeserializationContext.deserialize(typeElement, MessageType.class);

            if (MessageType.PONG.equals(messageType)) {
                return jsonDeserializationContext.deserialize(jsonObject, PongMessage.class);
            } else if (MessageType.ERROR.equals(messageType)) {
                return jsonDeserializationContext.deserialize(jsonObject, ErrorMessage.class);
            } else if (MessageType.REGISTERED.equals(messageType)) {
                return jsonDeserializationContext.deserialize(jsonObject, RegisteredMessage.class);
            } else if (MessageType.DATA.equals(messageType)) {
                return jsonDeserializationContext.deserialize(jsonObject, DataMessage.class);
            } else if (MessageType.CHAT.equals(messageType)) {
                return jsonDeserializationContext.deserialize(jsonObject, ChatMessage.class);
            } else {
                throw new JsonParseException("Unknown type " + messageType);
            }
        }

    }
}
