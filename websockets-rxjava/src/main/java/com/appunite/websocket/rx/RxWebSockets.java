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

package com.appunite.websocket.rx;

import com.appunite.websocket.rx.object.messages.RxObjectEvent;
import com.appunite.websocket.rx.messages.RxEvent;
import com.appunite.websocket.rx.messages.RxEventBinaryMessage;
import com.appunite.websocket.rx.messages.RxEventConnected;
import com.appunite.websocket.rx.messages.RxEventDisconnected;
import com.appunite.websocket.rx.messages.RxEventPong;
import com.appunite.websocket.rx.messages.RxEventServerRequestedClose;
import com.appunite.websocket.rx.messages.RxEventStringMessage;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ws.WebSocket;
import com.squareup.okhttp.ws.WebSocketCall;
import com.squareup.okhttp.ws.WebSocketListener;

import java.io.IOException;

import javax.annotation.Nonnull;

import okio.Buffer;
import okio.BufferedSource;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

/**
 * This class allows to retrieve messages from websocket
 */
public class RxWebSockets {

    @Nonnull
    private final OkHttpClient client;
    @Nonnull
    private final Request request;

    /**
     * Create instance of {@link RxWebSockets}
     * @param client {@link OkHttpClient} instance
     * @param request request to connect to websocket
     */
    public RxWebSockets(@Nonnull OkHttpClient client, @Nonnull Request request) {
        this.client = client;
        this.request = request;
    }


    /**
     * Returns observable that connected to a websocket and returns {@link RxObjectEvent}'s
     *
     * @return Observable that connects to websocket
     */
    @Nonnull
    public Observable<RxEvent> webSocketObservable() {
        return Observable.create(new Observable.OnSubscribe<RxEvent>() {

            private final Object lock = new Object();
            private WebSocket webSocketItem;
            private boolean requestClose;

            @Override
            public void call(final Subscriber<? super RxEvent> subscriber) {
                final WebSocketListener listener = new WebSocketListener() {
                    @Override
                    public void onOpen(WebSocket webSocket, Response response) {
                        final WebSocket notifyConnected;
                        synchronized (lock) {
                            if (requestClose) {
                                try {
                                    webSocketItem.close(0, "Just disconnect");
                                } catch (IOException e) {
                                    subscriber.onNext(new RxEventDisconnected(e));
                                }
                                webSocketItem = null;
                            } else {
                                webSocketItem = webSocket;
                            }
                            notifyConnected = webSocketItem;
                        }
                        if (notifyConnected != null) {
                            subscriber.onNext(new RxEventConnected(notifyConnected));
                        }
                    }

                    @Nonnull
                    WebSocket webSocketOrThrow() {
                        synchronized (lock) {
                            if (webSocketItem == null) {
                                throw new IllegalArgumentException("Web socket should not be null");
                            }
                            return webSocketItem;
                        }
                    }

                    @Override
                    public void onFailure(IOException e, Response response) {
                        subscriber.onNext(new RxEventDisconnected(e));
                    }

                    @Override
                    public void onMessage(BufferedSource payload, WebSocket.PayloadType type) throws IOException {
                        try {
                            if (WebSocket.PayloadType.BINARY.equals(type)) {
                                subscriber.onNext(new RxEventBinaryMessage(webSocketOrThrow(), payload.readByteArray()));
                            } else if (WebSocket.PayloadType.TEXT.equals(type)) {
                                subscriber.onNext(new RxEventStringMessage(webSocketOrThrow(), payload.readUtf8()));
                            }
                        } finally {
                            payload.close();
                        }
                    }

                    @Override
                    public void onPong(Buffer payload) {
                        subscriber.onNext(new RxEventPong(webSocketOrThrow(), payload.readByteArray()));

                    }

                    @Override
                    public void onClose(int code, String reason) {
                        subscriber.onNext(new RxEventServerRequestedClose(webSocketOrThrow(), code, reason));
                        synchronized (lock) {
                            webSocketItem = null;
                        }
                    }

                };
                final WebSocketCall webSocketCall = WebSocketCall.create(client, request);
                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        synchronized (lock) {
                            if (webSocketItem != null) {
                                try {
                                    webSocketItem.close(0, "Just disconnect");
                                } catch (IOException e) {
                                    subscriber.onNext(new RxEventDisconnected(e));
                                }
                                webSocketItem = null;
                            } else {
                                requestClose = true;
                            }

                        }
                        webSocketCall.cancel();
                    }
                }));
                webSocketCall.enqueue(listener);
            }
        });
    }
}
