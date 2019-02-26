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

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appunite.websocket.rx.RxWebSockets
import com.appunite.websocket.rx.`object`.GsonObjectSerializer
import com.appunite.websocket.rx.`object`.ObjectParseException
import com.appunite.websocket.rx.`object`.ObjectSerializer
import com.appunite.websocket.rx.`object`.RxObjectWebSockets
import com.example.Socket
import com.example.SocketConnectionImpl
import com.example.model.Message
import com.example.model.MessageType
import com.google.gson.GsonBuilder
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.main_activity.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.logging.Level
import java.util.logging.Logger

class MainActivity : FragmentActivity() {

    private var subs: CompositeDisposable? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val presenter = getRetentionFragment(savedInstanceState).presenter()
        setContentView(R.layout.main_activity)

        val recyclerView = main_activity_recycler_view as RecyclerView
        val layout = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recyclerView.layoutManager = layout
        val adapter = RxUniversalAdapter(listOf(MainViewHolder()))
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = DefaultItemAnimator()

        val isLastSubject = BehaviorSubject.createDefault(true)
        recyclerView.setOnScrollListener(object : RecyclerView.OnScrollListener() {
            var manualScrolling = false

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    manualScrolling = true
                }
                if (manualScrolling && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    manualScrolling = false

                    val lastVisibleItemPosition = layout.findLastVisibleItemPosition()
                    val previousItemsCount = adapter.itemCount
                    val isLast = previousItemsCount - 1 == lastVisibleItemPosition
                    isLastSubject.onNext(isLast)
                }
            }
        })


        subs = CompositeDisposable(
                isLastSubject.switchMapSingle { presenter.lastItemInViewSingle(it) }
                        .subscribe(),
                presenter.itemsWithScrollObservable()
                        .subscribe { itemsWithScroll ->
                            adapter.call(itemsWithScroll.items)
                            if (itemsWithScroll.shouldScroll) {
                                recyclerView.post { recyclerView.smoothScrollToPosition(itemsWithScroll.scrollToPosition) }
                            }
                        },
                presenter.connectButtonEnabledObservable()
                        .subscribe { main_activity_connect_button.isEnabled = it },
                presenter.disconnectButtonEnabledObservable()
                        .subscribe { macin_activity_disconnect_button.isEnabled = it },
                presenter.sendButtonEnabledObservable()
                        .subscribe { main_activity_send_button.isEnabled = it },
                main_activity_connect_button.clicks()
                        .switchMapSingle { presenter.connectClickSingle() }
                        .subscribe(),
                macin_activity_disconnect_button.clicks()
                        .switchMapSingle { presenter.disconnectClickSingle() }
                        .subscribe(),
                main_activity_send_button.clicks()
                        .switchMapSingle { presenter.sendClickSingle() }
                        .subscribe(),
                presenter.create())
    }

    private fun getRetentionFragment(savedInstanceState: Bundle?): RetentionFragment {
        if (savedInstanceState == null) {
            val retentionFragment = RetentionFragment()
            supportFragmentManager
                    .beginTransaction()
                    .add(retentionFragment, RETENTION_FRAGMENT_TAG)
                    .commit()
            return retentionFragment
        } else {
            return supportFragmentManager
                    .findFragmentByTag(RETENTION_FRAGMENT_TAG) as RetentionFragment
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        subs!!.dispose()
    }

    class RetentionFragment : Fragment() {

        private val presenter: MainPresenter

        init {
            val gson = GsonBuilder()
                    .registerTypeAdapter(Message::class.java, Message.Deserializer())
                    .registerTypeAdapter(MessageType::class.java, MessageType.SerializerDeserializer())
                    .create()

            val okHttpClient = OkHttpClient()

            val webSockets = RxWebSockets(okHttpClient, Request.Builder()
                    .get()
                    .url("https://connect-kc.appunite.net/" + "ws")
                    .addHeader("Content-Type", "application/x-protobuf")
                    .addHeader("Accept", "application/x-protobuf")
                    .addHeader("X-AUTH-TOKEN", "yn8DJF+QFwoJME/ZylShsNBgb/Q=")
//                    .addHeader("Last-Synchronization-Token", nextToken)
                    .build())


//            val webSockets = RxWebSockets(okHttpClient, Request.Builder()
//                    .get()
//                    .url("ws://coreos2.appunite.net:8080/ws")
//                    .addHeader("Sec-WebSocket-Protocol", "chat")
//                    .build())
            val serializer = GsonObjectSerializer(gson, Message::class.java)
            val jsonWebSockets = RxObjectWebSockets(webSockets, serializer)
            val socketConnection = SocketConnectionImpl(jsonWebSockets, Schedulers.io())
            presenter = MainPresenter(Socket(socketConnection, Schedulers.io()), Schedulers.io(), AndroidSchedulers.mainThread())
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            retainInstance = true
        }

        fun presenter(): MainPresenter {
            return presenter
        }
    }

    companion object {

        private val RETENTION_FRAGMENT_TAG = "retention_fragment_tag"

        private class LoggingSerializer(private val impl: ObjectSerializer, val logger: Logger) : ObjectSerializer {

            @Throws(ObjectParseException::class)
            override fun serialize(message: String): Any {
                val serialize = impl.serialize(message)
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Serialized string message: $serialize")
                }
                return serialize
            }

            @Throws(ObjectParseException::class)
            override fun serialize(message: ByteArray): Any {
                val serialize = impl.serialize(message)
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Serialized binary message: $serialize")
                }
                return serialize
            }

            @Throws(ObjectParseException::class)
            override fun deserializeBinary(message: Any): ByteArray {
                val bytes = impl.deserializeBinary(message)
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Deserialized binary message: $message")
                }
                return bytes
            }

            @Throws(ObjectParseException::class)
            override fun deserializeString(message: Any): String {
                val s = impl.deserializeString(message)
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Deserialized string message: $message")
                }
                return s
            }

            override fun isBinary(message: Any): Boolean {
                return impl.isBinary(message)
            }
        }

    }

}
