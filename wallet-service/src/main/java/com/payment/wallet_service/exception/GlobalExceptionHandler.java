package com.payment.wallet_service.exception;

// DTO chuẩn hóa response trả về khi có lỗi.
import com.payment.wallet_service.dto.response.ErrorResponse;
// Annotation tạo logger 'log' tự động.
import lombok.extern.slf4j.Slf4j;
// Enum mã trạng thái HTTP.
import org.springframework.http.HttpStatus;
// Kiểu dữ liệu response có thể tùy biến status/body.
import org.springframework.http.ResponseEntity;
// Exception cho cơ chế optimistic locking (xung đột cập nhật đồng thời).
import org.springframework.orm.ObjectOptimisticLockingFailureException;
// Annotation đánh dấu method xử lý cho một loại exception.
import org.springframework.web.bind.annotation.ExceptionHandler;
// Biến class này thành bộ xử lý exception dùng chung cho REST API.
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Dùng để gắn thời điểm phát sinh lỗi vào response.
import java.time.LocalDateTime;

// Kích hoạt logger (log.info/warn/error...).
@Slf4j
// Đăng ký class này là global exception handler cho toàn bộ controller REST.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Bắt lỗi: Thiếu tiền
    // Method này chạy khi ném InsufficientBalanceException.
    @ExceptionHandler(InsufficientBalanceException.class)
    // Trả về HTTP response chứa body kiểu ErrorResponse.
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
        // Ghi log mức cảnh báo với message từ exception.
        log.warn("Lỗi nghiệp vụ: {}", ex.getMessage());
        // Trả về status 400 và payload lỗi chuẩn hóa.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                // Dùng builder để tạo object ErrorResponse.
                ErrorResponse.builder()
                        // Mã lỗi nội bộ để FE/monitoring dễ phân loại.
                        .code("ERR_400_BALANCE")
                        // Message chi tiết lấy trực tiếp từ exception.
                        .message(ex.getMessage())
                        // Thời điểm lỗi xảy ra.
                        .timestamp(LocalDateTime.now())
                        // Kết thúc builder, tạo instance ErrorResponse.
                        .build());
    }

    // Bắt lỗi: Không tìm thấy Ví
    // Method này chạy khi ném ResourceNotFoundException.
    @ExceptionHandler(ResourceNotFoundException.class)
    // Trả response 404 kèm cấu trúc lỗi thống nhất.
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        // Ghi log cảnh báo để tiện truy vết nghiệp vụ tìm kiếm.
        log.warn("Lỗi tìm kiếm: {}", ex.getMessage());
        // Trả về status 404 và body thông tin lỗi.
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                // Tạo object response bằng builder.
                ErrorResponse.builder()
                        // Mã lỗi nội bộ cho trường hợp không tìm thấy ví.
                        .code("ERR_404_WALLET")
                        // Message từ exception được đưa ra client.
                        .message(ex.getMessage())
                        // Gắn timestamp hiện tại.
                        .timestamp(LocalDateTime.now())
                        // Hoàn tất object response.
                        .build());
    }

    // Bắt lỗi quan trọng: Xung đột dữ liệu (Tương tranh - Có người khác vừa trừ
    // tiền xong)
    // Method này xử lý xung đột version khi cập nhật đồng thời.
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    // Trả về HTTP 409 Conflict cho lỗi race condition.
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(ObjectOptimisticLockingFailureException ex) {
        // Ghi log mức error để ưu tiên theo dõi vì có ảnh hưởng giao dịch.
        log.error("Xung đột Version Database (Race Condition)!");
        // Trả về status 409 và message thân thiện cho người dùng.
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                // Khởi tạo payload lỗi.
                ErrorResponse.builder()
                        // Mã lỗi nội bộ cho conflict.
                        .code("ERR_409_CONFLICT")
                        // Message cố định, không lộ chi tiết kỹ thuật nội bộ.
                        .message("Hệ thống đang bận xử lý giao dịch khác trên ví này. Vui lòng thử lại sau.")
                        // Thời gian lỗi.
                        .timestamp(LocalDateTime.now())
                        // Tạo object response hoàn chỉnh.
                        .build());
    }

    // Bắt các lỗi hệ thống không lường trước được
    // Fallback handler: bắt mọi exception chưa được handler cụ thể xử lý.
    @ExceptionHandler(Exception.class)
    // Trả về lỗi 500 để biểu thị lỗi nội bộ server.
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        // Log full stack trace để phục vụ debug/giám sát.
        log.error("Lỗi hệ thống không xác định: ", ex);
        // Trả response chuẩn hóa cho client, tránh lộ thông tin nhạy cảm.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                // Tạo payload lỗi.
                ErrorResponse.builder()
                        // Mã lỗi nội bộ cho lỗi hệ thống.
                        .code("ERR_500_SYSTEM")
                        // Message chung cho người dùng cuối.
                        .message("Đã xảy ra lỗi hệ thống, vui lòng liên hệ Admin.")
                        // Thời điểm lỗi xảy ra.
                        .timestamp(LocalDateTime.now())
                        // Hoàn tất tạo object.
                        .build());
    }
}