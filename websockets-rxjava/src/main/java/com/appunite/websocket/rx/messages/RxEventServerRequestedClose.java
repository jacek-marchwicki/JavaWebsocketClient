package com.appunite.websocket.rx.messages;

import com.appunite.websocket.WebSocketSender;

import java.util.Arrays;

import javax.annotation.Nonnull;

public class RxEventServerRequestedClose extends RxEventBinaryMessageAbs {

    public RxEventServerRequestedClose(@Nonnull WebSocketSender sender, @Nonnull byte[] message) {
        super(sender, message);
    }

    @Override
    public String toString() {
        return "ServerRequestedCloseMessageRxEvent{" +
                "message=" + Arrays.toString(message()) +
                '}';
    }
}
