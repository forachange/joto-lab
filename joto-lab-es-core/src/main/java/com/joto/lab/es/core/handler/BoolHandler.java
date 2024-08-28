package com.joto.lab.es.core.handler;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import com.joto.lab.es.core.annotations.EsField;
import com.joto.lab.es.core.utils.EsTypeUtil;

/**
 * @author joey
 * @date 2024/8/19 9:23
 */
public class BoolHandler implements IEsPropertyHandler {

    private static final Property PROPERTY_BOOL = Property.of(pBuilder -> pBuilder.boolean_(bBuilder -> bBuilder));

    @Override
    public Property build(EsField esField) {
        return Property.of(pBuilder -> pBuilder.boolean_(builder -> {

            EsTypeUtil.docValues(builder, esField);
            if (!esField.index()) {
                builder.index(false);
            }
            if (esField.store()) {
                builder.store(true);
            }
            
            return builder;
        }));
    }
}
