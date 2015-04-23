package com.example.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Locale;

public enum MessageType {
    REGISTER, REGISTERED, PING, DATA, PONG, ERROR, CHAT;

    public static class SerializerDeserializer implements JsonDeserializer<MessageType>, JsonSerializer<MessageType> {


        @Override
        public MessageType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            if (json.isJsonNull()) {
                return null;
            }
            final JsonPrimitive primitive = json.getAsJsonPrimitive();
            if (!primitive.isString()) {
                throw new JsonParseException("Non string type of type");
            }

            final String asString = json.getAsString();
            try {
                return MessageType.valueOf(asString.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new JsonParseException("Unknown request type: " + asString);
            }
        }


        @Override
        public JsonElement serialize(MessageType messageType, Type typeOfSrc, JsonSerializationContext context) {
            if (messageType == null) {
                return null;
            }
            return new JsonPrimitive(messageType.toString().toLowerCase(Locale.US));
        }
    }
}
