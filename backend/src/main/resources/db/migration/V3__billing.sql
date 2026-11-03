-- One invoice per order, so a delivery can never be billed twice.
ALTER TABLE invoices ADD UNIQUE KEY uk_invoices_order (order_id);

-- Cash payments often have no reference number.
ALTER TABLE payments MODIFY reference_number VARCHAR(100) NULL;
