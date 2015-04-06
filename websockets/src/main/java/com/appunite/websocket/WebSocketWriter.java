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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.util.EncodingUtils;

/**
 * Helper to write data to websocket
 * 
 * @author Jacek Marchwicki <jacek.marchwicki@gmail.com>
 * 
 */
class WebSocketWriter {
	private final DataOutputStream mOutputStream;

	public WebSocketWriter(OutputStream outputStream) {
		this.mOutputStream = new DataOutputStream(outputStream);
	}

	private static byte[] NEW_LINE = new byte[] { '\r', '\n' };

	public void writeNewLine() throws IOException {
		mOutputStream.write(NEW_LINE);
	}

	public void writeLine(String line) throws IOException {
		mOutputStream.write(EncodingUtils.getAsciiBytes(line));
		writeNewLine();
	}

	public void flush() throws IOException {
		mOutputStream.flush();
	}

	public void writeByte(int oneByte) throws IOException {
		mOutputStream.write(oneByte);
	}

	public void writeLong64(long length) throws IOException {
		mOutputStream.writeLong(length);
	}

	public void writeInt16(int length) throws IOException {
		mOutputStream.writeShort(length);
	}

	public void writeBytes(byte[] buffer) throws IOException {
		mOutputStream.write(buffer);
	}
}
