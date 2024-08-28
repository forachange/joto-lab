package com.joto.lab.es.core.handler;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import com.joto.lab.es.core.annotations.EsField;
import com.joto.lab.es.core.utils.EsTypeUtil;

/**
 * @author joey
 * @date 2024/8/19 9:23
 */
public class IntegerHandler implements IEsPropertyHandler{

    private static final Property PROPERTY_INTEGER = Property.of(pBuilder -> pBuilder.integer(iBuilder -> iBuilder));

    @Override
    public Property build(EsField esField) {
        return Property.of(pBuilder -> pBuilder.integer(builder -> {
            EsTypeUtil.docValues(builder, esField);
            EsTypeUtil.index(builder, esField);
            EsTypeUtil.store(builder, esField);

            return builder;
        }));
    }
}
