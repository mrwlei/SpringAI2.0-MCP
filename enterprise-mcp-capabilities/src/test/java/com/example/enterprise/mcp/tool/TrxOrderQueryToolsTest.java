package com.example.enterprise.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.enterprise.query.model.TrxOrderSummary;
import com.example.enterprise.query.port.TrxOrderQueryPort;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class TrxOrderQueryToolsTest {

    private static final TrxOrderSummary ORDER = new TrxOrderSummary(
            "order-new",
            "测试商户",
            new BigDecimal("200.00"),
            new BigDecimal("2.00"),
            new BigDecimal("198.00"),
            "ORDER-NEW",
            "MERCHANT-NEW",
            "BATCH-001",
            "测*",
            "138****0000",
            "6222********1234",
            "TEST-ID-****-001",
            "测试银行",
            1,
            "BANK",
            1001,
            "TRX-NEW",
            null,
            "CONFIRMED",
            "DIRECT",
            "测试任务",
            "TASK-001",
            new BigDecimal("10.00"),
            LocalDateTime.of(2026, 1, 2, 10, 0),
            LocalDateTime.of(2026, 1, 2, 11, 0),
            LocalDateTime.of(2026, 1, 2, 10, 30));

    private final TrxOrderQueryTools tools = new TrxOrderQueryTools(new StubTrxOrderQueryPort());

    @Test
    void shouldGetOrderById() {
        assertThat(tools.getTrxOrderById(" order-new ")).isEqualTo(ORDER);
    }

    @Test
    void shouldGetLatestOrderByPayeeIdCardNo() {
        assertThat(tools.getLatestTrxOrderByPayeeIdCardNo(" TEST-ID-CARD-001 ")).containsExactly(ORDER);
    }

    @Test
    void shouldRejectBlankArguments() {
        assertThatThrownBy(() -> tools.getTrxOrderById(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("订单主键不能为空");
        assertThatThrownBy(() -> tools.getLatestTrxOrderByPayeeIdCardNo(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("收款人身份证号不能为空");
    }

    @Test
    void shouldReturnEmptyListWhenOrderDoesNotExist() {
        assertThat(tools.getLatestTrxOrderByPayeeIdCardNo("SENSITIVE-ID-CARD")).isEmpty();
    }

    @Test
    void shouldManageTraceIdWithoutOverwritingExistingContext() {
        MDC.remove("traceId");
        tools.getTrxOrderById("order-new");
        assertThat(MDC.get("traceId")).isNull();

        MDC.put("traceId", "existing-trace-id");
        try {
            tools.getTrxOrderById("order-new");
            assertThat(MDC.get("traceId")).isEqualTo("existing-trace-id");
        } finally {
            MDC.remove("traceId");
        }
    }

    private static class StubTrxOrderQueryPort implements TrxOrderQueryPort {

        @Override
        public Optional<TrxOrderSummary> findById(String id) {
            return "order-new".equals(id) ? Optional.of(ORDER) : Optional.empty();
        }

        @Override
        public List<TrxOrderSummary> findLatestByPayeeIdCardNo(String payeeIdCardNo) {
            return "TEST-ID-CARD-001".equals(payeeIdCardNo) ? List.of(ORDER) : List.of();
        }
    }
}
