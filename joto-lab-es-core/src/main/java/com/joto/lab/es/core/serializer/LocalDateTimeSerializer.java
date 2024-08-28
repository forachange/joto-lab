package com.joto.lab.es.core.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * @author joey
 * @description
 * @date 2023/12/18 16:55
 */
public class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {

    private static final ZoneOffset ZONE_OFFSET = ZoneOffset.of("+8");

    @Override
    public void serialize(LocalDateTime localDateTime, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeNumber(localDateTime.toInstant(ZONE_OFFSET).toEpochMilli());
    }
}
