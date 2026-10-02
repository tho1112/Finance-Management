package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentRequest {

    private Long groupId;

    private Long groupTransactionId;

    @NotBlank(message = "Nội dung bình luận không được để trống!")
    private String content;
}
