package com.example.model;

import javax.annotation.Nonnull;

public class DataMessage extends Message {
    @Nonnull
    private final String id;
    @Nonnull
    private final String message;

    public DataMessage(@Nonnull String id, @Nonnull String message) {
        super(MessageType.DATA);
        this.id = id;
        this.message = message;
    }

    @Nonnull
    public String message() {
        return message;
    }

    @Nonnull
    public String id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataMessage)) return false;
        if (!super.equals(o)) return false;

        DataMessage that = (DataMessage) o;

        if (!id.equals(that.id)) return false;
        return message.equals(that.message);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + id.hashCode();
        result = 31 * result + message.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DataResponse{" +
                "id='" + id + '\'' +
                ", message='" + message + '\'' +
                "} " + super.toString();
    }
}
