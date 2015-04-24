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

import com.appunite.websocket.tools.Base64;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SecureRandomProviderImpl implements SecureRandomProvider {

    private boolean error = false;
    private SecureRandom secureRandom;


    @Nullable
    private synchronized SecureRandom getSecureRandom() {
        if (error) {
            return null;
        }
        if (secureRandom != null) {
            return secureRandom;
        }
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
            return secureRandom;
        } catch (NoSuchAlgorithmException e) {
            // if we do not have secure random we have to leave data unmasked
            error = true;
            return null;
        }
    }

    @Nonnull
    @Override
    public synchronized String generateHandshakeSecret() {
        final SecureRandom secureRandom = getSecureRandom();

        byte[] nonce = new byte[16];
        if (secureRandom != null) {
            secureRandom.nextBytes(nonce);
        } else {
            Arrays.fill(nonce, (byte) 0);
        }
        return Base64.encodeToString(nonce, Base64.NO_WRAP);
    }

    @Nullable
    @Override
    public synchronized byte[] generateMask() {
        final SecureRandom secureRandom = getSecureRandom();
        if (secureRandom == null)
            return null;

        final byte[] bytes = new byte[4];
        secureRandom.nextBytes(bytes);
        return bytes;
    }
}
