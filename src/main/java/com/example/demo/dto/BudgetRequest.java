package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetRequest {

    @NotNull(message = "Vui lòng chọn danh mục áp dụng ngân sách!")
    private Long categoryId;

    @NotNull(message = "Hạn mức ngân sách không được để trống!")
    @DecimalMin(value = "0.01", message = "Hạn mức phải lớn hơn 0!")
    private BigDecimal amount;

    @NotNull(message = "Ngày bắt đầu không được để trống!")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống!")
    private LocalDate endDate;

    private Integer alertThresholdPercent; // Mặc định 80%
}
