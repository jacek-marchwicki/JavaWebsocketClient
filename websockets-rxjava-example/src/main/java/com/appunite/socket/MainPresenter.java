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

import android.util.Pair;

import com.appunite.detector.SimpleDetector;
import com.appunite.websocket.rx.object.messages.RxObjectEvent;
import com.appunite.websocket.rx.object.messages.RxObjectEventConn;
import com.appunite.websocket.rx.object.messages.RxObjectEventDisconnected;
import com.appunite.websocket.rx.object.messages.RxObjectEventMessage;
import com.appunite.websocket.rx.object.messages.RxObjectEventWrongMessageFormat;
import com.appunite.websocket.rx.object.messages.RxObjectEventWrongStringMessageFormat;
import com.example.Socket;
import com.example.model.DataMessage;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observers.Observers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public class MainPresenter {

    private final BehaviorSubject<ImmutableList<AdapterItem>> items;
    private final Observable<Boolean> connected;
    private final BehaviorSubject<Boolean> requestConnection = BehaviorSubject.create();
    private final PublishSubject<Object> connectClick = PublishSubject.create();
    private final PublishSubject<Object> disconnectClick = PublishSubject.create();
    private final PublishSubject<Object> sendClick = PublishSubject.create();
    private final BehaviorSubject<Boolean> lastItemInView = BehaviorSubject.create();
    private final PublishSubject<AdapterItem> addItem = PublishSubject.create();

    public MainPresenter(@Nonnull final Socket socket,
                         @Nonnull final Scheduler networkScheduler,
                         @Nonnull final Scheduler uiScheduler) {
        items = BehaviorSubject.create();

        Observable.merge(connectClick.map(funcTrue()), disconnectClick.map(funcFalse()))
                .startWith(false)
                .subscribe(requestConnection);

        sendClick
                .flatMap(flatMapClicksToSendMessageAndResult(socket))
                .map(mapDataMessageOrErrorToPair())
                .map(mapPairToNewAdapterItem())
                .subscribeOn(networkScheduler)
                .observeOn(uiScheduler)
                .subscribe(addItem);

        requestConnection
                .subscribe(new Action1<Boolean>() {

                    private Subscription subscribe;

                    @Override
                    public void call(Boolean requestConnection) {
                        if (requestConnection) {
                            if (subscribe == null) {
                                subscribe = socket
                                        .connection()
                                        .subscribeOn(networkScheduler)
                                        .observeOn(uiScheduler)
                                        .subscribe();
                            }
                        } else {
                            if (subscribe != null) {
                                subscribe.unsubscribe();
                                subscribe = null;
                            }
                        }
                    }
                });

        requestConnection
                .map(mapConnectingStatusToString())
                .map(mapStringToNewAdapterItem())
                .subscribe(addItem);

        addItem
                .scan(ImmutableList.<AdapterItem>of(), new Func2<ImmutableList<AdapterItem>, AdapterItem, ImmutableList<AdapterItem>>() {
                    @Override
                    public ImmutableList<AdapterItem> call(ImmutableList<AdapterItem> adapterItems, AdapterItem adapterItem) {
                        return ImmutableList.<AdapterItem>builder().addAll(adapterItems).add(adapterItem).build();
                    }
                })
                .subscribe(items);

        socket.events()
                .subscribeOn(networkScheduler)
                .observeOn(uiScheduler)
                .lift(liftRxJsonEventToPairMessage())
                .map(mapPairToNewAdapterItem())
                .subscribe(addItem);

        connected = socket.connectedAndRegistered()
                .map(new Func1<RxObjectEventConn, Boolean>() {
                    @Override
                    public Boolean call(RxObjectEventConn rxJsonEventConn) {
                        return rxJsonEventConn != null;
                    }
                })
                .distinctUntilChanged()
                .subscribeOn(networkScheduler)
                .observeOn(uiScheduler);

        connected
                .map(mapConnectedStatusToString())
                .map(mapStringToNewAdapterItem())
                .subscribe(addItem);
    }

    @Nonnull
    public Observable<ItemsWithScroll> itemsWithScrollObservable() {
        return Observable.combineLatest(items, lastItemInView, new Func2<ImmutableList<MainPresenter.AdapterItem>, Boolean, ItemsWithScroll>() {
            @Override
            public ItemsWithScroll call(ImmutableList<MainPresenter.AdapterItem> adapterItems, Boolean isLastItemInList) {
                final int lastItemPosition = adapterItems.size() - 1;
                final boolean shouldScroll = isLastItemInList && lastItemPosition >= 0;
                return new ItemsWithScroll(adapterItems, shouldScroll, lastItemPosition);
            }
        });
    }

    public Observer<Boolean> lastItemInViewObserver() {
        return lastItemInView;
    }

    private Func1<Boolean, String> mapConnectedStatusToString() {
        return new Func1<Boolean, String>() {
            @Override
            public String call(Boolean connected) {
                return connected ? "connected" : "disconnected";
            }
        };
    }

    private Observable.Operator<Pair<String, String>, RxObjectEvent> liftRxJsonEventToPairMessage() {
        return new Observable.Operator<Pair<String, String>, RxObjectEvent>() {
            @Override
            public Subscriber<? super RxObjectEvent> call(final Subscriber<? super Pair<String, String>> subscriber) {
                return new Subscriber<RxObjectEvent>(subscriber) {
                    @Override
                    public void onCompleted() {
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        subscriber.onError(e);
                    }

                    @Override
                    public void onNext(RxObjectEvent rxObjectEvent) {
                        if (rxObjectEvent instanceof RxObjectEventMessage) {
                            subscriber.onNext(new Pair<>("message", ((RxObjectEventMessage) rxObjectEvent).message().toString()));
                        } else if (rxObjectEvent instanceof RxObjectEventWrongMessageFormat) {
                            final RxObjectEventWrongStringMessageFormat wrongMessageFormat = (RxObjectEventWrongStringMessageFormat) rxObjectEvent;
                            //noinspection ThrowableResultOfMethodCallIgnored
                            subscriber.onNext(new Pair<>("could not parse message", wrongMessageFormat.message()
                                    + ", " + wrongMessageFormat.exception().toString()));
                        } else if (rxObjectEvent instanceof RxObjectEventDisconnected) {
                            //noinspection ThrowableResultOfMethodCallIgnored
                            final Throwable exception = ((RxObjectEventDisconnected) rxObjectEvent).exception();
                            if (!(exception instanceof InterruptedException)) {
                                subscriber.onNext(new Pair<>("error", exception.toString()));
                            }
                        }
                    }
                };
            }
        };
    }

    private Func1<Boolean, String> mapConnectingStatusToString() {
        return new Func1<Boolean, String>() {
            @Override
            public String call(Boolean aBoolean) {
                return aBoolean ? "connecting" : "disconnecting";
            }
        };
    }

    private Func1<Object, Observable<DataMessageOrError>> flatMapClicksToSendMessageAndResult(@Nonnull final Socket socket) {
        return new Func1<Object, Observable<DataMessageOrError>>() {
            @Override
            public Observable<DataMessageOrError> call(Object o) {
                addItem.onNext(newItem("sending...", null));
                return socket
                        .sendMessageOnceWhenConnected(new Func1<String, Observable<Object>>() {
                            @Override
                            public Observable<Object> call(String id) {
                                return Observable.<Object>just(new DataMessage(id, "krowa"));
                            }
                        })
                        .map(new Func1<DataMessage, DataMessageOrError>() {
                            @Override
                            public DataMessageOrError call(DataMessage dataMessage) {
                                return new DataMessageOrError(dataMessage, null);
                            }
                        })
                        .onErrorResumeNext(new Func1<Throwable, Observable<DataMessageOrError>>() {
                            @Override
                            public Observable<DataMessageOrError> call(Throwable throwable) {
                                return Observable.just(new DataMessageOrError(null, throwable));
                            }
                        });
            }
        };
    }

    private Func1<DataMessageOrError, Pair<String, String>> mapDataMessageOrErrorToPair() {
        return new Func1<DataMessageOrError, Pair<String, String>>() {
            @Override
            public Pair<String, String> call(DataMessageOrError dataMessageOrError) {
                if (dataMessageOrError.error != null) {
                    return new Pair<>("sending error", dataMessageOrError.error.toString());
                } else {
                    return new Pair<>("sending response", dataMessageOrError.message.toString());
                }
            }
        };
    }


    @Nonnull
    private Func1<? super Object, Boolean> funcTrue() {
        return new Func1<Object, Boolean>() {
            @Override
            public Boolean call(Object o) {
                return true;
            }
        };
    }

    @Nonnull
    private Func1<? super Object, Boolean> funcFalse() {
        return new Func1<Object, Boolean>() {
            @Override
            public Boolean call(Object o) {
                return false;
            }
        };
    }

    @Nonnull
    private Func1<String, AdapterItem> mapStringToNewAdapterItem() {
        return new Func1<String, AdapterItem>() {
            @Override
            public AdapterItem call(String s) {
                return newItem(s, null);
            }
        };
    }

    @Nonnull
    private Func1<Pair<String, String>, AdapterItem> mapPairToNewAdapterItem() {
        return new Func1<Pair<String, String>, AdapterItem>() {
            @Override
            public AdapterItem call(Pair<String, String> s) {
                return newItem(s.first, s.second);
            }
        };
    }

    private final Object idLock = new Object();
    private long id = 0;

    @Nonnull
    private String newId() {
        synchronized (idLock) {
            final long id = this.id;
            this.id += 1;
            return String.valueOf(id);
        }
    }

    @Nonnull
    private AdapterItem newItem(@Nonnull String message, @Nullable String details) {

        return new AdapterItem(newId(), System.currentTimeMillis(), message, details);
    }

    @Nonnull
    public Observer<Object> connectClickObserver() {
        return connectClick;
    }

    @Nonnull
    public Observer<Object> disconnectClickObserver() {
        return disconnectClick;
    }

    @Nonnull
    public Observer<Object> sendClickObserver() {
        return sendClick;
    }

    @Nonnull
    public Observable<Boolean> connectButtonEnabledObservable() {
        return requestConnection.map(not());
    }

    @Nonnull
    public Observable<Boolean> disconnectButtonEnabledObservable() {
        return requestConnection;
    }

    @Nonnull
    public Observable<Boolean> sendButtonEnabledObservable() {
        return connected;
    }

    @Nonnull
    private Func1<Boolean, Boolean> not() {
        return new Func1<Boolean, Boolean>() {
            @Override
            public Boolean call(Boolean aBoolean) {
                return !aBoolean;
            }
        };
    }

    public class AdapterItem implements SimpleDetector.Detectable<AdapterItem> {

        @Nonnull
        private final String id;
        private final long publishTime;
        @Nullable
        private final String text;
        @Nullable
        private final String details;

        public AdapterItem(@Nonnull String id,
                           long publishTime,
                           @Nullable String text,
                           @Nullable String details) {
            this.id = id;
            this.publishTime = publishTime;
            this.text = text;
            this.details = details;
        }

        @Nonnull
        public String id() {
            return id;
        }

        @Nullable
        public String text() {
            return text;
        }

        @Nullable
        public String details() {
            return details;
        }

        public long publishTime() {
            return publishTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AdapterItem)) return false;

            final AdapterItem that = (AdapterItem) o;

            return id.equals(that.id)
                    && !(text != null ? !text.equals(that.text) : that.text != null)
                    && publishTime == that.publishTime
                    && !(details != null ? !details.equals(that.details) : that.details != null);
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + (text != null ? text.hashCode() : 0);
            result = 31 * result + (details != null ? details.hashCode() : 0);
            result = 31 * result + (int)publishTime;
            return result;
        }

        @Override
        public boolean matches(@Nonnull AdapterItem item) {
            return id.equals(item.id);
        }

        @Override
        public boolean same(@Nonnull AdapterItem item) {
            return equals(item);
        }

        @Nonnull
        public Observer<Object> clickObserver() {
            return Observers.create(new Action1<Object>() {
                @Override
                public void call(Object o) {
                }
            });
        }
    }

    public static class ItemsWithScroll {
        private final ImmutableList<AdapterItem> items;
        private final boolean shouldScroll;
        private final int scrollToPosition;

        public ItemsWithScroll(ImmutableList<AdapterItem> items, boolean shouldScroll, int scrollToPosition) {
            this.items = items;
            this.shouldScroll = shouldScroll;
            this.scrollToPosition = scrollToPosition;
        }

        public ImmutableList<AdapterItem> items() {
            return items;
        }

        public boolean shouldScroll() {
            return shouldScroll;
        }

        public int scrollToPosition() {
            return scrollToPosition;
        }
    }

    static class DataMessageOrError {
        private final DataMessage message;
        private final Throwable error;

        public DataMessageOrError(DataMessage message, Throwable error) {
            this.message = message;
            this.error = error;
        }
    }
}
