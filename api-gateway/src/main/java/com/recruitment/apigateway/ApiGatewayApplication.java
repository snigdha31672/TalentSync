package com.recruitment.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// JWT validation for every routed request (except /api/auth/** and
// /actuator/**) is implemented in filter/JwtAuthenticationFilter — see that
// class and config/JwtConfig for the wiring.
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
