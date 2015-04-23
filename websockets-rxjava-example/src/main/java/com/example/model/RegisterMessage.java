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
