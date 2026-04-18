# Theo dõi tiến độ đồ án hệ thống thanh toán

Cập nhật lần cuối: 2026-04-18

## 1) Tổng quan hiện tại

Dự án đang ở giai đoạn đã có khung microservices và đã code xong một số thành phần cốt lõi:

- Đã có 3 module code: api-gateway, identity-service, wallet-service.
- Đã có hạ tầng local với Docker Compose: MySQL, Redis, Kafka.
- Đã có script khởi tạo schema cho nhiều DB: identity, wallet, transaction, recon, notification.

Đánh giá nhanh:

- Hoàn thành nền tảng: trung bình-khá.
- Hoàn thành business flow end-to-end: chưa.

## 2) Những phần đã làm được

### 2.1 Infrastructure

- [x] Docker Compose cho MySQL + Redis + Kafka.
- [x] Có network + volume để chạy local ổn định.
- [x] Có SQL init tạo database và bảng cần thiết cho nhiều service.

File liên quan:

- infrastructure/docker-compose.yml
- infrastructure/mysql-init/02-create-tables.sql

### 2.2 Identity Service

- [x] Có đăng ký tài khoản (register).
- [x] Có đăng nhập (login).
- [x] Có refresh access token bằng refresh token.
- [x] Có logout và xóa refresh token.
- [x] JWT RSA signing/verification (private/public key).
- [x] Password hashing bằng BCrypt.
- [x] Lưu refresh token trong DB.
- [x] Đã cấu hình Spring Security stateless + custom JWT filter.

File liên quan:

- identity-service/src/main/java/com/payment/identity_service/controller/AuthController.java
- identity-service/src/main/java/com/payment/identity_service/service/AuthService.java
- identity-service/src/main/java/com/payment/identity_service/service/JwtService.java
- identity-service/src/main/java/com/payment/identity_service/config/SecurityConfig.java
- identity-service/src/main/java/com/payment/identity_service/config/JwtAuthenticationFilter.java

### 2.3 API Gateway

- [x] Đã route request theo path đến identity, wallet, transaction-service.
- [x] Có global filter check JWT từ cookie access_token.
- [x] Có trích X-User-Id và X-User-Role để chuyển tiếp xuống service sau.
- [x] Có rate limiting theo IP bằng Redis.
- [x] Có custom JSON response cho lỗi 429 và 401.
- [x] Có CORS mở cho frontend.

File liên quan:

- api-gateway/src/main/resources/application.yml
- api-gateway/src/main/java/com/payment/api_gateway/filter/AuthenticationFilter.java
- api-gateway/src/main/java/com/payment/api_gateway/config/RateLimiterConfig.java
- api-gateway/src/main/java/com/payment/api_gateway/filter/RateLimitCustomizerFilter.java

### 2.4 Wallet Service

- [x] Đã có entity Wallet và LedgerEntry.
- [x] Đã có xử lý cộng/trừ số dư.
- [x] Đã có ghi sổ cái (ledger) sau mỗi giao dịch.
- [x] Đã có optimistic locking với trường version.

File liên quan:

- wallet-service/src/main/java/com/payment/wallet_service/entity/Wallet.java
- wallet-service/src/main/java/com/payment/wallet_service/entity/LedgerEntry.java
- wallet-service/src/main/java/com/payment/wallet_service/service/WalletService.java

### 2.5 Kiểm thử

- [x] Đã có bộ test contextLoads cho cả 3 service.
- [ ] Chưa có unit test cho business logic.
- [ ] Chưa có integration test qua gateway -> identity -> wallet.
- [ ] Chưa có test concurrency cho wallet.

## 3) Bạn đang đứng ở đâu

Hiện tại dự án đã vượt qua bước "dựng khung và xây lớp nền".

Trạng thái gần đúng:

- Nền tảng và bảo mật cơ bản: đã có.
- Domain giao dịch thanh toán end-to-end: chưa khớp.
- Mức sẵn sàng demo luồng thanh toán thật: chưa đạt.

Nói cách khác: bạn đã xây xong khung xương và một số cột trụ lớn, giờ cần lắp "dòng chảy nghiệp vụ" và "độ bền".

## 4) Khoảng trống và rủi ro cần xử lý sớm

1. Chưa có transaction-service trong workspace

- Gateway đã route /api/v1/transactions/** đến cổng 8083 nhưng chưa có module code tương ứng.

2. Wallet service chưa lộ endpoint API

- Hiện có service layer, repository, entity nhưng chưa thấy controller để gọi từ gateway.




3. Cấu hình bảo mật chưa sẵn sàng production

- JWT private/public key đang hardcode trong application.yml.
- Cookie đang để secure=false, chưa thấy same-site policy rõ ràng.
- Access token đang set thời hạn rất dài (200 ngày) trong flow hiện tại.

4. Test còn rất mỏng

- Mới dừng ở mức contextLoads, chưa cover logic đăng ký/đăng nhập/ví/ledger/rate-limit.

## 5) Kế hoạch bước tiếp theo (ưu tiên từ trên xuống)

### Phase A - Khép flow tối thiểu để demo (ưu tiên cao nhất)

- [ ] Tạo transaction-service tối thiểu (tạo giao dịch + gọi wallet-service debit/credit).
- [ ] Tạo wallet controller và endpoint cần thiết (vd: get balance, process transaction nội bộ).
- [ ] Đồng bộ endpoint refresh giữa gateway và identity.
- [ ] Chạy thông 1 luồng demo:
  - register -> login -> tạo giao dịch -> cập nhật wallet -> ghi ledger.

Mục tiêu Phase A: có demo end-to-end chạy được trên local.

### Phase B - Làm chắc tính đúng và an toàn

- [ ] Đồng bộ role enum giữa Java và schema SQL.
- [ ] Đưa JWT key ra environment variable/secret.
- [ ] Chỉnh lại cookie policy (secure, same-site) theo môi trường.
- [ ] Thêm idempotency key cho giao dịch để tránh thanh toán đúp.

Mục tiêu Phase B: tránh lỗi nghiệp vụ và lỗi bảo mật cơ bản.

### Phase C - Kiểm thử và quan sát hệ thống

- [ ] Unit test cho AuthService và WalletService.
- [ ] Integration test qua Gateway.
- [ ] Test đồng thời cho WalletService để xác nhận optimistic lock.
- [ ] Bổ sung logging có cấu trúc và trace request id.

Mục tiêu Phase C: tự tin khi demo/bảo vệ đồ án.

## 6) Đề xuất cách cập nhật file này mỗi ngày

Mỗi lần code xong 1 hạng mục, cập nhật nhanh 4 dòng:

- Đã xong: ...
- Đang làm: ...
- Bị tắc ở: ...
- Bước tiếp theo ngày mai: ...

Bạn có thể giữ nguyên file này làm "dashboard tiến độ" cho đến lúc bảo vệ.

## 7) Mốc tiếp theo ngay lập tức (gợi ý 1-2 ngày)

1. Chốt lại API contract cho transaction-service và wallet-service.
2. Tạo transaction-service bản tối thiểu để gọi WalletService.processTransaction.
3. Sửa mismatch endpoint refresh tại gateway/identity.
4. Viết 2 test quan trọng nhất:
   - login trả cookie token
   - processTransaction debit khi đủ số dư và fail khi không đủ
