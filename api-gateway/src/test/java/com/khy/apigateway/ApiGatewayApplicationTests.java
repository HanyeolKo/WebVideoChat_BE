package com.khy.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.config.import=",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false"
})
class ApiGatewayApplicationTests {
    @Test
    void contextLoads() {}
}
