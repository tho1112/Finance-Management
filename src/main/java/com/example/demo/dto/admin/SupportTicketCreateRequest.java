package com.example.demo.dto.admin;

import com.example.demo.model.TicketCategory;
import com.example.demo.model.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupportTicketCreateRequest {
    @NotBlank(message = "Tiêu đề không được để trống!")
    private String subject;

    @NotBlank(message = "Nội dung mô tả không được để trống!")
    private String description;

    private TicketCategory category = TicketCategory.OTHER;
    private TicketPriority priority = TicketPriority.MEDIUM;
}
