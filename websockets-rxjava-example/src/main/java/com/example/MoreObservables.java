package com.example;

import com.appunite.websocket.rx.json.messages.RxJsonEvent;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.internal.operators.OperatorMulticast;
import rx.subjects.PublishSubject;
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
                        return PublishSubject.<T>create();
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

    public static Observable.Operator<Object, RxJsonEvent> ignoreNext() {
        return new Observable.Operator<Object, RxJsonEvent>() {
            @Override
            public Subscriber<? super RxJsonEvent> call(final Subscriber<? super Object> subscriber) {
                return new Subscriber<RxJsonEvent>(subscriber) {
                    @Override
                    public void onCompleted() {
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        subscriber.onError(e);
                    }

                    @Override
                    public void onNext(RxJsonEvent rxJsonEvent) {}
                };
            }
        };
    }
}
