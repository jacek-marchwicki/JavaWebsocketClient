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
import com.appunite.websocket.rx.messages.RxEventStringMessage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.ws.WebSocket;
import okhttp3.ws.WebSocketCall;
import okhttp3.ws.WebSocketListener;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import okio.Buffer;
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
                                notifyConnected = null;
                                try {
                                    webSocket.close(0, "Just disconnect");
                                } catch (IOException e) {
                                    subscriber.onNext(new RxEventDisconnected(e));
                                }
                            } else {
                                notifyConnected = webSocket;
                            }
                            webSocketItem = notifyConnected;
                        }
                        if (notifyConnected != null) {
                            subscriber.onNext(new RxEventConnected(notifyConnected));
                        }
                    }

                    @Nullable
                    WebSocket webSocketOrNull() {
                        synchronized (lock) {
                            return webSocketItem;
                        }
                    }

                    @Override
                    public void onFailure(IOException e, Response response) {
                        returnException(e);
                    }

                    private void returnException(IOException e) {
                        subscriber.onNext(new RxEventDisconnected(e));
                        subscriber.onError(e);
                        synchronized (lock) {
                            webSocketItem = null;
                            requestClose = false;
                        }
                    }

                    @Override
                    public void onMessage(ResponseBody message) throws IOException {
                        try {
                            final WebSocket sender = webSocketOrNull();
                            if (sender == null) {
                                return;
                            }
                            if (WebSocket.BINARY.equals(message.contentType())) {
                                subscriber.onNext(new RxEventBinaryMessage(sender, message.bytes()));
                            } else if (WebSocket.TEXT.equals(message.contentType())) {
                                subscriber.onNext(new RxEventStringMessage(sender, message.string()));
                            }
                        } finally {
                            message.close();
                        }
                    }

                    @Override
                    public void onPong(Buffer payload) {final WebSocket sender = webSocketOrNull();
                        if (sender == null) {
                            return;
                        }
                        subscriber.onNext(new RxEventPong(sender, payload.readByteArray()));

                    }

                    @Override
                    public void onClose(int code, String reason) {
                        returnException(new ServerRequestedCloseException(code, reason));
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
                                    subscriber.onError(e);
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
