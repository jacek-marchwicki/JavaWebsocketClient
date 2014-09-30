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

import java.io.IOException;

import com.appunite.socketio.NotConnectedException;

/**
 * Listener to get websocket responses
 * 
 * @author Jacek Marchwicki <jacek.marchwicki@gmail.com>
 *
 */
public interface WebSocketListener {

	/**
	 * On connected to websocket
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws NotConnectedException
	 */
	public void onConnected() throws IOException, InterruptedException,
			NotConnectedException;

	/**
	 * On received string message
	 * @param message string message
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws NotConnectedException
	 */
    public void onStringMessage(String message) throws IOException,
			InterruptedException, NotConnectedException;

	/**
	 * On received binary data
	 * @param data binary data
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws NotConnectedException
	 */
    public void onBinaryMessage(byte[] data) throws IOException,
			InterruptedException, NotConnectedException;

	/**
	 * On received ping 
	 * @param data binary data
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws NotConnectedException
	 */
    public void onPing(byte[] data) throws IOException, InterruptedException,
			NotConnectedException;

	/**
	 * On received pong
	 * @param data binary data
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws NotConnectedException
	 */
    public void onPong(byte[] data) throws IOException, InterruptedException,
			NotConnectedException;

	/**
	 * On received request close from server
	 * @param data binary data
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws NotConnectedException
	 */
    public void onServerRequestedClose(byte[] data) throws IOException,
			InterruptedException, NotConnectedException;

	/**
	 * On received unknown message
	 * @param data binary data
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws NotConnectedException
	 */
    public void onUnknownMessage(byte[] data) throws IOException,
			InterruptedException, NotConnectedException;

}
