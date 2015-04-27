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

import com.appunite.websocket.WebSocketSender;
import com.appunite.websocket.rx.json.JsonWebSocketSender;
import com.appunite.websocket.rx.json.RxJsonWebSockets;
import com.appunite.websocket.rx.json.messages.RxJsonEventConn;
import com.appunite.websocket.rx.messages.RxEventConn;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func1;

public class RxMoreObservables {

    public static final Logger logger = Logger.getLogger("RxWebSockets");

    public RxMoreObservables() {
    }

    @Nonnull
    private static Observable<Object> sendMessage(final @Nonnull WebSocketSender sender, final @Nonnull String message) {
        return Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                try {
                    logger.log(Level.FINE, "sendStringMessage: {0}", message);
                    sender.sendStringMessage(message);
                    subscriber.onNext(new Object());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    /**
     * Transformer that convert String message to observable that returns if message was sent
     *
     * @param connection connection event that is used to send message
     * @return Observable that returns {@link Observer#onNext(Object)} with new Object()
     *         and {@link Observer#onCompleted()} or {@link Observer#onError(Throwable)}
     *
     * @see #sendMessage(JsonWebSocketSender, Object)
     */
    @SuppressWarnings("unused")
    @Nonnull
    public static Observable.Transformer<String, Object> sendMessage(@Nonnull final RxEventConn connection) {
        return new Observable.Transformer<String, Object>() {
            @Override
            public Observable<Object> call(Observable<String> stringObservable) {
                return stringObservable.flatMap(new Func1<String, Observable<?>>() {
                    @Override
                    public Observable<?> call(String message) {
                        return sendMessage(connection.sender(), message);
                    }
                });
            }
        };
    }

    @Nonnull
    private static Observable<Object> sendMessage(final @Nonnull JsonWebSocketSender sender, final @Nonnull Object message) {
        return Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                try {
                    logger.log(Level.FINE, "sendStringMessage: {0}", message.toString());
                    sender.sendObjectMessage(message);
                    subscriber.onNext(new Object());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }


    /**
     * Transformer that convert Object message to observable that returns if message was sent
     *
     * Object is parsed via gson given by {@link RxJsonWebSockets#RxJsonWebSockets(RxWebSockets, Gson, Type)}
     * @param connection connection event that is used to send message
     * @return Observable that returns {@link Observer#onNext(Object)} with new Object()
     *         and {@link Observer#onCompleted()} or {@link Observer#onError(Throwable)}
     *
     * @see #sendMessage(RxEventConn)
     */
    @SuppressWarnings("unused")
    @Nonnull
    public static Observable.Transformer<Object, Object> sendMessage(@Nonnull final RxJsonEventConn connection) {
        return new Observable.Transformer<Object, Object>() {
            @Override
            public Observable<Object> call(Observable<Object> stringObservable) {
                return stringObservable.flatMap(new Func1<Object, Observable<?>>() {
                    @Override
                    public Observable<?> call(Object message) {
                        return sendMessage(connection.sender(), message);
                    }
                });
            }
        };
    }
}
