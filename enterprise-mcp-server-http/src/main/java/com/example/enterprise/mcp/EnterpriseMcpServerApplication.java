package com.example.enterprise.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.enterprise")
public class EnterpriseMcpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EnterpriseMcpServerApplication.class, args);
    }
}
