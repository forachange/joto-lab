package com.joto.lab.es.core.dto;

import lombok.Data;

import java.util.List;

/**
 * pit, 请求参数
 * @author joey
 * @date 2023/12/22 17:39
 */
@Data
public class Pit {

    /**
     * 深分页指定参数。第一次查询时不需要，如果返回值中 MORE 是 TRUE，再次查询时就需要将返回值里的 PIT 放到请求参数中
     */
    private String pit;

    /**
     * 数据排序方式
     * 第一次查询时不需要，如果返回值中 MORE 是 TRUE，再次查询时就需要将返回值里的 sorts 放到请求参数中
     */
    private List<SortFiledDto> sorts;
}
