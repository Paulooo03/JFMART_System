-- JF MiniMart schema (replaces the root jf-mart.sql draft; see docs/screen-map.md section 2)

CREATE TABLE clients_list (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  hospital_name    VARCHAR(256) NOT NULL,
  contact_person   VARCHAR(256) NOT NULL,
  phone_number     VARCHAR(32)  NOT NULL,
  email            VARCHAR(256) NOT NULL,
  delivery_address VARCHAR(512) NOT NULL,
  department       VARCHAR(256) NULL,
  created_at       DATETIME     NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE users (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  username      VARCHAR(256) NOT NULL,
  password_hash VARCHAR(256) NOT NULL,
  role          VARCHAR(32)  NOT NULL,
  status        VARCHAR(32)  NOT NULL,
  client_id     BIGINT       NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  CONSTRAINT fk_users_client FOREIGN KEY (client_id) REFERENCES clients_list (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Price list. Stock is not tracked tightly, so quantity is optional.
CREATE TABLE products (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  name         VARCHAR(256)  NOT NULL,
  price        DECIMAL(15,2) NOT NULL,
  unit         VARCHAR(64)   NOT NULL,
  quantity     INT           NULL,
  last_shelved DATE          NULL,
  is_active    TINYINT(1)    NOT NULL DEFAULT 1,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE orders (
  id                   BIGINT        NOT NULL AUTO_INCREMENT,
  reference_number     VARCHAR(32)   NOT NULL,
  client_id            BIGINT        NOT NULL,
  order_status         VARCHAR(32)   NOT NULL,
  total_amount         DECIMAL(15,2) NULL,
  special_instructions VARCHAR(512)  NULL,
  created_at           DATETIME      NOT NULL,
  confirmed_at         DATETIME      NULL,
  delivered_at         DATETIME      NULL,
  cancelled_reason     VARCHAR(256)  NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_orders_reference (reference_number),
  CONSTRAINT fk_orders_client FOREIGN KEY (client_id) REFERENCES clients_list (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_items (
  id                BIGINT        NOT NULL AUTO_INCREMENT,
  order_id          BIGINT        NOT NULL,
  product_id        BIGINT        NULL,
  custom_item_name  VARCHAR(256)  NULL,
  is_custom_request TINYINT(1)    NOT NULL DEFAULT 0,
  quantity          INT           NOT NULL,
  unit              VARCHAR(64)   NOT NULL,
  unit_price        DECIMAL(15,2) NULL,
  instructions      VARCHAR(512)  NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_items_order   FOREIGN KEY (order_id)   REFERENCES orders (id) ON DELETE CASCADE,
  CONSTRAINT fk_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE deliveries (
  id                  BIGINT       NOT NULL AUTO_INCREMENT,
  order_id            BIGINT       NOT NULL,
  delivery_person_id  BIGINT       NOT NULL,
  delivery_status     VARCHAR(32)  NOT NULL,
  scheduled_date      DATE         NOT NULL,
  delivery_location   VARCHAR(512) NOT NULL,
  qr_code_token       VARCHAR(128) NOT NULL,
  qr_expires_at       DATETIME     NULL,
  qr_used_at          DATETIME     NULL,
  delivered_at        DATETIME     NULL,
  unsuccessful_reason VARCHAR(256) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_deliveries_qr (qr_code_token),
  CONSTRAINT fk_deliveries_order FOREIGN KEY (order_id)           REFERENCES orders (id),
  CONSTRAINT fk_deliveries_user  FOREIGN KEY (delivery_person_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE invoices (
  invoice_number       BIGINT        NOT NULL AUTO_INCREMENT,
  sales_invoice_number VARCHAR(64)   NULL,
  client_id            BIGINT        NOT NULL,
  order_id             BIGINT        NOT NULL,
  start_date           DATE          NOT NULL,
  due_date             DATE          NOT NULL,
  amount               DECIMAL(15,2) NOT NULL,
  payment_status       VARCHAR(32)   NOT NULL,
  PRIMARY KEY (invoice_number),
  UNIQUE KEY uk_invoices_sales_no (sales_invoice_number),
  CONSTRAINT fk_invoices_client FOREIGN KEY (client_id) REFERENCES clients_list (id),
  CONSTRAINT fk_invoices_order  FOREIGN KEY (order_id)  REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payments (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  invoice_id       BIGINT        NOT NULL,
  client_id        BIGINT        NOT NULL,
  amount_paid      DECIMAL(15,2) NOT NULL,
  payment_method   VARCHAR(64)   NOT NULL,
  payment_date     DATE          NOT NULL,
  reference_number VARCHAR(100)  NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (invoice_number),
  CONSTRAINT fk_payments_client  FOREIGN KEY (client_id)  REFERENCES clients_list (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
