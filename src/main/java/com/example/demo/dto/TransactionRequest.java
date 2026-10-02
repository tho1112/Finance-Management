package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequest {

    @NotNull(message = "Số tiền không được để trống!")
    @DecimalMin(value = "0.01", message = "Số tiền giao dịch phải lớn hơn 0!")
    private BigDecimal amount;

    // Nguồn tiền (hỗ trợ accountId / walletId)
    private Long accountId;
    private Long walletId;

    // Có thể null đối với giao dịch Thu nhập
    private Long categoryId;

    private String transactionDate;

    public String getEffectiveTransactionDate() {
        if (transactionDate != null && !transactionDate.isBlank()) {
            return transactionDate;
        }
        return java.time.LocalDateTime.now().toString();
    }

    @Size(max = 1000, message = "Ghi chú không được dài quá 1000 ký tự!")
    private String note;

    private String transactionType; // EXPENSE, INCOME, TRANSFER
    private String type;            // Tương thích ngược

    private String aiRawText;

    @Size(max = 500)
    private String receiptImageUrl;

    private Boolean isRecurring;

    public Long getEffectiveAccountId() {
        return accountId != null ? accountId : walletId;
    }

    public String getEffectiveType() {
        if (transactionType != null && !transactionType.isBlank()) {
            return transactionType;
        }
        if (type != null && !type.isBlank()) {
            return type;
        }
        return "EXPENSE";
    }
}