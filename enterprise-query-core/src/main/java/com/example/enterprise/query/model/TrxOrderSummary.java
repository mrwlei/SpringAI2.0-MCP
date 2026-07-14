package com.example.enterprise.query.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TrxOrderSummary(
        String id,
        String merchantName,
        BigDecimal amount,
        BigDecimal charge,
        BigDecimal actualAmount,
        String orderSerialNum,
        String merchantSerialNum,
        String batchNo,
        String maskedPayeeName,
        String maskedPayeeMobile,
        String maskedPayeeBankCard,
        String maskedPayeeIdCardNo,
        String bankName,
        Integer orderStatus,
        String paymentChannel,
        Integer merchantId,
        String trxNo,
        String errorCode,
        String receiveStatus,
        String tradeMode,
        String missionName,
        String taskNumber,
        BigDecimal personTaxAmount,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        LocalDateTime completeDateTime) {
}
