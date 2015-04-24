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

package com.appunite.websocket;

import com.appunite.websocket.internal.SecureRandomProvider;
import com.appunite.websocket.tools.Base64;
import com.appunite.websocket.tools.Strings;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicLineParser;
import org.apache.http.util.EncodingUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.appunite.websocket.tools.Preconditions.checkArgument;
import static com.appunite.websocket.tools.Preconditions.checkNotNull;
import static com.appunite.websocket.tools.Preconditions.checkState;

public class WebSocketConnection implements WebSocketSender {

    // Default ports
    private static final int DEFAULT_WSS_PORT = 443;
    private static final int DEFAULT_WS_PORT = 80;

    // WebSocket states
    private enum State {
        NOT_STARTED, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED
    }

    // Magic string for header verification
    private static final String MAGIC_STRING = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    // Payload packet consts
    private static final int RESERVED = 0x03 << 4;
    private static final int FIN = 0x01 << 7;
    private static final int OPCODE = 0x0f;
    private static final int PAYLOAD_MASK = 0x01 << 7;

    // Opcodes
    private static final int OPCODE_CONTINUED_FRAME = 0x00;
    private static final int OPCODE_TEXT_FRAME = 0x01;
    private static final int OPCODE_BINARY_FRAME = 0x02;
    private static final int OPCODE_CONNECTION_CLOSE_FRAME = 0x08;
    private static final int OPCODE_PING_FRAME = 0x09;
    private static final int OPCODE_PONG_FRAME = 0x0A;

    @Nonnull
    // only accessable while CONNECTING/CONNECTED
    private final Socket socket;
    @Nonnull
    private final WebSocketListener listener;
    @Nonnull
    private final URI uri;
    @Nonnull
    private final SecureRandomProvider secureRandomProvider;


    @Nonnull
    private final Object stateLock = new Object(); // 2
    @Nonnull
    private State state = State.NOT_STARTED;
    private WebSocketWriter outputStream;

	@Nonnull
    private final Object writeLock = new Object(); // 3
    // only accessable while CONNECTED
    private int writing = 0;

	/**
	 * @see NewWebSocket#create(URI, WebSocketListener)
	 */
	WebSocketConnection(@Nonnull Socket socket,
							   @Nonnull WebSocketListener listener,
							   @Nonnull URI uri,
							   @Nonnull SecureRandomProvider secureRandomProvider) {
        this.socket = socket;
        this.listener = listener;
        this.uri = uri;
        this.secureRandomProvider = secureRandomProvider;
    }

    /**
     * Connect to websocket
     *
     * @throws UnknownHostException
     *             when could not connect to selected host
     * @throws IOException
     *             thrown when I/O exception occur
     * @throws WrongWebsocketResponse
     *             thrown when wrong web socket response received
     * @throws InterruptedException
     *             thrown when interrupt method was invoked
     */
    public void connect() throws WrongWebsocketResponse,
            InterruptedException,
            IOException {
        synchronized (stateLock) {
            checkState(State.NOT_STARTED.equals(state), "Only in not started state can be called connect");
            state = State.CONNECTING;
            stateLock.notifyAll();
        }
        final WebSocketReader inputStream;
        try {
            socket.connect(new InetSocketAddress(uri.getHost(), getPort(uri)));
            inputStream = new WebSocketReader(socket.getInputStream());
            synchronized (stateLock) {
                outputStream = new WebSocketWriter(socket.getOutputStream());
            }
            final String secret = generateHandshakeSecret();
            writeHeaders(uri, secret);
            readHandshakeHeaders(inputStream, secret);
        } catch (IOException e) {
            synchronized (stateLock) {
                outputStream = null;
                if (State.DISCONNECTING.equals(state)) {
                    state = State.DISCONNECTED;
                    stateLock.notifyAll();
                    throw new InterruptedException();
                } else {
                    state = State.DISCONNECTED;
                    stateLock.notifyAll();
                    throw e;
                }
            }
        }

        try {
            synchronized (stateLock) {
                state = State.CONNECTED;
                stateLock.notifyAll();
            }
            listener.onConnected();
            //noinspection InfiniteLoopStatement
            for (;;) {
                doRead(inputStream);
            }
        } catch (NotConnectedException e) {
            synchronized (stateLock) {
                if (State.DISCONNECTING.equals(state)) {
                    throw new InterruptedException();
                } else {
                    throw new RuntimeException();
                }
            }
        } catch (IOException e) {
            synchronized (stateLock) {
                if (State.DISCONNECTING.equals(state)) {
                    throw new InterruptedException();
                } else {
                    throw e;
                }
            }
        } finally {
            synchronized (stateLock) {
                while (writing != 0) {
                    stateLock.wait();
                }
                state = State.DISCONNECTED;
                outputStream = null;
                stateLock.notifyAll();
            }
        }
    }

