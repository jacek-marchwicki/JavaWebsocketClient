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

package com.example;

import com.appunite.websocket.rx.*;
import com.appunite.websocket.rx.object.messages.RxObjectEvent;
import com.appunite.websocket.rx.object.messages.RxObjectEventConn;
import com.appunite.websocket.rx.object.messages.RxObjectEventConnected;
import com.appunite.websocket.rx.object.messages.RxObjectEventDisconnected;
import com.appunite.websocket.rx.object.messages.RxObjectEventMessage;
import com.example.model.DataMessage;
import com.example.model.PingMessage;
import com.example.model.RegisterMessage;
import com.example.model.RegisteredMessage;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public class Socket {
    public static final Logger LOGGER = Logger.getLogger("Rx");

    private final Observable<RxObjectEvent> events;
    private final Observable<Object> connection;
    private final BehaviorSubject<RxObjectEventConn> connectedAndRegistered;
    @Nonnull
    private final Scheduler scheduler;

    public Socket(@Nonnull SocketConnection socketConnection, @Nonnull Scheduler scheduler) {
        this.scheduler = scheduler;
        final PublishSubject<RxObjectEvent>events = PublishSubject.create();
        connection = socketConnection.connection()
                .lift(new OperatorDoOnNext<>(events))
                .lift(MoreObservables.ignoreNext())
                .compose(MoreObservables.behaviorRefCount());
        this.events = events;


        final Observable<RxObjectEventMessage> registeredMessage = events
                .compose(com.example.MoreObservables.filterAndMap(RxObjectEventMessage.class))
                .filter(new FilterRegisterMessage());

        final Observable<RxObjectEventDisconnected> disconnectedMessage = events
                .compose(com.example.MoreObservables.filterAndMap(RxObjectEventDisconnected.class));

        connectedAndRegistered = BehaviorSubject.create((RxObjectEventConn) null);
        disconnectedMessage
                .map(new Func1<RxObjectEventDisconnected, RxObjectEventConn>() {
                    @Override
                    public RxObjectEventConn call(RxObjectEventDisconnected rxEventDisconnected) {
                        return null;
                    }
                })
                .mergeWith(registeredMessage)
                .subscribe(connectedAndRegistered);

        // Register on connected
        final Observable<RxObjectEventConnected> connectedMessage = events
                .compose(com.example.MoreObservables.filterAndMap(RxObjectEventConnected.class))
                .lift(LoggingObservables.<RxObjectEventConnected>loggingLift(LOGGER, "ConnectedEvent"));

        connectedMessage
                .flatMap(new FlatMapToRegisterMessage())
                .lift(LoggingObservables.loggingOnlyErrorLift(LOGGER, "SendRegisterEvent"))
                .onErrorReturn(com.example.MoreObservables.throwableToIgnoreError())
                .subscribe();

        // Log events
        LOGGER.setLevel(Level.ALL);
        RxMoreObservables.logger.setLevel(Level.ALL);
        events
                .subscribe(LoggingObservables.logging(LOGGER, "Events"));
        connectedAndRegistered
                .subscribe(LoggingObservables.logging(LOGGER, "ConnectedAndRegistered"));
    }
    public Observable<RxObjectEvent> events() {
        return events;
    }

    public Observable<RxObjectEventConn> connectedAndRegistered() {
        return connectedAndRegistered;
    }

    public Observable<Object> connection() {
        return connection;
    }

    public void sendPingWhenConnected() {
        Observable.combineLatest(
                Observable.interval(5, TimeUnit.SECONDS, scheduler),
                connectedAndRegistered,
                new Func2<Long, RxObjectEventConn, RxObjectEventConn>() {
                    @Override
                    public RxObjectEventConn call(Long aLong, RxObjectEventConn rxEventConn) {
                        return rxEventConn;
                    }
                })
                .compose(isConnected())
                .flatMap(new Func1<RxObjectEventConn, Observable<?>>() {
                    @Override
                    public Observable<?> call(RxObjectEventConn rxEventConn) {
                        return Observable.just(new PingMessage("send_only_when_connected"))
                                .compose(RxMoreObservables.sendMessage(rxEventConn));
                    }
                })
                .subscribe();
    }

    public void sendPingEvery5seconds() {
        Observable.interval(5, TimeUnit.SECONDS, scheduler)
                .flatMap(new Func1<Long, Observable<?>>() {
                    @Override
                    public Observable<?> call(Long aLong) {
                        return connectedAndRegistered
                                .compose(isConnected())
                                .first()
                                .flatMap(new Func1<RxObjectEventConn, Observable<?>>() {
                                    @Override
                                    public Observable<?> call(RxObjectEventConn rxEventConn) {
                                        return Observable.just(new PingMessage("be_sure_to_send"))
                                                .compose(RxMoreObservables.sendMessage(rxEventConn));
                                    }
                                });
                    }
                })
                .subscribe();
    }

    private final Object lock = new Object();
    private int counter = 0;
    @Nonnull
    public Observable<String> nextId() {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                final int current;
                synchronized (lock) {
                    current = counter;
                    counter += 1;
                }
                subscriber.onNext(String.valueOf(current));
                subscriber.onCompleted();
            }
        });
    }

    @Nonnull
    public Observable<DataMessage> sendMessageOnceWhenConnected(final Func1<String, Observable<Object>> createMessage) {
        return connectedAndRegistered
                .compose(isConnected())
                .first()
                .flatMap(new Func1<RxObjectEventConn, Observable<DataMessage>>() {
                    @Override
                    public Observable<DataMessage> call(final RxObjectEventConn rxEventConn) {
                        return requestData(rxEventConn, createMessage);
                    }
                });
    }

    @Nonnull
    private Observable<DataMessage> requestData(final RxObjectEventConn rxEventConn,
                                                 final Func1<String, Observable<Object>> createMessage) {
        return nextId()
                .flatMap(new Func1<String, Observable<DataMessage>>() {
                    @Override
                    public Observable<DataMessage> call(final String messageId) {

                        final Observable<Object> sendMessageObservable = createMessage.call(messageId)
                                .compose(RxMoreObservables.sendMessage(rxEventConn));

                        final Observable<DataMessage> waitForResponseObservable = events
                                .compose(com.example.MoreObservables.filterAndMap(RxObjectEventMessage.class))
                                .compose(RxObjectEventMessage.filterAndMap(DataMessage.class))
                                .filter(new Func1<DataMessage, Boolean>() {
                                    @Override
                                    public Boolean call(DataMessage dataMessage) {
                                        return dataMessage.id().equals(messageId);
                                    }
                                })
                                .first()
                                .timeout(5, TimeUnit.SECONDS, scheduler);
                        return Observable.combineLatest(waitForResponseObservable, sendMessageObservable,
                                new Func2<DataMessage, Object, DataMessage>() {
                                    @Override
                                    public DataMessage call(DataMessage dataResponse, Object o) {
                                        return dataResponse;
                                    }
                                });
                    }
                });
    }

    @Nonnull
    private static Observable.Transformer<RxObjectEventConn, RxObjectEventConn> isConnected() {
        return new Observable.Transformer<RxObjectEventConn, RxObjectEventConn>() {
            @Override
            public Observable<RxObjectEventConn> call(Observable<RxObjectEventConn> rxEventConnObservable) {
                return rxEventConnObservable.filter(new Func1<RxObjectEventConn, Boolean>() {
                    @Override
                    public Boolean call(RxObjectEventConn rxEventConn) {
                        return rxEventConn != null;
                    }
                });
            }
        };
    }


    private static class FilterRegisterMessage implements Func1<RxObjectEventMessage, Boolean> {
        @Override
        public Boolean call(RxObjectEventMessage rxEvent) {
            return rxEvent.message() instanceof RegisteredMessage;
        }
    }


    private class FlatMapToRegisterMessage implements Func1<RxObjectEventConnected, Observable<Object>> {
        @Override
        public Observable<Object> call(RxObjectEventConnected rxEventConn) {
            return Observable.just(new RegisterMessage("asdf"))
                    .compose(RxMoreObservables.sendMessage(rxEventConn));
        }
    }
}
