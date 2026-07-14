package com.example.enterprise.query.mysql.repository;

import com.example.enterprise.query.mysql.mapper.MysqlConnectionMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MysqlConnectionProbe {
    private final MysqlConnectionMapper connectionMapper;

    public MysqlConnectionProbe(MysqlConnectionMapper connectionMapper) {
        this.connectionMapper = connectionMapper;
    }

    public boolean isReachable() {
        return connectionMapper.ping() == 1;
    }
}
