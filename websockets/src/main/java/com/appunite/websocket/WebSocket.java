/*
 * Copyright (C) 2015 Jacek Marchwicki <jacek.marchwicki@gmail.com>
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

import org.apache.http.Header;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.annotation.Nonnull;

import static com.appunite.websocket.tools.Preconditions.checkNotNull;
import static com.appunite.websocket.tools.Preconditions.checkState;

/**
 * Allows user to create to websocket
 * 
 * @author Jacek Marchwicki <jacek.marchwicki@gmail.com>
 * 
 */
public class WebSocket {

	@Nonnull
	private final WebSocketListener listener;
	@Nonnull
	private final NewWebSocket newWebSocket;
	@Nonnull
	private final Object connectLock = new Object(); // 1
	private WebSocketConnection connect;

	/**
	 * Create instance of WebSocket
	 * 
	 * @param listener
	 *            where all calls will be thrown
	 * @see NewWebSocket
	 */
	@Deprecated
	public WebSocket(@Nonnull WebSocketListener listener) {
		checkNotNull(listener, "Lister cannot be null");
		this.listener = listener;
		newWebSocket = new NewWebSocket();

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
	 * @throws WrongWebsocketResponse
	 *             thrown when wrong web socket response received
	 * @throws InterruptedException
	 *             thrown when interrupt method was invoked
	 * @see NewWebSocket#create(URI, WebSocketListener)
	 * @see WebSocketConnection#connect()
	 */
	@Deprecated
	public void connect(@Nonnull URI uri) throws IOException,
			WrongWebsocketResponse, InterruptedException {
		checkNotNull(uri, "Uri cannot be null");
		WebSocketConnection connect;
		synchronized (connectLock) {
			checkState(this.connect == null);
			final ArrayList<String> subProtocols = new ArrayList<>(1);
			subProtocols.add("chat");
			connect = newWebSocket.create(uri,
					subProtocols,
					new ArrayList<Header>(),
					listener);
			this.connect = connect;
		}
		connect.connect();
	}

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
	 * @see WebSocketConnection#sendByteMessage(byte[])
	 */
	@SuppressWarnings("UnusedDeclaration")
	@Deprecated
    public void sendByteMessage(@Nonnull byte[] buffer) throws IOException,
			InterruptedException, NotConnectedException {
		synchronized (connectLock) {
			if (connect == null) {
				throw new NotConnectedException();
			}
			connect.sendByteMessage(buffer);
		}
	}

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
	 * @see WebSocketConnection#sendPingMessage(byte[])
     */
    @SuppressWarnings("UnusedDeclaration")
	@Deprecated
    public void sendPingMessage(@Nonnull byte[] buffer) throws IOException,
            InterruptedException, NotConnectedException {
		synchronized (connectLock) {
			if (connect == null) {
				throw new NotConnectedException();
			}
			connect.sendPingMessage(buffer);
		}
    }

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
	 * @see WebSocketConnection#sendStringMessage(String)
	 */
	@Deprecated
	public void sendStringMessage(@Nonnull String message) throws IOException,
			InterruptedException, NotConnectedException {
		synchronized (connectLock) {
			if (connect == null) {
				throw new NotConnectedException();
			}
			connect.sendStringMessage(message);
		}
	}


	/**
	 * Interrupt connect method. After interruption connect should return
	 * InterruptedException (thread safe)
	 * @see WebSocketConnection#interrupt()
	 */
	@Deprecated
	public void interrupt() {
		synchronized (connectLock) {
			while (connect == null) {
				try {
					connectLock.wait();
				} catch (InterruptedException ignore) {
				}
			}
			connect.interrupt();
			connect = null;
		}
	}
}
