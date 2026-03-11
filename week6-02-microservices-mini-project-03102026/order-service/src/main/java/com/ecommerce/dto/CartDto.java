package com.ecommerce.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class CartDto {
    private Long id;
    private Long customerId;
    private List<CartItemDto> items;
    private BigDecimal totalAmount;

}
