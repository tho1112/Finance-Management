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
public class ReceiptItemDto {
    private String name;
    private Double quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
