package com.joto.lab.es.core.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * @author joey
 * @date 2023/12/22 17:28
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PitDto<E> {

    /**
     * Point In Time
     * 如果返回值中 MORE 是 TRUE，再次查询时就需要将返回值里的 PIT 放到请求参数中
     */
    private String pit;

    /**
     * 数据排序方式
     * 如果返回值中 MORE 是 TRUE，再次查询时就需要将返回值里的 sorts 放到请求参数中
     */
    private List<SortFiledDto> sorts;

    /**
     * 具体数据集合
     */
    private List<E> list;

    /**
     * 数据总数
     */
    private Long total;

    /**
     * 是否还有更多数据
     * 如果是 true，表示还有更多数据，需要再次调用接口获取，并将 pit 及 sorts 放到请求参数中
     */
    private boolean more;
}
