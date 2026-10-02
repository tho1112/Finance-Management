package com.example.demo.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketReplyRequest {
    @NotBlank(message = "Nội dung phản hồi không được để trống!")
    private String content;

    private String attachmentUrl;

    private Boolean isInternalNote = false;
}
