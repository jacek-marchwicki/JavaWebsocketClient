package com.appunite.websocket.rx.json.messages;

import com.appunite.websocket.rx.json.JsonWebSocketSender;
import com.google.gson.JsonParseException;

import javax.annotation.Nonnull;

public class RxJsonEventWrongMessageFormat extends RxJsonEventConn {
    @Nonnull
    private final String message;
    @Nonnull
    private final JsonParseException exception;

    public RxJsonEventWrongMessageFormat(@Nonnull JsonWebSocketSender sender, @Nonnull String message, @Nonnull JsonParseException exception) {
        super(sender);
        this.message = message;
        this.exception = exception;
    }

    @Nonnull
    public String message() {
        return message;
    }

    @Nonnull
    public JsonParseException exception() {
        return exception;
    }

    @Override
    public String toString() {
        return "RxJsonEventCouldNotParse{" +
                "message='" + message + '\'' +
                '}';
    }
}
