package com.example;

import com.appunite.websocket.NotConnectedException;
import com.appunite.websocket.rx.json.JsonWebSocketSender;
import com.appunite.websocket.rx.json.messages.RxJsonEvent;
import com.appunite.websocket.rx.json.messages.RxJsonEventConnected;
import com.appunite.websocket.rx.json.messages.RxJsonEventMessage;
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

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;
import rx.subjects.TestSubject;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SocketTest {

    @Mock
    SocketConnection socketConnection;
    @Mock
    Observer<RxJsonEvent> observer;
    @Mock
    JsonWebSocketSender sender;
    @Mock
    Observer<DataMessage> dataObserver;

    private Socket socket;

    private final TestScheduler testScheduler = Schedulers.test();
    private final TestSubject<RxJsonEvent> connection = TestSubject.create(testScheduler);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(socketConnection.connection()).thenReturn(connection);
        socket = new Socket(socketConnection, testScheduler);
    }

    @Test
    public void testConnection_registerIsSent() throws Exception {
        final Subscription subscribe = socket.connection().subscribe(observer);
        try {
            connection.onNext(new RxJsonEventConnected(sender), 0);
            testScheduler.triggerActions();
            verify(sender).sendObjectMessage(new RegisterMessage("asdf"));
        } finally {
            subscribe.unsubscribe();
        }
    }

    private void register() throws IOException, InterruptedException, NotConnectedException {
        connection.onNext(new RxJsonEventConnected(sender), 0);
        testScheduler.triggerActions();
        verify(sender).sendObjectMessage(new RegisterMessage("asdf"));
        connection.onNext(new RxJsonEventMessage(sender, new RegisteredMessage()), 0);
        testScheduler.triggerActions();
    }

    @Test
    public void testWhenNoResponse_throwError() throws Exception {
        final Subscription subscribe = socket.connection().subscribe(observer);
        socket.sendMessageOnceWhenConnected(
                new Func1<String, Observable<Object>>() {
                    @Override
                    public Observable<Object> call(String id) {
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
            subscribe.unsubscribe();
        }
    }

    @Test
    public void testWhenResponseOnDifferentMessage_throwError() throws Exception {
        final Subscription subscribe = socket.connection().subscribe(observer);
        socket.sendMessageOnceWhenConnected(
                new Func1<String, Observable<Object>>() {
                    @Override
                    public Observable<Object> call(String id) {
                        return Observable.<Object>just(new DataMessage(id, "krowa"));
                    }
                })
                .subscribe(dataObserver);
        try {
            register();
            verify(sender).sendObjectMessage(new DataMessage("0", "krowa"));

            connection.onNext(new RxJsonEventMessage(sender, new DataMessage("100", "asdf")), 0);
            testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);

            verify(dataObserver).onError(any(Throwable.class));
        } finally {
            subscribe.unsubscribe();
        }
    }

    @Test
    public void testWhenResponse_messageSuccess() throws Exception {
        final Subscription subscribe = socket.connection().subscribe(observer);
        socket.sendMessageOnceWhenConnected(
                new Func1<String, Observable<Object>>() {
                    @Override
                    public Observable<Object> call(String id) {
                        return Observable.<Object>just(new DataMessage(id, "krowa"));
                    }
                })
                .subscribe(dataObserver);
        try {
            register();
            verify(sender).sendObjectMessage(new DataMessage("0", "krowa"));

            connection.onNext(new RxJsonEventMessage(sender, new DataMessage("0", "asdf")), 0);
            testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);

            verify(dataObserver).onNext(new DataMessage("0", "asdf"));
            verify(dataObserver).onCompleted();
        } finally {
            subscribe.unsubscribe();
        }
    }

    @Test
    public void testAfterConnection_registerSuccess() throws Exception {
        final Subscription subscribe = socket.connection().subscribe(observer);
        try {
            register();
        } finally {
            subscribe.unsubscribe();
        }
    }

    @Test
    public void testConnectionSuccessAfterAWhile_registerSuccess() throws Exception {
        final Subscription subscribe = socket.connection().subscribe(observer);
        try {
            testScheduler.advanceTimeBy(30, TimeUnit.SECONDS);
            register();
        } finally {
            subscribe.unsubscribe();
        }
    }

    @Test
    public void testWhenTimePassBeforeConnection_sendAllPings() throws Exception {
        final Subscription subscribe = socket.connection().subscribe(observer);
        socket.sendPingEvery5seconds();
        try {
            testScheduler.advanceTimeBy(30, TimeUnit.SECONDS);
            register();


            verify(sender, times(6)).sendObjectMessage(new PingMessage("be_sure_to_send"));
        } finally {
            subscribe.unsubscribe();
        }
    }

    @Test
    public void testWhenTimePassBeforeConnection_sendPingOnlyOnce() throws Exception {
        final Subscription subscribe = socket.connection().subscribe(observer);
        socket.sendPingWhenConnected();
        try {
            testScheduler.advanceTimeBy(30, TimeUnit.SECONDS);
            register();


            verify(sender).sendObjectMessage(new PingMessage("send_only_when_connected"));
        } finally {
            subscribe.unsubscribe();
        }
    }
}