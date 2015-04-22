package com.appunite.websocket.rx.messages;

import com.appunite.websocket.WebSocketSender;

import javax.annotation.Nonnull;

public abstract class RxEventConn extends RxEvent {
    @Nonnull
    private final WebSocketSender sender;

    public RxEventConn(@Nonnull WebSocketSender sender) {
        this.sender = sender;
    }

    @Nonnull
    public WebSocketSender sender() {
        return sender;
    }

}
