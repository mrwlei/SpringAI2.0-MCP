package com.example.enterprise.query.mysql;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.enterprise.query.mysql.config.MysqlMapperConfiguration;
import com.example.enterprise.query.mysql.mapper.TrxOrderMapper;
import com.example.enterprise.query.mysql.model.TrxOrder;
import com.example.enterprise.query.mysql.repository.MysqlConnectionProbe;
import com.example.enterprise.query.mysql.repository.TrxOrderQueryService;
import com.example.enterprise.query.mysql.utils.SM4Utils;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(
        classes = MysqlMapperConfigurationTest.TestApplication.class,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:enterprise-query;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.type=com.zaxxer.hikari.HikariDataSource",
            "spring.sql.init.mode=always",
            "mybatis.mapper-locations=classpath*:mapper/**/*.xml",
            "mybatis.configuration.map-underscore-to-camel-case=true"
        })
class MysqlMapperConfigurationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MysqlConnectionProbe connectionProbe;

    @Autowired
    private TrxOrderMapper trxOrderMapper;

    @Autowired
    private TrxOrderQueryService trxOrderQueryService;

    @Test
    void shouldUseHikariAndExecuteMybatisMapper() {
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        assertThat(connectionProbe.isReachable()).isTrue();
    }

    @Test
    void shouldFindOrdersByPayeeIdCardNo() {
        var orders = trxOrderMapper.findByPayeeIdCardNo(SM4Utils.encryptDataForECB("TEST-ID-CARD-001"));

        assertThat(orders).extracting(TrxOrder::getId).containsExactly("order-new", "order-old");
        assertThat(orders.getFirst()).satisfies(value -> {
            assertThat(value.getId()).isEqualTo("order-new");
            assertThat(value.getTrxNo()).isEqualTo("TRX-NEW");
            assertThat(value.getMerchantName()).isEqualTo("测试商户");
            assertThat(value.getAmount()).isEqualByComparingTo("200.00");
            assertThat(value.getOrderStatus()).isEqualTo(1);
            assertThat(value.getBelongAgentId()).isEqualTo(10000000001L);
        });
    }

    @Test
    void shouldReturnEmptyWhenPayeeIdCardNoDoesNotExist() {
        assertThat(trxOrderMapper.findByPayeeIdCardNo(SM4Utils.encryptDataForECB("MISSING-ID-CARD"))).isEmpty();
    }

    @Test
    void shouldFindOrderByIdThroughQueryService() {
        assertThat(trxOrderQueryService.findById("order-new")).satisfies(order -> {
            assertThat(order.getId()).isEqualTo("order-new");
            assertThat(order.getTrxNo()).isEqualTo("TRX-NEW");
            assertThat(order.getAmount()).isEqualByComparingTo("200.00");
            assertThat(order.getPayeeNameDec()).isEqualTo("测*");
            assertThat(order.getPayeeMobileDec()).isEqualTo("138****0000");
            assertThat(order.getPayeeBankCardDec()).isEqualTo("6222********1234");
            assertThat(order.getPayeeIdCardNoDec()).isEqualTo("TEST-ID-****-001");
        });
    }

    @Test
    void shouldFindOrdersByPayeeIdCardNoThroughQueryService() {
        assertThat(trxOrderQueryService.findByPayeeIdCardNo("TEST-ID-CARD-001"))
                .extracting(TrxOrder::getId)
                .containsExactly("order-new", "order-old");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({MysqlMapperConfiguration.class, MysqlConnectionProbe.class, TrxOrderQueryService.class})
    static class TestApplication {
    }
}
