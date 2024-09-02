package com.joto.lab.es.core.handler;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import com.joto.lab.es.core.annotations.EsField;
import com.joto.lab.es.core.utils.MybatisPluginUtil;

/**
 * @author joey
 * @date 2024/8/19 9:19
 */
public class KeywordHandler implements IEsPropertyHandler{
    @Override
    public Property build(EsField esField) {
        return Property.of(pBuilder -> pBuilder.keyword(builder -> {
            if (esField.ignoreAbove() > 0) {
                builder.ignoreAbove(esField.ignoreAbove());
            }
            MybatisPluginUtil.docValues(builder, esField);
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
