package com.ecommerce.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.dto.PaymentRequest;
import com.ecommerce.dto.PaymentResponse;
import com.ecommerce.service.PaymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Internal endpoint — called by order-service via Feign.
     * Processes a payment and returns the result.
     */
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.processPayment(request));
    }

    /** Get the payment details for a specific order (owner or admin). */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getPaymentByOrder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role,
            @PathVariable Long orderId) {
        try {
            PaymentResponse payment = paymentService.getPaymentByOrderId(orderId);
            // Admins can see any payment; customers only see their own
            if (!"ROLE_ADMIN".equals(role) && !payment.getCustomerId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
            }
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /** Get payment history for the currently logged-in customer. */
    @GetMapping("/my")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(paymentService.getPaymentsByCustomer(userId));
    }

    /** Admin: get all payments. */
    @GetMapping("/all")
    public ResponseEntity<?> getAllPayments(
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role) {
        if (!"ROLE_ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin access required"));
        }
        return ResponseEntity.ok(paymentService.getAllPayments());
    }
}
