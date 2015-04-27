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

package com.appunite.websocket.internal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class for generating random numbers
 */
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
