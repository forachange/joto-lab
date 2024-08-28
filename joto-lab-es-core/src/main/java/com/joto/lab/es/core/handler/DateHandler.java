package com.joto.lab.es.core.handler;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import com.joto.lab.es.core.annotations.EsField;
import com.joto.lab.es.core.utils.EsTypeUtil;

/**
 * @author joey
 * @date 2024/8/19 9:23
 */
public class DateHandler implements IEsPropertyHandler{

    private static   final Property PROPERTY_DATE = Property.of(pBuilder -> pBuilder.date(dBuilder ->
            dBuilder.format("strict_date_optional_time||epoch_millis||yyyy-MM-dd HH:mm:ss||yyyy-MM-dd")));

    @Override
    public Property build(EsField esField) {
        return Property.of(pBuilder -> pBuilder.date(builder -> {
            EsTypeUtil.docValues(builder, esField);
            if (!esField.index()) {
                builder.index(false);
            }
            if (esField.store()) {
                builder.store(true);
            }

            return builder.format(esField.dateFormat());
        }));
    }
}
