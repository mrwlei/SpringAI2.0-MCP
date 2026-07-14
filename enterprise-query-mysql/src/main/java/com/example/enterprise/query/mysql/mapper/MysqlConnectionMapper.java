package com.example.enterprise.query.mysql.mapper;

import org.apache.ibatis.annotations.Select;

public interface MysqlConnectionMapper {

    @Select("SELECT 1")
    int ping();
}
