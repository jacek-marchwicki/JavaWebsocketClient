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

package com.appunite.websocket.rx.json.messages;

import com.appunite.websocket.rx.json.JsonWebSocketSender;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.functions.Func1;

public class RxJsonEventMessage extends RxJsonEventConn {
    @Nonnull
    private final Object message;

    public RxJsonEventMessage(@Nonnull JsonWebSocketSender sender, @Nonnull Object message) {
        super(sender);
        this.message = message;
    }

    @Nonnull
    public <T> T message() {
        //noinspection unchecked
        return (T) message;
    }

    @Override
    public String toString() {
        return "RxJsonEventMessage{" +
                "message='" + message + '\'' +
                '}';
    }

    @Nonnull
    public static <T> Observable.Transformer<RxJsonEventMessage, T> filterAndMap(@Nonnull final Class<T> clazz) {
        return new Observable.Transformer<RxJsonEventMessage, T>() {
            @Override
            public Observable<T> call(Observable<RxJsonEventMessage> observable) {
                return observable
                        .filter(new Func1<RxJsonEventMessage, Boolean>() {
                            @Override
                            public Boolean call(RxJsonEventMessage o) {
                                return o != null && clazz.isInstance(o.message());
                            }
                        })
                        .map(new Func1<RxJsonEventMessage, T>() {
                            @Override
                            public T call(RxJsonEventMessage o) {
                                return o.message();
                            }
                        });
            }
        };
    }
}
