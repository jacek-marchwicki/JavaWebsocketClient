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

package com.appunite.websocket.rx.`object`

import com.google.gson.Gson
import com.google.gson.JsonParseException

import java.lang.reflect.Type

class GsonObjectSerializer(private val gson: Gson, private val typeOfT: Type) : ObjectSerializer {

    @Throws(ObjectParseException::class)
    override fun serialize(message: String): Any {
        try {
            return gson.fromJson(message, typeOfT)
        } catch (e: JsonParseException) {
            throw ObjectParseException("Could not parse", e)
        }

    }

    @Throws(ObjectParseException::class)
    override fun serialize(message: ByteArray): Any {
        throw ObjectParseException("Could not parse binary messages")
    }

    @Throws(ObjectParseException::class)
    override fun deserializeBinary(message: Any): ByteArray {
        throw IllegalStateException("Only serialization to string is available")
    }

    @Throws(ObjectParseException::class)
    override fun deserializeString(message: Any): String {
        try {
            return gson.toJson(message)
        } catch (e: JsonParseException) {
            throw ObjectParseException("Could not parse", e)
        }

    }

    override fun isBinary(message: Any): Boolean {
        return false
    }
}