    /**
     * This method will create 16 bity random key encoded to base64 for header.
     * If secureRandom generator is not accessible it will generate empty array
     * encoded with base64 (thread safe)
     *
     * @return random handshake key
     */
    @Nonnull
    private String generateHandshakeSecret() {
        return secureRandomProvider.generateHandshakeSecret();
    }

    /**
     * Write websocket headers to outputStream (not thread safe)
     *
     * @param uri uri
     * @param secret secret that is written to headers
     * @throws IOException
     */
    private void writeHeaders(@Nonnull URI uri, @Nonnull String secret) throws IOException {
        checkNotNull(uri);
        checkNotNull(secret);
        final String portPart = uri.getPort() < 0 || uri.getPort() == 80 ? "" : ":"+String.valueOf(uri.getPort());

        outputStream.writeLine("GET " + uri.getPath() + " HTTP/1.1");
        outputStream.writeLine("Upgrade: websocket");
        outputStream.writeLine("Connection: Upgrade");
        outputStream.writeLine("Host: " + uri.getHost()+portPart);
        outputStream.writeLine("Origin: " + uri);
        outputStream.writeLine("Sec-WebSocket-Key: " + secret);
        outputStream.writeLine("Sec-WebSocket-Protocol: chat");
        outputStream.writeLine("Sec-WebSocket-Version: 13");
        outputStream.writeNewLine();
        outputStream.flush();
    }

    /**
     * Read headers from connection input stream, parse them and ensure that
     * everything is correct. (not thread safe)
     *
     * @param inputStream input stream to read data
     * @param key string sent by client to server
     * @throws IOException throw when connection was broken
     * @throws WrongWebsocketResponse throw when wrong response was given from server (hanshake
     *  error)
     */
    private void readHandshakeHeaders(@Nonnull WebSocketReader inputStream, @Nonnull String key) throws IOException,
            WrongWebsocketResponse {
        checkNotNull(key);
        // schould get response:
        // HTTP/1.1 101 Switching Protocols
        // Upgrade: websocket
        // Connection: Upgrade
        // Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
        final StatusLine statusLine;
        final List<Header> headers = new ArrayList<>();
        try {
            final String statusLineStr = inputStream.readLine();
            if (Strings.isNullOrEmpty(statusLineStr)) {
                throw new WrongWebsocketResponse(
                        "Wrong HTTP response status line");
            }
            statusLine = BasicLineParser.parseStatusLine(statusLineStr, null);
            for (;;) {
                final String headerLineStr = inputStream.readLine();
                if (Strings.isNullOrEmpty(headerLineStr))
                    break;
                final Header header = BasicLineParser
                        .parseHeader(headerLineStr, null);
                headers.add(header);
            }
        } catch (ParseException e) {
            throw new WrongWebsocketResponse("Wrong HTTP response", e);
        }

        verifyHandshakeStatusLine(statusLine);
        verifyHanshakeHeaders(key, headers);
    }

    /**
     * Verify is status line code is correct (thread safe)
     *
     * @param statusLine
     *            status line from server response
     * @throws WrongWebsocketResponse
     *             thrown when status line is incorrect
     */
    private static void verifyHandshakeStatusLine(@Nonnull StatusLine statusLine)
            throws WrongWebsocketResponse {
        checkNotNull(statusLine);
        if (statusLine.getStatusCode() != HttpStatus.SC_SWITCHING_PROTOCOLS) {
            throw new WrongWebsocketResponse("Wrong http response status");
        }
    }

    @Override
    public void sendByteMessage(@Nonnull byte[] buffer) throws IOException,
            InterruptedException, NotConnectedException {
        sendMessage(OPCODE_BINARY_FRAME, generateMask(), buffer);
    }

