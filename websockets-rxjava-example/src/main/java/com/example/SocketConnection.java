package com.example;

import com.appunite.websocket.rx.json.messages.RxJsonEvent;
import com.appunite.websocket.rx.messages.RxEvent;

import rx.Observable;

public interface SocketConnection {
    Observable<RxJsonEvent> connection();
}
