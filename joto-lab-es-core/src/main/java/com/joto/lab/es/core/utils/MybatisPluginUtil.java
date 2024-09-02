package com.joto.lab.es.core.utils;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import co.elastic.clients.elasticsearch._types.mapping.DocValuesPropertyBase;
import co.elastic.clients.elasticsearch._types.mapping.NumberPropertyBase;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.JsonpSerializable;
import co.elastic.clients.json.JsonpUtils;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joto.lab.es.core.annotations.EsField;
import com.joto.lab.es.core.enmus.EsFieldType;
import com.joto.lab.es.core.handler.*;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.TopLevelClass;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author joey
 * @date 2024/8/14 16:05
 */
public class MybatisPluginUtil {
    private static final Map<String, EsFieldType> TYPE_MAP = new HashMap<>();
    private static final Map<EsFieldType, IEsPropertyHandler> TYPE_PROPERTY_MAP = new HashMap<>();


    private static final String TPL_ES_FIELD = "@EsField(fieldName = \"{}\", fieldType = EsFieldType.{})";

    private static final String TPL_KEYWORD_FIELD = "@EsField(fieldName = \"{}\", fieldType = EsFieldType.KEYWORD, ignoreAbove = 128)";

    private static final String TPL_TEXT_FIELD = "@EsField(fieldName = \"{}\", fieldType = EsFieldType.TEXT, analyzer = EsAnalyzer.IK_MAX, searchAnalyzer = EsAnalyzer.IK_SMART)";

    private static final String IMPORT_SERIALIZER = "com.joto.lab.es.core.serializer.*";
    private static final String IMPORT_JACKSON = "com.fasterxml.jackson.databind.annotation.*";
    private static final String IMPORT_JSON_FORMAT = "com.fasterxml.jackson.annotation.JsonFormat";

    private static final String BIG_DECIMAL_STR = "java.math.BigDecimal";
    private static final String LOCAL_DATE_TIME_STR = "java.time.LocalDateTIme";

    static {

        TYPE_MAP.put(Object.class.getName(), EsFieldType.OBJECT);
        TYPE_MAP.put(String.class.getName(), EsFieldType.KEYWORD);
        TYPE_MAP.put(Boolean.class.getName(), EsFieldType.BOOLEAN);
        TYPE_MAP.put(Long.class.getName(), EsFieldType.LONG);
        TYPE_MAP.put(Integer.class.getName(), EsFieldType.INTEGER);
        TYPE_MAP.put(Double.class.getName(), EsFieldType.DOUBLE);
        TYPE_MAP.put(BigDecimal.class.getName(), EsFieldType.DOUBLE);
        TYPE_MAP.put(Float.class.getName(), EsFieldType.DOUBLE);
        TYPE_MAP.put(Short.class.getName(), EsFieldType.SHORT);
        TYPE_MAP.put(Date.class.getName(), EsFieldType.DATE);
        TYPE_MAP.put(LocalDate.class.getName(), EsFieldType.DATE);
        TYPE_MAP.put(LocalDateTime.class.getName(), EsFieldType.DATE);

        TYPE_PROPERTY_MAP.put(EsFieldType.KEYWORD, new KeywordHandler());
        TYPE_PROPERTY_MAP.put(EsFieldType.TEXT, new TextHandler());
        TYPE_PROPERTY_MAP.put(EsFieldType.LONG, new LongHandler());
        TYPE_PROPERTY_MAP.put(EsFieldType.INTEGER, new IntegerHandler());
        TYPE_PROPERTY_MAP.put(EsFieldType.BOOLEAN, new BoolHandler());
        TYPE_PROPERTY_MAP.put(EsFieldType.DOUBLE, new DoubleHandler());
        TYPE_PROPERTY_MAP.put(EsFieldType.SHORT, new ShortHandler());
        TYPE_PROPERTY_MAP.put(EsFieldType.DATE, new DateHandler());
    }

    private MybatisPluginUtil() {
    }

    /**
     * get handler
     *
     * @param esField es field
     * @return handler
     */
    public static IEsPropertyHandler getHandler(EsField esField) {
        return TYPE_PROPERTY_MAP.get(esField.fieldType());
    }

