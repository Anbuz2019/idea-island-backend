package com.anbuz.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
@Disabled("Replaced by focused MockMvc tests for user-facing endpoints.")
class ApiTest {

    @Test
    void contextLoads() {
        log.info("Application context loaded.");
    }

}
