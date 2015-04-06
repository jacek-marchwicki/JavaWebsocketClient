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

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Helper to read data from WebSocket
 * 
 * @author Jacek Marchwicki <jacek.marchwicki@gmail.com>
 * 
 */
class WebSocketReader {
	private final DataInputStream inputStream;

	public WebSocketReader(InputStream inputStream) {
		this.inputStream = new DataInputStream(inputStream);
	}

	public int readByteOrThrow() throws IOException {
		int read = inputStream.read();
		if (read == -1) {
			throw new EOFException("Socket closed");
		}
		return read;
	}

	public void readBytesOrThrow(byte[] buffer) throws IOException,
			WrongWebsocketResponse {
		readBytesOrThrow(buffer, buffer.length);
	}

	public void readBytesOrThrow(byte[] buffer, int length) throws IOException,
			WrongWebsocketResponse {
		int offset = 0;
		while (length > 0) {
			int read = inputStream.read(buffer, offset, length);
			if (read == -1) {
				// maybe return closed connection exception
				throw new WrongWebsocketResponse("Socket closed");
			}
			offset += read;
			length -= read;
		}
	}

	public long read64Long() throws IOException, WrongWebsocketResponse {
		return inputStream.readLong();
	}

	public long read16Int() throws IOException, WrongWebsocketResponse {
		return inputStream.readUnsignedShort();
	}

	public String readLine() throws IOException, WrongWebsocketResponse {
		StringBuilder string = new StringBuilder();
		for (;;) {
			int read = inputStream.read();
			if (read == -1) {
				throw new WrongWebsocketResponse("Empty response from server");
			} else if (read == '\n') {
				return string.toString();
			} else if (read != '\r') {
				string.append((char) read);
			}
		}
	}
}
