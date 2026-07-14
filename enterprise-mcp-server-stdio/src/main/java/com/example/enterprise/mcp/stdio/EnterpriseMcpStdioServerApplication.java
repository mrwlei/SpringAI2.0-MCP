package com.example.enterprise.mcp.stdio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.enterprise")
public class EnterpriseMcpStdioServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EnterpriseMcpStdioServerApplication.class, args);
    }
}
