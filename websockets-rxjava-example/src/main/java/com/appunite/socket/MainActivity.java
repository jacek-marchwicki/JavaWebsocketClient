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
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;

import com.appunite.websocket.NewWebSocket;
import com.appunite.websocket.rx.RxWebSockets;
import com.appunite.websocket.rx.json.RxJsonWebSockets;
import com.example.Socket;
import com.example.SocketConnection;
import com.example.SocketConnectionImpl;
import com.example.model.Message;
import com.example.model.MessageType;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.android.view.ViewActions;
import rx.android.view.ViewObservable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends Activity {

	private static final URI ADDRESS;

	static {
		try {
			ADDRESS = new URI("ws://192.168.0.112:8080/ws");
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private CompositeSubscription subs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Gson gson = new GsonBuilder()
				.registerTypeAdapter(Message.class, new Message.Deserializer())
				.registerTypeAdapter(MessageType.class, new MessageType.SerializerDeserializer())
				.create();

		final NewWebSocket newWebSocket = new NewWebSocket();
		final RxWebSockets webSockets = new RxWebSockets(newWebSocket, ADDRESS);
		final RxJsonWebSockets jsonWebSockets = new RxJsonWebSockets(webSockets, gson, Message.class);
		final SocketConnection socketConnection = new SocketConnectionImpl(jsonWebSockets, Schedulers.io());
		final MainPresenter presenter = new MainPresenter(new Socket(socketConnection, Schedulers.io()), Schedulers.io(), AndroidSchedulers.mainThread());


		setContentView(R.layout.main);
		final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.main_activity_recycler_view);
		final LinearLayoutManager layout = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
		recyclerView.setLayoutManager(layout);
		final MainAdapter adapter = new MainAdapter();
		recyclerView.setAdapter(adapter);
		recyclerView.setItemAnimator(new DefaultItemAnimator());

		final BehaviorSubject<Boolean> isLastSubject = BehaviorSubject.create(true);

		recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
			boolean manualScrolling = false;

			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
					manualScrolling = true;
				}
				if (manualScrolling && newState == RecyclerView.SCROLL_STATE_IDLE) {
					manualScrolling = false;

					final int lastVisibleItemPosition = layout.findLastVisibleItemPosition();
					final int previousItemsCount = adapter.getItemCount();
					final boolean isLast = previousItemsCount - 1 == lastVisibleItemPosition;
					isLastSubject.onNext(isLast);
				}
			}
		});


		subs = new CompositeSubscription(
				isLastSubject.subscribe(presenter.lastItemInViewObserver()),
				presenter.itemsWithScrollObservable()
						.subscribe(new Action1<MainPresenter.ItemsWithScroll>() {
							@Override
							public void call(final MainPresenter.ItemsWithScroll itemsWithScroll) {
								adapter.call(itemsWithScroll.items());
								if (itemsWithScroll.shouldScroll()) {
									recyclerView.post(new Runnable() {
										@Override
										public void run() {
											recyclerView.smoothScrollToPosition(itemsWithScroll.scrollToPosition());
										}
									});
								}
							}
						}),
				presenter.connectButtonEnabledObservable()
					.subscribe(ViewActions.setEnabled(findViewById(R.id.connect_button))),
				presenter.disconnectButtonEnabledObservable()
						.subscribe(ViewActions.setEnabled(findViewById(R.id.disconnect_button))),
				presenter.sendButtonEnabledObservable()
						.subscribe(ViewActions.setEnabled(findViewById(R.id.send_button))),
				ViewObservable.clicks(findViewById(R.id.connect_button))
						.subscribe(presenter.connectClickObserver()),
				ViewObservable.clicks(findViewById(R.id.disconnect_button))
						.subscribe(presenter.disconnectClickObserver()),
				ViewObservable.clicks(findViewById(R.id.send_button))
						.subscribe(presenter.sendClickObserver()));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		subs.unsubscribe();
	}

}
