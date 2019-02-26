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

package com.appunite.socket

import android.util.Pair
import com.appunite.detector.DefinedAdapterItem

import com.appunite.detector.SimpleDetector
import com.appunite.websocket.rx.`object`.messages.RxObjectEvent
import com.appunite.websocket.rx.`object`.messages.RxObjectEventConn
import com.appunite.websocket.rx.`object`.messages.RxObjectEventDisconnected
import com.appunite.websocket.rx.`object`.messages.RxObjectEventMessage
import com.appunite.websocket.rx.`object`.messages.RxObjectEventWrongMessageFormat
import com.appunite.websocket.rx.`object`.messages.RxObjectEventWrongStringMessageFormat
import com.example.Socket
import com.example.model.DataMessage
import com.google.common.collect.ImmutableList

import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import org.funktionale.option.Option
import org.funktionale.option.toOption

class MainPresenter(private val socket: Socket, val networkScheduler: Scheduler, val uiScheduler: Scheduler) {

    private val items: BehaviorSubject<ImmutableList<AdapterItem>> = BehaviorSubject.create()
    private val connected: Observable<Boolean>
    private val requestConnection = BehaviorSubject.create<Boolean>()
    private val connectClick = PublishSubject.create<Any>()
    private val disconnectClick = PublishSubject.create<Any>()
    private val sendClick = PublishSubject.create<Any>()
    private val lastItemInView = BehaviorSubject.create<Boolean>()
    private val addItem = PublishSubject.create<AdapterItem>()

    private val idLock = Any()
    private var id: Long = 0

    init {

        Observable.merge(connectClick.map{ true }, disconnectClick.map(funcFalse()))
                .startWith(false)
                .subscribe(requestConnection)

        sendClick
                .flatMap(flatMapClicksToSendMessageAndResult(socket))
                .map(mapDataMessageOrErrorToPair())
                .map(mapPairToNewAdapterItem())
                .subscribeOn(networkScheduler)
                .observeOn(uiScheduler)
                .subscribe(addItem)



        requestConnection
                .map(mapConnectingStatusToString())
                .map(mapStringToNewAdapterItem())
                .subscribe(addItem)

        addItem
                .scan(ImmutableList.of(), BiFunction<ImmutableList<AdapterItem>, AdapterItem, ImmutableList<AdapterItem>> { adapterItems, adapterItem -> ImmutableList.builder<AdapterItem>().addAll(adapterItems).add(adapterItem).build() })
                .subscribe(items)

        socket.events()
                .subscribeOn(networkScheduler)
                .observeOn(uiScheduler)
                .map{ rxObjectEvent ->
                   return@map if (rxObjectEvent is RxObjectEventMessage) {
                       Pair("message", rxObjectEvent.message<Any>().toString()).toOption()
                   } else if (rxObjectEvent is RxObjectEventWrongMessageFormat) {
                       val wrongMessageFormat = rxObjectEvent as RxObjectEventWrongStringMessageFormat

                       Pair("could not parse message", wrongMessageFormat.message() + ", " + wrongMessageFormat.exception().toString()).toOption()
                   } else if (rxObjectEvent is RxObjectEventDisconnected) {
                       val exception = rxObjectEvent.exception()
                       if (exception !is InterruptedException) {
                           Pair("error", exception.toString()).toOption()
                       } else {
                           Option.empty()
                       }
                   } else {
                       Option.empty()
                   }
                }
                .filter { it.isDefined() }
                .map { it.get() }
                .map(mapPairToNewAdapterItem())
                .subscribe(addItem)

        connected = socket.connectedAndRegistered()
                .map {
                    it.isDefined()
                }
                .distinctUntilChanged()
                .subscribeOn(networkScheduler)
                .observeOn(uiScheduler)

        connected
                .map(mapConnectedStatusToString())
                .map(mapStringToNewAdapterItem())
                .subscribe(addItem)
    }

