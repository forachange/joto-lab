package com.joto.lab.es.core.enmus;

/**
 * ES 内置分析器
 * @author joey
 */
public enum EsAnalyzer {

    /**
     * standard tokenizer, standard filter, lower case filter, stop filter
     */
    STANDARD("standard"),

    /**
     * 按照非字母切分（符号被过滤），小写处理
     */
    SIMPLE("simple"),

    /**
     * 小写处理，停用词过滤（the，a，is）
     */
    STOP("stop"),

    /**
     * 不分词，内容整体作为一个token(not_analyzed)
     */
    KEYWORD("keyword"),

    /**
     * 按照空格切分，不转小写
     */
    WHITESPACE("whitespace"),

    /**
     * 正则表达式，默认 \W+ (非字符分隔)
     */
    PATTERN("pattern"),

    /**
     * 提供了30多种常见语言的分词器
     */
    LANGUAGE("language"),

    /**
     * 不进行索引
     */
    NOT_ANALYZED("not_analyzed"),

    /**
     * 细粒度分割
     */
    IK_MAX("ik_max_word"),

    /**
     * 粗粒度分割
     */
    IK_SMART("ik_smart");

    private final String analyzer;

    EsAnalyzer(String analyzer) {
        this.analyzer = analyzer;
    }

    public String getAnalyzer() {
        return analyzer;
    }
}