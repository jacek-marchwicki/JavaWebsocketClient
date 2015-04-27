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

public class ChatMessage extends Message {
    @Nonnull
    public final String message;
    @Nonnull
    public final String from;

    public ChatMessage(@Nonnull String message, @Nonnull String from) {
        super(MessageType.CHAT);
        this.message = message;
        this.from = from;
    }

    @Nonnull
    public String message() {
        return message;
    }

    @Nonnull
    public String from() {
        return from;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatMessage)) return false;

        ChatMessage that = (ChatMessage) o;

        return message.equals(that.message) && from.equals(that.from);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + from.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "RegisterRequest{" +
                "message='" + message + '\'' +
                "from='" + from + '\'' +
                "} " + super.toString();
    }
}
