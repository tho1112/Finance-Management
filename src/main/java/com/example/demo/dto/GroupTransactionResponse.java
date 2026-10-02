package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupTransactionResponse {
    private Long id;
    private Long groupId;
    private Long payerId;
    private String payerName;
    private String payerAvatar;
    private BigDecimal amount;
    private String description;
    private LocalDateTime transactionDate;
    private String splitType;
    private LocalDateTime createdAt;
    private List<SplitDetailDTO> splits;
    private List<CommentResponse> comments;
}
