package com.joto.lab.es.test;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author joey
 * @date 2024/8/26 9:32
 */
@SpringBootApplication
//@MapperScan(value = "com.exacom.joto.lab.es.test.mapper")
public class AppStarter {

    public static void main(String[] args) {
        SpringApplication.run(AppStarter.class, args);
    }
}
