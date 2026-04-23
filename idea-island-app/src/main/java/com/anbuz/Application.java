package com.anbuz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

/**
 * 应用启动入口，负责装配 Spring Boot、MyBatis Mapper 扫描和各模块 Bean。
 */
@SpringBootApplication
@MapperScan("com.anbuz.infrastructure.persistent.dao")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
