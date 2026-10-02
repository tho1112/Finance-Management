package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_user_date", columnList = "user_id, transaction_date DESC"),
        @Index(name = "idx_tx_user_cat", columnList = "user_id, category_id"),
        @Index(name = "idx_tx_account", columnList = "account_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Giao dịch bắt buộc phải thuộc về một người dùng!")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private User user;

    @NotNull(message = "Giao dịch bắt buộc phải gắn với một Tài khoản/Ví!")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private Category category;

    @NotNull(message = "Số tiền giao dịch không được để trống!")
    @DecimalMin(value = "0.01", message = "Số tiền giao dịch phải lớn hơn 0!")
    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "transaction_type", length = 50, nullable = false)
    @Builder.Default
    private String transactionType = "EXPENSE"; // EXPENSE, INCOME, TRANSFER

    @NotNull(message = "Ngày giờ giao dịch không được để trống!")
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "ai_raw_text", columnDefinition = "TEXT")
    private String aiRawText;

    @Size(max = 500)
    @Column(name = "receipt_image_url", length = 500)
    private String receiptImageUrl;

    @Column(name = "is_recurring")
    @Builder.Default
    private Boolean isRecurring = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.transactionDate == null) {
            this.transactionDate = LocalDateTime.now();
        }
        if (this.isRecurring == null) {
            this.isRecurring = false;
        }
    }
}