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

/**
 * An interfeca that allows user to send data to socketio server
 * 
 * @author Jacek Marchwicki <jacek.marchwicki@gmail.com>
 * 
 */
public interface SocketWriter {
	/**
	 * Send string message to server
	 * 
	 * @param messageEndpoint message endpoint or empty string
	 * @param message string message
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws NotConnectedException
	 */
	public void sendStringMessage(String messageEndpoint, String message) throws IOException,
			InterruptedException, NotConnectedException;

	/**
	 * Send JSON message to server
	 * 
	 * @param messageEndpoint message endpoint or empty string
	 * @param object json object
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws NotConnectedException
	 */
	public void sendJsonMessage(String messageEndpoint, JSONObject object) throws IOException,
			InterruptedException, NotConnectedException;

	/**
	 * Send event to server
	 * 
	 * @param messageEndpoint message endpoint or empty string
	 * @param name name of event
	 * @param args arguments
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws NotConnectedException
	 */
	public void sendJsonEvent(String messageEndpoint, String name, JSONArray args) throws IOException,
			InterruptedException, NotConnectedException;

	/**
	 * Register to server endpoint
	 * 
	 * @param endpoint server endpoint eg. "/your_endpoint"
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws NotConnectedException
	 */
	void registerToEndpoint(String endpoint) throws IOException,
			InterruptedException, NotConnectedException;
}