    fun create(): Disposable = CompositeDisposable(
            socket.create(),
            requestConnection
                    .subscribe(object : Consumer<Boolean> {

                        private var subscribe: Disposable? = null

                        override fun accept(requestConnection: Boolean?) {
                            if (requestConnection!!) {
                                if (subscribe == null) {
                                    subscribe = socket
                                            .connection()
                                            .subscribeOn(networkScheduler)
                                            .observeOn(uiScheduler)
                                            .subscribe()
                                }
                            } else {
                                if (subscribe != null) {
                                    subscribe!!.dispose()
                                    subscribe = null
                                }
                            }
                        }
                    })
    )



    fun itemsWithScrollObservable(): Observable<ItemsWithScroll> = Observable.combineLatest(items, lastItemInView, BiFunction<List<AdapterItem>, Boolean, ItemsWithScroll> { adapterItems, isLastItemInList ->
        val lastItemPosition = adapterItems.size - 1
        val shouldScroll = isLastItemInList && lastItemPosition >= 0
        ItemsWithScroll(adapterItems, shouldScroll, lastItemPosition)
    })

    fun lastItemInViewSingle(isLastItem: Boolean): Single<Unit> = lastItemInView.executeFromSingle(isLastItem)

    private fun mapConnectedStatusToString(): Function<Boolean, String> = Function { connected -> if (connected) "connected" else "disconnected" }

    private fun mapConnectingStatusToString(): Function<Boolean, String> = Function { aBoolean -> if (aBoolean) "connecting" else "disconnecting" }

    private fun flatMapClicksToSendMessageAndResult(socket: Socket): Function<Any, Observable<DataMessageOrError>> {
        return Function {
            addItem.onNext(newItem("sending...", null))
            socket
                    .sendMessageOnceWhenConnected(Function { id -> Observable.just(DataMessage(id, "krowa")) })
                    .map { dataMessage -> DataMessageOrError(dataMessage.toOption(), Option.empty()) }
                    .onErrorResumeNext{ throwable: Throwable -> Observable.just(DataMessageOrError(Option.empty(), throwable.toOption())) }
        }
    }

    private fun mapDataMessageOrErrorToPair(): Function<DataMessageOrError, Pair<String, String>> {
        return Function { dataMessageOrError ->
            if (dataMessageOrError.error.isDefined()) {
                Pair("sending error", dataMessageOrError.error.toString())
            } else {
                Pair("sending response", dataMessageOrError.message.toString())
            }
        }
    }


    private fun funcTrue(): Function<in Any, Boolean> {
        return Function { true }
    }

    private fun funcFalse(): Function<in Any, Boolean> {
        return Function { false }
    }

    private fun mapStringToNewAdapterItem(): Function<String, AdapterItem> {
        return Function { s -> newItem(s, null) }
    }

    private fun mapPairToNewAdapterItem(): Function<Pair<String, String>, AdapterItem> = Function { s -> newItem(s.first, s.second) }

    private fun newId(): String {
        synchronized(idLock) {
            val id = this.id
            this.id += 1
            return id.toString()
        }
    }

    private fun newItem(message: String, details: String?): AdapterItem {
        //details nullable
        return AdapterItem(newId(), System.currentTimeMillis(), message, details)
    }

    fun connectClickSingle(): Single<Unit> = connectClick.executeFromSingle(Unit)

    fun disconnectClickSingle(): Single<Unit> = disconnectClick.executeFromSingle(Unit)

    fun sendClickSingle(): Single<Unit> = sendClick.executeFromSingle(Unit)

    fun connectButtonEnabledObservable(): Observable<Boolean> = requestConnection.map{ !it }

    fun disconnectButtonEnabledObservable(): Observable<Boolean> = requestConnection

    fun sendButtonEnabledObservable(): Observable<Boolean> = connected


    data class AdapterItem(val id: String,
                           val publishTime: Long,
                           val text: String?,
                           val details: String?) : DefinedAdapterItem<String> {

        override fun itemId(): String = id

    }

    data class ItemsWithScroll( val items: List<AdapterItem>,  val shouldScroll: Boolean,  val scrollToPosition: Int)



    internal class DataMessageOrError(val message: Option<DataMessage>, val error: Option<Throwable>)
}
fun <T> Observer<T>.executeFromSingle(onNextValue: T): Single<Unit> = Single.fromCallable {
    this.onNext(onNextValue)
}
