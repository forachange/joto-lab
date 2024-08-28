package com.joto.lab.es.core.dto;

import lombok.Data;

/**
 * 分页，请求参数
 * @author joey
 * @date 2024/8/20 16:17
 */
@Data
public class Paging {

    /**
     * 每页条数
     */
    private int size;

    /**
     * 当前页
     */
    private int index;
}
