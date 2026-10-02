package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AutoBudgetRequest {

    @NotNull(message = "Vui lòng nhập tổng thu nhập dự kiến!")
    @DecimalMin(value = "1000.0", message = "Thu nhập phải lớn hơn 1.000 VNĐ!")
    private BigDecimal income;

    @NotBlank(message = "Tháng áp dụng không được để trống!")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "Tháng phải tuân thủ đúng định dạng YYYY-MM (Ví dụ: 2026-04)!")
    private String month;
}