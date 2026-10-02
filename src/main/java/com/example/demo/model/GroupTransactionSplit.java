package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "group_transaction_splits")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupTransactionSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Khoản chia phải gắn với một giao dịch nhóm!")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_transaction_id", nullable = false)
    @JsonIgnore
    private GroupTransaction groupTransaction;

    @NotNull(message = "Người chịu chi phí không được để trống!")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Số tiền phải chia không được để trống!")
    @Column(name = "owed_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal owedAmount;

    @Column(name = "is_settled")
    @Builder.Default
    private Boolean isSettled = false;
}
