package com.joto.lab.es.core.handler;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import com.joto.lab.es.core.annotations.EsField;
import com.joto.lab.es.core.enmus.EsAnalyzer;


/**
 * @author joey
 * @date 2024/8/19 9:21
 */
public class TextHandler implements IEsPropertyHandler {
    @Override
    public Property build(EsField esField) {
        return Property.of(pBuilder ->
                pBuilder.text(tBuilder -> {
                    if (esField.analyzer() != EsAnalyzer.NOT_ANALYZED) {
                        tBuilder.analyzer(esField.analyzer().getAnalyzer());
                    }
                    if (esField.searchAnalyzer() != EsAnalyzer.NOT_ANALYZED) {
                        tBuilder.searchAnalyzer(esField.searchAnalyzer().getAnalyzer());
                    }
                    return tBuilder;
                }));
    }
}
