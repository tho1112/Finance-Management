package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String nickname;
    private String gender;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    private String avatarUrl;
    private String language;
    private String currency;
    private String authProvider;
    private String userCode;
    private com.example.demo.model.UserRole role;
    private com.example.demo.model.UserStatus status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}