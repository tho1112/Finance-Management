package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptScanResponse {
    private boolean success;
    private String message;
    private BigDecimal amount;
    private String transactionDate;
    private String merchantName;
    private Long suggestedCategoryId;
    private String suggestedCategoryName;
    private String suggestedCategoryIcon;
    private String suggestedJar;
    private String transactionType; // EXPENSE hoặc INCOME
    private String note;
    private String aiRawText;
    private String receiptImageUrl;
    private List<ReceiptItemDto> items;
    private Double confidence;
    private String source;
}
