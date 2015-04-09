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

import com.appunite.websocket.rx.json.RxJsonWebSockets;
import com.appunite.websocket.rx.json.messages.RxJsonEvent;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

public class SocketConnectionImpl implements SocketConnection {

    @Nonnull
    private final RxJsonWebSockets sockets;
    @Nonnull
    private final Scheduler scheduler;

    public SocketConnectionImpl(@Nonnull RxJsonWebSockets sockets, @Nonnull Scheduler scheduler) {
        this.sockets = sockets;
        this.scheduler = scheduler;
    }

    @Nonnull
    @Override
    public Observable<RxJsonEvent> connection() {
        return sockets.webSocketObservable()
                .retryWhen(repeatDuration(1, TimeUnit.SECONDS));
    }

    @Nonnull
    private Func1<Observable<? extends Throwable>, Observable<?>> repeatDuration(final long delay,
                                                                                 @Nonnull final TimeUnit timeUnit) {
        return new Func1<Observable<? extends Throwable>, Observable<?>>() {
            @Override
            public Observable<?> call(Observable<? extends Throwable> attemps) {
                return attemps
                        .flatMap(new Func1<Throwable, Observable<?>>() {
                            @Override
                            public Observable<?> call(Throwable aLong) {
                                return Observable.timer(delay, timeUnit, scheduler);
                            }
                        });
            }
        };
    }
}
