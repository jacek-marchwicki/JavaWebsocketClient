package com.appunite.websocket;

import com.appunite.websocket.internal.SecureRandomProvider;
import com.appunite.websocket.internal.SocketProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.Charset;

import javax.annotation.Nonnull;

import static com.google.common.truth.Truth.assert_;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NewWebSocketTest {
    @Mock
    Socket socket;
    @Mock
    SocketProvider socketProvider;
    @Mock
    SecureRandomProvider secureRandomProvider;
    @Mock
    WebSocketListener listener;

    private NewWebSocket webSocket;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(socketProvider.getSocket(any(URI.class))).thenReturn(socket);
        when(secureRandomProvider.generateHandshakeSecret()).thenReturn("dGhlIHNhbXBsZSBub25jZQ==");
        webSocket = new NewWebSocket(secureRandomProvider, socketProvider);
    }

    private boolean close = false;
    @Nonnull
    InputStream prepareResponse(@Nonnull String string) throws Exception {
        doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                close = true;
                return null;
            }
        }).when(socket).close();

        return new SequenceInputStream(new ByteArrayInputStream(string.getBytes(Charset.forName("UTF-8"))), new InputStream() {
            @Override
            public int read() throws IOException {
                while (!close) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignore) {
                        return -1;
                    }
                }
                throw new EOFException();
            }
        });
    }

    @Test
    public void testAfterSuccessResponseOnConnectedCalled() throws Exception {
        final InputStream inputStream = prepareResponse(
                "HTTP/1.1 101 Switching Protocols\n" +
                "Upgrade: websocket\n" +
                "Connection: Upgrade\n" +
                "Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=\n\n");
        when(socket.getInputStream()).thenReturn(inputStream);
        when(socket.getOutputStream()).thenReturn(System.out);

        final WebSocketConnection test = webSocket.create(new URI("ws://test"), listener);

        final Async async = new Async(test);
        async.connect();
        try {
            verify(listener, timeout(1000)).onConnected();
        } finally {
            async.interrupt();
        }
    }

    @Test
    public void testCorrectHeadersWasSent() throws Exception {
        final InputStream inputStream = prepareResponse(
                "HTTP/1.1 101 Switching Protocols\n" +
                        "Upgrade: websocket\n" +
                        "Connection: Upgrade\n" +
                        "Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=\n\n");
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(socket.getInputStream()).thenReturn(inputStream);
        when(socket.getOutputStream()).thenReturn(outputStream);
        final WebSocketConnection test = webSocket.create(new URI("ws://test"), listener);

        final Async async = new Async(test);
        async.connect();
        try {
            verify(listener, timeout(1000)).onConnected();
        } finally {
            async.interrupt();
        }

        final String s = outputStream.toString("UTF-8");
        assert_().that(s).startsWith("GET  HTTP/1.1\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Host: test\r\n" +
                "Origin: ws://test\r\n" +
                "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" +
                "Sec-WebSocket-Protocol: chat\r\n" +
                "Sec-WebSocket-Version: 13\r\n" +
                "\r\n");
    }

    @Test
    public void testMask() throws Exception {
        final byte[] src = {0x01, 0x02, 0x03};
        final byte[] toMask = src.clone();
        final byte[] mask = {0x51, 0x52, 0x53, 0x54};
        WebSocketConnection.maskBuffer(toMask, mask);

        assertThat(toMask, is(not(equalTo(src))));

        WebSocketConnection.maskBuffer(toMask, mask);

        assertThat(toMask, is(equalTo(src)));

    }

    static class Async {
        private final WebSocketConnection connection;

        public boolean finished = false;
        public Exception ret = null;

        Async(WebSocketConnection connection) {
            this.connection = connection;
        }

        public void connect() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        connection.connect();
                    } catch (Exception  e) {
                        synchronized (Async.this) {
                            ret = e;
                        }
                    } finally {
                        synchronized (Async.this) {
                            finished = true;
                            Async.this.notify();
                        }
                    }
                }
            }).start();
        }

        public void interrupt() throws Exception {
            connection.interrupt();
            synchronized (this) {
                if (!finished) {
                    try {
                        this.wait();
                    } catch (InterruptedException ignore) {}
                }
                try {
                    throw ret;
                } catch (InterruptedException ignore) {}
            }
        }


    }
}
