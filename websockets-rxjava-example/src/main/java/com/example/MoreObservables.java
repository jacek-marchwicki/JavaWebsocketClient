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

package com.example;

import com.appunite.websocket.rx.object.messages.RxObjectEvent;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.internal.operators.OperatorMulticast;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;

public class MoreObservables {

    @Nonnull
    public static <T> Observable.Transformer<? super T, ? extends T> behaviorRefCount() {
        return new Observable.Transformer<T, T>() {
            @Override
            public Observable<T> call(Observable<T> tObservable) {
                return new OperatorMulticast<>(tObservable, new Func0<Subject<? super T, ? extends T>>() {

                    @Override
                    public Subject<? super T, ? extends T> call() {
                        return BehaviorSubject.<T>create();
                    }

                }).refCount();
            }
        };
    }

    @Nonnull
    public static <T> Observable.Transformer<Object, T> filterAndMap(@Nonnull final Class<T> clazz) {
        return new Observable.Transformer<Object, T>() {
            @Override
            public Observable<T> call(Observable<Object> observable) {
                return observable
                        .filter(new Func1<Object, Boolean>() {
                            @Override
                            public Boolean call(Object o) {
                                return o != null && clazz.isInstance(o);
                            }
                        })
                        .map(new Func1<Object, T>() {
                            @Override
                            public T call(Object o) {
                                //noinspection unchecked
                                return (T) o;
                            }
                        });
            }
        };
    }

    @Nonnull
    public static Func1<Throwable, Object> throwableToIgnoreError() {
        return new Func1<Throwable, Object>() {
            @Override
            public Object call(Throwable throwable) {
                return new Object();
            }
        };
    }

    public static Observable.Operator<Object, RxObjectEvent> ignoreNext() {
        return new Observable.Operator<Object, RxObjectEvent>() {
            @Override
            public Subscriber<? super RxObjectEvent> call(final Subscriber<? super Object> subscriber) {
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
                    public void onNext(RxObjectEvent rxObjectEvent) {}
                };
            }
        };
    }
}
