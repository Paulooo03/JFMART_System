SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

CREATE DATABASE IF NOT EXISTS `jf-mart` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `jf-mart`;

-- --------------------------------------------------------
-- Table: users (Spring Security Authentication & RBAC)
-- --------------------------------------------------------
CREATE TABLE `users` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(256) NOT NULL UNIQUE,
  `password_hash` VARCHAR(256) NOT NULL,
  `role` VARCHAR(256) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Table: clients_list
-- --------------------------------------------------------
CREATE TABLE `clients_list` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `hospital_name` VARCHAR(256) NOT NULL,
  `contact_person` VARCHAR(256) NOT NULL,
  `phone_number` VARCHAR(32) NOT NULL,
  `email` VARCHAR(256) NOT NULL,
  `department` VARCHAR(256) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Table: inventory
-- --------------------------------------------------------
CREATE TABLE `inventory` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `item` VARCHAR(256) NOT NULL,
  `price` DECIMAL(15,2) NOT NULL,
  `quantity` INT(11) NOT NULL,
  `last_shelved` DATE NOT NULL,
  `unit` VARCHAR(256) NOT NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Table: orders
-- --------------------------------------------------------
CREATE TABLE `orders` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `client_id` INT(11) NOT NULL,
  `order_status` VARCHAR(256) NOT NULL,
  `total_amount` DECIMAL(15,2) NOT NULL,
  `special_instructions` VARCHAR(256) DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_orders_client` FOREIGN KEY (`client_id`) REFERENCES `clients_list` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Table: order_items
-- --------------------------------------------------------
CREATE TABLE `order_items` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `order_id` INT(11) NOT NULL,
  `product_id` INT(11) DEFAULT NULL,
  `custom_item_name` VARCHAR(256) DEFAULT NULL,
  `quantity` INT(11) NOT NULL,
  `unit` VARCHAR(256) NOT NULL,
  `unit_price` DECIMAL(15,2) NOT NULL,
  `instructions` VARCHAR(256) DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_items_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_items_product` FOREIGN KEY (`product_id`) REFERENCES `inventory` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Table: deliveries
-- --------------------------------------------------------
CREATE TABLE `deliveries` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `order_id` INT(11) NOT NULL,
  `delivery_person_id` INT(11) NOT NULL,
  `delivery_status` VARCHAR(256) NOT NULL,
  `scheduled_date` DATE NOT NULL,
  `delivery_location` VARCHAR(256) NOT NULL,
  `qr_code_token` VARCHAR(256) NOT NULL,
  `unsuccessful_reason` VARCHAR(256) DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_deliveries_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_deliveries_user` FOREIGN KEY (`delivery_person_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Table: ldl_grocery (Invoices)
-- --------------------------------------------------------
CREATE TABLE `ldl_grocery` (
  `invoice_number` INT(11) NOT NULL AUTO_INCREMENT,
  `client_id` INT(11) NOT NULL,
  `order_id` INT(11) NOT NULL,
  `start_date` DATE NOT NULL,
  `due_date` DATE NOT NULL,
  `amount` DECIMAL(15,2) NOT NULL,
  `payment_status` VARCHAR(256) NOT NULL,
  PRIMARY KEY (`invoice_number`),
  CONSTRAINT `fk_invoice_client` FOREIGN KEY (`client_id`) REFERENCES `clients_list` (`id`),
  CONSTRAINT `fk_invoice_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Table: payments
-- --------------------------------------------------------
CREATE TABLE `payments` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `invoice_id` INT(11) NOT NULL,
  `client_id` INT(11) NOT NULL,
  `amount_paid` DECIMAL(15,2) NOT NULL,
  `payment_method` VARCHAR(256) NOT NULL,
  `payment_date` DATE NOT NULL,
  `reference_number` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_payments_invoice` FOREIGN KEY (`invoice_id`) REFERENCES `ldl_grocery` (`invoice_number`),
  CONSTRAINT `fk_payments_client` FOREIGN KEY (`client_id`) REFERENCES `clients_list` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

COMMIT;
