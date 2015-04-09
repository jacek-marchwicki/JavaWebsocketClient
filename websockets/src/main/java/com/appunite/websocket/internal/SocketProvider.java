package com.appunite.websocket.internal;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;

import javax.annotation.Nonnull;

public interface SocketProvider {

    @Nonnull
	Socket getSocket(@Nonnull URI uri) throws IOException;
}
