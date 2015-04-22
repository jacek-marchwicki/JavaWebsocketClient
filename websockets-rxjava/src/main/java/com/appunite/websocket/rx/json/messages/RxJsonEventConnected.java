package com.appunite.websocket.rx.json.messages;

import com.appunite.websocket.rx.json.JsonWebSocketSender;

import javax.annotation.Nonnull;

public class RxJsonEventConnected extends RxJsonEventConn {
    public RxJsonEventConnected(@Nonnull JsonWebSocketSender sender) {
        super(sender);
    }

    @Override
    public String toString() {
        return "RxJsonEventConnected{}";
    }
}
