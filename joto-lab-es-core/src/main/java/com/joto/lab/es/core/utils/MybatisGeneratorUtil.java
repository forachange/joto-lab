package com.joto.lab.es.core.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ClassLoaderUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import com.joto.lab.es.core.annotations.EsField;
import com.joto.lab.es.core.annotations.EsIndex;
import com.joto.lab.es.core.config.MyBatisGeneratorConfig;
import com.joto.lab.es.core.plugins.EsDtoPlugin;
import com.joto.lab.es.core.plugins.EsMybatisPlugin;
import com.joto.lab.es.core.plugins.EsServicePlugin;
import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.api.ProgressCallback;
import org.mybatis.generator.config.*;
import org.mybatis.generator.exception.InvalidConfigurationException;
import org.mybatis.generator.internal.DefaultShellCallback;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.AnnotationFormatError;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author joey
 * @date 2024/8/23 15:47
 */
public class MybatisGeneratorUtil {

    static String tpl_mapping = "PUT {}\r\n{\r    \"mappings\": {}\r\n}";

    /**
     * generate entity and service
     *
     * @param config config
     * @throws InvalidConfigurationException ice
     * @throws InterruptedException          ie
     * @throws SQLException                  sql
     * @throws IOException                   io
     */
    public static void generateClass(MyBatisGeneratorConfig config, String[] tables, String[] domains) throws InvalidConfigurationException, InterruptedException, SQLException, IOException {

        generateClass(config, tables, domains, null);
    }

    public static void generateClass(MyBatisGeneratorConfig config, String[] tables, String[] domains,
                                     ProgressCallback callback) throws InvalidConfigurationException, InterruptedException, SQLException, IOException {

        final Configuration configuration = new Configuration();

        Context context = new Context(null);
        context.setId("simple");
        context.setTargetRuntime("MyBatis3");

        final PluginConfiguration esPlugin = new PluginConfiguration();
        esPlugin.setConfigurationType(EsMybatisPlugin.class.getName());
        context.addPluginConfiguration(esPlugin);

        final PluginConfiguration servicePlugin = new PluginConfiguration();
        servicePlugin.setConfigurationType(EsServicePlugin.class.getName());
        servicePlugin.addProperty("targetPackage", config.getServiceTargetPackage());
        servicePlugin.addProperty("targetProject", config.getTargetProject());
        context.addPluginConfiguration(servicePlugin);

        final PluginConfiguration dtoPlugin = new PluginConfiguration();
        dtoPlugin.setConfigurationType(EsDtoPlugin.class.getName());
        dtoPlugin.addProperty("targetPackage", config.getDtoTargetPackage());
        dtoPlugin.addProperty("targetProject", config.getTargetProject());
        context.addPluginConfiguration(dtoPlugin);

        final CommentGeneratorConfiguration commentGeneratorConfiguration = new CommentGeneratorConfiguration();
        commentGeneratorConfiguration.addProperty("suppressAllComments", "true");
        commentGeneratorConfiguration.addProperty("suppressDate", "true");
        commentGeneratorConfiguration.addProperty("addRemarkComments", "false");
        commentGeneratorConfiguration.addProperty("author", config.getAuthor());
        context.setCommentGeneratorConfiguration(commentGeneratorConfiguration);

        JDBCConnectionConfiguration jdbcConnectionConfiguration = new JDBCConnectionConfiguration();
        jdbcConnectionConfiguration.setConnectionURL(config.getMysqlUrl());
        jdbcConnectionConfiguration.setDriverClass("com.mysql.cj.jdbc.Driver");
        jdbcConnectionConfiguration.setUserId(config.getMysqlUser());
        jdbcConnectionConfiguration.setPassword(config.getMysqlPwd());
        // 获取表注释需要设置为 true, 而且不能通过 mycat
        jdbcConnectionConfiguration.addProperty("useInformationSchema", "true");
        context.setJdbcConnectionConfiguration(jdbcConnectionConfiguration);

        final JavaTypeResolverConfiguration javaTypeResolverConfiguration = new JavaTypeResolverConfiguration();
        javaTypeResolverConfiguration.addProperty("forceBigDecimals", "true");
        javaTypeResolverConfiguration.addProperty("useJSR310Types", "true");
        context.setJavaTypeResolverConfiguration(javaTypeResolverConfiguration);

        JavaModelGeneratorConfiguration javaModelGeneratorConfiguration = new JavaModelGeneratorConfiguration();
        javaModelGeneratorConfiguration.setTargetPackage(config.getEntityTargetPackage());
        javaModelGeneratorConfiguration.setTargetProject(config.getTargetProject());
        javaModelGeneratorConfiguration.addProperty("enableSubPackages", "true");
        javaModelGeneratorConfiguration.addProperty("trimStrings", "true");
        context.setJavaModelGeneratorConfiguration(javaModelGeneratorConfiguration);

        for (int i = 0; i < tables.length; i++) {
            TableConfiguration tableConfiguration = new TableConfiguration(context);
            tableConfiguration.setTableName(tables[i]);
            tableConfiguration.setDomainObjectName(domains[i]);
            context.addTableConfiguration(tableConfiguration);
        }

        configuration.addContext(context);

        List<String> warnings = new ArrayList<>();
        DefaultShellCallback shellCallback = new DefaultShellCallback(true);
        MyBatisGenerator generator = new MyBatisGenerator(configuration, shellCallback, warnings);
        generator.generate(callback);
    }

