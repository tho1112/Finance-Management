package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final EmailService emailService;
    private final Map<String, OtpEntry> otpCache = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private static final int MAX_ATTEMPTS = 5;

    private static class OtpEntry {
        final String code;
        final long expiresAt;
        final AtomicInteger attempts = new AtomicInteger(0);

        OtpEntry(String code, long expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }

    /**
     * Tạo và gửi mã OTP 6 số đến email
     */
    public void generateAndSendOtp(String email) {
        String cleanEmail = email.trim().toLowerCase();
        int number = random.nextInt(900000) + 100000;
        String otpCode = String.valueOf(number);

        // Hạn 5 phút
        long expiresAt = System.currentTimeMillis() + (5 * 60 * 1000);
        otpCache.put(cleanEmail, new OtpEntry(otpCode, expiresAt));

        emailService.sendOtpEmail(cleanEmail, otpCode);
    }

    /**
     * Xác thực mã OTP người dùng nhập (Tối đa 5 lần thử)
     */
    public boolean verifyOtp(String email, String inputOtp) {
        if (email == null || inputOtp == null) return false;

        String cleanEmail = email.trim().toLowerCase();
        OtpEntry entry = otpCache.get(cleanEmail);

        if (entry == null) {
            log.warn("[OtpService] Không tìm thấy mã OTP cho email: {}", cleanEmail);
            return false;
        }

        if (System.currentTimeMillis() > entry.expiresAt) {
            log.warn("[OtpService] Mã OTP cho email {} đã hết hạn!", cleanEmail);
            otpCache.remove(cleanEmail);
            return false;
        }

        int currentAttempts = entry.attempts.incrementAndGet();
        if (currentAttempts > MAX_ATTEMPTS) {
            log.warn("[OtpService] Quá số lần thử OTP cho email {}. Đã thu hồi OTP!", cleanEmail);
            otpCache.remove(cleanEmail);
            return false;
        }

        boolean isValid = entry.code.equals(inputOtp.trim());
        if (isValid) {
            // Xác thực thành công thì xoá mã khỏi cache để tránh dùng lại
            otpCache.remove(cleanEmail);
            log.info("[OtpService] Xác thực OTP thành công cho: {}", cleanEmail);
        } else {
            log.warn("[OtpService] Mã OTP không khớp cho email: {} (Lần thử {}/{})", cleanEmail, currentAttempts, MAX_ATTEMPTS);
        }

        return isValid;
    }

    /**
     * Dọn dẹp OTP hết hạn định kỳ mỗi 10 phút để giải phóng bộ nhớ
     */
    @Scheduled(fixedRate = 600000)
    public void cleanupExpiredOtps() {
        long now = System.currentTimeMillis();
        otpCache.entrySet().removeIf(entry -> now > entry.getValue().expiresAt);
        log.debug("[OtpService] Đã dọn dẹp các mã OTP hết hạn.");
    }
}
