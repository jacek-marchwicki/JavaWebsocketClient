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
