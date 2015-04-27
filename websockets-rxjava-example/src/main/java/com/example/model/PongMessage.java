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

import javax.annotation.Nonnull;

public class PongMessage extends Message {
    @Nonnull
    private final String response;

    public PongMessage(@Nonnull String response) {
        super(MessageType.PONG);
        this.response = response;
    }

    @Nonnull
    public String response() {
        return response;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PongMessage)) return false;
        if (!super.equals(o)) return false;

        PongMessage that = (PongMessage) o;

        return response.equals(that.response);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + response.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PongResponse{" +
                "message='" + response + '\'' +
                "} " + super.toString();
    }
}
