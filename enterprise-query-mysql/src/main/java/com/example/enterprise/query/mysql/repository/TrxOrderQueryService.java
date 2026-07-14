package com.example.enterprise.query.mysql.repository;

import com.example.enterprise.query.mysql.mapper.TrxOrderMapper;
import com.example.enterprise.query.mysql.model.TrxOrder;
import com.example.enterprise.query.mysql.utils.SM4Utils;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class TrxOrderQueryService {
    private static final Logger log = LoggerFactory.getLogger(TrxOrderQueryService.class);

    private final TrxOrderMapper trxOrderMapper;

    public TrxOrderQueryService(TrxOrderMapper trxOrderMapper) {
        this.trxOrderMapper = trxOrderMapper;
    }

    public TrxOrder findById(String id) {
        return execute("findById", () -> trxOrderMapper.findById(id));
    }

    public List<TrxOrder> findByPayeeIdCardNo(String payeeIdCardNo) {
        return execute(
                "findByPayeeIdCardNo",
                () -> trxOrderMapper.findByPayeeIdCardNo(SM4Utils.encryptDataForECB(payeeIdCardNo)));
    }

    private <T> T execute(String operation, Supplier<T> action) {
        var startedAt = System.nanoTime();
        log.info("layer=query_service event=query_start operation={}", operation);
        try {
            var result = action.get();
            log.info(
                    "layer=query_service event=query_success operation={} found={} durationMs={}",
                    operation,
                    isFound(result),
                    elapsedMillis(startedAt));
            return result;
        } catch (RuntimeException exception) {
            log.error(
                    "layer=query_service event=query_failure operation={} errorType={} durationMs={}",
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

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
