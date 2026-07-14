package com.example.enterprise.query.mysql.mapper;

import com.example.enterprise.query.mysql.model.TrxOrder;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TrxOrderMapper {

    List<TrxOrder> findByPayeeIdCardNo(@Param("payeeIdCardNo") String payeeIdCardNo);

    @Select("""
    SELECT *
    FROM tbl_trx_order
    WHERE id = #{id}
    LIMIT 1
    """)
    TrxOrder findById(@Param("id") String id);

}
