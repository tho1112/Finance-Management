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
public class UserRequest {

    // Bỏ hoàn toàn logic xác thực tên đăng nhập trong phần đăng ký (hệ thống sử dụng Email)
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống!")
    @Size(min = 6, max = 30, message = "Mật khẩu phải từ 6 đến 30 ký tự!")
    private String password;

    private String confirmPassword;

    // Mã xác thực OTP gửi qua email
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

    // Các thuộc tính mở rộng mới theo ERD
    private String nickname;
    private String gender;
    private String avatarUrl;
    private String language;
    private String currency;
    private String authProvider;
}