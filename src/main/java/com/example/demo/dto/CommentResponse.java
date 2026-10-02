package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private Long groupId;
    private Long groupTransactionId;
    private Long userId;
    private String userName;
    private String userNickname;
    private String userAvatar;
    private String content;
    private String transactionDescription;
    private Double transactionAmount;
    private String transactionPayerName;
    private LocalDateTime createdAt;
}
