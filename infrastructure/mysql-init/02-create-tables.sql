-- =========================================================================
-- 1. IDENTITY SERVICE (Database: identity_db)
-- =========================================================================
USE `identity_db`;

CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================================================
-- 2. WALLET & LEDGER SERVICE (Database: wallet_db)
-- =========================================================================
USE `wallet_db`;

CREATE TABLE IF NOT EXISTS `wallets` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT 'Khóa ngoại mềm trỏ tới identity_db.users.id',
    `type` VARCHAR(30) NOT NULL COMMENT 'PERSONAL, MERCHANT...',
    `balance` DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    `version` INT NOT NULL DEFAULT 0 COMMENT 'Phục vụ Optimistic Lock chống tương tranh'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ledger_entries` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `transaction_id` BIGINT NOT NULL COMMENT 'Mã giao dịch sinh ra bút toán này',
    `wallet_id` BIGINT NOT NULL,
    `amount` DECIMAL(15, 2) NOT NULL,
    `direction` VARCHAR(10) NOT NULL COMMENT 'DEBIT (Trừ) / CREDIT (Cộng)',
    `post_balance` DECIMAL(15, 2) NOT NULL COMMENT 'Số dư ngay sau khi biến động',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================================================
-- 3. TRANSACTION SERVICE (Database: transaction_db)
-- =========================================================================
USE `transaction_db`;

CREATE TABLE IF NOT EXISTS `transaction_requests` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `idempotency_key` VARCHAR(100) UNIQUE NOT NULL COMMENT 'Khóa chống thanh toán đúp',
    `from_wallet_id` BIGINT NOT NULL,
    `to_wallet_id` BIGINT NOT NULL,
    `amount` DECIMAL(15, 2) NOT NULL,
    `type` VARCHAR(30) NOT NULL COMMENT 'P2P, CASH_OUT, PAYMENT...',
    `status` VARCHAR(20) NOT NULL COMMENT 'PENDING, SUCCESS, FAILED, ROLLBACKED'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================================================
-- 4. RECONCILIATION SERVICE (Database: recon_db)
-- =========================================================================
USE `recon_db`;

CREATE TABLE IF NOT EXISTS `reconciliation_reports` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `report_date` DATE NOT NULL,
    `total_debit` DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    `total_credit` DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    `discrepancy` DECIMAL(15, 2) NOT NULL DEFAULT 0.00 COMMENT 'Độ lệch, bắt buộc phải = 0',
    `status` VARCHAR(20) NOT NULL COMMENT 'MATCHED, UNMATCHED'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================================================
-- 5. NOTIFICATION SERVICE (Database: notification_db)
-- =========================================================================
USE `notification_db`;

CREATE TABLE IF NOT EXISTS `notifications` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `status` VARCHAR(20) NOT NULL COMMENT 'SENT, FAILED, UNREAD'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;