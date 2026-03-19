package com.ecommerce.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentRequest {
    private Long orderId;
    private Long customerId;
    private BigDecimal amount;
    private String cardNumber;
    private String cardExpiry;
    private String cardCvv;
    private String cardHolderName;
}
