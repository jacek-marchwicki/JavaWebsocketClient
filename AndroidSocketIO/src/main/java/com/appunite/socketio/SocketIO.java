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
import java.net.UnknownHostException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Message;
import android.util.Log;
import static com.google.common.base.Preconditions.*;

import com.appunite.websocket.WebSocket;
import com.appunite.websocket.WebSocketListener;
import com.appunite.websocket.WrongWebsocketResponse;
import com.google.common.collect.ImmutableSet;

/**
 * 
 * @author Jacek Marchwicki <jacek.marchwicki@gmail.com>
 *
 */
public class SocketIO extends SocketIOBase implements WebSocketListener,
		SocketWriter {
	private static final String TAG = SocketIO.class.getCanonicalName();
	private static final ImmutableSet<String> DISALLOWED_NAMES = ImmutableSet
			.of("message", "connect", "disconnect", "open", "close", "error",
					"retry", "reconnect");
	private static final int MSG_KILL = 0;
	private WebSocket mWebSocket;
	private ConnectionResult mConnect;
	
	// should be protected by mInterruptionLock
	private boolean mIfSocketStarted = false;

	public SocketIO(String url, SocketListener listener) {
		super(url, listener);
		mWebSocket = new WebSocket(this);
	}

	@Override
	protected void interrupt() {
		super.interrupt();
		
		if (mIfSocketStarted) {
			mWebSocket.interrupt();
			mIfSocketStarted = false;
		}
	}

	void connectToTransport(ConnectionResult connect)
			throws InterruptedException {
		this.mConnect = connect;
		try {
			synchronized (mInterruptionLock) {
				if (mInterrupted)
					throw new InterruptedException();
				mIfSocketStarted = true;
			}
			mWebSocket.connect(connect.socketUri);
		} catch (UnknownHostException e) {
			error(e);
		} catch (IOException e) {
			error(e);
		} catch (WrongWebsocketResponse e) {
			error(e);
		} finally {
			synchronized (mInterruptionLock) {
				mIfSocketStarted = false;
			}
		}
	}

	@Override
	public void onConnected() throws IOException, InterruptedException,
			NotConnectedException {
		mListener.onConnected(this);
	}

	public void registerToEndpoint(String endpoint) throws IOException,
			InterruptedException, NotConnectedException {
		sendMessage(new IOMessage(IOMessage.MSG_CONNECT, "", endpoint));
	}
	
	@SuppressWarnings("StatementWithEmptyBody")
    @Override
	public void onStringMessage(String message) throws IOException,
			InterruptedException, NotConnectedException {
		try {
			if (BuildConfig.DEBUG) {
				Log.v(TAG, "> " + message);
			}
			IOMessage msg = IOMessage.parse(message);
			redelayKill();
            if (msg.mMessageType == IOMessage.MSG_DISCONNECT) {
				// TODO Im not quite sure what we have to do if we will receive
				// message like this - maybe we will implement this later
			} else if (msg.mMessageType == IOMessage.MSG_CONNECT) {
				mListener.onConnectionAccepted(msg.mMessageEndpoint);
			} else if (msg.mMessageType == IOMessage.MSG_HARTBEAT) {
				sendMessage(new IOMessage(IOMessage.MSG_HARTBEAT));
			} else if (msg.mMessageType == IOMessage.MSG_MESSAGE) {
				receivedSocketMessage(msg);
			} else if (msg.mMessageType == IOMessage.MSG_JSON_MESSAGE) {
				receivedSocketJsonMessage(msg);
			} else if (msg.mMessageType == IOMessage.MSG_EVENT) {
				receivedSocketEvent(msg);
			} else if (msg.mMessageType == IOMessage.MSG_ACK) {
			} else if (msg.mMessageType == IOMessage.MSG_ERROR) {
				mListener.onReceiverError(msg.mMessageData);
			} else if (msg.mMessageType == IOMessage.MSG_NOP) {
			} else {
				throw new RuntimeException("Unknown message");
			}
		} catch (WrongSocketIOResponse e) {
			error(e);
		}
	}

	private void receivedSocketJsonMessage(IOMessage msg)
			throws WrongSocketIOResponse, IOException, InterruptedException,
			NotConnectedException {
		if (!msg.mMessageData.isPresent()) {
			throw new WrongSocketIOResponse(
					"message data have to be set for event");
		}
		String jsonString = msg.mMessageData.get();
		JSONObject object;
		try {
			object = new JSONObject(jsonString);
		} catch (JSONException e) {
			throw new WrongSocketIOResponse(
					"Event message should be a valid JSON", e);
		}
		mListener.onReceivedJsonMessage(object);
		// send ack
		sendMessage(new IOMessage(IOMessage.MSG_ACK, msg.mMessageId));
	}

	private void receivedSocketMessage(IOMessage msg) throws IOException,
			InterruptedException, NotConnectedException, WrongSocketIOResponse {
		if (!msg.mMessageData.isPresent()) {
			throw new WrongSocketIOResponse(
					"message data have to be set for event");
		}
		mListener.onReceivedStringMessage(msg.mMessageData.get());
		// send ack
		sendMessage(new IOMessage(IOMessage.MSG_ACK, msg.mMessageId));
	}

	private void receivedSocketEvent(IOMessage msg)
			throws WrongSocketIOResponse, IOException, InterruptedException,
			NotConnectedException {
		if (!msg.mMessageData.isPresent()) {
			throw new WrongSocketIOResponse(
					"message data have to be set for event");
		}
		String jsonString = msg.mMessageData.get();
		JSONObject jsonObject;
		String name;
		JSONArray args;
		try {
			jsonObject = new JSONObject(jsonString);
		} catch (JSONException e) {
			throw new WrongSocketIOResponse(
					"Event message should be a valid JSON", e);
		}
		try {
			name = jsonObject.getString("name");
		} catch (JSONException e) {
			throw new WrongSocketIOResponse(
					"Even should have \"name\" string parameter", e);
		}
		try {
			args = jsonObject.getJSONArray("args");
		} catch (JSONException e) {
			throw new WrongSocketIOResponse(
					"Even should have \"args\" array parameter", e);
		}
		mListener.onReceivedEvent(name, args);
		// send ack
		sendMessage(new IOMessage(IOMessage.MSG_ACK, msg.mMessageId,
				msg.mMessageEndpoint));
	}

	@Override
	public void onServerRequestedClose(byte[] data) {
		// TODO Auto-generated method stub
	}

	@Override
	public void handlerMessage(Message msg) {
		int what = msg.what;
		if (what == MSG_KILL) {
			mListener.onTimeout();
		} else {
			throw new RuntimeException();
		}
	}

	private void redelayKill() {
		if (!mConnect.timeout.isPresent())
			return;
		int delay = mConnect.timeout.get();
		removeMessages(MSG_KILL);
		sendEmptyMessageDelayed(MSG_KILL, delay * 1000);
	}

	private void sendMessage(IOMessage message) throws IOException,
			InterruptedException, NotConnectedException {

		final String msgStr = message.getMessage();
		if (BuildConfig.DEBUG) {
			Log.v(TAG, "< " + msgStr);
		}
		mWebSocket.sendStringMessage(msgStr);
	}

	/**
	 * do not use directly
	 */
	@Override
	public void sendStringMessage(String messageEndpoint, String message) throws IOException,
			InterruptedException, NotConnectedException {
		checkArgument(messageEndpoint != null, "Message endpoint could not be null");
		checkArgument(message != null, "Message could not be null");
		IOMessage msg = new IOMessage(IOMessage.MSG_MESSAGE, "", messageEndpoint, message);
		sendMessage(msg);
	}

	/**
	 * do not use directly
	 */
	@Override
	public void sendJsonMessage(String messageEndpoint, JSONObject object) throws IOException,
			InterruptedException, NotConnectedException {
		checkNotNull(object, "Object could not be null");
        checkNotNull(messageEndpoint, "Message endpoint could not be null");
		IOMessage msg = new IOMessage(IOMessage.MSG_MESSAGE, "", messageEndpoint,
				object.toString());
		sendMessage(msg);
	}

	/**
	 * do not use directly
	 */
	@Override
	public void sendJsonEvent(String messageEndpoint, String name, JSONArray args) throws IOException,
			InterruptedException, NotConnectedException {
        checkNotNull(messageEndpoint, "Message endpoint could not be null");
        checkNotNull(args, "Args could not be null");
        checkNotNull(name, "Name could not be null");
		checkArgument(!DISALLOWED_NAMES.contains(name),
				"Name could not be one of the reserved values: "
						+ DISALLOWED_NAMES.toString());
		JSONObject object = new JSONObject();
		try {
			object.put("name", name);
			object.put("args", args);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		IOMessage msg = new IOMessage(IOMessage.MSG_EVENT, "", messageEndpoint,
				object.toString());
		sendMessage(msg);
	}

	@Override
	public void onBinaryMessage(byte[] data) {
		// not used by socketio
	}

	@Override
	public void onPing(byte[] data) {
		// not used for socketio
	}

	@Override
	public void onPong(byte[] data) {
		// not used for socketio
	}

	@Override
	public void onUnknownMessage(byte[] data) {
		// not used for socketio
	}

}
