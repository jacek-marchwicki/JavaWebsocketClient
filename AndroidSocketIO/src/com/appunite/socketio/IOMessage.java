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

import static com.google.common.base.Preconditions.checkArgument;


import com.google.common.base.Optional;

/**
 * Socket IO message parser
 * 
 * @author Jacek Marchwicki <jacek.marchwicki@gmail.com>
 *
 */
class IOMessage {
	int mMessageType;
	String mMessageId;
	String mMessageEndpoint;
	Optional<String> mMessageData;
	
	public static final int MSG_DISCONNECT = 0;
	public static final int MSG_CONNECT = 1;
	public static final int MSG_HARTBEAT = 2;
	public static final int MSG_MESSAGE = 3;
	public static final int MSG_JSON_MESSAGE = 4;
	public static final int MSG_EVENT = 5;
	public static final int MSG_ACK = 6;
	public static final int MSG_ERROR = 7;
	public static final int MSG_NOP = 8;
	
	private IOMessage() {
	}
	
	public IOMessage(int messageType) {
		this(messageType, "");
	}
	
	public IOMessage(int messageType, String messageId) {
		this(messageType, messageId, "");
	}
	
	public IOMessage(int messageType, String messageId, String messageEndpoint) {
		checkArgument(MSG_DISCONNECT >= 0 && messageType <= MSG_NOP, "Unknown message type");
		checkArgument(messageId != null, "message could not be null");
		checkArgument(messageEndpoint != null, "message endpoint could not be null");
		this.mMessageType = messageType;
		this.mMessageId = messageId;
		this.mMessageEndpoint = messageEndpoint;
		this.mMessageData = Optional.absent();
	}
	
	public IOMessage(int messageType, String messageId, String messageEndpoint, String messageData) {
		checkArgument(MSG_DISCONNECT >= 0 && messageType <= MSG_NOP, "Unknown message type");
		checkArgument(messageId != null, "message could not be null");
		checkArgument(messageEndpoint != null, "message endpoint could not be null");
		checkArgument(messageData != null, "messag could not be null");
		this.mMessageType = messageType;
		this.mMessageId = messageId;
		this.mMessageEndpoint = messageEndpoint;
		this.mMessageData = Optional.of(messageData);
	}
	
	@Override
	public String toString() {
		return new StringBuilder().append("Type: ").append(mMessageType)
				.append(", Id: ").append(mMessageId).append(", Endpoint: ")
				.append(mMessageEndpoint).append(", Data: ")
				.append(mMessageData).toString();
	}

	public String getMessage() {
		return new StringBuilder().append(mMessageType).append(':')
				.append(mMessageId)
				.append(':').append(mMessageEndpoint).append(':')
				.append(mMessageData.isPresent() ? mMessageData.get() : "")
				.toString();
	}

	public static IOMessage parse(String message) throws WrongSocketIOResponse {
		checkArgument(message != null, "Message should not be null");
		int messageTypeEnd = message.indexOf(':');
		if (messageTypeEnd < 1) {
			throw new WrongSocketIOResponse("Wrong response from socket io");
		}
		String messageTypeStr = message.substring(0, messageTypeEnd);
		IOMessage ioMessage = new IOMessage();
		try {
			ioMessage.mMessageType = Integer.parseInt(messageTypeStr);
		} catch (NumberFormatException e) {
			throw new WrongSocketIOResponse("Wrong response from socket io", e);
		}
		if (ioMessage.mMessageType < MSG_DISCONNECT
				|| ioMessage.mMessageType > MSG_NOP) {
			throw new WrongSocketIOResponse("Unknown message type");
		}
		int messageIdEnd = message.indexOf(':', messageTypeEnd+1);
		if (messageIdEnd < 0) {
			ioMessage.mMessageId = message.substring(messageTypeEnd+1);
			ioMessage.mMessageEndpoint = "";
			ioMessage.mMessageData = Optional.absent();
			return ioMessage;
		}
		ioMessage.mMessageId = message.substring(messageTypeEnd+1,
				messageIdEnd);

		int messageEndpointEnd = message.indexOf(':', messageIdEnd+1);
		if (messageEndpointEnd < 0) {
			ioMessage.mMessageEndpoint = message.substring(messageIdEnd+1);
			ioMessage.mMessageData = Optional.absent();
			return ioMessage;
		}
		ioMessage.mMessageEndpoint = message.substring(messageIdEnd+1,
				messageEndpointEnd);
		ioMessage.mMessageData = Optional.of(message
				.substring(messageEndpointEnd+1));
		return ioMessage;
	}
	
}
