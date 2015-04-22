package com.appunite.websocket.rx.json.messages;

import javax.annotation.Nonnull;

public class RxJsonEventDisconnected extends RxJsonEvent {
    @Nonnull
    private final Exception exception;

    public RxJsonEventDisconnected(@Nonnull Exception exception) {
        super();
        this.exception = exception;
    }

    @Nonnull
    public Exception exception() {
        return exception;
    }

    @Override
    public String toString() {
        return "RxJsonEventDisconnected{" + "exception=" + exception + '}';
    }
}
