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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mockito.Matchers;

import android.test.AndroidTestCase;

import com.appunite.socketio.helpers.HTTPUtils.GetBuilder;

public class SocketIOTest extends AndroidTestCase {
	
	private String mUrl;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		mUrl = new GetBuilder("http://your_server:1000/socket.io/1/")
				.addParam("campaign", "test").addParam("player", "test")
				.build();
	}
	
	public void testPrecondition() {
		assertThat(mUrl, is(notNullValue()));
	}
	
	public void testConnectionDisconnection() throws Exception {
		SocketListener listener = mock(SocketListener.class);
		boolean alive;
		SocketIO socket = new SocketIO(mUrl, listener);
		socket.connect();
		alive = socket.disconnectIfAlive();
		assertThat(alive, is(true));
		
		socket.connect();
		alive = socket.disconnectIfAlive();
		assertThat(alive, is(true));
		
		socket.connect();
		Thread.sleep(100);
		alive = socket.disconnectIfAlive();
		assertThat(alive, is(true));
		
		socket.connect();
		Thread.sleep(500);
		alive = socket.disconnectIfAlive();
		assertThat(alive, is(true));
		
		socket.connect();
		Thread.sleep(1000);
		alive = socket.disconnectIfAlive();
		assertThat(alive, is(true));
		
		socket.connect();
		Thread.sleep(2000);
		alive = socket.disconnectIfAlive();
		assertThat(alive, is(true));
		
		reset(listener);
		Thread.sleep(1000);
		verify(listener, times(0)).onDisconnected(org.mockito.Matchers.anyBoolean());
		verify(listener, times(0)).onConnected(
				org.mockito.Matchers.any(SocketWriter.class));
	}
	
	public void testConnection() throws Exception {
		final SocketListener listener = mock(SocketListener.class);
		SocketIO socket = new SocketIO(mUrl, listener);
		socket.connect();
		verify(listener, timeout(1000 * 10).times(1)).onConnected(
				notNull(SocketWriter.class));
		socket.disconnectIfAlive();
		verify(listener, timeout(1000).times(1)).onDisconnected(true);
		reset(listener);
		Thread.sleep(1000);
		verify(listener, times(0)).onDisconnected(Matchers.anyBoolean());
		verify(listener, times(0)).onConnected(
				org.mockito.Matchers.any(SocketWriter.class));
	}
	
}
