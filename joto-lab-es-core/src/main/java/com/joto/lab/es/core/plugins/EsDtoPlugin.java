package com.joto.lab.es.core.plugins;

import cn.hutool.core.date.DateTime;
import com.joto.lab.es.core.annotations.EsField;
import com.joto.lab.es.core.dto.Paging;
import com.joto.lab.es.core.utils.MybatisPluginUtil;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.generator.api.GeneratedJavaFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.TopLevelClass;

import java.util.ArrayList;
import java.util.List;

/**
 * 生成对应的 es dto类
 * @author joey
 * @date 2024/9/2 10:56
 */
@Slf4j
public class EsDtoPlugin extends PluginAdapter {

    private String targetProject;
    private String targetPackage;

    @Override
    public boolean validate(List<String> list) {
        this.targetPackage = this.properties.getProperty("targetPackage");
        this.targetProject = this.properties.getProperty("targetProject");

        return true;
    }

    @Override
    public List<GeneratedJavaFile> contextGenerateAdditionalJavaFiles(IntrospectedTable introspectedTable) {
        List<GeneratedJavaFile> javaFileList = new ArrayList<>(4);
        javaFileList.add(generatePagingDto(introspectedTable));
        javaFileList.add(generatePitDto(introspectedTable));

        return javaFileList;
    }

    private GeneratedJavaFile generatePagingDto(IntrospectedTable introspectedTable) {
        final String pagingDtoName = introspectedTable.getTableConfiguration().getDomainObjectName() + "Paging";
        final FullyQualifiedJavaType pagingDtoJavaType = new FullyQualifiedJavaType(targetPackage + "." + pagingDtoName);
        final FullyQualifiedJavaType absBaseService = new FullyQualifiedJavaType("Paging");

        TopLevelClass clazz = new TopLevelClass(pagingDtoJavaType);

        // 描述类的作用域修饰符
        clazz.setVisibility(JavaVisibility.PUBLIC);

        MybatisPluginUtil.addClassJavaDoc(clazz, introspectedTable,
            introspectedTable.getRemarks() + " PagingDto");

        clazz.setSuperClass(absBaseService);

        clazz.addImportedType(Paging.class.getName());
        clazz.addImportedType(EsField.class.getName());
        clazz.addImportedType("com.joto.lab.es.core.enmus.*");
        clazz.addImportedType("lombok.Data");
        clazz.addImportedType("lombok.EqualsAndHashCode");

        clazz.addAnnotation("@Data");
        clazz.addAnnotation("@EqualsAndHashCode(callSuper = true");

        introspectedTable.getAllColumns().forEach( column -> {
            Field field = new Field();
            field.setName(column.getJavaProperty());
            field.setInitializationString("");
            field.setFinal(false);
            field.setVisibility(JavaVisibility.PRIVATE);
            field.setType(column.getFullyQualifiedJavaType());

            MybatisPluginUtil.setFieldEsAnnotation(field, column, clazz);
            MybatisPluginUtil.localDateTimeForamtter(field, clazz);

            clazz.addField(field);
        });

        return new GeneratedJavaFile(clazz, targetProject, context.getJavaFormatter());
    }

    private GeneratedJavaFile generatePitDto(IntrospectedTable introspectedTable) {
        final String pitDtoName = introspectedTable.getTableConfiguration().getDomainObjectName() + "Pit";
        final FullyQualifiedJavaType pagingDtoJavaType = new FullyQualifiedJavaType(targetPackage + "." + pitDtoName);
        final FullyQualifiedJavaType absBaseService = new FullyQualifiedJavaType("Pit");

        TopLevelClass clazz = new TopLevelClass(pagingDtoJavaType);

        // 描述类的作用域修饰符
        clazz.setVisibility(JavaVisibility.PUBLIC);

        MybatisPluginUtil.addClassJavaDoc(clazz, introspectedTable,
            introspectedTable.getRemarks() + " PitDto");

        clazz.setSuperClass(absBaseService);

        clazz.addImportedType(Paging.class.getName());
        clazz.addImportedType(EsField.class.getName());
        clazz.addImportedType("com.joto.lab.es.core.enmus.*");
        clazz.addImportedType("lombok.Data");
        clazz.addImportedType("lombok.EqualsAndHashCode");

        clazz.addAnnotation("@Data");
        clazz.addAnnotation("@EqualsAndHashCode(callSuper = true");

        introspectedTable.getAllColumns().forEach( column -> {
            Field field = new Field();
            field.setName(column.getJavaProperty());
            field.setInitializationString("");
            field.setFinal(false);
            field.setVisibility(JavaVisibility.PRIVATE);
            field.setType(column.getFullyQualifiedJavaType());

            MybatisPluginUtil.setFieldEsAnnotation(field, column, clazz);
            MybatisPluginUtil.localDateTimeForamtter(field, clazz);

            clazz.addField(field);
        });

        return new GeneratedJavaFile(clazz, targetProject, context.getJavaFormatter());
    }
}
