package com.appunite.websocket.rx.messages;

import com.appunite.websocket.WebSocketSender;

import java.util.Arrays;

import javax.annotation.Nonnull;

public class RxEventPing extends RxEventBinaryMessageAbs {

    public RxEventPing(@Nonnull WebSocketSender sender, @Nonnull byte[] message) {
        super(sender, message);
    }

    @Override
    public String toString() {
        return "RxEventPing{" +
                "message=" + Arrays.toString(message()) +
                '}';
    }
}
