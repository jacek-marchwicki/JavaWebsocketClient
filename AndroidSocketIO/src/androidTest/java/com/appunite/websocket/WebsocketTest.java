package com.appunite.websocket;

import android.net.Uri;
import android.test.AndroidTestCase;

import com.appunite.socketio.NotConnectedException;

import org.hamcrest.Matchers;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class WebsocketTest extends AndroidTestCase {

    private Uri mUri;
    private WebSocketListener mListener;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mUri = Uri.parse("wss://www.yourserver.com/");
        mListener = mock(WebSocketListener.class);
    }

    public void testPreconditions() throws Exception {
        assertThat(mUri, is(notNullValue()));
    }

    public void testNoError() throws Exception {
        final WebSocket webSocket = new WebSocket(mListener);
        final AtomicReference<Exception> reference = new AtomicReference<Exception>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    webSocket.connect(mUri);
                } catch (IOException e) {
                    reference.set(e);
                } catch (WrongWebsocketResponse e) {
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

    public void testConnection() throws Exception {
        final WebSocket webSocket = new WebSocket(mListener);

        final AtomicBoolean interrupted = new AtomicBoolean(false);
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    webSocket.connect(mUri);
                } catch (IOException ignore) {
                } catch (WrongWebsocketResponse ignore) {
                } catch (InterruptedException e) {
                    interrupted.set(true);
                }
            }
        });
        thread.start();

        verify(mListener, timeout(2000)).onConnected();

        webSocket.interrupt();

        thread.join(1000);

        assertThat(interrupted.get(), Matchers.is(equalTo(true)));
    }

    public void testMask() throws Exception {
        final WebSocket webSocket = new WebSocket(mListener);

        final byte[] src = {0x01, 0x02, 0x03};
        final byte[] toMask = src.clone();
        final byte[] mask = {0x51, 0x52, 0x53, 0x54};
        webSocket.maskBuffer(toMask, mask);

        assertThat(toMask, is(not(equalTo(src))));

        webSocket.maskBuffer(toMask, mask);

        assertThat(toMask, is(equalTo(src)));

    }

    public void testPing() throws Exception {
        final WebSocket webSocket = new WebSocket(mListener);

        final AtomicBoolean interrupted = new AtomicBoolean(false);
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    webSocket.connect(mUri);
                } catch (IOException ignore) {
                } catch (WrongWebsocketResponse ignore) {
                } catch (InterruptedException e) {
                    interrupted.set(true);
                }
            }
        });
        thread.start();

        verify(mListener, timeout(2000)).onConnected();
        reset(mListener);
        final byte[] data = {0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x02, 0x03};
        webSocket.sendPingMessage(data.clone());

        verify(mListener, timeout(2000)).onPong(data);

        webSocket.interrupt();

        thread.join(1000);

        assertThat(interrupted.get(), Matchers.is(equalTo(true)));


    }

    public void testReconnection() throws Exception {
        final WebSocket webSocket = new WebSocket(mListener);
        final Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    webSocket.connect(mUri);
                } catch (IOException ignore) {
                } catch (WrongWebsocketResponse ignore) {
                } catch (InterruptedException ignore) {
                }
            }
        });
        thread1.start();

        verify(mListener, timeout(2000)).onConnected();
        reset(mListener);
        webSocket.interrupt();
        thread1.join(1000);


        final Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    webSocket.connect(mUri);
                } catch (IOException ignore) {
                } catch (WrongWebsocketResponse ignore) {
                } catch (InterruptedException ignore) {
                }
            }
        });
        thread2.start();

        verify(mListener, timeout(2000)).onConnected();
        reset(mListener);
        webSocket.interrupt();
        thread2.join(1000);
    }

    public void testSendData() throws Exception {
        final WebSocket webSocket = authorize();

        webSocket.interrupt();
    }

    public void testRequestUsers() throws Exception {
        final WebSocket webSocket = authorize();

        webSocket.sendStringMessage("{\"action\": \"observe\", \"users\": [12354]}");
        verify(mListener, timeout(2000)).onStringMessage(argThat(containsString("\"1235664\"")));

        webSocket.interrupt();
    }

    private WebSocket connectWebsocket() throws IOException, InterruptedException, NotConnectedException {
        final WebSocket webSocket = new WebSocket(mListener);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    webSocket.connect(mUri);
                } catch (IOException ignore) {
                } catch (WrongWebsocketResponse ignore) {
                } catch (InterruptedException ignore) {
                }
            }
        }).start();

        verify(mListener, timeout(2000)).onConnected();
        reset(mListener);
        return webSocket;
    }

    private WebSocket authorize() throws IOException, InterruptedException, NotConnectedException {
        final WebSocket webSocket = connectWebsocket();

        webSocket.sendStringMessage("{\"action\": \"auth\", \"auth_token\": \"1ZizC3fpskE3YEHexfgX\"}");

        final ArgumentCaptor<String> responseMessage = ArgumentCaptor.forClass(String.class);
        verify(mListener, timeout(2000)).onStringMessage(responseMessage.capture());
        reset(mListener);

        assertThat(responseMessage.getValue(), is(containsString("auth")));
        assertThat(responseMessage.getValue(), is(containsString("OK")));
        return webSocket;
    }
}
