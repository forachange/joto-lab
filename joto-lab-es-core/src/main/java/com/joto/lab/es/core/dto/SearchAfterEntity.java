package com.joto.lab.es.core.dto;

import co.elastic.clients.elasticsearch._types.FieldValue;
import lombok.Data;

import java.util.List;

/**
 * @author joey
 * @description
 * @date 2023/12/13 15:10
 */
@Data
public class SearchAfterEntity<E> {
    private String pit;
    private boolean result;
    private List<E> docList;
    private List<FieldValue> sorts;
}
