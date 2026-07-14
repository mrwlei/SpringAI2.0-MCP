INSERT INTO `tbl_trx_order` (
    `id`, `merchant_name`, `amount`, `payee_name`, `payee_id_card_no`,
    `payee_name_dec`, `payee_mobile_dec`, `payee_bank_card_dec`, `payee_id_card_no_dec`,
    `order_status`, `trx_no`, `belong_agent_id`, `create_time`
) VALUES (
    'order-old', '测试商户', 100.00, '测试收款人', 'JpkaGk9bTVTNaPUj0FM3XH4lbQQ/ctwoWffbhwaM5Ng=$',
    '测*', '138****0000', '6222********1234', 'TEST-ID-****-001',
    0, 'TRX-OLD', 10000000001, TIMESTAMP '2026-01-01 10:00:00'
);

INSERT INTO `tbl_trx_order` (
    `id`, `merchant_name`, `amount`, `payee_name`, `payee_id_card_no`,
    `payee_name_dec`, `payee_mobile_dec`, `payee_bank_card_dec`, `payee_id_card_no_dec`,
    `order_status`, `trx_no`, `belong_agent_id`, `create_time`
) VALUES (
    'order-new', '测试商户', 200.00, '测试收款人', 'JpkaGk9bTVTNaPUj0FM3XH4lbQQ/ctwoWffbhwaM5Ng=$',
    '测*', '138****0000', '6222********1234', 'TEST-ID-****-001',
    1, 'TRX-NEW', 10000000001, TIMESTAMP '2026-01-02 10:00:00'
);
