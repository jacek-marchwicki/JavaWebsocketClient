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

import com.appunite.websocket.rx.`object`.RxObjectWebSockets
import com.appunite.websocket.rx.`object`.messages.RxObjectEvent

import java.util.concurrent.TimeUnit


import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.functions.Function

class SocketConnectionImpl(private val sockets: RxObjectWebSockets, private val scheduler: Scheduler) : SocketConnection {


    override fun connection(): Observable<RxObjectEvent> {
        return sockets.webSocketObservable()
                .retryWhen(repeatDuration(1, TimeUnit.SECONDS))
    }


    private fun repeatDuration(delay: Long,
                               timeUnit: TimeUnit): Function<Observable<out Throwable>, Observable<*>> {
        return Function { attemps ->
            attemps
                    .flatMap { Observable.timer(delay, timeUnit, scheduler) }
        }
    }
}
