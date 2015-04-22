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

    @Override
    public Observable<RxJsonEvent> connection() {
        return sockets.webSocketObservable()
                .retryWhen(repeatDuration(5, TimeUnit.SECONDS));
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
