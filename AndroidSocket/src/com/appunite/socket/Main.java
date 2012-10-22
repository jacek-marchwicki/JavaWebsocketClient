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

package com.appunite.socket;

import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.appunite.socket.R;
import com.appunite.socketio.NotConnectedException;
import com.appunite.socketio.SocketIO;
import com.appunite.socketio.SocketListener;
import com.appunite.socketio.SocketWriter;
import com.appunite.socketio.WrongSocketIOResponse;
import com.appunite.socketio.helpers.HTTPUtils.GetBuilder;
import com.appunite.socketio.helpers.HTTPUtils.WrongHttpResponseCode;
import com.appunite.websocket.WrongWebsocketResponse;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class Main extends Activity implements OnClickListener, SocketListener {

	private View mConnectButton;
	private View mDisconnectButton;
	private View mSendButton;
	private ListView mListView;

	private List<String> mMessages;
	private ArrayAdapter<String> mAdapter;

	private SocketIO mSocketIO;

	private SocketWriter mSocketWriter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mConnectButton = findViewById(R.id.connect_button);
		mDisconnectButton = findViewById(R.id.disconnect_button);
		mSendButton = findViewById(R.id.send_button);
		mListView = (ListView) findViewById(android.R.id.list);

		mMessages = Lists.newArrayList();
		mAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, mMessages);

		mListView.setAdapter(mAdapter);

		mConnectButton.setOnClickListener(this);
		mDisconnectButton.setOnClickListener(this);
		mSendButton.setOnClickListener(this);

		String url = new GetBuilder("http://your_server:1000/socket.io/1/")
				.addParam("campaign", "test").addParam("player", "test")
				.build();
		mSocketIO = new SocketIO(url, this);
	}

	private void connect() {
		mConnectButton.setEnabled(false);
		mDisconnectButton.setEnabled(true);
		addMessageOnList("connecting");
		mSocketIO.connect();
	}

	private void send() {
		try {
			addMessageOnList("sending");
			mSocketWriter.sendStringMessage("/hunt", "krowa");
		} catch (IOException e) {
		} catch (InterruptedException e) {
		} catch (NotConnectedException e) {
		}
	}

	@Override
	public void onConnected(SocketWriter socketWriter) throws IOException,
			InterruptedException, NotConnectedException {
		mSocketWriter = socketWriter;
		socketWriter.registerToEndpoint("/hunt");
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				addMessageOnList("connected");
			}
		});
	}

	@Override
	public void onReceivedEvent(final String name, final JSONArray args)
			throws IOException, InterruptedException, NotConnectedException {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {

				try {
					addMessageOnList("event received: " + name + ", args: "
							+ args.toString(1));
				} catch (JSONException e) {
					throw new RuntimeException();
				}
			}
		});
	}

	@Override
	public void onReceivedJsonMessage(final JSONObject object)
			throws IOException, InterruptedException, NotConnectedException {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				try {
					addMessageOnList("message received: " + object.toString(1));
				} catch (JSONException e) {
					throw new RuntimeException();
				}
			}
		});
	}

	@Override
	public void onReceivedStringMessage(final String string)
			throws IOException, InterruptedException, NotConnectedException {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				addMessageOnList("string received: " + string);
			}
		});
	}

	@Override
	public void onReceiverError(final Optional<String> messageData)
			throws IOException, InterruptedException, NotConnectedException {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (messageData.isPresent()) {
					addMessageOnList("error received: " + messageData.get());
				} else {
					addMessageOnList("empty-error received");
				}
			}
		});
	}

	private void disconnect() {
		mSocketIO.disconnectIfAlive();
	}

	private void addMessageOnList(String msg) {
		mMessages.add(msg);
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.connect_button:
			connect();
			break;
		case R.id.disconnect_button:
			disconnect();
			break;
		case R.id.send_button:
			send();
			break;
		default:
			throw new RuntimeException();
		}
	}

	@Override
	public boolean onDisconnected(final boolean interrupted) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				addMessageOnList("disconnected");
				if (interrupted) {
					mConnectButton.setEnabled(true);
					mDisconnectButton.setEnabled(false);
				}
				mSendButton.setEnabled(false);
			}
		});
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {}
		return !interrupted;
	}
	
	private void onSocketError(final Exception e) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				addMessageOnList("error:" + e.getMessage());
			}
		});
	}

	@Override
	public void onNetworkError(WrongSocketIOResponse e) {
		onSocketError(e);
	}

	@Override
	public void onNetworkError(IOException e) {
		onSocketError(e);
	}

	@Override
	public void onNetworkError(WrongHttpResponseCode e) {
		onSocketError(e);
	}

	@Override
	public void onNetworkError(WrongWebsocketResponse e) {
		onSocketError(e);
	}

	@Override
	public void onTimeout() {
		mSocketIO.disconnectIfAlive();
		addMessageOnList("connecting");
		mConnectButton.setEnabled(false);
		mDisconnectButton.setEnabled(true);
		mSocketIO.connect();
	}

	@Override
	public void onConnectionAccepted(String endpoint) throws IOException,
			InterruptedException, NotConnectedException {
		if (!"/hunt".equals(endpoint))
			return;
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				addMessageOnList("established");
				mSendButton.setEnabled(true);
			}
		});
	}

}
