package com.example;

import com.appunite.websocket.NewWebSocket;
import com.appunite.websocket.rx.RxWebSockets;
import com.appunite.websocket.rx.json.RxJsonWebSockets;
import com.example.model.Message;
import com.example.model.MessageType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import rx.Subscription;
import rx.schedulers.Schedulers;

public class RealTest3 {

    private static final URI SERVER_URI;

    static {
        try {
            SERVER_URI = new URI("ws://192.168.0.142:8080/ws");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    private RxJsonWebSockets socket;

    @Before
    public void setUp() throws Exception {
        final Gson gson = new GsonBuilder()
                .registerTypeAdapter(Message.class, new Message.Deserializer())
                .registerTypeAdapter(MessageType.class, new MessageType.SerializerDeserializer())
                .create();

        final NewWebSocket newWebSocket = new NewWebSocket();
        socket = new RxJsonWebSockets(new RxWebSockets(newWebSocket, SERVER_URI), gson, Message.class);;

    }

    @Test
    @Ignore
    public void testName() throws Exception {
        final Subscription subscribe = socket.webSocketObservable()
                .subscribeOn(Schedulers.io())
                .subscribe();
        Thread.sleep(5000);
        subscribe.unsubscribe();
        Thread.sleep(5000);
    }

}