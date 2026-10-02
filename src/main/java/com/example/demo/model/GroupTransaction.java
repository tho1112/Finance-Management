package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "group_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Giao dịch phải thuộc về một nhóm!")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @JsonIgnore
    private AppGroup group;

    @NotNull(message = "Người thanh toán không được để trống!")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer;

    @NotNull(message = "Số tiền không được để trống!")
    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0!")
    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Ngày giao dịch không được để trống!")
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "split_type", length = 50)
    @Builder.Default
    private String splitType = "EQUAL"; // EQUAL, EXACT, PERCENTAGE

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "groupTransaction", cascade = CascadeType.ALL)
    private List<GroupTransactionSplit> splits;

    @OneToMany(mappedBy = "groupTransaction", cascade = CascadeType.ALL)
    private List<TransactionComment> comments;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.transactionDate == null) {
            this.transactionDate = LocalDateTime.now();
        }
    }
}
