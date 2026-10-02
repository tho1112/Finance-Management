package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SplitDetailDTO {
    private Long userId;
    private String username;
    private String fullName;
    private BigDecimal owedAmount;
    private Boolean isSettled;
}
