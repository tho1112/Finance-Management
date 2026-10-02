package com.example.demo.dto.admin;

import com.example.demo.model.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRoleUpdateRequest {
    @NotNull(message = "Vai trò không được để trống!")
    private UserRole role;
}
