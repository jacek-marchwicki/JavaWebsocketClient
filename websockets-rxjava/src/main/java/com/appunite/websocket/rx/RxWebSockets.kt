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

package com.appunite.websocket.rx

import com.appunite.websocket.rx.messages.RxEvent
import com.appunite.websocket.rx.messages.RxEventBinaryMessage
import com.appunite.websocket.rx.messages.RxEventConnected
import com.appunite.websocket.rx.messages.RxEventDisconnected
import com.appunite.websocket.rx.messages.RxEventStringMessage
import com.appunite.websocket.rx.`object`.messages.RxObjectEvent

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import io.reactivex.Observable
import io.reactivex.disposables.Disposables

/**
 * This class allows to retrieve messages from websocket
 */
class RxWebSockets
/**
 * Create instance of [Rx WebSockets]
 * @param client [OkHttpClient] instance
 * @param request request to connect to websocket
 */
(private val client: OkHttpClient, private val request: Request) {

    /**
     * Returns observable that connected to a websocket and returns [RxObjectEvent]'s
     *
     * @return Observable that connects to websocket
     */
    fun webSocketObservable(): Observable<RxEvent> {
        return Observable.create { emitter ->

            val webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket?, response: Response?) {
                    emitter.onNext(RxEventConnected(webSocket!!))
                }

                override fun onMessage(webSocket: WebSocket?, text: String?) {
                    emitter.onNext(RxEventStringMessage(webSocket!!, text!!))
                }

                override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
                    emitter.onNext(RxEventBinaryMessage(webSocket!!, bytes!!.toByteArray()))
                }

                override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
                    super.onClosing(webSocket, code, reason)
                    val exception = ServerRequestedCloseException(code, reason!!)
                    emitter.onNext(RxEventDisconnected(exception))
                    emitter.tryOnError(exception)
                }

                override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
                    val exception = ServerRequestedCloseException(code, reason!!)
                    emitter.onNext(RxEventDisconnected(exception))
                    emitter.tryOnError(exception)
                }

                override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
                    if (response != null) {
                        val exception = ServerHttpError(response)
                        emitter.onNext(RxEventDisconnected(exception))
                        emitter.tryOnError(exception)
                    } else {
                        emitter.onNext(RxEventDisconnected(t!!))
                        emitter.tryOnError(t)
                    }
                }
            })
            emitter.setDisposable(Disposables.fromAction {
                    webSocket.close(1000, "Just disconnect")
                    emitter.onComplete()
            })
        }
    }

}
