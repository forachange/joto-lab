package com.joto.lab.es.test;

import com.joto.lab.es.core.config.MyBatisGeneratorConfig;
import com.joto.lab.es.core.utils.MybatisGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mybatis.generator.exception.InvalidConfigurationException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * @author joey
 * @date 2024/8/26 10:17
 */
@Slf4j
public class GenerateClassTest {

    @Test
    public void generateTest() throws InterruptedException, SQLException, InvalidConfigurationException, IOException, ClassNotFoundException {

        MyBatisGeneratorConfig config = new MyBatisGeneratorConfig();
        config.setMysqlUrl("jdbc:mysql://127.0.0.1:23306/datacap?useSSL=false&useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true&serverTimezone=Asia/Shanghai");
        config.setMysqlUser("root");
        config.setMysqlPwd("123456");
        config.setAuthor("joey");
        config.setTables("student_info;teacher_info");
        config.setDomains("StudentInfo;TeacherInfo");
        config.setEntityTargetPackage("com.joto.entity");
        config.setServiceTargetPackage("com.joto.service");
        config.setTargetProject("/Users/joey/Workspace/tmp");

        MybatisGeneratorUtil.generateClassAndMapping(config);
    }
}
