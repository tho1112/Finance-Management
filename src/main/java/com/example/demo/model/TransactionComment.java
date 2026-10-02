package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Bình luận có thể gắn trực tiếp với Nhóm (Group Discussion)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = true)
    @JsonIgnore
    private AppGroup group;

    // Hoặc gắn với một Giao dịch cụ thể trong nhóm
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_transaction_id", nullable = true)
    @JsonIgnore
    private GroupTransaction groupTransaction;

    @NotNull(message = "Người bình luận không được để trống!")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "Nội dung bình luận không được để trống!")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
