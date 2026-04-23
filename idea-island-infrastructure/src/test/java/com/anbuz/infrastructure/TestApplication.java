package com.anbuz.infrastructure;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@SpringBootConfiguration
@EnableAutoConfiguration
@MapperScan("com.anbuz.infrastructure.persistent.dao")
public class TestApplication {
}
