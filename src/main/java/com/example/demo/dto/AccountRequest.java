package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountRequest {

    @NotBlank(message = "Tên tài khoản/ví không được để trống!")
    @Size(max = 100, message = "Tên tài khoản/ví không được dài quá 100 ký tự!")
    private String accountName;

    @Size(max = 50)
    @Builder.Default
    private String type = "CASH"; // CASH, BANK, CREDIT_CARD, E_WALLET

    @NotNull(message = "Số dư ban đầu không được để trống!")
    private BigDecimal balance;

    // Helper tương thích nếu frontend gửi "name"
    public String getAccountName() {
        return accountName;
    }

    public void setName(String name) {
        this.accountName = name;
    }
}