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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.appunite.websocket.NotConnectedException;
import com.appunite.websocket.WebSocket;
import com.appunite.websocket.WebSocketListener;
import com.appunite.websocket.WrongWebsocketResponse;

public class Main extends Activity implements OnClickListener {


	private static final URI ADDRESS;

	static {
		try {
			ADDRESS = new URI("https://some.websocket.server:443/");
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private View mConnectButton;
	private View mDisconnectButton;
	private View mSendButton;
	private ListView mListView;

	private List<String> mMessages;
	private ArrayAdapter<String> mAdapter;
	private WebSocket webSocket;

	private final WebSocketListener listener = new WebSocketListener() {
        @Override
        public void onConnected() throws IOException, InterruptedException, NotConnectedException {

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    addMessageOnList("connected");
                    mSendButton.setEnabled(true);
                }
            });
        }

        @Override
        public void onStringMessage(final String message) throws IOException, InterruptedException, NotConnectedException {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    addMessageOnList("message received: " + message);
                }
            });
        }

        @Override
        public void onBinaryMessage(byte[] data) throws IOException, InterruptedException, NotConnectedException {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    addMessageOnList("message binary message");
                }
            });
        }

        @Override
        public void onPing(byte[] data) throws IOException, InterruptedException, NotConnectedException {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    addMessageOnList("ping");
                }
            });
        }

        @Override
        public void onPong(byte[] data) throws IOException, InterruptedException, NotConnectedException {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    addMessageOnList("pong");
                }
            });
        }

        @Override
        public void onServerRequestedClose(byte[] data) throws IOException, InterruptedException, NotConnectedException {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    addMessageOnList("server requested close");
                }
            });
        }

        @Override
        public void onUnknownMessage(byte[] data) throws IOException, InterruptedException, NotConnectedException {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    addMessageOnList("unknown message");
                }
            });
        }
    };

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mConnectButton = findViewById(R.id.connect_button);
		mDisconnectButton = findViewById(R.id.disconnect_button);
		mSendButton = findViewById(R.id.send_button);
		mListView = (ListView) findViewById(android.R.id.list);

		mMessages = new ArrayList<>();
		mAdapter = new ArrayAdapter<>(this,
				android.R.layout.simple_list_item_1, mMessages);

		mListView.setAdapter(mAdapter);

		mConnectButton.setOnClickListener(this);
		mDisconnectButton.setOnClickListener(this);
		mSendButton.setOnClickListener(this);

		webSocket = new WebSocket(listener);
	}

	private void connect() {
		mConnectButton.setEnabled(false);
		mDisconnectButton.setEnabled(true);
		addMessageOnList("connecting");

		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						webSocket.connect(ADDRESS);
					} catch (IOException | WrongWebsocketResponse e) {

						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								addMessageOnList("error:" + e.getMessage());
							}
						});
					} catch (InterruptedException e) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								addMessageOnList("disconnected");
								mSendButton.setEnabled(false);
								mConnectButton.setEnabled(true);
								mDisconnectButton.setEnabled(false);
							}
						});
						break;
					}
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							addMessageOnList("connecting");
							mSendButton.setEnabled(false);
							mConnectButton.setEnabled(false);
							mDisconnectButton.setEnabled(true);
						}
					});

				}
			}
		}).start();
	}

	private void send() {
		try {
			addMessageOnList("sending");
			webSocket.sendStringMessage("krowa");
		} catch (IOException | InterruptedException | NotConnectedException ignore) {
		}
	}

	private void disconnect() {
		webSocket.interrupt();
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

}
