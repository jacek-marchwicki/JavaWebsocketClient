/*
 * Copyright (C) 2012 Jacek Marchwicki <jacek.marchwicki@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.appunite.websocket;

import com.appunite.websocket.internal.SecureRandomProvider;
import com.appunite.websocket.internal.SecureRandomProviderImpl;
import com.appunite.websocket.internal.SocketProvider;
import com.appunite.websocket.internal.SocketProviderImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;

import javax.annotation.Nonnull;

import static com.appunite.websocket.tools.Preconditions.checkNotNull;

/**
 * Allows user to connect to websocket
 * 
 * @author Jacek Marchwicki <jacek.marchwicki@gmail.com>
 * 
 */
public class NewWebSocket {

	// Not need to be locked
	@Nonnull
    private final SecureRandomProvider secureRandomProvider;
	@Nonnull
	private final SocketProvider socketProvider;

	/**
	 * Create instance of WebSocket
	 */
	public NewWebSocket() {
		this(new SecureRandomProviderImpl(), new SocketProviderImpl());
	}

	/**
	 * Create instance of WebSocket
	 *
	 * @param secureRandomProvider
	 *            provider for random values
	 * @param socketProvider
	 *            provider for socket
	 */
	public NewWebSocket(@Nonnull SecureRandomProvider secureRandomProvider,
						@Nonnull SocketProvider socketProvider) {
		checkNotNull(secureRandomProvider, "secureRandomProvider cannot be null");
		checkNotNull(socketProvider, "socketProvider cannot be null");
        this.secureRandomProvider = secureRandomProvider;
		this.socketProvider = socketProvider;
	}

	/**
	 * This method will be alive until error occur or interrupt was executed.
	 * This method always throws some exception. (not thread safe)
	 * 
	 * @param uri
	 *            uri of websocket
	 * @throws UnknownHostException
	 *             when could not connect to selected host
	 * @throws IOException
	 *             thrown when I/O exception occur
	 */
	public WebSocketConnection create(@Nonnull URI uri,
									  @Nonnull WebSocketListener listener) throws IOException {
		checkNotNull(uri, "Uri cannot be null");

		final Socket socket = socketProvider.getSocket(uri);
		return new WebSocketConnection(socket, listener, uri, secureRandomProvider);
	}




}
