package com.appunite.websocket.rx.messages;

import com.appunite.websocket.WebSocketSender;

import java.util.Arrays;

import javax.annotation.Nonnull;

public class RxEventUnknownMessage extends RxEventBinaryMessageAbs {

    public RxEventUnknownMessage(@Nonnull WebSocketSender sender, @Nonnull byte[] message) {
        super(sender, message);
    }

    @Override
    public String toString() {
        return "UnknownMessageMessageRxEvent{" +
                "message=" + Arrays.toString(message()) +
                '}';
    }
}
