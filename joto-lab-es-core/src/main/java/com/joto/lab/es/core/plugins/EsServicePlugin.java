package com.joto.lab.es.core.plugins;

import cn.hutool.core.date.DateTime;
import com.joto.lab.es.core.service.AbsBaseService;
import com.joto.lab.es.core.utils.MybatisPluginUtil;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.generator.api.GeneratedJavaFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.TopLevelClass;

import java.util.ArrayList;
import java.util.List;

/**
 * 生成对应的 es 查询服务类
 * @author joey
 * @date 2024/8/22 8:45
 */
@Slf4j
public class EsServicePlugin extends PluginAdapter {

    private static final String ABS_BASE_SERVICE = AbsBaseService.class.getName();
    private String targetProject;
    private String targetPackage;
    private String serviceFullName;
    private String serviceName;

    @Override
    public boolean validate(List<String> list) {
        this.targetPackage = this.properties.getProperty("targetPackage");
        this.targetProject = this.properties.getProperty("targetProject");

        return true;
    }

    @Override
    public List<GeneratedJavaFile> contextGenerateAdditionalJavaFiles(IntrospectedTable introspectedTable) {

        final String objectName = introspectedTable.getTableConfiguration().getDomainObjectName();

        serviceName = objectName + "ServiceImpl";
        serviceFullName = targetPackage + "." + serviceName;

        List<GeneratedJavaFile> javaFileList = new ArrayList<>(4);
        javaFileList.add(generateServiceImpl(introspectedTable));

        return javaFileList;
    }

    private GeneratedJavaFile generateServiceImpl(IntrospectedTable introspectedTable) {
        final FullyQualifiedJavaType serviceImpl = new FullyQualifiedJavaType(serviceFullName);
        final FullyQualifiedJavaType absBaseService = new FullyQualifiedJavaType("AbsBaseService");
        absBaseService.addTypeArgument(new FullyQualifiedJavaType(introspectedTable.getTableConfiguration().getDomainObjectName()));
        final FullyQualifiedJavaType stereotypeService = new FullyQualifiedJavaType("org.springframework.stereotype.Service");

        TopLevelClass clazz = new TopLevelClass(serviceImpl);

        // 描述类的作用域修饰符
        clazz.setVisibility(JavaVisibility.PUBLIC);

        MybatisPluginUtil.addClassJavaDoc(clazz, introspectedTable,
            introspectedTable.getRemarks() + " Service");

        clazz.addImportedType(introspectedTable.getBaseRecordType());
        clazz.addImportedType(ABS_BASE_SERVICE);
        clazz.setSuperClass(absBaseService);

        clazz.addImportedType(stereotypeService);
        clazz.addAnnotation("@Service(\"" + serviceName + "\")");

        return new GeneratedJavaFile(clazz, targetProject, context.getJavaFormatter());
    }


}
