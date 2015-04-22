package com.example.model;

public class RegisteredMessage extends Message {

    public RegisteredMessage() {
        super(MessageType.REGISTERED);
    }

    @Override
    public String toString() {
        return "RegisteredResponse{}";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RegisteredMessage;
    }

    @Override
    public int hashCode() {
        return 1;
    }
}
