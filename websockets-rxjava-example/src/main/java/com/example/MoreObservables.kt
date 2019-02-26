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

package com.example

import com.appunite.websocket.rx.`object`.messages.RxObjectEvent

import io.reactivex.Observable
import io.reactivex.ObservableOperator
import io.reactivex.Observer
import io.reactivex.disposables.Disposable


fun <T: RxObjectEvent> Observable<RxObjectEvent>.filterAndMap(clazz: Class<T>): Observable<T> {
    return this
            .filter { o ->
                clazz.isInstance(o)
            }
            .map {
                it as T
            }
}

fun Throwable.throwableToIgnoreError(): Any {
    return Any()
}

fun ignoreNext(): ObservableOperator<Any, RxObjectEvent> = ObservableOperator { observer ->
    object : Observer<RxObjectEvent> {
        override fun onSubscribe(d: Disposable) {
        }

        override fun onNext(t: RxObjectEvent) {
        }

        override fun onError(e: Throwable) {
            observer.onError(e)
        }

        override fun onComplete() {
            observer.onComplete()
        }
    }
}
