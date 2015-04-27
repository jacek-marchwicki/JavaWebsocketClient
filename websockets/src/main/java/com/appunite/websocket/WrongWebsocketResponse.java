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


import javax.annotation.Nonnull;

/**
 * Thrown when unknown response from WebSocket was received
 * 
 * @author Jacek Marchwicki <jacek.marchwicki@gmail.com>
 *
 */
public class WrongWebsocketResponse extends Exception {

	private static final long serialVersionUID = 1L;

	public WrongWebsocketResponse(@Nonnull String msg, @Nonnull Throwable e) {
		super(msg, e);
	}

	public WrongWebsocketResponse(@Nonnull String msg) {
		super(msg);
	}

}
