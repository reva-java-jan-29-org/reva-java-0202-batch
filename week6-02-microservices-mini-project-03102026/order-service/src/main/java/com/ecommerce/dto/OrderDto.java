package com.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class OrderDto {
    private Long id;
    private Long customerId;
    private List<CartItemDto> items;
    private BigDecimal totalAmount;
    private String status;
    private String shippingAddress;
    private LocalDateTime createdAt;
    private PaymentResponse payment;  // attached after payment is processed
}
