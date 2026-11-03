-- Hibernate maps Boolean to BIT(1) on MySQL/MariaDB, so schema validation expects BIT rather than TINYINT(1).
ALTER TABLE products    MODIFY is_active         BIT(1) NOT NULL DEFAULT b'1';
ALTER TABLE order_items MODIFY is_custom_request BIT(1) NOT NULL DEFAULT b'0';
