package com.appunite.websocket;

import java.io.IOException;

import javax.annotation.Nonnull;

public interface WebSocketSender {
    /**
     * Send ping request (thread safe). Can be called after onConnect and
     * before onDisconnect by any thread. Thread will be blocked until send
     *
     * @param buffer
     *            buffer to send
     * @throws IOException
     *             when exception occur while sending
     * @throws InterruptedException
     *             when user call disconnect
     * @throws NotConnectedException
     *             when called before onConnect or after onDisconnect
     */
    void sendPingMessage(@Nonnull byte[] buffer) throws IOException,
            InterruptedException, NotConnectedException;

    /**
     * Send binary message (thread safe). Can be called after onConnect and
     * before onDisconnect by any thread. Thread will be blocked until send
     *
     * @param buffer
     *            buffer to send
     * @throws IOException
     *             when exception occur while sending
     * @throws InterruptedException
     *             when user call disconnect
     * @throws NotConnectedException
     *             when called before onConnect or after onDisconnect
     */
    void sendByteMessage(@Nonnull byte[] buffer) throws IOException,
            InterruptedException, NotConnectedException;

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
    void sendStringMessage(@Nonnull String message) throws IOException,
            InterruptedException, NotConnectedException;
}
