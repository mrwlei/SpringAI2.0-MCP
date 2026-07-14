package com.example.enterprise.query.mysql.config;

import com.example.enterprise.query.mysql.logging.MybatisQueryLoggingInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@MapperScan("com.example.enterprise.query.mysql.mapper")
public class MysqlMapperConfiguration {

    @Bean
    MybatisQueryLoggingInterceptor mybatisQueryLoggingInterceptor() {
        return new MybatisQueryLoggingInterceptor();
    }
}
