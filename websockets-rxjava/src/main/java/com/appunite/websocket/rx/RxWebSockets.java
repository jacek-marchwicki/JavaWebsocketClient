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

import com.appunite.websocket.NewWebSocket;
import com.appunite.websocket.NotConnectedException;
import com.appunite.websocket.WebSocketConnection;
import com.appunite.websocket.WebSocketListener;
import com.appunite.websocket.rx.messages.RxEvent;
import com.appunite.websocket.rx.messages.RxEventBinarryMessage;
import com.appunite.websocket.rx.messages.RxEventConnected;
import com.appunite.websocket.rx.messages.RxEventDisconnected;
import com.appunite.websocket.rx.messages.RxEventPing;
import com.appunite.websocket.rx.messages.RxEventPong;
import com.appunite.websocket.rx.messages.RxEventServerRequestedClose;
import com.appunite.websocket.rx.messages.RxEventStringMessage;
import com.appunite.websocket.rx.messages.RxEventUnknownMessage;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.internal.schedulers.ScheduledAction;
import rx.observers.Subscribers;
import rx.subscriptions.Subscriptions;

/**
 * This class allows to retrieve messages from websocket
 */
public class RxWebSockets {

    public static final Logger logger = Logger.getLogger("RxWebSockets");

    public RxWebSockets(@Nonnull NewWebSocket newWebSocket, @Nonnull final URI uri) {
        this.newWebSocket = newWebSocket;
        this.uri = uri;
    }

    @Nonnull
    private final NewWebSocket newWebSocket;
    @Nonnull
    private final URI uri;

    @Nonnull
    public Observable<RxEvent> webSocketObservable() {
        return Observable.create(new Observable.OnSubscribe<RxEvent>() {

            private WebSocketConnection connection;

            @Override
            public void call(final Subscriber<? super RxEvent> subscriber) {
                final WebSocketListener listener = new WebSocketListener() {
                    @Override
                    public void onConnected() throws IOException, InterruptedException, NotConnectedException {
                        subscriber.onNext(new RxEventConnected(connection));
                    }

                    @Override
                    public void onStringMessage(@Nonnull String message) throws IOException, InterruptedException, NotConnectedException {
                        subscriber.onNext(new RxEventStringMessage(connection, message));
                    }

                    @Override
                    public void onBinaryMessage(@Nonnull byte[] data) throws IOException, InterruptedException, NotConnectedException {
                        subscriber.onNext(new RxEventBinarryMessage(connection, data));
                    }

                    @Override
                    public void onPing(@Nonnull byte[] data) throws IOException, InterruptedException, NotConnectedException {
                        subscriber.onNext(new RxEventPing(connection, data));
                    }

                    @Override
                    public void onPong(@Nonnull byte[] data) throws IOException, InterruptedException, NotConnectedException {
                        subscriber.onNext(new RxEventPong(connection, data));
                    }

                    @Override
                    public void onServerRequestedClose(@Nonnull byte[] data) throws IOException, InterruptedException, NotConnectedException {
                        subscriber.onNext(new RxEventServerRequestedClose(connection, data));
                    }

                    @Override
                    public void onUnknownMessage(@Nonnull byte[] data) throws IOException, InterruptedException, NotConnectedException {
                        subscriber.onNext(new RxEventUnknownMessage(connection, data));
                    }
                };
                try {
                    connection = newWebSocket.create(uri, listener);
                } catch (IOException e) {
                    subscriber.onNext(new RxEventDisconnected(e));
                    subscriber.onError(e);
                    return;
                }
                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        connection.interrupt();
                    }
                }));

                try {
                    connection.connect();
                } catch (InterruptedException e) {
                    subscriber.onNext(new RxEventDisconnected(e));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onNext(new RxEventDisconnected(e));
                    subscriber.onError(e);
                }

            }
        });
    }
}
