package com.appunite.websocket.rx.messages;

import com.appunite.websocket.WebSocketSender;

import javax.annotation.Nonnull;

public class RxEventConnected extends RxEventConn {

    public RxEventConnected(@Nonnull WebSocketSender sender) {
        super(sender);
    }

    @Override
    public String toString() {
        return "ConnectedRxEvent{}";
    }
}
