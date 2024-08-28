package com.joto.lab.es.core.plugins;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.StrUtil;
import com.joto.lab.es.core.annotations.EsField;
import com.joto.lab.es.core.annotations.EsIndex;
import com.joto.lab.es.core.dto.EsId;
import com.joto.lab.es.core.enmus.EsFieldType;
import com.joto.lab.es.core.utils.EsTypeUtil;
import com.joto.lab.es.core.enmus.EsAnalyzer;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;

import java.util.List;

/**
 * 在类及字段上增加 es 相关的注解
 * @author joey
 * @date 2024/8/12 20:16
 */
public class EsMybatisPlugin extends PluginAdapter {

    private static final String TPL_ES_INDEX = "@EsIndex(name = \"{}\")";

    private static final String TPL_ES_FIELD = "@EsField(fieldName = \"{}\", fieldType = EsFieldType.{})";

    private static final String TPL_KEYWORD_FIELD = "@EsField(fieldName = \"{}\", fieldType = EsFieldType.KEYWORD, ignoreAbove = 128)";

    private static final String TPL_TEXT_FIELD = "@EsField(fieldName = \"{}\", fieldType = EsFieldType.TEXT, analyzer = EsAnalyzer.IK_MAX, searchAnalyzer = EsAnalyzer.IK_SMART)";


    private static final String IMPORT_SERIALIZER = "com.joto.lab.es.core.serializer.*";
    private static final String IMPORT_JACKSON = "com.fasterxml.jackson.databind.annotation.*";

    @Override
    public boolean modelFieldGenerated(Field field, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {

        final EsFieldType esFieldType = EsTypeUtil.getEsFieldType(field.getType().toString());

        if (esFieldType == EsFieldType.KEYWORD) {
            if (introspectedColumn.getLength() > 128) {
                field.addAnnotation(StrUtil.format(TPL_TEXT_FIELD, field.getName()));
            } else {
                field.addAnnotation(StrUtil.format(TPL_KEYWORD_FIELD, field.getName()));
            }
        } else {
            field.addAnnotation(StrUtil.format(TPL_ES_FIELD, field.getName(), esFieldType));
        }

        if (esFieldType == EsFieldType.DATE) {
            field.addAnnotation("@JsonSerialize(using = LocalDateTimeSerializer.class)");
            field.addAnnotation("@JsonDeserialize(using = ZoneDateTimeDeserializer.class)");

            topLevelClass.addImportedType(IMPORT_SERIALIZER);
            topLevelClass.addImportedType(IMPORT_JACKSON);
        }

        String remarks = introspectedColumn.getRemarks();
        if (StrUtil.isBlank(remarks)) {
            remarks = field.getName();
        }

        field.addJavaDocLine("/**");
        field.addJavaDocLine(" * " + remarks);
        field.addJavaDocLine(" */");

        return super.modelFieldGenerated(field, topLevelClass, introspectedColumn, introspectedTable, modelClassType);
    }

    @Override
    public boolean modelGetterMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        return false;
    }

    @Override
    public boolean modelSetterMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        return false;
    }

    @Override
    public boolean validate(List<String> list) {
        return true;
    }

    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {

        final FullyQualifiedJavaType interfaceEsId = new FullyQualifiedJavaType(EsId.class.getName());
        topLevelClass.addImportedType(interfaceEsId);
        topLevelClass.addImportedType(EsIndex.class.getName());
        topLevelClass.addImportedType(EsField.class.getName());
        topLevelClass.addImportedType(EsAnalyzer.class.getName());
        topLevelClass.addImportedType(EsFieldType.class.getName());
        topLevelClass.addImportedType("lombok.Data");
        topLevelClass.addImportedType("lombok.NoArgsConstructor");
        topLevelClass.addImportedType("lombok.AllArgsConstructor");

        topLevelClass.addAnnotation("@Data");
        topLevelClass.addAnnotation("@NoArgsConstructor");
        topLevelClass.addAnnotation("@AllArgsConstructor");
        topLevelClass.addAnnotation(StrUtil.format(TPL_ES_INDEX,  StrUtil.toSymbolCase(introspectedTable.getTableConfiguration().getDomainObjectName(), '-')));

        String remarks = introspectedTable.getRemarks();
        if (StrUtil.isBlank(remarks)) {
            remarks = "EsMybatisPlugin Generated";
        }

        topLevelClass.addJavaDocLine("/**");
        topLevelClass.addJavaDocLine(" * " + remarks);
        topLevelClass.addJavaDocLine(" * @author " + introspectedTable.getContext().getCommentGeneratorConfiguration().getProperty("author"));
        topLevelClass.addJavaDocLine(" * @date " + DateTime.now());
        topLevelClass.addJavaDocLine(" */");

        topLevelClass.addSuperInterface(interfaceEsId);

        return super.modelBaseRecordClassGenerated(topLevelClass, introspectedTable);
    }

}

