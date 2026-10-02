package com.example.demo.model;

public enum TicketCategory {
    OCR_ISSUE,           // Lỗi quét hóa đơn
    TRANSACTION_ERROR,   // Lỗi giao dịch / số dư ví
    ACCOUNT,             // Tài khoản, mật khẩu, OTP
    GROUP,               // Nhóm chi tiêu / chia tiền
    FEEDBACK,            // Góp ý tính năng
    OTHER                // Khác
}
