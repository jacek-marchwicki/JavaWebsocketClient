package com.appunite.websocket.internal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface SecureRandomProvider {

    /**
     * The value of this header field MUST be a
     * nonce consisting of a randomly selected 16-byte value that has
     * been base64-encoded (see Section 4 of [RFC4648]).  The nonce
     * MUST be selected randomly for each connection.
     * @return random base64 encoded string
     */
    @Nonnull
    String generateHandshakeSecret();

    /**
     * Random byte[4] or Null
     */
    @Nullable
    byte[] generateMask();
}
