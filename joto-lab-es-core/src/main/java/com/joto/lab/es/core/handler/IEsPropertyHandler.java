package com.joto.lab.es.core.handler;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import com.joto.lab.es.core.annotations.EsField;

/**
 * @author joey
 * @date 2024/8/19 9:16
 */
public interface IEsPropertyHandler {

    /**
     * build
     * @param esField field annotation
     * @return property
     */
    Property build(EsField esField);
}
