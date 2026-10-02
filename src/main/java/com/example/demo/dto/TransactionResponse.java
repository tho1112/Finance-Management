package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private BigDecimal amount;
    private String note;
    private LocalDateTime transactionDate;
    private String transactionType;
    private String type;

    // Thông tin Tài khoản / Ví
    private Long accountId;
    private String accountName;
    private Long walletId;     // Alias
    private String walletName; // Alias

    // Thông tin Danh mục
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;

    // Các trường mở rộng mới theo ERD
    private String aiRawText;
    private String receiptImageUrl;
    private Boolean isRecurring;
    private LocalDateTime createdAt;
}