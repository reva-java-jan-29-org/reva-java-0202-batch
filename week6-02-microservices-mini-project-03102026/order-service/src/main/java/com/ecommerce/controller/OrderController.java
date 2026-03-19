package com.ecommerce.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.dto.OrderDto;
import com.ecommerce.dto.PlaceOrderRequest;
import com.ecommerce.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /** Place a new order (customer). Includes payment card details. */
    @PostMapping
    public ResponseEntity<?> placeOrder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody PlaceOrderRequest request) {
        try {
            OrderDto order = orderService.placeOrder(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Get current customer's orders. */
    @GetMapping
    public ResponseEntity<List<OrderDto>> getMyOrders(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    /** Get a specific order — only the owning customer or an admin can access. */
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role,
            @PathVariable Long orderId) {
        try {
            if ("ROLE_ADMIN".equals(role)) {
                return ResponseEntity.ok(orderService.getOrderByIdAsAdmin(orderId));
            }
            return ResponseEntity.ok(orderService.getOrderById(orderId, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    /** Admin: view all orders across all customers. */
    @GetMapping("/all")
    public ResponseEntity<?> getAllOrders(
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role) {
        if (!"ROLE_ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin access required"));
        }
        return ResponseEntity.ok(orderService.getAllOrders());
    }
}
