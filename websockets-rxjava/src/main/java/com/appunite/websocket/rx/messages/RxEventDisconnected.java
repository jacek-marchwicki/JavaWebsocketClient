package com.appunite.websocket.rx.messages;

import javax.annotation.Nonnull;

public class RxEventDisconnected extends RxEvent {

    @Nonnull
    private Exception exception;

    public RxEventDisconnected(@Nonnull Exception exception) {
        super();
        this.exception = exception;
    }

    @Nonnull
    public Exception exception() {
        return exception;
    }

    @Override
    public String toString() {
        return "DisconnectedRxEvent{" + "e=" + exception + '}';
    }
}