    @Override
    public void sendPingMessage(@Nonnull byte[] buffer) throws IOException,
            InterruptedException, NotConnectedException {
        sendMessage(OPCODE_PING_FRAME, generateMask(), buffer);
    }

    private void sendPongMessage(@Nonnull byte[] buffer) throws IOException,
            InterruptedException, NotConnectedException {
        sendMessage(OPCODE_PONG_FRAME, generateMask(), buffer);
    }


    @Override
    public void sendStringMessage(@Nonnull String message) throws IOException,
            InterruptedException, NotConnectedException {
        checkNotNull(message, "Message can not be null");
        final byte[] buffer = message.getBytes("UTF-8");
        sendMessage(OPCODE_TEXT_FRAME, generateMask(), buffer);
    }



    /**
     * Read a single message from websocket (not thread safe)
     *
     * @param inputStream input stream
     *
     * @throws IOException
     * @throws WrongWebsocketResponse
     * @throws InterruptedException
     * @throws NotConnectedException
     */
    private void doRead(@Nonnull WebSocketReader inputStream) throws IOException, WrongWebsocketResponse,
            InterruptedException, NotConnectedException {
        final int first = inputStream.readByteOrThrow();
        final int reserved = first & RESERVED;
        if (reserved != 0) {
            throw new WrongWebsocketResponse(
                    "Server expected some negotiation that is not supported");
        }
        final boolean fin = (first & FIN) != 0;
        final int opcode = first & OPCODE;
        final int second = inputStream.readByteOrThrow();
        final boolean payloadMask = (second & PAYLOAD_MASK) != 0;

        long payloadLen = (second & (~PAYLOAD_MASK));
        if (payloadLen == 127) {
            payloadLen = inputStream.read64Long();
        } else if (payloadLen == 126) {
            payloadLen = inputStream.read16Int();
        }
        @Nullable
        final byte[] maskingKey;
        if (payloadMask) {
            byte[] mask_key = new byte[4];
            inputStream.readBytesOrThrow(mask_key);
            maskingKey = mask_key;
        } else {
            maskingKey = null;
        }
        readPayload(inputStream, fin, opcode, maskingKey, payloadLen);
    }

    /**
     * Read payload from websocket (not thread safe)
     *
     * Method currently does not support
     *
     * @param inputStream Input stream to read data
     * @param fin Indicates that this is the final fragment in a message.  The first
     * fragment MAY also be the final fragment.
     * @param opcode Defines the interpretation of the "Payload data"
     * @param maskingKey Defines whether the "Payload data" is masked.
     * @param payloadLen The length of the "Payload data"
     * @throws WrongWebsocketResponse when there is an error in websocket message
     * @throws IOException
     * @throws InterruptedException
     * @throws NotConnectedException
     */
    private void readPayload(@Nonnull WebSocketReader inputStream,
                             boolean fin,
                             int opcode,
                             @Nullable byte[] maskingKey,
                             long payloadLen)
            throws WrongWebsocketResponse, IOException, InterruptedException,
            NotConnectedException {

        if (payloadLen > 1024 * 1024 || payloadLen < 0) {
            throw new WrongWebsocketResponse("too large payload");
        }
        if (!fin) {
            // TODO
            throw new WrongWebsocketResponse(
                    "We do not support not continued frames");
        }
        byte[] payload = new byte[(int) payloadLen];
        inputStream.readBytesOrThrow(payload);

        if (maskingKey != null) {
            maskBuffer(payload, maskingKey);
        }

        if (opcode == OPCODE_CONTINUED_FRAME) {
            // TODO
            throw new WrongWebsocketResponse(
                    "We do not support not continued frames");
        } else if (opcode == OPCODE_TEXT_FRAME) {
            String message = new String(payload, "UTF-8");
            listener.onStringMessage(message);
        } else if (opcode == OPCODE_BINARY_FRAME) {
            listener.onBinaryMessage(payload);
        } else if (opcode == OPCODE_CONNECTION_CLOSE_FRAME) {
            listener.onServerRequestedClose(payload);
        } else if (opcode == OPCODE_PONG_FRAME) {
            listener.onPong(payload);
        } else if (opcode == OPCODE_PING_FRAME) {
            listener.onPing(payload);
            sendPongMessage(payload);
        } else {
            listener.onUnknownMessage(payload);
        }
    }

