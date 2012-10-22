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

package com.appunite.socketio;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import com.appunite.socketio.helpers.HTTPUtils.WrongHttpResponseCode;
import com.appunite.websocket.WrongWebsocketResponse;
import com.google.common.base.Optional;

/**
 * 
 * @author Jacek Marchwicki <jacek.marchwicki@gmail.com>
 * 
 */
public interface SocketListener {

	/**
	 * Occurs when connection was established. Called on Socket thread
	 * 
	 * @param socketWriter
	 * @throws IOException
	 *             can be thrown when socket exception occur
	 * @throws InterruptedException
	 *             can be thrown when interrupt occur
	 * @throws NotConnectedException
	 *             can be thrown when not connected to socket
	 */
	public void onConnected(SocketWriter socketWriter) throws IOException,
			InterruptedException, NotConnectedException;

	/**
	 * Occurs when connection on given endpoint was established. Called on Socket
	 * thread
	 * 
	 * @param endpoint
	 *            endpoint on which connection was established
	 * @throws IOException
	 *             can be thrown when socket exception occur
	 * @throws InterruptedException
	 *             can be thrown when interrupt occur
	 * @throws NotConnectedException
	 *             can be thrown when not connected to socket
	 */
	public void onConnectionAccepted(String endpoint) throws IOException,
			InterruptedException, NotConnectedException;

	/**
	 * Occurs when socket was disconnected. After this method was called every
	 * try to call some {@link SocketWriter} method will cause
	 * {@link NotConnectedException}. This method is called even if error occur.
	 * 
	 * Called on socket caller thread
	 * 
	 * @param interrupted
	 *            true if disconnection was requested by user
	 * @return true if socket should reconnect (this value should be false if
	 *         disconnection was requested by user)
	 */
	public boolean onDisconnected(boolean interrupted);

	/**
	 * Occurs when received event from socket
	 * 
	 * Called on socket thread.
	 * 
	 * @param name
	 *            event name
	 * @param args
	 *            event json arguments
	 * @throws IOException
	 *             can be thrown when socket exception occur
	 * @throws InterruptedException
	 *             can be thrown when interrupt occur
	 * @throws NotConnectedException
	 *             can be thrown when not connected to socket
	 */
	public void onReceivedEvent(String name, JSONArray args)
			throws IOException, InterruptedException, NotConnectedException;

	/**
	 * Occurs when received json message
	 * 
	 * Called on socket thread.
	 * 
	 * @param object
	 * @throws IOException
	 *             can be thrown when socket exception occur
	 * @throws InterruptedException
	 *             can be thrown when interrupt occur
	 * @throws NotConnectedException
	 *             can be thrown when not connected to socket
	 */
	public void onReceivedJsonMessage(JSONObject object) throws IOException,
			InterruptedException, NotConnectedException;

	/**
	 * Occurs when received message from socket
	 * 
	 * Called on socket thread.
	 * 
	 * @param string
	 * @throws IOException
	 *             can be thrown when socket exception occur
	 * @throws InterruptedException
	 *             can be thrown when interrupt occur
	 * @throws NotConnectedException
	 *             can be thrown when not connected to socket
	 */
	public void onReceivedStringMessage(String string) throws IOException,
			InterruptedException, NotConnectedException;

	/**
	 * Occurs when error was sent through socket. Client will not be
	 * disconnected.
	 * 
	 * Called on socket thread.
	 * 
	 * @param messageData
	 * @throws IOException
	 *             can be thrown when socket exception occur
	 * @throws InterruptedException
	 *             can be thrown when interrupt occur
	 * @throws NotConnectedException
	 *             can be thrown when not connected to socket
	 */
	public void onReceiverError(Optional<String> messageData)
			throws IOException, InterruptedException, NotConnectedException;

	/**
	 * {@link WrongSocketIOResponse} exception occurs client would be
	 * disconnected
	 * 
	 * @param exception
	 */
	public void onNetworkError(WrongSocketIOResponse e);

	/**
	 * {@link IOException} exception occurs client would be disconnected
	 * 
	 * @param exception
	 */
	public void onNetworkError(IOException e);

	/**
	 * {@link WrongWebsocketResponse} exception occurs client would be
	 * disconnected
	 * 
	 * @param exception
	 */
	public void onNetworkError(WrongHttpResponseCode e);

	/**
	 * {@link WrongWebsocketResponse} exception occurs client would be
	 * disconnected
	 * 
	 * @param exception
	 */
	public void onNetworkError(WrongWebsocketResponse exception);

	/**
	 * Timeout occur - probably you should disconnect and connect again This
	 * method is called on same thread as "connect()" was called
	 */
	public void onTimeout();

}
