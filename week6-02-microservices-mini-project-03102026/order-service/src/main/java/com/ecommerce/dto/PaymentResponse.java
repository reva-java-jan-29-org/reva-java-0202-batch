package com.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private Long customerId;
    private BigDecimal amount;
    private String status;          // SUCCESS | FAILED
    private String failureReason;   // populated when status = FAILED
    private String transactionId;   // mock transaction reference
    private String cardLastFour;
    private LocalDateTime processedAt;
}
