package com.example.enterprise.mcp.adapter;

import com.example.enterprise.query.model.TrxOrderSummary;
import com.example.enterprise.query.mysql.model.TrxOrder;
import com.example.enterprise.query.mysql.repository.TrxOrderQueryService;
import com.example.enterprise.query.port.TrxOrderQueryPort;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class TrxOrderQueryAdapter implements TrxOrderQueryPort {
    private static final Logger log = LoggerFactory.getLogger(TrxOrderQueryAdapter.class);

    private final TrxOrderQueryService trxOrderQueryService;

    public TrxOrderQueryAdapter(TrxOrderQueryService trxOrderQueryService) {
        this.trxOrderQueryService = trxOrderQueryService;
    }

    @Override
    public Optional<TrxOrderSummary> findById(String id) {
        return execute("findById", () -> Optional.ofNullable(trxOrderQueryService.findById(id)).map(this::toSummary));
    }

    @Override
    public List<TrxOrderSummary> findLatestByPayeeIdCardNo(String payeeIdCardNo) {
        return execute(
                "findLatestByPayeeIdCardNo",
                () -> trxOrderQueryService.findByPayeeIdCardNo(payeeIdCardNo).stream()
                        .map(this::toSummary)
                        .toList());
    }

    private <T> T execute(String operation, Supplier<T> action) {
        var startedAt = System.nanoTime();
        log.info("layer=query_adapter event=query_start operation={}", operation);
        try {
            var result = action.get();
            log.info(
                    "layer=query_adapter event=query_success operation={} found={} durationMs={}",
                    operation,
                    isFound(result),
                    elapsedMillis(startedAt));
            return result;
        } catch (RuntimeException exception) {
            log.error(
                    "layer=query_adapter event=query_failure operation={} errorType={} durationMs={}",
                    operation,
                    exception.getClass().getSimpleName(),
                    elapsedMillis(startedAt));
            throw exception;
        }
    }

    private boolean isFound(Object result) {
        if (result instanceof Optional<?> optional) {
            return optional.isPresent();
        }
        if (result instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        return result != null;
    }

    private TrxOrderSummary toSummary(TrxOrder order) {
        return new TrxOrderSummary(
                order.getId(),
                order.getMerchantName(),
                order.getAmount(),
                order.getCharge(),
                order.getActualAmount(),
                order.getOrderSerialNum(),
                order.getMerchantSerialNum(),
                order.getBatchNo(),
                order.getPayeeNameDec(),
                order.getPayeeMobileDec(),
                order.getPayeeBankCardDec(),
                order.getPayeeIdCardNoDec(),
                order.getBankName(),
                order.getOrderStatus(),
                order.getPaymentChannel(),
                order.getMerchantId(),
                order.getTrxNo(),
                order.getErrorCode(),
                order.getReceiveStatus(),
                order.getTradeMode(),
                order.getMissionName(),
                order.getTaskNumber(),
                order.getPersonTaxAmount(),
                order.getCreateTime(),
                order.getUpdateTime(),
                order.getCompleteDateTime());
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
