package com.example;

import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.exceptions.OnErrorThrowable;

public class OperatorDoOnNext<T> implements Operator<T, T> {
    private final Observer<? super T> doOnNextObserver;

    public OperatorDoOnNext(Observer<? super T> doOnNextObserver) {
        this.doOnNextObserver = doOnNextObserver;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> observer) {
        return new Subscriber<T>(observer) {

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(T value) {
                doOnNextObserver.onNext(value);
            }
        };
    }
}