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

class PingMessage(private val message: String) : Message(MessageType.PING) {

    fun message(): String {
        return message
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is PingMessage) return false
        if (!super.equals(o)) return false

        val that = o as PingMessage?

        return message == that!!.message

    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + message.hashCode()
        return result
    }

    override fun toString(): String {
        return "PingMessage{" +
                "message='" + message + '\''.toString() +
                "} " + super.toString()
    }
}
