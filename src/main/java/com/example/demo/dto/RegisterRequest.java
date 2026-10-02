package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    private String username;

    @NotBlank(message = "Mật khẩu không được để trống!")
    @Size(min = 6, max = 30, message = "Mật khẩu phải từ 6 đến 30 ký tự!")
    private String password;

    private String confirmPassword;

    private String otp;

    @NotBlank(message = "Họ và tên không được để trống!")
    @Size(max = 100, message = "Họ và tên không được dài quá 100 ký tự!")
    private String fullName;

    @NotBlank(message = "Email không được để trống!")
    @Email(message = "Email không đúng định dạng! (Ví dụ: name@gmail.com)")
    @Pattern(
        regexp = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$",
        message = "Địa chỉ email không hợp lệ! Vui lòng nhập email thật (Ví dụ: name@gmail.com)"
    )
    private String email;

    private String nickname;
    private String gender;
    private String avatarUrl;
    private String language;
    private String currency;
    private String authProvider;
}
