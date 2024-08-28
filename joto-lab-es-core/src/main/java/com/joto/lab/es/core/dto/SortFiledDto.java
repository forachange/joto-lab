package com.joto.lab.es.core.dto;

import cn.hutool.core.convert.ConverterRegistry;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.json.JsonData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author joey
 * @date 2023/12/25 8:55
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SortFiledDto {

    private FieldValue.Kind kind;
    private Object value;

    public FieldValue convert() {
        final ConverterRegistry converterRegistry = ConverterRegistry.getInstance();
        switch (kind) {
            case Null:
                return FieldValue.NULL;
            case String:
                return FieldValue.of(value + "");
            case Boolean:
                final Boolean b = converterRegistry.convert(Boolean.class, value);
                return FieldValue.of(b);
            case Long:
                final Long l = converterRegistry.convert(Long.class, value);
                return FieldValue.of(l);
            case Double:
                final Double dbl = converterRegistry.convert(Double.class, value);
                return FieldValue.of(dbl);
            case Any:
                return FieldValue.of(JsonData.of(value + ""));
            default:
                return null;
        }
    }
}
