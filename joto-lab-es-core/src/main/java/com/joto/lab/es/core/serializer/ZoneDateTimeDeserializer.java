package com.joto.lab.es.core.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * es 8.8
 * @author joey
 * @date 2024/4/8 10:05
 */
public class ZoneDateTimeDeserializer extends LocalDateTimeDeserializer {

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        if (parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            final long value = parser.getValueAsLong();
            final Instant instant = Instant.ofEpochMilli(value);

            return LocalDateTime.ofInstant(instant, ZoneOffset.ofHours(8));
        }

        return super.deserialize(parser, ctxt);
    }
}
