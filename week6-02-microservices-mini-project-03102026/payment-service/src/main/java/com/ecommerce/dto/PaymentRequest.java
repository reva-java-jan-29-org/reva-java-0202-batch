package com.ecommerce.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PaymentRequest {
    private Long orderId;
    private Long customerId;
    private BigDecimal amount;
    private String cardNumber;     // 16-digit, no spaces
    private String cardExpiry;     // MM/YY
    private String cardCvv;        // 3-digit
    private String cardHolderName;
}
