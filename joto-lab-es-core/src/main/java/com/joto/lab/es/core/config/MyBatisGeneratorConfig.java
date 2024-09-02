package com.joto.lab.es.core.config;

import lombok.Data;

/**
 * @author joey
 * @date 2024/8/23 15:49
 */
@Data
public class MyBatisGeneratorConfig {
    /**
     * mysql url
     * jdbc:mysql://ip:port/db?useSSL=false&useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true&serverTimezone=Asia/Shanghai
     */
    private String mysqlUrl;

    /**
     * mysql user
     */
    private String mysqlUser;

    /**
     * mysql pwd
     */
    private String mysqlPwd;
    /**
     * target entity package
     */
    private String entityTargetPackage;

    /**
     * target service package
     */
    private String serviceTargetPackage;

    /**
     * target dto package
     */
    private String dtoTargetPackage;

    /**
     * target project path
     */
    private String targetProject;

    /**
     * author
     */
    private String author;

    /**
     * tables
     */
    private String tables;

    /**
     * domains
     */
    private String domains;
}
