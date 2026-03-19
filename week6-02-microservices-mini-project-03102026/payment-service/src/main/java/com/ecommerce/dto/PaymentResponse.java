package com.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ecommerce.entity.Payment;

import lombok.Data;

@Data
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private Long customerId;
    private BigDecimal amount;
    private String status;         // "SUCCESS" | "FAILED"
    private String failureReason;
    private String transactionId;
    private String cardLastFour;
    private LocalDateTime processedAt;

    public static PaymentResponse from(Payment p) {
        PaymentResponse r = new PaymentResponse();
        r.setId(p.getId());
        r.setOrderId(p.getOrderId());
        r.setCustomerId(p.getCustomerId());
        r.setAmount(p.getAmount());
        r.setStatus(p.getStatus().name());
        r.setFailureReason(p.getFailureReason());
        r.setTransactionId(p.getTransactionId());
        r.setCardLastFour(p.getCardLastFour());
        r.setProcessedAt(p.getProcessedAt());
        return r;
    }
}
