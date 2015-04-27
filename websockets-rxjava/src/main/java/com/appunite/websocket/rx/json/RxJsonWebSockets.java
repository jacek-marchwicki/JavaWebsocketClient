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

package com.appunite.websocket.rx.json;

import com.appunite.websocket.NotConnectedException;
import com.appunite.websocket.WebSocketSender;
import com.appunite.websocket.rx.RxWebSockets;
import com.appunite.websocket.rx.json.messages.RxJsonEvent;
import com.appunite.websocket.rx.json.messages.RxJsonEventConnected;
import com.appunite.websocket.rx.json.messages.RxJsonEventWrongMessageFormat;
import com.appunite.websocket.rx.json.messages.RxJsonEventDisconnected;
import com.appunite.websocket.rx.json.messages.RxJsonEventMessage;
import com.appunite.websocket.rx.messages.RxEvent;
import com.appunite.websocket.rx.messages.RxEventConnected;
import com.appunite.websocket.rx.messages.RxEventDisconnected;
import com.appunite.websocket.rx.messages.RxEventStringMessage;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.lang.reflect.Type;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.Subscriber;

/**
 * This class allows to retrieve json messages from websocket
 */
public class RxJsonWebSockets {
    @Nonnull
    private final RxWebSockets rxWebSockets;
    @Nonnull
    private final Gson gson;
    @Nonnull
    private final Type typeOfT;

    /**
     * Creates {@link RxJsonWebSockets}
     *
     * @param rxWebSockets socket that is used to connect to server
     * @param gson that is used to parse messages {@link RxJsonEventMessage}
     *             and {@link JsonWebSocketSender#sendObjectMessage(Object)}
     * @param typeOfT type of class that is parsed to {@link RxJsonEventMessage}
     */
    public RxJsonWebSockets(@Nonnull RxWebSockets rxWebSockets, @Nonnull Gson gson, @Nonnull Type typeOfT) {
        this.rxWebSockets = rxWebSockets;
        this.gson = gson;
        this.typeOfT = typeOfT;
    }

    /**
     * Returns observable that connected to a websocket and returns {@link RxJsonEvent}s
     *
     * @return Observable that connects to websocket
     * @see RxWebSockets#webSocketObservable()
     */
    @Nonnull
    public Observable<RxJsonEvent> webSocketObservable() {
        return rxWebSockets.webSocketObservable()
                .lift(new Observable.Operator<RxJsonEvent, RxEvent>() {
                    @Override
                    public Subscriber<? super RxEvent> call(final Subscriber<? super RxJsonEvent> subscriber) {
                        return new Subscriber<RxEvent>(subscriber) {

                            @Override
                            public void onCompleted() {
                                subscriber.onCompleted();
                            }

                            @Override
                            public void onError(Throwable e) {
                                subscriber.onError(e);
                            }

                            @Override
                            public void onNext(RxEvent rxEvent) {
                                if (rxEvent instanceof RxEventConnected) {
                                    subscriber.onNext(new RxJsonEventConnected(jsonSocketSender(((RxEventConnected) rxEvent).sender())));
                                } else if (rxEvent instanceof RxEventDisconnected) {
                                    subscriber.onNext(new RxJsonEventDisconnected(((RxEventDisconnected) rxEvent).exception()));
                                } else if (rxEvent instanceof RxEventStringMessage) {
                                    final RxEventStringMessage stringMessage = (RxEventStringMessage) rxEvent;
                                    subscriber.onNext(parseMessage(stringMessage));
                                }
                            }

                            private RxJsonEvent parseMessage(RxEventStringMessage stringMessage) {
                                final String message = stringMessage.message();
                                try {
                                    final Object object = gson.fromJson(message, typeOfT);
                                    return new RxJsonEventMessage(jsonSocketSender(stringMessage.sender()), object);
                                } catch (JsonParseException e) {
                                    return new RxJsonEventWrongMessageFormat(jsonSocketSender(stringMessage.sender()), message, e);
                                }
                            }
                        };
                    }
                });
    }

    @Nonnull
    private JsonWebSocketSender jsonSocketSender(@Nonnull final WebSocketSender sender) {
        return new JsonWebSocketSender() {
            @Override
            public void sendObjectMessage(@Nonnull Object message) throws IOException, InterruptedException, NotConnectedException {
                sender.sendStringMessage(gson.toJson(message));
            }
        };
    }
}
