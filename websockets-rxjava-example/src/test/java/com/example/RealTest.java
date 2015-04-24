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

import com.appunite.websocket.NewWebSocket;
import com.appunite.websocket.rx.*;
import com.appunite.websocket.rx.json.RxJsonWebSockets;
import com.example.model.DataMessage;
import com.example.model.MessageType;
import com.example.model.Message;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class RealTest {

    private static final URI SERVER_URI;

    static {
        try {
            SERVER_URI = new URI("ws://192.168.0.142:8080/ws");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    private Socket socket;

    @Before
    public void setUp() throws Exception {
        final Gson gson = new GsonBuilder()
                .registerTypeAdapter(Message.class, new Message.Deserializer())
                .registerTypeAdapter(MessageType.class, new MessageType.SerializerDeserializer())
                .create();

        final NewWebSocket newWebSocket = new NewWebSocket();
        final RxWebSockets webSockets = new RxWebSockets(newWebSocket, SERVER_URI);
        final RxJsonWebSockets jsonWebSockets = new RxJsonWebSockets(webSockets, gson, Message.class);
        final SocketConnection socketConnection = new SocketConnectionImpl(jsonWebSockets, Schedulers.computation());
        socket = new Socket(socketConnection, Schedulers.computation());

    }

    @Test
    @Ignore
    public void testName() throws Exception {
        socket.sendMessageOnceWhenConnected(new Func1<String, Observable<Object>>() {
            @Override
            public Observable<Object> call(String messageId) {
                return Observable.<Object>just(new DataMessage(messageId, "some message"));
            }
        })
                .subscribe(LoggingObservables.logging(Logger.getLogger("Rx"), "SendMessage"));

        socket.sendPingWhenConnected();
        socket.sendPingEvery5seconds();
        final Subscription subscribe = socket.connection()
                .subscribeOn(Schedulers.io())
                .subscribe();
        Thread.sleep(10000);
        subscribe.unsubscribe();
        Thread.sleep(10000);
    }

}