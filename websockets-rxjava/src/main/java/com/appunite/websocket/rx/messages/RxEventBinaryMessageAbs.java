package com.appunite.websocket.rx.messages;

import com.appunite.websocket.WebSocketSender;

import javax.annotation.Nonnull;

abstract class RxEventBinaryMessageAbs extends RxEventConn {

    @Nonnull
    private final byte[] message;

    public RxEventBinaryMessageAbs(@Nonnull WebSocketSender sender, @Nonnull byte[] message) {
        super(sender);
        this.message = message;
    }

    @Nonnull
    public byte[] message() {
        return message;
    }

}
