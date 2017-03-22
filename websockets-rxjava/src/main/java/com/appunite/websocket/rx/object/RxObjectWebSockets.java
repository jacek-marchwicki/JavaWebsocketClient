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

package com.appunite.websocket.rx.object;

import com.appunite.websocket.rx.RxWebSockets;
import com.appunite.websocket.rx.messages.RxEventBinaryMessage;
import com.appunite.websocket.rx.object.messages.RxObjectEvent;
import com.appunite.websocket.rx.object.messages.RxObjectEventMessage;
import com.appunite.websocket.rx.messages.RxEvent;
import com.appunite.websocket.rx.messages.RxEventConnected;
import com.appunite.websocket.rx.messages.RxEventDisconnected;
import com.appunite.websocket.rx.messages.RxEventStringMessage;
import com.appunite.websocket.rx.object.messages.RxObjectEventWrongBinaryMessageFormat;
import com.appunite.websocket.rx.object.messages.RxObjectEventConnected;
import com.appunite.websocket.rx.object.messages.RxObjectEventWrongStringMessageFormat;
import com.appunite.websocket.rx.object.messages.RxObjectEventDisconnected;

import okhttp3.WebSocket;

import javax.annotation.Nonnull;

import okio.ByteString;
import rx.Observable;
import rx.Subscriber;

/**
 * This class allows to retrieve json messages from websocket
 */
public class RxObjectWebSockets {
    @Nonnull
    private final RxWebSockets rxWebSockets;
    @Nonnull
    private final ObjectSerializer objectSerializer;

    /**
     * Creates {@link RxObjectWebSockets}
     * @param rxWebSockets socket that is used to connect to server
     * @param objectSerializer that is used to parse messages
     */
    public RxObjectWebSockets(@Nonnull RxWebSockets rxWebSockets, @Nonnull ObjectSerializer objectSerializer) {
        this.rxWebSockets = rxWebSockets;
        this.objectSerializer = objectSerializer;
    }

    /**
     * Returns observable that connected to a websocket and returns {@link RxObjectEvent}s
     *
     * @return Observable that connects to websocket
     * @see RxWebSockets#webSocketObservable()
     */
    @Nonnull
    public Observable<RxObjectEvent> webSocketObservable() {
        return rxWebSockets.webSocketObservable()
                .lift(new Observable.Operator<RxObjectEvent, RxEvent>() {
                    @Override
                    public Subscriber<? super RxEvent> call(final Subscriber<? super RxObjectEvent> subscriber) {
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
                                    subscriber.onNext(new RxObjectEventConnected(jsonSocketSender(((RxEventConnected) rxEvent).sender())));
                                } else if (rxEvent instanceof RxEventDisconnected) {
                                    subscriber.onNext(new RxObjectEventDisconnected(((RxEventDisconnected) rxEvent).exception()));
                                } else if (rxEvent instanceof RxEventStringMessage) {
                                    final RxEventStringMessage stringMessage = (RxEventStringMessage) rxEvent;
                                    subscriber.onNext(parseMessage(stringMessage));
                                } else if (rxEvent instanceof RxEventBinaryMessage) {
                                    final RxEventBinaryMessage binaryMessage = (RxEventBinaryMessage) rxEvent;
                                    subscriber.onNext(parseMessage(binaryMessage));
                                } else {
                                    throw new RuntimeException("Unknown message type");
                                }
                            }

                            private RxObjectEvent parseMessage(RxEventStringMessage stringMessage) {
                                final String message = stringMessage.message();
                                final Object object;
                                try {
                                    object = objectSerializer.serialize(message);
                                } catch (ObjectParseException e) {
                                    return new RxObjectEventWrongStringMessageFormat(jsonSocketSender(stringMessage.sender()), message, e);
                                }
                                return new RxObjectEventMessage(jsonSocketSender(stringMessage.sender()), object);
                            }

                            private RxObjectEvent parseMessage(RxEventBinaryMessage binaryMessage) {
                                final byte[] message = binaryMessage.message();
                                final Object object;
                                try {
                                    object = objectSerializer.serialize(message);
                                } catch (ObjectParseException e) {
                                    return new RxObjectEventWrongBinaryMessageFormat(jsonSocketSender(binaryMessage.sender()), message, e);
                                }
                                return new RxObjectEventMessage(jsonSocketSender(binaryMessage.sender()), object);
                            }
                        };
                    }
                });
    }

    @Nonnull
    private ObjectWebSocketSender jsonSocketSender(@Nonnull final WebSocket sender) {
        return new ObjectWebSocketSender() {
            @Override
            public boolean sendObjectMessage(@Nonnull Object message) throws ObjectParseException {
                if (objectSerializer.isBinary(message)) {
                    return sender.send(ByteString.of(objectSerializer.deserializeBinary(message)));
                } else {
                    return sender.send(objectSerializer.deserializeString(message));
                }
            }
        };
    }
}
