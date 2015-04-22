package com.appunite.websocket.rx.json;

import com.appunite.websocket.NotConnectedException;

import java.io.IOException;

import javax.annotation.Nonnull;

public interface JsonWebSocketSender {
    /**
     * Send text message (thread safe). Can be called after onConnect and before
     * onDisconnect by any thread. Thread will be blocked until send
     *
     * @param message
     *            message to send
     * @throws IOException
     *             when exception occur while sending
     * @throws InterruptedException
     *             when user call disconnect
     * @throws NotConnectedException
     *             when called before onConnect or after onDisconnect
     */
    void sendObjectMessage(@Nonnull Object message) throws IOException,
            InterruptedException, NotConnectedException;
}
