package com.ecommerce.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.dto.CartDto;
import com.ecommerce.dto.CartItemRequest;
import com.ecommerce.service.CartService;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public ResponseEntity<CartDto> getCart(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody CartItemRequest request) {
        try {
            return ResponseEntity.ok(cartService.addToCart(userId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDto> removeFromCart(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeFromCart(userId, itemId));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDto> updateCartItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long itemId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(cartService.updateCartItem(userId, itemId, quantity));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(@RequestHeader("X-User-Id") Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}

