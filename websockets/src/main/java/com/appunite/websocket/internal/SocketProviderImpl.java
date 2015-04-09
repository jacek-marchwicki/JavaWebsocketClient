package com.appunite.websocket.internal;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;

import javax.annotation.Nonnull;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import static com.appunite.websocket.tools.Preconditions.checkNotNull;

public class SocketProviderImpl implements SocketProvider {

    @Nonnull
    @Override
    public Socket getSocket(@Nonnull URI uri) throws IOException {
        if (isSsl(uri)) {
            return SSLSocketFactory.getDefault().createSocket();
        } else {
            return SocketFactory.getDefault().createSocket();
        }
    }

    /**
     * Return if given uri is ssl encrypted (thread safe)
     *
     * @param uri uri to check
     * @return true if uri is wss
     * @throws IllegalArgumentException
     *             if unkonwo schema
     */
    private static boolean isSsl(@Nonnull URI uri) {
        checkNotNull(uri);
        final String scheme = uri.getScheme();
        if ("wss".equals(scheme)) {
            return true;
        } else if ("ws".equals(scheme)) {
            return false;
        } else {
            throw new IllegalArgumentException("Unknown schema");
        }
    }
}
