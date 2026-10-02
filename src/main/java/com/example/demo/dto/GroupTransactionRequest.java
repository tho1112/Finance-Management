package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupTransactionRequest {

    @NotNull(message = "ID nhóm không được để trống!")
    private Long groupId;

    private Long payerId; // Nếu null thì lấy user hiện tại

    @NotNull(message = "Số tiền không được để trống!")
    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0!")
    private BigDecimal amount;

    private String description;

    private String transactionDate;

    private String splitType; // EQUAL, EXACT, PERCENTAGE

    private List<SplitDetailDTO> splits;
}
