package com.appunite.websocket.rx.object;

public class ObjectParseException extends Exception {

    public ObjectParseException() {
    }

    public ObjectParseException(String message) {
        super(message);
    }

    public ObjectParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ObjectParseException(Throwable cause) {
        super(cause);
    }

    public ObjectParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