    /**
     * Generate 4 bit random mask to send message (thread safe)
     *
     * @return 4 bit random mask
     */
    @Nullable
    private byte[] generateMask() {
        return secureRandomProvider.generateMask();
    }

    /**
     * This method will apply mask to given buffer (thread safe)
     *
     * @param buffer
     *            buffer to apply mask
     * @param mask
     *            4 byte length mask to apply
     */
    static void maskBuffer(@Nonnull byte[] buffer, @Nonnull byte[] mask) {
        checkNotNull(mask);
        checkNotNull(buffer);
        checkArgument(mask.length == 4);

        for (int i = 0; i < buffer.length; i++) {
            int j = i % 4;
            buffer[i] = (byte) (buffer[i] ^ mask[j]);
        }
    }

    /**
     * Send a message to socket
     *
     * (thread safe)
     *
     * @param opcode
     *            - type of message (0x00-0x0f) <a
     *            href="http://tools.ietf.org/html/rfc6455#section-11.8">rfc6455
     *            opcode</a>
     * @param mask
     *            - message mask key (4 byte length) or null if
     *            message should not be masked
     * @param buffer
     *            buffer that will be sent to user. This buffer will be changed
     *            if mask will be set
     * @throws IOException
     *             - when write error occur
     * @throws InterruptedException
     *             - when socket was interrupted
     * @throws NotConnectedException
     *             - when socket was not connected
     */
    private void sendMessage(int opcode, @Nullable byte[] mask, @Nonnull byte[] buffer)
            throws IOException, InterruptedException, NotConnectedException {
        checkNotNull(buffer, "buffer should not be null");
        final WebSocketWriter outputStream;
        synchronized (stateLock) {
            if (!State.CONNECTED.equals(state)) {
                throw new NotConnectedException();
            }
            writing += 1;
            outputStream = this.outputStream;
        }
        try {
            synchronized (writeLock) {
                sendHeader(outputStream, true, opcode, mask, buffer.length);
                if (mask != null) {
                    maskBuffer(buffer, mask);
                }
                outputStream.writeBytes(buffer);
                outputStream.flush();

            }
        } catch (IOException e) {
            synchronized (stateLock) {
                if (State.DISCONNECTING.equals(state)) {
                    throw new InterruptedException();
                } else {
                    throw e;
                }
            }
        } finally {
            synchronized (stateLock) {
                writing -= 1;
                stateLock.notifyAll();
            }
        }
    }

    /**
     * Send message header to output stream. Should be safed with writeLock and
     * should append mWrite lock. Look at
     * {@link #sendMessage(int, byte[], byte[])}.
     *
     * (not thread safe)
     *
     *
     * @param outputStream
     *          output stream for sending header data
     * @param fin
     *            if message is last from sequence
     * @param opcode
     *            - type of message (0x00-0x0f)<a
     *            href="http://tools.ietf.org/html/rfc6455#section-11.8">rfc6455
     *            opcode</a>
     * @param mask
     *            - message mask key (4 byte length) or null if
     *            message should not be masked
     * @param length
     *            - length of message that will be sent after header
     * @throws IOException
     *             - thrown if connection was broken
     *
     * @see <a href="http://tools.ietf.org/html/rfc6455">rfc6455</a>
     * @see <a href="http://tools.ietf.org/html/rfc6455#section-5.2">rfc6455
     *      frame</a>
     */
    private static void sendHeader(@Nonnull WebSocketWriter outputStream, boolean fin, int opcode, @Nullable byte[] mask,
                                   long length) throws IOException {
        checkNotNull(outputStream);
        checkArgument(opcode >= 0x00 && opcode <= 0x0f,
                "opcode value should be between 0x00 and 0x0f");
        checkArgument(length >= 0, "length should not be negative");
        if (mask != null) {
            checkArgument(mask.length == 4, "Mask have to contain 4 bytes");
        }
        int first = opcode | (fin ? FIN : 0);
        outputStream.writeByte(first);

        int payloadMask = mask != null ? PAYLOAD_MASK : 0;

        if (length > 0xffff) {
            outputStream.writeByte(127 | payloadMask);
            outputStream.writeLong64(length);
        } else if (length >= 126) {
            outputStream.writeByte(126 | payloadMask);
            outputStream.writeInt16((int) length);
        } else {
            outputStream.writeByte((int) length | payloadMask);
        }

        if (mask != null) {
            outputStream.writeBytes(mask);
        }
    }



