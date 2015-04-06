package com.appunite.websocket;


import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class WebsocketTest {

    private URI uri;
    private WebSocketListener listener;

    @Before
    public void setUp() throws Exception {
        uri = new URI("wss://www.yourserver.com/");
        listener = Mockito.mock(WebSocketListener.class);
    }

    @Test
    public void testPreconditions() throws Exception {
        assertThat(uri, is(Matchers.notNullValue()));
    }

    @Test
    public void testNoError() throws Exception {
        final WebSocket webSocket = new WebSocket(listener);
        final AtomicReference<Exception> reference = new AtomicReference<>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    webSocket.connect(uri);
                } catch (IOException | WrongWebsocketResponse e) {
                    reference.set(e);
                } catch (InterruptedException ignore) {
                }
            }
        }).start();

        Thread.sleep(2000);
        final Exception exception = reference.get();
        if (exception != null) {
            throw exception;
        }

        webSocket.interrupt();
    }

    @Test
    public void testConnection() throws Exception {
        final WebSocket webSocket = new WebSocket(listener);

        final AtomicBoolean interrupted = new AtomicBoolean(false);
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    webSocket.connect(uri);
                } catch (IOException | WrongWebsocketResponse ignore) {
                } catch (InterruptedException e) {
                    interrupted.set(true);
                }
            }
        });
        thread.start();

        verify(listener, timeout(2000)).onConnected();

        webSocket.interrupt();

        thread.join(1000);

        assertThat(interrupted.get(), is(equalTo(true)));
    }

    @Test
    public void testMask() throws Exception {
        final WebSocket webSocket = new WebSocket(listener);

        final byte[] src = {0x01, 0x02, 0x03};
        final byte[] toMask = src.clone();
        final byte[] mask = {0x51, 0x52, 0x53, 0x54};
        webSocket.maskBuffer(toMask, mask);

        assertThat(toMask, is(not(equalTo(src))));

        webSocket.maskBuffer(toMask, mask);

        assertThat(toMask, is(equalTo(src)));

    }

    @Test
    public void testPing() throws Exception {
        final WebSocket webSocket = new WebSocket(listener);

        final AtomicBoolean interrupted = new AtomicBoolean(false);
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    webSocket.connect(uri);
                } catch (IOException | WrongWebsocketResponse ignore) {
                } catch (InterruptedException e) {
                    interrupted.set(true);
                }
            }
        });
        thread.start();

        verify(listener, timeout(2000)).onConnected();
        reset(listener);
        final byte[] data = {0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x02, 0x03};
        webSocket.sendPingMessage(data.clone());

        verify(listener, timeout(2000)).onPong(data);

        webSocket.interrupt();

        thread.join(1000);

        assertThat(interrupted.get(), is(equalTo(true)));
    }

    @Test
    public void testReconnection() throws Exception {
        final WebSocket webSocket = new WebSocket(listener);
        final Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    webSocket.connect(uri);
                } catch (IOException | WrongWebsocketResponse | InterruptedException ignore) {
                }
            }
        });
        thread1.start();

        verify(listener, timeout(2000)).onConnected();
        reset(listener);
        webSocket.interrupt();
        thread1.join(1000);


        final Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    webSocket.connect(uri);
                } catch (IOException | WrongWebsocketResponse | InterruptedException ignore) {
                }
            }
        });
        thread2.start();

        verify(listener, timeout(2000)).onConnected();
        reset(listener);
        webSocket.interrupt();
        thread2.join(1000);
    }

    @Test
    public void testSendData() throws Exception {
        final WebSocket webSocket = authorize();

        webSocket.interrupt();
    }

    @Test
    public void testRequestUsers() throws Exception {
        final WebSocket webSocket = authorize();

        webSocket.sendStringMessage("{\"action\": \"observe\", \"users\": [12354]}");
        verify(listener, timeout(2000)).onStringMessage(argThat(containsString("\"1235664\"")));

        webSocket.interrupt();
    }

    private WebSocket connectWebsocket() throws IOException, InterruptedException, NotConnectedException {
        final WebSocket webSocket = new WebSocket(listener);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    webSocket.connect(uri);
                } catch (IOException | WrongWebsocketResponse | InterruptedException ignore) {
                }
            }
        }).start();

        verify(listener, timeout(2000)).onConnected();
        reset(listener);
        return webSocket;
    }

    private WebSocket authorize() throws IOException, InterruptedException, NotConnectedException {
        final WebSocket webSocket = connectWebsocket();

        webSocket.sendStringMessage("{\"action\": \"auth\", \"auth_token\": \"1ZizC3fpskE3YEHexfgX\"}");

        final ArgumentCaptor<String> responseMessage = ArgumentCaptor.forClass(String.class);
        verify(listener, timeout(2000)).onStringMessage(responseMessage.capture());
        reset(listener);

        assertThat(responseMessage.getValue(), is(containsString("auth")));
        assertThat(responseMessage.getValue(), is(containsString("OK")));
        return webSocket;
    }
}
