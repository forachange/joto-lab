package com.joto.lab.es.core.handler;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import com.joto.lab.es.core.annotations.EsField;
import com.joto.lab.es.core.utils.MybatisPluginUtil;


/**
 * @author joey
 * @date 2024/8/19 9:23
 */
public class LongHandler implements IEsPropertyHandler{

    @Override
    public Property build(EsField esField) {
        return Property.of(pBuilder -> pBuilder.long_(builder -> {
            MybatisPluginUtil.docValues(builder, esField);
            MybatisPluginUtil.index(builder, esField);
            MybatisPluginUtil.store(builder, esField);

            return builder;
        }));
    }
}
