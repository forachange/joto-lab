package com.joto.lab.es.core.annotations;

import com.joto.lab.es.core.enmus.EsFieldType;
import com.joto.lab.es.core.enmus.EsAnalyzer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * es doc field 注解
 * @author joey
 * @date 2024/8/13 17:23
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EsField {

    /**
     * 字段名称
     * @return name
     */
    String fieldName();

    /**
     * 字段类型,默认为 string
     * @return EsFieldType.KEYWORD
     */
    EsFieldType fieldType() default EsFieldType.KEYWORD;

    /**
     * keyword 类型时有效，默认 0
     * @return
     */
    int ignoreAbove() default 0;

    /**
     * 分词器,默认不进行分词
     * @return EsAnalyzer.NOT_ANALYZED
     */
    EsAnalyzer analyzer() default EsAnalyzer.NOT_ANALYZED;

    /**
     * 查询分词器,默认不进行分词
     * @return EsAnalyzer.NOT_ANALYZED
     */
    EsAnalyzer searchAnalyzer() default EsAnalyzer.NOT_ANALYZED;

    /**
     * doc_values, should the field be stored on disk in a column-stride fashion, used for sorting, aggregations, or scripting.
     * @return default true
     */
    boolean docValues() default true;

    /**
     * should the field be quickly searchable. 数字也可以仅通过 doc_values 进行查询，但比较慢。
     * @return default true
     */
    boolean index() default true;

    /**
     * Whether the field value should be stored and retrievable separately form the _source field.
     * @return default false
     */
    boolean store() default false;

    /**
     * 时间格式化
     * @return format
     */
    String dateFormat() default "strict_date_optional_time||epoch_millis||yyyy-MM-dd HH:mm:ss||yyyy-MM-dd";

}
