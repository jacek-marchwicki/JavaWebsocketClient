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
