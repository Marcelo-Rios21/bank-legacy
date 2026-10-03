package com.bank.cloud.authorizationserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "bank.oauth2.client-id=test-client",
        "bank.oauth2.client-secret=test-secret",
        "bank.oauth2.issuer=http://localhost:9000"
})
class AuthorizationServerApplicationTests {

    @Test
    void contextLoads() {
    }
}