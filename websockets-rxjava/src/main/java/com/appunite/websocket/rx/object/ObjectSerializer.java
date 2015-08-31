package com.appunite.websocket.rx.object;

import javax.annotation.Nonnull;

public interface ObjectSerializer {
    @Nonnull Object serialize(@Nonnull String message) throws ObjectParseException;

    @Nonnull Object serialize(@Nonnull byte[] message) throws ObjectParseException;

    @Nonnull byte[] deserializeBinary(@Nonnull Object message) throws ObjectParseException;

    @Nonnull String deserializeString(@Nonnull Object message) throws ObjectParseException;

    boolean isBinary(@Nonnull Object message);
}
