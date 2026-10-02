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
import java.time.LocalDate;

@Entity
@Table(name = "budgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Ngân sách bắt buộc phải gắn với một người dùng!")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "limit_amount", precision = 19, scale = 2)
    private BigDecimal limitAmount;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "alert_threshold_percent")
    @Builder.Default
    private Integer alertThresholdPercent = 80;

    @Column(name = "spent_amount", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal spentAmount = BigDecimal.ZERO;

    @Column(name = "name", length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "jar_type")
    private JarType jarType;

    @Column(name = "month_string", length = 7)
    private String monthString;

    @Column(name = "icon", length = 50)
    private String icon;

    @PrePersist
    @PreUpdate
    protected void onPrePersistOrUpdate() {
        if (this.spentAmount == null) {
            this.spentAmount = BigDecimal.ZERO;
        }
        if (this.amount == null && this.limitAmount != null) {
            this.amount = this.limitAmount;
        } else if (this.limitAmount == null && this.amount != null) {
            this.limitAmount = this.amount;
        }
        if (this.monthString == null && this.startDate != null) {
            this.monthString = this.startDate.toString().substring(0, 7);
        }
    }

    public BigDecimal getAmount() {
        return this.amount != null ? this.amount : this.limitAmount;
    }

    public BigDecimal getLimitAmount() {
        return this.limitAmount != null ? this.limitAmount : this.amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
        this.limitAmount = amount;
    }

    public void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
        this.amount = limitAmount;
    }
}