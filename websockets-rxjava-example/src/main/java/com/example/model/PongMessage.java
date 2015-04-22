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
