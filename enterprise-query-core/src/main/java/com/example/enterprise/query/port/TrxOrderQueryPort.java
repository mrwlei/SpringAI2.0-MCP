package com.example.enterprise.query.port;

import com.example.enterprise.query.model.TrxOrderSummary;
import java.util.List;
import java.util.Optional;

public interface TrxOrderQueryPort {
    Optional<TrxOrderSummary> findById(String id);
    List<TrxOrderSummary> findLatestByPayeeIdCardNo(String payeeIdCardNo);
}
