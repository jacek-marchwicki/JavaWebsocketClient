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

import com.appunite.websocket.rx.*
import com.appunite.websocket.rx.`object`.messages.*
import com.example.model.DataMessage
import com.example.model.PingMessage
import com.example.model.RegisterMessage
import com.example.model.RegisteredMessage

import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import org.funktionale.option.Option
import org.funktionale.option.toOption

class Socket(socketConnection: SocketConnection, private val scheduler: Scheduler) {

    private val events: Observable<RxObjectEvent>
    private val connection: Observable<Any>
    private val connectedAndRegistered: BehaviorSubject<Option<RxObjectEventConn>>

    private val lock = Any()
    private var counter = 0

    init {
        val events = PublishSubject.create<RxObjectEvent>()
        connection = socketConnection.connection()
                .lift(OperatorDoOnNext(events))
                .lift(ignoreNext())
                .replay(1)
                .refCount()
        this.events = events


        val registeredMessage = events
                .filterAndMap(RxObjectEventMessage::class.java)
                .filter{ it.filterRegisterMessage() }
                .map { it.toOption() }

        val disconnectedMessage = events
                .filterAndMap(RxObjectEventDisconnected::class.java)

        connectedAndRegistered = BehaviorSubject.createDefault(Option.empty())
        disconnectedMessage
                .map{ Option.empty<RxObjectEventMessage>() }
                .mergeWith(registeredMessage)
                .subscribe(connectedAndRegistered)

        // Register on connected
        val connectedMessage = events
                .filterAndMap(RxObjectEventConnected::class.java)

        connectedMessage
                .flatMap {
                    RxMoreObservables.sendObjectMessage(it.sender(), RegisterMessage("asdf"))
                            .toObservable()
                }
                .onErrorReturn { true }
                .subscribe()

        // Log events
        LOGGER.level = Level.ALL
        RxMoreObservables.logger.level = Level.ALL


    }


    fun create(): Disposable = CompositeDisposable(
            events.subscribeWith(LoggingObservables.logging(LOGGER, "Events")),
            connectedAndRegistered.subscribeWith(LoggingObservables.logging(LOGGER, "ConnectedAndRegistered"))
    )

    fun events(): Observable<RxObjectEvent> = events

    fun connectedAndRegistered(): Observable<Option<RxObjectEventConn>> = connectedAndRegistered

    fun connection(): Observable<Any> = connection

    fun sendPingWhenConnected() {
        Observables.combineLatest(
                Observable.interval(5, TimeUnit.SECONDS, scheduler),
                connectedAndRegistered) { _, rxEventConn -> rxEventConn }
                .isConnected()
                .flatMap {
                    RxMoreObservables.sendObjectMessage(it.sender(), PingMessage("send_only_when_connected"))
                            .toObservable()
                }

                .subscribe()
    }

    fun sendPingEvery5seconds() {
        Observable.interval(5, TimeUnit.SECONDS, scheduler)
                .flatMapSingle {
                    connectedAndRegistered
                            .isConnected()
                            .firstOrError() // check
                            .flatMap {
                                RxMoreObservables.sendObjectMessage(it.sender(), PingMessage("be_sure_to_send"))
                            }
                }
                .subscribe()
    }

    fun nextId(): Observable<String> {
        return Observable.create {
            var current: Int
            synchronized(lock) {
                current = counter
                counter += 1
            }
            it.onNext(current.toString())
            it.onComplete()
        }
    }

    fun sendMessageOnceWhenConnected(createMessage: Function<String, Observable<Any>>): Observable<DataMessage> {
        return connectedAndRegistered
                .isConnected()
                .firstOrError() // check
                .toObservable()
                .flatMap{ rxEventConn -> requestData(rxEventConn, createMessage) }
    }

    private fun requestData(rxEventConn: RxObjectEventConn,
                            createMessage: Function<String, Observable<Any>>): Observable<DataMessage> {
        return nextId()
                .flatMap { messageId ->
                    val sendMessageObservable = createMessage.apply(messageId)
                            .flatMap { s ->
                                RxMoreObservables.sendObjectMessage(rxEventConn.sender(), s)
                                        .toObservable()
                            }

                    val waitForResponseObservable = events
                            .filterAndMap(RxObjectEventMessage::class.java)
                            .filterAndMap2(DataMessage::class.java)
                            .filter{ it.id() == messageId }
                            .firstOrError() //check
                            .timeout(5, TimeUnit.SECONDS, scheduler).toObservable()
                    Observables.combineLatest(waitForResponseObservable, sendMessageObservable) { data, _ -> data}
                }
    }


    private fun RxObjectEventMessage.filterRegisterMessage() : Boolean = this.message<Any>() is RegisteredMessage

    companion object {
        val LOGGER = Logger.getLogger("Rx")

        private fun Observable<Option<RxObjectEventConn>>.isConnected(): Observable<RxObjectEventConn> = this.filter { it.isDefined() }.map { it.get() }
    }


}