    /**
     * 根据 class name 生成 es mapping
     *
     * @param className class name
     * @return es mapping
     * @throws ClassNotFoundException cnfe
     */
    public static String generateEsMappings(String className) throws ClassNotFoundException {

        Class<?> clazz = ClassLoaderUtil.loadClass(className);

        return generateEsMappings(clazz);
    }

    /**
     * 根据 class 生成 es mapping
     *
     * @param clazz class
     * @return es mapping
     * @throws ClassNotFoundException cnfe
     */
    public static String generateEsMappings(Class<?> clazz) throws ClassNotFoundException {

        final Map<String, Field> fieldMap = ReflectUtil.getFieldMap(clazz);
        final TypeMapping.Builder builder = new TypeMapping.Builder();
        builder.dynamic(DynamicMapping.False);
        builder.numericDetection(false);
        fieldMap.forEach((key, value) -> {
            final EsField annotation = value.getAnnotation(EsField.class);

            if (annotation == null) {
                return;
            }

            final Property property = MybatisPluginUtil.build(annotation);
            builder.properties(key, property);
        });

        final TypeMapping typeMapping = builder.build();

        return MybatisPluginUtil.typeMapping2PrettyJsonStr(typeMapping);
    }

    /**
     * 加载实体类文件生成 es mapping
     *
     * @param config config
     * @return es mapping
     * @throws IOException            io
     * @throws ClassNotFoundException cnfe
     */
    public static Class<?> loadFileClass(MyBatisGeneratorConfig config, String domain) throws IOException, ClassNotFoundException {

        String filePathStr = config.getTargetProject() + FileUtil.FILE_SEPARATOR +
                StrUtil.replace(config.getEntityTargetPackage(), ".", FileUtil.FILE_SEPARATOR);

        String filePath = filePathStr + FileUtil.FILE_SEPARATOR + domain + ".java";
        File sourceFile = new File(filePath);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        final StandardJavaFileManager standardFileManager =
                compiler.getStandardFileManager(null, null, null);

        File targetPath = new File(config.getTargetProject() +
                FileUtil.FILE_SEPARATOR + "target" +
                FileUtil.FILE_SEPARATOR + "classes");

        final boolean exist = FileUtil.exist(targetPath);
        if (!exist) {
            FileUtil.mkdir(targetPath);
        }

        standardFileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(targetPath));

        final Iterable<? extends JavaFileObject> objectsFromFiles = standardFileManager.getJavaFileObjectsFromFiles(Collections.singletonList(sourceFile));

        final JavaCompiler.CompilationTask task = compiler.getTask(null,
                standardFileManager, null, null, null, objectsFromFiles);

        final Boolean success = task.call();

        if (success) {
            return ClassLoaderUtil.loadClass(targetPath, config.getEntityTargetPackage() + "." + domain);
        }

        throw new ClassNotFoundException("无法加载类信息");
    }

    /**
     * 生成类文件及 ES MAPPINGS
     *
     * @param config config
     * @throws InterruptedException          ie
     * @throws SQLException                  sql
     * @throws InvalidConfigurationException ice
     * @throws IOException                   io
     * @throws ClassNotFoundException        cnfx
     */
    public static void generateClassAndMapping(MyBatisGeneratorConfig config) throws InterruptedException, SQLException, InvalidConfigurationException, IOException, ClassNotFoundException {

        generateClassAndMapping(config, null);
    }

    public static void generateClassAndMapping(MyBatisGeneratorConfig config, ProgressCallback callback)
            throws InterruptedException, SQLException, InvalidConfigurationException, IOException, ClassNotFoundException {

        final String[] tables = config.getTables().split(";");
        final String[] domains = config.getDomains().split(";");

        if (tables.length != domains.length) {
            throw new IllegalArgumentException("Tables 与 Domains 不匹配");
        }

        generateClass(config, tables, domains, callback);

        final String mappingsPath = config.getTargetProject() +
                FileUtil.FILE_SEPARATOR + "mappings";
        final boolean exist = FileUtil.exist(mappingsPath);
        if (!exist) {
            FileUtil.mkdir(mappingsPath);
        }

        for (String domain : domains) {
            final Class<?> aClass = loadFileClass(config, domain);

            final String mappings = generateEsMappings(aClass);

            final EsIndex esIndex = aClass.getAnnotation(EsIndex.class);

            if (esIndex == null) {
                throw new AnnotationFormatError("没有找到 EsIndex 注解");
            }

            final String properties = StrUtil.format(tpl_mapping, esIndex.name(), mappings);

            final String saveProperties =  mappingsPath +
                    FileUtil.FILE_SEPARATOR + StrUtil.toSymbolCase(domain, '-') + ".txt";

            FileUtil.writeString(properties, saveProperties, StandardCharsets.UTF_8);
        }
    }
}
