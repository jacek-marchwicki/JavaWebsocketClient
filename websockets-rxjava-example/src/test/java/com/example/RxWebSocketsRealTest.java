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

import com.appunite.websocket.rx.RxWebSockets;
import com.appunite.websocket.rx.messages.RxEvent;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class RxWebSocketsRealTest {

    private RxWebSockets socket;

    @Before
    public void setUp() throws Exception {
        socket = new RxWebSockets(new OkHttpClient(),
                new Request.Builder()
                        .get()
                        .url("ws://10.10.0.2:8080/ws")
                        .addHeader("Sec-WebSocket-Protocol", "chat")
                        .build());

    }

    @Test
    @Ignore
    public void testName() throws Exception {
        final Subscription subscribe = socket.webSocketObservable()
                .subscribeOn(Schedulers.io())
                .doOnNext(new Action1<RxEvent>() {
                    @Override
                    public void call(RxEvent rxEvent) {
                        System.out.println("Event: " + rxEvent);
                    }
                })
                .subscribe();
        Thread.sleep(5000);
        subscribe.unsubscribe();
        Thread.sleep(5000);
    }

}