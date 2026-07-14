package com.example.enterprise.query.mysql.logging;

import java.util.Collection;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Intercepts({
    @Signature(
            type = Executor.class,
            method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class MybatisQueryLoggingInterceptor implements Interceptor {
    private static final Logger log = LoggerFactory.getLogger(MybatisQueryLoggingInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        var mappedStatement = (MappedStatement) invocation.getArgs()[0];
        var statementId = mappedStatement.getId();
        var startedAt = System.nanoTime();
        log.info(
                "layer=mapper event=query_start statement={} commandType={}",
                statementId,
                mappedStatement.getSqlCommandType());
        try {
            var result = invocation.proceed();
            log.info(
                    "layer=mapper event=query_success statement={} resultCount={} durationMs={}",
                    statementId,
                    resultCount(result),
                    elapsedMillis(startedAt));
            return result;
        } catch (Throwable throwable) {
            log.error(
                    "layer=mapper event=query_failure statement={} errorType={} durationMs={}",
                    statementId,
                    throwable.getClass().getSimpleName(),
                    elapsedMillis(startedAt));
            throw throwable;
        }
    }

    private int resultCount(Object result) {
        if (result instanceof Collection<?> collection) {
            return collection.size();
        }
        return result == null ? 0 : 1;
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
