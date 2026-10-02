package com.example.demo.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnnouncementRequest {
    @NotBlank(message = "Tiêu đề thông báo không được để trống!")
    private String title;

    @NotBlank(message = "Nội dung thông báo không được để trống!")
    private String content;

    private String type = "INFO"; // INFO, WARNING, MAINTENANCE, PROMOTION
    private Boolean isActive = true;
}
