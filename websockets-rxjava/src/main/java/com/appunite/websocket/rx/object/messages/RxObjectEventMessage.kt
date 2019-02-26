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

package com.appunite.websocket.rx.`object`.messages

import com.appunite.websocket.rx.messages.RxEventStringMessage
import com.appunite.websocket.rx.`object`.ObjectParseException
import com.appunite.websocket.rx.`object`.ObjectWebSocketSender

import io.reactivex.Observable

/**
 * Event indicating that data returned by server was parsed
 *
 * If [ObjectParseException] occur than [RxObjectEventWrongMessageFormat] event
 * will be served
 *
 * @see RxEventStringMessage
 */
class RxObjectEventMessage(sender: ObjectWebSocketSender, private val message: Any) : RxObjectEventConn(sender) {

    /**
     * Served parse message
     * @param <T> Class type of message
     * @return a message that was returned
     *
     * @throws ClassCastException when wrong try to get wrong type of message
    </T> */
    @Throws(ClassCastException::class)
    fun <T> message(): T {
        return message as T
    }

    override fun toString(): String {
        return "RxJsonEventMessage{" +
                "message='" + message + '\''.toString() +
                '}'.toString()
    }

}
/**
 * Transform one observable to observable of given type filtering by a type
 *
 * @param clazz type of message that you would like get
 * @param <T> type of message that you would like get
 * @return Observable that returns given type of message
</T> */
fun <T> Observable<RxObjectEventMessage>.filterAndMap2(clazz: Class<T>): Observable<T> {
    return this
            .filter { o ->
                o != null && clazz.isInstance(o.message())
            }
            .map {
                it.message<T>()
            }
}
