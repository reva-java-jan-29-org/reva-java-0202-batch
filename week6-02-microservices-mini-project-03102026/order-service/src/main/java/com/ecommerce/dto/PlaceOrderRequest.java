package com.ecommerce.dto;

import lombok.Data;

@Data
public class PlaceOrderRequest {
    private String shippingAddress;

    // Payment card details (test cards supported by mock PSP)
    // Test cards: 4242424242424242 → success | 4000000000000002 → declined
    private String cardNumber;    // 16-digit card number (no spaces)
    private String cardExpiry;    // MM/YY  e.g. "12/26"
    private String cardCvv;       // 3-digit CVV
    private String cardHolderName;
}
