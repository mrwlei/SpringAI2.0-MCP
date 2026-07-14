package com.example.enterprise.query.mysql.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TrxOrder {
    private String id;
    private String merchantName;
    private BigDecimal amount;
    private BigDecimal taxRate;
    private BigDecimal charge;
    private BigDecimal actualAmount;
    private String orderSerialNum;
    private String merchantSerialNum;
    private String batchNo;
    private String paymentVoucher;
    private String payeeName;
    private String payeeMobile;
    private String payeeBankCard;
    private String bankName;
    private String payeeIdCardNo;
    private Integer orderStatus;
    private String batchPaymentRecordId;
    private String paymentChannel;
    private Integer merchantId;
    private String dna;
    private String trxNo;
    private String comment;
    private String subjectConscription;
    private String errorCode;
    private Integer providerId;
    private String fillupChargeType;
    private BigDecimal fillupCharge;
    private String errorMsg;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String originalTrxNo;
    private String subrogateId;
    private String payeeNameDec;
    private String payeeMobileDec;
    private String payeeBankCardDec;
    private String payeeIdCardNoDec;
    private BigDecimal personalRate;
    private BigDecimal companyRate;
    private Integer fillupFeeBears;
    private BigDecimal personalCharge;
    private BigDecimal companyCharge;
    private String taskUserId;
    private String receiveStatus;
    private String taskId;
    private String tradeMode;
    private String realSubrogateId;
    private String realSubrogate;
    private BigDecimal transferFee;
    private String taskOrderId;
    private String missionName;
    private String taskNumber;
    private BigDecimal actualCharge;
    private BigDecimal actualFillupCharge;
    private Long belongAgentId;
    private Long superAgentId;
    private Long topAgentId;
    private Integer quotaType;
    private BigDecimal actualTaxRate;
    private LocalDateTime completeDateTime;
    private String invokeMode;
    private String bankSerialNum;
    private String bankFundSerialNum;
    private String transferSceneReportInfo;
    private String appId;
    private BigDecimal personTaxAmount;
    private BigDecimal channelRate;
    private BigDecimal channelFee;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getCharge() {
        return charge;
    }

    public void setCharge(BigDecimal charge) {
        this.charge = charge;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
    }

    public String getOrderSerialNum() {
        return orderSerialNum;
    }

    public void setOrderSerialNum(String orderSerialNum) {
        this.orderSerialNum = orderSerialNum;
    }

    public String getMerchantSerialNum() {
        return merchantSerialNum;
    }

    public void setMerchantSerialNum(String merchantSerialNum) {
        this.merchantSerialNum = merchantSerialNum;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getPaymentVoucher() {
        return paymentVoucher;
    }

    public void setPaymentVoucher(String paymentVoucher) {
        this.paymentVoucher = paymentVoucher;
    }

    public String getPayeeName() {
        return payeeName;
    }

    public void setPayeeName(String payeeName) {
        this.payeeName = payeeName;
    }

    public String getPayeeMobile() {
        return payeeMobile;
    }

    public void setPayeeMobile(String payeeMobile) {
        this.payeeMobile = payeeMobile;
    }

    public String getPayeeBankCard() {
        return payeeBankCard;
    }

    public void setPayeeBankCard(String payeeBankCard) {
        this.payeeBankCard = payeeBankCard;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getPayeeIdCardNo() {
        return payeeIdCardNo;
    }

    public void setPayeeIdCardNo(String payeeIdCardNo) {
        this.payeeIdCardNo = payeeIdCardNo;
    }

    public Integer getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(Integer orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getBatchPaymentRecordId() {
        return batchPaymentRecordId;
    }

    public void setBatchPaymentRecordId(String batchPaymentRecordId) {
        this.batchPaymentRecordId = batchPaymentRecordId;
    }

    public String getPaymentChannel() {
        return paymentChannel;
    }

    public void setPaymentChannel(String paymentChannel) {
        this.paymentChannel = paymentChannel;
    }

    public Integer getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Integer merchantId) {
        this.merchantId = merchantId;
    }

    public String getDna() {
        return dna;
    }

    public void setDna(String dna) {
        this.dna = dna;
    }

    public String getTrxNo() {
        return trxNo;
    }

    public void setTrxNo(String trxNo) {
        this.trxNo = trxNo;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getSubjectConscription() {
        return subjectConscription;
    }

    public void setSubjectConscription(String subjectConscription) {
        this.subjectConscription = subjectConscription;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Integer getProviderId() {
        return providerId;
    }

    public void setProviderId(Integer providerId) {
        this.providerId = providerId;
    }

    public String getFillupChargeType() {
        return fillupChargeType;
    }

    public void setFillupChargeType(String fillupChargeType) {
        this.fillupChargeType = fillupChargeType;
    }

    public BigDecimal getFillupCharge() {
        return fillupCharge;
    }

    public void setFillupCharge(BigDecimal fillupCharge) {
        this.fillupCharge = fillupCharge;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getOriginalTrxNo() {
        return originalTrxNo;
    }

    public void setOriginalTrxNo(String originalTrxNo) {
        this.originalTrxNo = originalTrxNo;
    }

    public String getSubrogateId() {
        return subrogateId;
    }

    public void setSubrogateId(String subrogateId) {
        this.subrogateId = subrogateId;
    }

    public String getPayeeNameDec() {
        return payeeNameDec;
    }

    public void setPayeeNameDec(String payeeNameDec) {
        this.payeeNameDec = payeeNameDec;
    }

    public String getPayeeMobileDec() {
        return payeeMobileDec;
    }

    public void setPayeeMobileDec(String payeeMobileDec) {
        this.payeeMobileDec = payeeMobileDec;
    }

    public String getPayeeBankCardDec() {
        return payeeBankCardDec;
    }

    public void setPayeeBankCardDec(String payeeBankCardDec) {
        this.payeeBankCardDec = payeeBankCardDec;
    }

    public String getPayeeIdCardNoDec() {
        return payeeIdCardNoDec;
    }

    public void setPayeeIdCardNoDec(String payeeIdCardNoDec) {
        this.payeeIdCardNoDec = payeeIdCardNoDec;
    }

    public BigDecimal getPersonalRate() {
        return personalRate;
    }

    public void setPersonalRate(BigDecimal personalRate) {
        this.personalRate = personalRate;
    }

    public BigDecimal getCompanyRate() {
        return companyRate;
    }

    public void setCompanyRate(BigDecimal companyRate) {
        this.companyRate = companyRate;
    }

    public Integer getFillupFeeBears() {
        return fillupFeeBears;
    }

    public void setFillupFeeBears(Integer fillupFeeBears) {
        this.fillupFeeBears = fillupFeeBears;
    }

    public BigDecimal getPersonalCharge() {
        return personalCharge;
    }

    public void setPersonalCharge(BigDecimal personalCharge) {
        this.personalCharge = personalCharge;
    }

    public BigDecimal getCompanyCharge() {
        return companyCharge;
    }

    public void setCompanyCharge(BigDecimal companyCharge) {
        this.companyCharge = companyCharge;
    }

    public String getTaskUserId() {
        return taskUserId;
    }

    public void setTaskUserId(String taskUserId) {
        this.taskUserId = taskUserId;
    }

    public String getReceiveStatus() {
        return receiveStatus;
    }

    public void setReceiveStatus(String receiveStatus) {
        this.receiveStatus = receiveStatus;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTradeMode() {
        return tradeMode;
    }

    public void setTradeMode(String tradeMode) {
        this.tradeMode = tradeMode;
    }

    public String getRealSubrogateId() {
        return realSubrogateId;
    }

    public void setRealSubrogateId(String realSubrogateId) {
        this.realSubrogateId = realSubrogateId;
    }

    public String getRealSubrogate() {
        return realSubrogate;
    }

    public void setRealSubrogate(String realSubrogate) {
        this.realSubrogate = realSubrogate;
    }

    public BigDecimal getTransferFee() {
        return transferFee;
    }

    public void setTransferFee(BigDecimal transferFee) {
        this.transferFee = transferFee;
    }

    public String getTaskOrderId() {
        return taskOrderId;
    }

    public void setTaskOrderId(String taskOrderId) {
        this.taskOrderId = taskOrderId;
    }

    public String getMissionName() {
        return missionName;
    }

    public void setMissionName(String missionName) {
        this.missionName = missionName;
    }

    public String getTaskNumber() {
        return taskNumber;
    }

    public void setTaskNumber(String taskNumber) {
        this.taskNumber = taskNumber;
    }

    public BigDecimal getActualCharge() {
        return actualCharge;
    }

    public void setActualCharge(BigDecimal actualCharge) {
        this.actualCharge = actualCharge;
    }

    public BigDecimal getActualFillupCharge() {
        return actualFillupCharge;
    }

    public void setActualFillupCharge(BigDecimal actualFillupCharge) {
        this.actualFillupCharge = actualFillupCharge;
    }

    public Long getBelongAgentId() {
        return belongAgentId;
    }

    public void setBelongAgentId(Long belongAgentId) {
        this.belongAgentId = belongAgentId;
    }

    public Long getSuperAgentId() {
        return superAgentId;
    }

    public void setSuperAgentId(Long superAgentId) {
        this.superAgentId = superAgentId;
    }

    public Long getTopAgentId() {
        return topAgentId;
    }

    public void setTopAgentId(Long topAgentId) {
        this.topAgentId = topAgentId;
    }

    public Integer getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(Integer quotaType) {
        this.quotaType = quotaType;
    }

    public BigDecimal getActualTaxRate() {
        return actualTaxRate;
    }

    public void setActualTaxRate(BigDecimal actualTaxRate) {
        this.actualTaxRate = actualTaxRate;
    }

    public LocalDateTime getCompleteDateTime() {
        return completeDateTime;
    }

    public void setCompleteDateTime(LocalDateTime completeDateTime) {
        this.completeDateTime = completeDateTime;
    }

    public String getInvokeMode() {
        return invokeMode;
    }

    public void setInvokeMode(String invokeMode) {
        this.invokeMode = invokeMode;
    }

    public String getBankSerialNum() {
        return bankSerialNum;
    }

    public void setBankSerialNum(String bankSerialNum) {
        this.bankSerialNum = bankSerialNum;
    }

    public String getBankFundSerialNum() {
        return bankFundSerialNum;
    }

    public void setBankFundSerialNum(String bankFundSerialNum) {
        this.bankFundSerialNum = bankFundSerialNum;
    }

    public String getTransferSceneReportInfo() {
        return transferSceneReportInfo;
    }

    public void setTransferSceneReportInfo(String transferSceneReportInfo) {
        this.transferSceneReportInfo = transferSceneReportInfo;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public BigDecimal getPersonTaxAmount() {
        return personTaxAmount;
    }

    public void setPersonTaxAmount(BigDecimal personTaxAmount) {
        this.personTaxAmount = personTaxAmount;
    }

    public BigDecimal getChannelRate() {
        return channelRate;
    }

    public void setChannelRate(BigDecimal channelRate) {
        this.channelRate = channelRate;
    }

    public BigDecimal getChannelFee() {
        return channelFee;
    }

    public void setChannelFee(BigDecimal channelFee) {
        this.channelFee = channelFee;
    }
}
