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

import com.appunite.websocket.rx.object.ObjectParseException;
import com.appunite.websocket.rx.object.ObjectWebSocketSender;
import com.appunite.websocket.rx.object.messages.RxObjectEvent;
import com.appunite.websocket.rx.object.messages.RxObjectEventConnected;
import com.appunite.websocket.rx.object.messages.RxObjectEventMessage;
import com.example.model.DataMessage;
import com.example.model.PingMessage;
import com.example.model.RegisterMessage;
import com.example.model.RegisteredMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subjects.PublishSubject;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SocketTest {

    @Mock
    SocketConnection socketConnection;
    @Mock
    Observer<Object> observer;
    @Mock
    ObjectWebSocketSender sender;
    @Mock
    Observer<DataMessage> dataObserver;

    private Socket socket;

    private final TestScheduler testScheduler = new TestScheduler();
    private final PublishSubject<RxObjectEvent> connection = PublishSubject.create();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(socketConnection.connection()).thenReturn(connection);
        socket = new Socket(socketConnection, testScheduler);
    }

    @Test
    public void testConnection_registerIsSent() throws Exception {
       final Disposable subscribe = socket.connection().test();
        try {
            connection.onNext(new RxObjectEventConnected(sender));
            testScheduler.triggerActions();
            verify(sender).sendObjectMessage(new RegisterMessage("asdf"));
        } finally {
            subscribe.dispose();
        }
    }

    private void register() throws IOException, InterruptedException,
            ObjectParseException {
        connection.onNext(new RxObjectEventConnected(sender));
        testScheduler.triggerActions();
        verify(sender).sendObjectMessage(new RegisterMessage("asdf"));
        connection.onNext(new RxObjectEventMessage(sender, new RegisteredMessage()));
        testScheduler.triggerActions();
    }

    @Test
    public void testWhenNoResponse_throwError() throws Exception {
       final Disposable subscribe = socket.connection().test();
        socket.sendMessageOnceWhenConnected(
                new Function<String, Observable<Object>>() {
                    @Override
                    public Observable<Object> apply(String id) {
                        return Observable.<Object>just(new DataMessage(id, "krowa"));
                    }
                })
                .subscribe(dataObserver);
        try {
            register();

            verify(sender).sendObjectMessage(new DataMessage("0", "krowa"));
            testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);

            verify(dataObserver).onError(any(Throwable.class));
        } finally {
            subscribe.dispose();
        }
    }

    @Test
    public void testWhenResponseOnDifferentMessage_throwError() throws Exception {
       final Disposable subscribe = socket.connection().test();
        socket.sendMessageOnceWhenConnected(
                new Function<String, Observable<Object>>() {
                    @Override
                    public Observable<Object> apply(String id) {
                        return Observable.<Object>just(new DataMessage(id, "krowa"));
                    }
                })
                .subscribe(dataObserver);
        try {
            register();
            verify(sender).sendObjectMessage(new DataMessage("0", "krowa"));

            connection.onNext(new RxObjectEventMessage(sender, new DataMessage("100", "asdf")));
            testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);

            verify(dataObserver).onError(any(Throwable.class));
        } finally {
            subscribe.dispose();
        }
    }

    @Test
    public void testWhenResponse_messageSuccess() throws Exception {
       final Disposable subscribe = socket.connection().test();
        socket.sendMessageOnceWhenConnected(
                new Function<String, Observable<Object>>() {
                    @Override
                    public Observable<Object> apply(String id) {
                        return Observable.<Object>just(new DataMessage(id, "krowa"));
                    }
                })
                .subscribe(dataObserver);
        try {
            register();
            verify(sender).sendObjectMessage(new DataMessage("0", "krowa"));

            connection.onNext(new RxObjectEventMessage(sender, new DataMessage("0", "asdf")));
            testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);

            verify(dataObserver).onNext(new DataMessage("0", "asdf"));
            verify(dataObserver).onComplete();
        } finally {
            subscribe.dispose();
        }
    }

    @Test
    public void testAfterConnection_registerSuccess() throws Exception {
       final Disposable subscribe = socket.connection().test();
        try {
            register();
        } finally {
            subscribe.dispose();
        }
    }

    @Test
    public void testConnectionSuccessAfterAWhile_registerSuccess() throws Exception {
       final Disposable subscribe = socket.connection().test();
        try {
            testScheduler.advanceTimeBy(30, TimeUnit.SECONDS);
            register();
        } finally {
            subscribe.dispose();
        }
    }

    @Test
    public void testWhenTimePassBeforeConnection_sendAllPings() throws Exception {
       final Disposable subscribe = socket.connection().test();
        socket.sendPingEvery5seconds();
        try {
            testScheduler.advanceTimeBy(30, TimeUnit.SECONDS);
            register();


            verify(sender, times(6)).sendObjectMessage(new PingMessage("be_sure_to_send"));
        } finally {
            subscribe.dispose();
        }
    }

    @Test
    public void testWhenTimePassBeforeConnection_sendPingOnlyOnce() throws Exception {
       final Disposable subscribe = socket.connection().test();
        socket.sendPingWhenConnected();
        try {
            testScheduler.advanceTimeBy(30, TimeUnit.SECONDS);
            register();


            verify(sender).sendObjectMessage(new PingMessage("send_only_when_connected"));
        } finally {
            subscribe.dispose();
        }
    }
}