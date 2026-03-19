package com.ecommerce.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecommerce.dto.PaymentRequest;
import com.ecommerce.dto.PaymentResponse;
import com.ecommerce.entity.Payment;
import com.ecommerce.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

/**
 * Mock Payment Service — simulates a Payment Service Provider (PSP) like Stripe.
 *
 * Supported test card numbers:
 * ┌──────────────────────┬──────────────────────────────┐
 * │ Card Number          │ Outcome                      │
 * ├──────────────────────┼──────────────────────────────┤
 * │ 4242424242424242     │ SUCCESS                      │
 * │ 4000000000000002     │ FAILED — Card declined       │
 * │ 4000000000009995     │ FAILED — Insufficient funds  │
 * │ 4000000000000069     │ FAILED — Expired card        │
 * │ Any other 16 digits  │ SUCCESS (teaching default)   │
 * └──────────────────────┴──────────────────────────────┘
 *
 * To integrate a real PSP (e.g. Stripe):
 *   1. Add `com.stripe:stripe-java` dependency to pom.xml
 *   2. Set STRIPE_SECRET_KEY environment variable
 *   3. Replace the mock logic below with Stripe API calls
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;

    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for orderId={}, amount={}", request.getOrderId(), request.getAmount());

        // Basic validation
        String cardNumber = request.getCardNumber() == null ? "" : request.getCardNumber().replaceAll("\\s", "");
        if (cardNumber.length() != 16 || !cardNumber.matches("\\d+")) {
            return saveAndReturn(buildFailedPayment(request, cardNumber, "Invalid card number"));
        }
        if (request.getCardCvv() == null || !request.getCardCvv().matches("\\d{3,4}")) {
            return saveAndReturn(buildFailedPayment(request, cardNumber, "Invalid CVV"));
        }
        if (request.getCardExpiry() == null || !request.getCardExpiry().matches("\\d{2}/\\d{2}")) {
            return saveAndReturn(buildFailedPayment(request, cardNumber, "Invalid expiry (use MM/YY)"));
        }

        // Mock PSP decision based on test card number
        String failureReason = getFailureReason(cardNumber);

        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setCustomerId(request.getCustomerId());
        payment.setAmount(request.getAmount());
        payment.setCardLastFour(cardNumber.substring(12));
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setProcessedAt(LocalDateTime.now());

        if (failureReason != null) {
            payment.setStatus(Payment.Status.FAILED);
            payment.setFailureReason(failureReason);
            log.warn("Payment FAILED for orderId={}: {}", request.getOrderId(), failureReason);
        } else {
            payment.setStatus(Payment.Status.SUCCESS);
            log.info("Payment SUCCESS for orderId={}, txn={}", request.getOrderId(), payment.getTransactionId());
        }

        return saveAndReturn(payment);
    }

    public PaymentResponse getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new RuntimeException("No payment found for order: " + orderId));
    }

    public List<PaymentResponse> getPaymentsByCustomer(Long customerId) {
        return paymentRepository.findByCustomerIdOrderByProcessedAtDesc(customerId)
                .stream().map(PaymentResponse::from).toList();
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream().map(PaymentResponse::from).toList();
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private Payment buildFailedPayment(PaymentRequest request, String cardNumber, String reason) {
        Payment p = new Payment();
        p.setOrderId(request.getOrderId());
        p.setCustomerId(request.getCustomerId());
        p.setAmount(request.getAmount());
        p.setCardLastFour(cardNumber.length() >= 4 ? cardNumber.substring(Math.max(0, cardNumber.length() - 4)) : "0000");
        p.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        p.setStatus(Payment.Status.FAILED);
        p.setFailureReason(reason);
        p.setProcessedAt(LocalDateTime.now());
        return p;
    }

    private PaymentResponse saveAndReturn(Payment payment) {
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    /**
     * Returns a failure reason string for known "decline" test cards,
     * or null if the card should succeed.
     */
    private String getFailureReason(String cardNumber) {
        return switch (cardNumber) {
            case "4000000000000002" -> "Card declined";
            case "4000000000009995" -> "Insufficient funds";
            case "4000000000000069" -> "Expired card";
            case "4000000000000127" -> "Incorrect CVV";
            default -> null; // all other cards succeed
        };
    }
}
