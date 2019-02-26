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

package com.example.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException

import java.lang.reflect.Type

abstract class Message(val type: MessageType) {


    fun type(): MessageType {
        return type
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is Message) return false

        val message = o as Message?

        return type == message!!.type

    }

    override fun hashCode(): Int {
        return type.hashCode()
    }

    override fun toString(): String {
        return "Response{" +
                "type=" + type +
                '}'.toString()
    }

    class Deserializer : JsonDeserializer<Message> {

        @Throws(JsonParseException::class)
        override fun deserialize(jsonElement: JsonElement,
                                 type: Type,
                                 jsonDeserializationContext: JsonDeserializationContext): Message {
            val jsonObject = jsonElement.asJsonObject
            val typeElement = jsonObject.get("type")
                    ?: throw JsonParseException("No \"type\" field in reference")
            val messageType = jsonDeserializationContext.deserialize<MessageType>(typeElement, MessageType::class.java)

            return if (MessageType.PONG == messageType) {
                jsonDeserializationContext.deserialize(jsonObject, PongMessage::class.java)
            } else if (MessageType.ERROR == messageType) {
                jsonDeserializationContext.deserialize(jsonObject, ErrorMessage::class.java)
            } else if (MessageType.REGISTERED == messageType) {
                jsonDeserializationContext.deserialize(jsonObject, RegisteredMessage::class.java)
            } else if (MessageType.DATA == messageType) {
                jsonDeserializationContext.deserialize(jsonObject, DataMessage::class.java)
            } else if (MessageType.CHAT == messageType) {
                jsonDeserializationContext.deserialize(jsonObject, ChatMessage::class.java)
            } else {
                throw JsonParseException("Unknown type $messageType")
            }
        }

    }
}
