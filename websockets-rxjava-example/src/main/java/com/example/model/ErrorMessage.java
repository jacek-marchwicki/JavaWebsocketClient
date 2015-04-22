package com.example.model;

import javax.annotation.Nonnull;

public class ErrorMessage extends Message {
    @Nonnull
    private final String response;

    public ErrorMessage(@Nonnull String response) {
        super(MessageType.ERROR);
        this.response = response;
    }

    @Nonnull
    public String response() {
        return response;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ErrorMessage)) return false;
        if (!super.equals(o)) return false;

        ErrorMessage that = (ErrorMessage) o;

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
        return "ErrorResponse{" +
                "message='" + response + '\'' +
                "} " + super.toString();
    }
}
