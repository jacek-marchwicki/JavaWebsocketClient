package com.appunite.websocket.rx.messages;

import com.appunite.websocket.WebSocketSender;

import java.util.Arrays;

import javax.annotation.Nonnull;

public class RxEventPong extends RxEventBinaryMessageAbs {

    public RxEventPong(@Nonnull WebSocketSender sender, @Nonnull byte[] message) {
        super(sender, message);
    }

    @Override
    public String toString() {
        return "PongMessageRxEvent{" +
                "message=" + Arrays.toString(message()) +
                '}';
    }
}
