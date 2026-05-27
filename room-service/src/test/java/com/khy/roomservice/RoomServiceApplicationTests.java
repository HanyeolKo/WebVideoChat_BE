package com.khy.roomservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.config.import=",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6370"
})
class RoomServiceApplicationTests {
    @Test
    void contextLoads() {}
}
