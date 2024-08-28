package com.joto.lab.es.core.enmus;

/**
 * @author joey
 * @date 2024/8/13 15:45
 */
public enum EsFieldType {

    /**
     * text
     */
    TEXT("text"),

    /**
     * keyword
     */
    KEYWORD("keyword"),

    /**
     * nested类型
     */
    NESTED("nested"),

    /**
     * nested类型
     */
    OBJECT("object"),

    /**
     * boolean 数据类型
     */
    BOOLEAN("boolean"),

    /**
     * date 数据类型
     */
    DATE("date"),

    /**
     * double 数据类型
     */
    DOUBLE("double"),
    /**
     * integer 数据类型
     */
    INTEGER("integer"),

    /**
     * short 数据类型
     */
    SHORT("short"),

    /**
     * long 数据类型
     */
    LONG("long");

    private final String fieldType;

    EsFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getFieldType() {
        return fieldType;
    }
}
