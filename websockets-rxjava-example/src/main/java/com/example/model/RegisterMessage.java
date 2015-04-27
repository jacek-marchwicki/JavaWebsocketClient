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

package com.example.model;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nonnull;

public class RegisterMessage extends Message {
    @SerializedName("auth_token")
    @Nonnull
    public final String authToken;

    public RegisterMessage(@Nonnull String authToken) {
        super(MessageType.REGISTER);
        this.authToken = authToken;
    }

    @Nonnull
    public String authToken() {
        return authToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegisterMessage)) return false;

        RegisterMessage that = (RegisterMessage) o;

        return authToken.equals(that.authToken);

    }

    @Override
    public int hashCode() {
        return authToken.hashCode();
    }

    @Override
    public String toString() {
        return "RegisterRequest{" +
                "message='" + authToken + '\'' +
                "} " + super.toString();
    }
}
