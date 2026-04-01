-- Tạo Database cho Identity Service (Quản lý User)
CREATE DATABASE IF NOT EXISTS `identity_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo Database cho Wallet & Ledger Service (Két sắt & Sổ cái)
CREATE DATABASE IF NOT EXISTS `wallet_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo Database cho Transaction Service (Điều phối SAGA)
CREATE DATABASE IF NOT EXISTS `transaction_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo Database cho Reconciliation Service (Đối soát)
CREATE DATABASE IF NOT EXISTS `recon_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo Database cho Notification Service (Thông báo)
CREATE DATABASE IF NOT EXISTS `notification_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Cấp quyền cho user (Mặc định sẽ dùng user root trong môi trường dev)
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%';
FLUSH PRIVILEGES;