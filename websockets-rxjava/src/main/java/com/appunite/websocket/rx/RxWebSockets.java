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

import com.appunite.websocket.rx.messages.RxEvent;
import com.appunite.websocket.rx.messages.RxEventBinaryMessage;
import com.appunite.websocket.rx.messages.RxEventConnected;
import com.appunite.websocket.rx.messages.RxEventDisconnected;
import com.appunite.websocket.rx.messages.RxEventStringMessage;
import com.appunite.websocket.rx.object.messages.RxObjectEvent;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
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

            @Override
            public void call(final Subscriber<? super RxEvent> subscriber) {
                final WebSocket webSocket = client.newWebSocket(request, new WebSocketListener() {
                    @Override
                    public void onOpen(WebSocket webSocket, Response response) {
                        subscriber.onNext(new RxEventConnected(webSocket));
                    }

                    @Override
                    public void onMessage(WebSocket webSocket, String text) {
                        subscriber.onNext(new RxEventStringMessage(webSocket, text));
                    }

                    @Override
                    public void onMessage(WebSocket webSocket, ByteString bytes) {
                        subscriber.onNext(new RxEventBinaryMessage(webSocket, bytes.toByteArray()));
                    }

                    @Override
                    public void onClosing(WebSocket webSocket, int code, String reason) {
                        super.onClosing(webSocket, code, reason);
                        final ServerRequestedCloseException exception = new ServerRequestedCloseException(code, reason);
                        subscriber.onNext(new RxEventDisconnected(exception));
                        subscriber.onError(exception);
                    }

                    @Override
                    public void onClosed(WebSocket webSocket, int code, String reason) {
                        final ServerRequestedCloseException exception = new ServerRequestedCloseException(code, reason);
                        subscriber.onNext(new RxEventDisconnected(exception));
                        subscriber.onError(exception);
                    }

                    @Override
                    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                        if (response != null) {
                            final ServerHttpError exception = new ServerHttpError(response);
                            subscriber.onNext(new RxEventDisconnected(exception));
                            subscriber.onError(exception);
                        } else {
                            subscriber.onNext(new RxEventDisconnected(t));
                            subscriber.onError(t);
                        }
                    }
                });
                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        webSocket.close(1000, "Just disconnect");
                        subscriber.onCompleted();
                    }
                }));
            }
        });
    }

}
