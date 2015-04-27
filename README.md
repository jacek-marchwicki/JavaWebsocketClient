# JavaWebsocketClient also for Android
JavaWebsocketClient is library is simple library for Websocket connection for java and Android.
It is designed to be fast and fault tolerant.

[![Build Status](https://travis-ci.org/jacek-marchwicki/JavaWebsocketClient.svg?branch=master)](https://travis-ci.org/jacek-marchwicki/JavaWebsocketClient)

## Content of the package

* Example websockets server [python twisted server](websockets-server/README.md)
* Imperative websocket client library `websockets/`
* Imperative websocket android example `websockets-example/`
* Rx-java websocket client library `websockets-rxjava/`
* Rx-java websocket android example `websockets-rxjava-example/`

## Imperative example

Connect to server and send message on connected:

```java
final NewWebSocket newWebSocket = new NewWebSocket(new SecureRandomProviderImpl(), new SocketProviderImpl());
final WebSocketConnection connection = newWebSocket.create(SERVER_URI, new WebSocketListener() {
    @Override
    public void onConnected() throws IOException, InterruptedException, NotConnectedException {
        connection.sendStringMessage("register");
    }
});
connection.connect();
```

For more examples look: 
* [Android example](websockets-example/src/main/java/com/appunite/socket/MainActivity.java)
* [Sample test](websockets/src/test/java/com/appunite/websocket/WebsocketTest.java)


## Reactive example

How to connect to server:

```java
final Subscription subscribe = new RxWebSockets(new NewWebSocket(), SERVER_URI)
        .webSocketObservable()
        .subscribe(new Action1<RxEvent>() {
            @Override
            public void call(RxEvent rxEvent) {
                System.out.println("Event: " + rxEvent.toString());
            }
        });
Thread.sleep(10000);
subscribe.unsubscribe();
```

Send message on connected:

```java
final Subscription subscribe = new RxWebSockets(newWebSocket, SERVER_URI)
        .webSocketObservable()
        .subscribe(new Action1<RxEvent>() {
            @Override
            public void call(RxEvent rxEvent) {
                if (rxEvent instanceof RxEventConnected) {
                    Observable.just("response")
                            .compose(RxMoreObservables.sendMessage((RxEventConnected) rxEvent))
                            .subscribe();
                }
            }
        });
Thread.sleep(1000);
subscribe.unsubscribe();
```

For examples look:
* Android example: [Activity](websockets-rxjava-example/src/main/java/com/appunite/socket/MainActivity.java) [Presenter](websockets-rxjava-example/src/main/java/com/appunite/socket/MainPresenter.java)
* Example Real tests: [RxJsonWebSocketsRealTest](websockets-rxjava-example/src/test/java/com/example/RxJsonWebSocketsRealTest.java), [RxWebSocketsRealTest](websockets-rxjava-example/src/test/java/com/example/RxWebSocketsRealTest.java), [SocketRealTest](websockets-rxjava-example/src/test/java/com/example/SocketRealTest.java)
* [Unit test](websockets-rxjava-example/src/test/java/com/example/SocketTest.java)

## Rx-java with json parser

```java
class YourMessage {
    public String response;
    public String error;
}

final RxJsonWebSockets rxJsonWebSockets = new RxJsonWebSockets(new RxWebSockets(new NewWebSocket(), SERVER_URI), new GsonBuilder().create(), Message.class);
rxJsonWebSockets.webSocketObservable()
        .compose(MoreObservables.filterAndMap(RxJsonEventMessage.class))
        .compose(RxJsonEventMessage.filterAndMap(YourMessage.class))
        .subscribe(new Action1<YourMessage>() {
            @Override
            public void call(YourMessage yourMessage) {
                System.out.println("your message: " + yourMessage.response);
            }
        });
```

## Run examples from gradle

To run example first run [websocket server](websockets-server/README.md), than update url to your host in:
* [Rx-java Activity](websockets-rxjava-example/src/main/java/com/appunite/socket/MainActivity.java)
* [Imperative Activity](websockets-example/src/main/java/com/appunite/socket/MainActivity.java)

Reactive (rx-java) example:

```bash
./gradlew :websockets-rxjava-example:installDebug
```

Imperative example:

```bash
./gradlew :websockets-example:installDebug
```

## How to add to your project

to your gradle file:

```groovy
compile "com.appunite:websockets-java:2.0.0"
compile "com.appunite:websockets-rxjava:2.0.0"
```
		
## License

    Copyright [2015] [Jacek Marchwicki <jacek.marchwicki@gmail.com>]
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
    	http://www.apache.org/licenses/LICENSE-2.0
        
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
