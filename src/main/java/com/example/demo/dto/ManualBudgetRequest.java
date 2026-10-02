package com.example.demo.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ManualBudgetRequest {
    @NotBlank(message = "Tên ngân sách không được để trống!")
    private String name;

    @NotNull(message = "Hạn mức không được để trống!")
    @DecimalMin(value = "0.0", message = "Hạn mức không được là số âm!")
    private BigDecimal limit;

    @NotBlank(message = "Tháng áp dụng không được để trống!")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "Tháng phải có định dạng YYYY-MM")
    private String month;
}