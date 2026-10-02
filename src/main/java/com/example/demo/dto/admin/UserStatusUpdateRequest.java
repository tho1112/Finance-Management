package com.example.demo.dto.admin;

import com.example.demo.model.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusUpdateRequest {
    @NotNull(message = "Trạng thái không được để trống!")
    private UserStatus status;
    private String reason;
}
