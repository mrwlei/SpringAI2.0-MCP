package com.example.enterprise.mcp.tool;

import com.example.enterprise.query.model.TrxOrderSummary;
import com.example.enterprise.query.port.TrxOrderQueryPort;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class TrxOrderQueryTools {
    private static final Logger log = LoggerFactory.getLogger(TrxOrderQueryTools.class);

    private final TrxOrderQueryPort queryPort;

    public TrxOrderQueryTools(TrxOrderQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @McpTool(
            name = "get_trx_order_by_id",
            title = "根据主键查询交易订单",
            description = "根据交易订单主键精确查询一条订单，只返回订单摘要和已脱敏的收款人信息",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public TrxOrderSummary getTrxOrderById(
            @McpToolParam(description = "交易订单主键 ID", required = true) String id) {
        return executeWithTrace("get_trx_order_by_id", () -> {
            var normalizedId = requireText(id, "订单主键不能为空");
            return queryPort.findById(normalizedId)
                    .orElseThrow(() -> new IllegalArgumentException("交易订单不存在: " + normalizedId));
        });
    }

    @McpTool(
            name = "get_latest_trx_order_by_payee_id_card_no",
            title = "根据收款人身份证号查询交易订单",
            description = "根据收款人身份证号精确查询交易订单列表，结果按创建时间倒序排列，只返回订单摘要和已脱敏的收款人信息",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public List<TrxOrderSummary> getLatestTrxOrderByPayeeIdCardNo(
            @McpToolParam(
                            description = "收款人身份证号（敏感参数，仅用于精确匹配，不会在响应中返回）",
                            required = true)
                    String payeeIdCardNo) {
        return executeWithTrace("get_latest_trx_order_by_payee_id_card_no", () -> {
            var normalizedPayeeIdCardNo = requireText(payeeIdCardNo, "收款人身份证号不能为空");
            return queryPort.findLatestByPayeeIdCardNo(normalizedPayeeIdCardNo);
        });
    }

    private <T> T executeWithTrace(String operation, Supplier<T> action) {
        var existingTraceId = MDC.get("traceId");
        var createdTraceId = existingTraceId == null || existingTraceId.isBlank();
        if (createdTraceId) {
            MDC.put("traceId", UUID.randomUUID().toString());
        }

        var startedAt = System.nanoTime();
        log.info("layer=mcp_tool event=query_start operation={}", operation);
        try {
            var result = action.get();
            log.info(
                    "layer=mcp_tool event=query_success operation={} durationMs={}",
                    operation,
                    elapsedMillis(startedAt));
            return result;
        } catch (RuntimeException exception) {
            log.warn(
                    "layer=mcp_tool event=query_failure operation={} errorType={} durationMs={}",
                    operation,
                    exception.getClass().getSimpleName(),
                    elapsedMillis(startedAt));
            throw exception;
        } finally {
            if (createdTraceId) {
                MDC.remove("traceId");
            }
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
