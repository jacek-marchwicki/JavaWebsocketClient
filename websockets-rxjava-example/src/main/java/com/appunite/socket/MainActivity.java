/*
 * Copyright (C) 2015 Jacek Marchwicki <jacek.marchwicki@gmail.com>
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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.appunite.websocket.rx.RxWebSockets;
import com.appunite.websocket.rx.object.GsonObjectSerializer;
import com.appunite.websocket.rx.object.RxObjectWebSockets;
import com.example.Socket;
import com.example.SocketConnection;
import com.example.SocketConnectionImpl;
import com.example.model.Message;
import com.example.model.MessageType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import rx.android.schedulers.AndroidSchedulers;
import rx.android.view.ViewActions;
import rx.android.view.ViewObservable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends FragmentActivity {

	private static final String RETENTION_FRAGMENT_TAG = "retention_fragment_tag";

	private CompositeSubscription subs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final MainPresenter presenter = getRetentionFragment(savedInstanceState).presenter();

		setContentView(R.layout.main_activity);
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
					.subscribe(ViewActions.setEnabled(findViewById(R.id.main_activity_connect_button))),
				presenter.disconnectButtonEnabledObservable()
						.subscribe(ViewActions.setEnabled(findViewById(R.id.macin_activity_disconnect_button))),
				presenter.sendButtonEnabledObservable()
						.subscribe(ViewActions.setEnabled(findViewById(R.id.main_activity_send_button))),
				ViewObservable.clicks(findViewById(R.id.main_activity_connect_button))
						.subscribe(presenter.connectClickObserver()),
				ViewObservable.clicks(findViewById(R.id.macin_activity_disconnect_button))
						.subscribe(presenter.disconnectClickObserver()),
				ViewObservable.clicks(findViewById(R.id.main_activity_send_button))
						.subscribe(presenter.sendClickObserver()));
	}

	private RetentionFragment getRetentionFragment(Bundle savedInstanceState) {
		if (savedInstanceState == null) {
			final RetentionFragment retentionFragment = new RetentionFragment();
			getSupportFragmentManager()
					.beginTransaction()
					.add(retentionFragment, RETENTION_FRAGMENT_TAG)
					.commit();
			return retentionFragment;
		} else {
			return (RetentionFragment) getSupportFragmentManager()
					.findFragmentByTag(RETENTION_FRAGMENT_TAG);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		subs.unsubscribe();
	}

	public static class RetentionFragment extends Fragment {

		private final MainPresenter presenter;

		public RetentionFragment() {
			final Gson gson = new GsonBuilder()
					.registerTypeAdapter(Message.class, new Message.Deserializer())
					.registerTypeAdapter(MessageType.class, new MessageType.SerializerDeserializer())
					.create();

			final OkHttpClient okHttpClient = new OkHttpClient();
			final RxWebSockets webSockets = new RxWebSockets(okHttpClient, new Request.Builder()
					.get()
					.url("ws://coreos2.appunite.net:8080/ws")
					.addHeader("Sec-WebSocket-Protocol", "chat")
					.build());
			final GsonObjectSerializer serializer = new GsonObjectSerializer(gson, Message.class);
			final RxObjectWebSockets jsonWebSockets = new RxObjectWebSockets(webSockets, serializer);
			final SocketConnection socketConnection = new SocketConnectionImpl(jsonWebSockets, Schedulers.io());
			presenter = new MainPresenter(new Socket(socketConnection, Schedulers.io()), Schedulers.io(), AndroidSchedulers.mainThread());
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
		}

		public MainPresenter presenter() {
			return presenter;
		}
	}

}
