package com.appunite.websocket.rx.json.messages;

import com.appunite.websocket.rx.json.JsonWebSocketSender;

import javax.annotation.Nonnull;

public abstract class RxJsonEventConn extends RxJsonEvent {
    @Nonnull
    private final JsonWebSocketSender sender;

    public RxJsonEventConn(@Nonnull JsonWebSocketSender sender) {
        this.sender = sender;
    }

    @Nonnull
    public JsonWebSocketSender sender() {
        return sender;
    }
}
