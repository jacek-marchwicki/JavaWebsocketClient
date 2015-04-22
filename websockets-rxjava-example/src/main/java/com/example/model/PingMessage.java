package com.example.model;

import javax.annotation.Nonnull;

public class PingMessage extends Message {
    @Nonnull
    private String message;

    public PingMessage(@Nonnull String message) {
        super(MessageType.PING);
        this.message = message;
    }

    @Nonnull
    public String message() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PingMessage)) return false;
        if (!super.equals(o)) return false;

        PingMessage that = (PingMessage) o;

        return message.equals(that.message);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + message.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PingMessage{" +
                "message='" + message + '\'' +
                "} " + super.toString();
    }
}
