package com.appunite.websocket.rx;

import com.appunite.websocket.NewWebSocket;
import com.appunite.websocket.NotConnectedException;
import com.appunite.websocket.WebSocketConnection;
import com.appunite.websocket.WebSocketListener;
import com.appunite.websocket.WrongWebsocketResponse;

import java.io.IOException;
import java.net.URI;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.observers.Subscribers;

public class RxWebSockets {

    public RxWebSockets(@Nonnull NewWebSocket newWebSocket) {
        this.newWebSocket = newWebSocket;
    }

    static abstract class RxEvent {
        private final WebSocketConnection connection;

        public RxEvent(@Nonnull WebSocketConnection connection) {
            this.connection = connection;
        }

        @Nonnull
        public Observable<Object> sendMessage(final @Nonnull String message) {
            return Observable.create(new Observable.OnSubscribe<Object>() {
                @Override
                public void call(Subscriber<? super Object> subscriber) {
                    try {
                        connection.sendStringMessage(message);
                        subscriber.onNext(new Object());
                        subscriber.onCompleted();
                    } catch (Exception e) {
                        subscriber.onError(e);
                    }
                }
            });
        }
    }

    static class ConnectedRxEvent extends RxEvent {

        public ConnectedRxEvent(@Nonnull WebSocketConnection connection) {
            super(connection);
        }
    }

    static class StringMessageRxEvent extends RxEvent {

        public StringMessageRxEvent(@Nonnull WebSocketConnection webSocketConnection, @Nonnull String message) {
            super(webSocketConnection);
        }
    }

    static class BinaryMessageRxEvent extends RxEvent {

        public BinaryMessageRxEvent(@Nonnull WebSocketConnection webSocketConnection, @Nonnull byte[] message) {
            super(webSocketConnection);
        }
    }

    private final NewWebSocket newWebSocket;

    public Observable<RxEvent> webSocketObservable(final URI uri) {
        return Observable.create(new Observable.OnSubscribe<RxEvent>() {

            private WebSocketConnection webSocketConnection;

            @Override
            public void call(final Subscriber<? super RxEvent> subscriber) {
                try {
                    webSocketConnection = newWebSocket.create(uri, new WebSocketListener() {
                        @Override
                        public void onConnected() throws IOException, InterruptedException, NotConnectedException {
                            subscriber.onNext(new ConnectedRxEvent(webSocketConnection));
                        }

                        @Override
                        public void onStringMessage(@Nonnull String message) throws IOException, InterruptedException, NotConnectedException {
                            subscriber.onNext(new StringMessageRxEvent(webSocketConnection, message));
                        }

                        @Override
                        public void onBinaryMessage(@Nonnull byte[] data) throws IOException, InterruptedException, NotConnectedException {
                            subscriber.onNext(new BinaryMessageRxEvent(webSocketConnection, data));
                        }

                        @Override
                        public void onPing(@Nonnull byte[] data) throws IOException, InterruptedException, NotConnectedException {
                        }

                        @Override
                        public void onPong(@Nonnull byte[] data) throws IOException, InterruptedException, NotConnectedException {
                        }

                        @Override
                        public void onServerRequestedClose(@Nonnull byte[] data) throws IOException, InterruptedException, NotConnectedException {
                        }

                        @Override
                        public void onUnknownMessage(@Nonnull byte[] data) throws IOException, InterruptedException, NotConnectedException {
                        }
                    });
                } catch (IOException e) {
                    subscriber.onError(e);
                    return;
                }
                subscriber.add(Subscribers.create(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        webSocketConnection.interrupt();
                    }
                }));
                try {
                    webSocketConnection.connect();
                } catch (WrongWebsocketResponse | IOException e) {
                    subscriber.onError(e);
                } catch (InterruptedException e) {
                    subscriber.onCompleted();
                }
            }
        });
    }
}
