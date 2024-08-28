package com.joto.lab.es.core.dto;

import co.elastic.clients.util.BinaryData;
import lombok.Data;

/**
 * @author joey
 * @description
 * @date 2023/12/15 9:35
 */
@Data
public class EsData {

    /**
     * 主键ID
     */
    private String id;

    /**
     * 索引名称
     */
    private String indexName;

    /**
     * json data
     */
    private BinaryData binaryData;
}
