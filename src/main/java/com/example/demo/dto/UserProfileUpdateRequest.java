package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileUpdateRequest {

    @Size(max = 100, message = "Họ và tên không được dài quá 100 ký tự!")
    private String fullName;

    @Size(max = 50, message = "Nickname không được dài quá 50 ký tự!")
    private String nickname;

    @Size(max = 20)
    private String gender;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private String avatarUrl;

    @Size(max = 20)
    private String language;

    @Size(max = 10)
    private String currency;
}