    /**
     * Verify if headers are correct with WebSocket handshake protocol. If
     * headers are not correct it will throw {@link WrongWebsocketResponse}
     * (thread safe)
     *
     * @param key
     *            - key sent to user
     * @param headers
     *            - headers received from server
     * @throws WrongWebsocketResponse
     *             - will be throw if headers are not correct
     */
    private static void verifyHanshakeHeaders(@Nonnull String key, @Nonnull List<Header> headers)
            throws WrongWebsocketResponse {
        checkNotNull(key);
        checkNotNull(headers);
        String webSocketAccept = null;
        String webSocketProtocol = null;
        for (Header header : headers) {
            String headerName = header.getName();
            if ("Sec-WebSocket-Accept".equalsIgnoreCase(headerName)) {
                if (webSocketAccept != null) {
                    throw new WrongWebsocketResponse(
                            "Sec-WebSocket-Accept should appear once");
                }
                webSocketAccept = header.getValue();
            } else if ("Sec-WebSocket-Protocol".equalsIgnoreCase(headerName)) {
                webSocketProtocol = header.getValue();
            }
        }
        if (webSocketAccept == null) {
            throw new WrongWebsocketResponse(
                    "Sec-WebSocket-Accept did not appear");
        }
        if (!verifyHandshakeAcceptValue(key, webSocketAccept)) {
            throw new WrongWebsocketResponse("Sec-WebSocket-Accept is wrong");
        }
        if (webSocketProtocol != null) {
            if ("chat".equals(webSocketProtocol)) {
                throw new WrongWebsocketResponse(
                        "Only supported is chat protocol");
            }
        }
    }

    /**
     * Verify if header Sec-WebSocket-Accept value is correct with sent key
     * value (thread safe)
     *
     * @param key key that was sent to server
     * @param acceptValue accept value received from server
     * @return true if accept value match key
     */
    private static boolean verifyHandshakeAcceptValue(@Nonnull String key,
                                                      @Nonnull String acceptValue) {
        checkNotNull(key);
        checkNotNull(acceptValue);
        String base = key + MAGIC_STRING;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] baseBytes = EncodingUtils.getAsciiBytes(base);
            md.update(baseBytes);
            byte[] sha1hash = md.digest();
            String expectedValue = Base64.encodeToString(sha1hash,
					Base64.NO_WRAP);
            return expectedValue.equals(acceptValue);
        } catch (NoSuchAlgorithmException e) {
            // if not found sha1 assume that response
            // value is OK
            return true;
        }
    }



    /**
     * Interrupt connect method. After interruption create should return
     * InterruptedException (thread safe)
     */
    public void interrupt() {
        synchronized (stateLock) {
            while (State.NOT_STARTED.equals(state)) {
                try {
                    stateLock.wait();
                } catch (InterruptedException ignore) {
                }
            }
            if (State.CONNECTING.equals(state)
                    || State.CONNECTED.equals(state)) {
                state = State.DISCONNECTING;
                stateLock.notifyAll();
            }
            while (!State.DISCONNECTED.equals(state)) {
                try {
                    socket.close();
                } catch (IOException ignore) {
                }
                try {
                    stateLock.wait();
                } catch (InterruptedException ignore) {
                }
            }
        }
    }

    /**
     * get websocket port from uri (thread safe)
     *
     * @param uri uri to get port from
     * @return port number
     * @throws IllegalArgumentException
     *             if unknwon schema
     */
    private static int getPort(@Nonnull URI uri) {
        checkNotNull(uri);
        int port = uri.getPort();
        if (port != -1)
            return port;

        String scheme = uri.getScheme();
        if ("wss".equals(scheme)) {
            return DEFAULT_WSS_PORT;
        } else if ("ws".equals(scheme)) {
            return DEFAULT_WS_PORT;
        } else {
            throw new IllegalArgumentException("Unknown schema");
        }
    }
}
