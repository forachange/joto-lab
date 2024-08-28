package com.joto.lab.es.core.dto;

/**
 * es id
 * @author joey
 * @date 2024/8/21 10:31
 */
public interface EsId {

    /**
     * es id, 如果不指定的话，默认为 Null
     * @return id
     */
    default String generateId() {
        return null;
    }
}