    /**
     * build
     *
     * @param esField es filed
     * @return property
     */
    public static Property build(EsField esField) {
        final IEsPropertyHandler handler = getHandler(esField);
        return handler.build(esField);
    }

    public static void docValues(DocValuesPropertyBase.AbstractBuilder<?> builder, EsField esField) {
        if (!esField.docValues()) {
            builder.docValues(false);
        }
    }

    public static void index(NumberPropertyBase.AbstractBuilder<?> builder, EsField esField) {
        if (!esField.index()) {
            builder.index(false);
        }
    }

    public static void store(NumberPropertyBase.AbstractBuilder<?> builder, EsField esField) {
        if (esField.store()) {
            builder.store(true);
        }
    }


    public static EsFieldType getEsFieldType(String javaType) {
        final EsFieldType esFieldType = TYPE_MAP.get(javaType);
        return esFieldType == null ? EsFieldType.KEYWORD : esFieldType;
    }

    public static String typeMapping2String(TypeMapping typeMapping) {

        StringBuilder sb = new StringBuilder();

        JsonpUtils.toString(typeMapping, sb);

        return  sb.toString();
    }

    public static String typeMapping2PrettyJsonStr(TypeMapping typeMapping) {
        String json = typeMapping2String(typeMapping);
        return JSONUtil.formatJsonStr(json);
    }

    public static void setFieldEsAnnotation(Field field, IntrospectedColumn column, TopLevelClass clazz) {
        final EsFieldType esFieldType = MybatisPluginUtil.getEsFieldType(field.getType().toString());

        if (esFieldType == EsFieldType.KEYWORD) {
            if (column.getLength() > 128) {
                field.addAnnotation(StrUtil.format(TPL_TEXT_FIELD, field.getName()));
            } else {
                field.addAnnotation(StrUtil.format(TPL_KEYWORD_FIELD, field.getName()));
            }
        } else {
            field.addAnnotation(StrUtil.format(TPL_ES_FIELD, field.getName(), esFieldType));
        }

        switch (column.getFullyQualifiedJavaType().getFullyQualifiedName()) {
            case BIG_DECIMAL_STR:
                clazz.addImportedType(BigDecimal.class.getName());
                break;
            case LOCAL_DATE_TIME_STR:
                clazz.addImportedType(LocalDateTime.class.getName());
                break;
            default:
                break;
        }

        String remarks = column.getRemarks();
        if (StrUtil.isBlank(remarks)) {
            remarks = field.getName();
        }

        field.addJavaDocLine("/**");
        field.addJavaDocLine(" * " + remarks);
        field.addJavaDocLine(" */");
    }

    public static void localDateTimeSerialize(Field field, TopLevelClass clazz) {
        if (MybatisPluginUtil.getEsFieldType(field.getType().toString()) == EsFieldType.DATE) {
            field.addAnnotation("@JsonSerialize(using = LocalDateTimeSerializer.class)");
            field.addAnnotation("@JsonDeserialize(using = ZoneDateTimeDeserializer.class)");

            clazz.addImportedType(IMPORT_SERIALIZER);
            clazz.addImportedType(IMPORT_JACKSON);
        }
    }

    public static void localDateTimeForamtter(Field field, TopLevelClass clazz) {
        if (MybatisPluginUtil.getEsFieldType(field.getType().toString()) == EsFieldType.DATE) {
            field.addAnnotation("@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = \"yyyy-MM-dd\", timezone = \"GMT+8\"");
            clazz.addImportedType(IMPORT_JSON_FORMAT);
        }
    }

    public static void addClassJavaDoc(TopLevelClass topLevelClass, IntrospectedTable introspectedTable, String remarks) {
        topLevelClass.addJavaDocLine("/**");
        topLevelClass.addJavaDocLine(" * " + remarks);
        topLevelClass.addJavaDocLine(" * @author " + introspectedTable.getContext().getCommentGeneratorConfiguration().getProperty("author"));
        topLevelClass.addJavaDocLine(" * @date " + DateTime.now());
        topLevelClass.addJavaDocLine(" */");
    }
}
