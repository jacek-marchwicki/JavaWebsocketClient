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

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.appunite.websocket.NewWebSocket;
import com.appunite.websocket.rx.RxWebSockets;
import com.appunite.websocket.rx.json.RxJsonWebSockets;
import com.appunite.websocket.rx.json.messages.RxJsonEvent;
import com.appunite.websocket.rx.json.messages.RxJsonEventConn;
import com.appunite.websocket.rx.json.messages.RxJsonEventDisconnected;
import com.appunite.websocket.rx.json.messages.RxJsonEventMessage;
import com.appunite.websocket.rx.json.messages.RxJsonEventWrongMessageFormat;
import com.example.Socket;
import com.example.SocketConnection;
import com.example.SocketConnectionImpl;
import com.example.model.DataMessage;
import com.example.model.Message;
import com.example.model.MessageType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.plugins.RxJavaErrorHandler;
import rx.plugins.RxJavaPlugins;
import rx.schedulers.Schedulers;

public class Main extends Activity implements OnClickListener {

	private static final URI ADDRESS;

	static {
		try {
			ADDRESS = new URI("ws://192.168.0.123:8080/ws");
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		RxJavaPlugins.getInstance().registerErrorHandler(new RxJavaErrorHandler() {
			@Override
			public void handleError(Throwable e) {
				Log.e("RxJava", "Error", e);
				super.handleError(e);
			}
		});
	}

	private View connectButton;
	private View disconnectButton;
	private View sendButton;

	private List<String> messages;
	private ArrayAdapter<String> adapter;

	private Socket socket;
	private Subscription connectionSubscription;
	private Subscription statusSubscription;
	private Subscription messageSubscription;
	private DateFormat timeInstance = DateFormat.getTimeInstance(DateFormat.MEDIUM);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final Gson gson = new GsonBuilder()
				.registerTypeAdapter(Message.class, new Message.Deserializer())
				.registerTypeAdapter(MessageType.class, new MessageType.SerializerDeserializer())
				.create();

		final NewWebSocket newWebSocket = new NewWebSocket();
		final RxWebSockets webSockets = new RxWebSockets(newWebSocket, ADDRESS);
		final RxJsonWebSockets jsonWebSockets = new RxJsonWebSockets(webSockets, gson, Message.class);
		final SocketConnection socketConnection = new SocketConnectionImpl(jsonWebSockets, Schedulers.io());
		socket = new Socket(socketConnection, Schedulers.io());
		messageSubscription = socket.events()
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Action1<RxJsonEvent>() {
					@Override
					public void call(RxJsonEvent rxJsonEvent) {
						if (rxJsonEvent instanceof RxJsonEventMessage) {
							addMessageOnList("message: " + ((RxJsonEventMessage) rxJsonEvent).message().toString());
						} else if (rxJsonEvent instanceof RxJsonEventWrongMessageFormat) {
							final RxJsonEventWrongMessageFormat wrongMessageFormat = (RxJsonEventWrongMessageFormat) rxJsonEvent;
							addMessageOnList("could not parse message: " + wrongMessageFormat.message()
									+ ", " + wrongMessageFormat.exception().toString());
						} else if (rxJsonEvent instanceof RxJsonEventDisconnected) {
							//noinspection ThrowableResultOfMethodCallIgnored
							final Exception exception = ((RxJsonEventDisconnected) rxJsonEvent).exception();
							if (!(exception instanceof InterruptedException)) {
								addMessageOnList("error:" + exception.toString());
							}
						}
					}
				});
		statusSubscription = socket.connectedAndRegistered()
				.map(new Func1<RxJsonEventConn, Boolean>() {
					@Override
					public Boolean call(RxJsonEventConn rxJsonEventConn) {
						return rxJsonEventConn != null;
					}
				})
				.subscribeOn(Schedulers.io())
				.distinctUntilChanged()
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Action1<Boolean>() {
					@Override
					public void call(Boolean connected) {
						addMessageOnList(connected ? "connected" : "disconnected");
						sendButton.setEnabled(connected);
					}
				});

		connectButton = findViewById(R.id.connect_button);
		disconnectButton = findViewById(R.id.disconnect_button);
		sendButton = findViewById(R.id.send_button);
		final ListView listView = (ListView) findViewById(android.R.id.list);

		messages = new ArrayList<>();
		adapter = new ArrayAdapter<>(this,
				android.R.layout.simple_list_item_1, messages);

		listView.setAdapter(adapter);

		connectButton.setOnClickListener(this);
		disconnectButton.setOnClickListener(this);
		sendButton.setOnClickListener(this);
		connectButton.setEnabled(true);
		disconnectButton.setEnabled(false);

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		statusSubscription.unsubscribe();
		if (connectionSubscription != null) {
			connectionSubscription.unsubscribe();
		}
		messageSubscription.unsubscribe();
	}

	private void connect() {
		connectButton.setEnabled(false);
		disconnectButton.setEnabled(true);
		addMessageOnList("connecting");
		if (connectionSubscription != null) {
			throw new IllegalStateException();
		}
		connectionSubscription = socket.connection()
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe();
	}

	private void send() {
		addMessageOnList("sending");
		socket.sendMessageOnceWhenConnected(new Func1<String, Observable<Object>>() {
			@Override
			public Observable<Object> call(String id) {
				return Observable.<Object>just(new DataMessage(id, "krowa"));
			}
		})
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Observer<DataMessage>() {
					@Override
					public void onCompleted() {}

					@Override
					public void onError(Throwable e) {
						addMessageOnList("sending error: " + e.toString());

					}

					@Override
					public void onNext(DataMessage dataMessage) {
						addMessageOnList("sending response: " + dataMessage.toString());
					}
				});
	}

	private void disconnect() {
		connectButton.setEnabled(true);
		disconnectButton.setEnabled(false);
		connectionSubscription.unsubscribe();
		connectionSubscription = null;
	}

	private void addMessageOnList(String msg) {
		messages.add(timeInstance.format(new Date()) + ": " + msg);
		adapter.notifyDataSetChanged();
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

}
