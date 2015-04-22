package com.appunite.websocket.rx.messages;

import com.appunite.websocket.WebSocketSender;

import javax.annotation.Nonnull;

public class RxEventStringMessage extends RxEventConn {

    @Nonnull
    private final String message;

    public RxEventStringMessage(@Nonnull WebSocketSender sender, @Nonnull String message) {
        super(sender);
        this.message = message;
    }

    @Nonnull
    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return "StringMessageRxEvent{" +
                "message='" + message + '\'' +
                '}';
    }
}
