package com.ecommerce.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecommerce.dto.CartItemDto;
import com.ecommerce.dto.OrderDto;
import com.ecommerce.dto.PaymentRequest;
import com.ecommerce.dto.PaymentResponse;
import com.ecommerce.dto.PlaceOrderRequest;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.feign.PaymentServiceClient;
import com.ecommerce.feign.ProductServiceClient;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.OrderRepository;

import jakarta.transaction.Transactional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductServiceClient productServiceClient;

    @Autowired
    private PaymentServiceClient paymentServiceClient;

    @Transactional
    public OrderDto placeOrder(Long userId, PlaceOrderRequest request) {
        Cart cart = cartRepository.findByCustomerId(userId)
                .orElseThrow(() -> new RuntimeException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot place order with empty cart");
        }

        // Create order in PENDING state first
        Order order = new Order();
        order.setCustomerId(userId);
        order.setShippingAddress(request.getShippingAddress());

        BigDecimal total = BigDecimal.ZERO;

        for (var cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setProductName(cartItem.getProductName());
            orderItem.setPrice(cartItem.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            order.getItems().add(orderItem);
            total = total.add(cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        order.setTotalAmount(total);
        Order savedOrder = orderRepository.save(order);

        // Process payment via payment-service
        PaymentResponse payment = paymentServiceClient.processPayment(
                PaymentRequest.builder()
                        .orderId(savedOrder.getId())
                        .customerId(userId)
                        .amount(total)
                        .cardNumber(request.getCardNumber())
                        .cardExpiry(request.getCardExpiry())
                        .cardCvv(request.getCardCvv())
                        .cardHolderName(request.getCardHolderName())
                        .build()
        );

        if ("SUCCESS".equals(payment.getStatus())) {
            // Reduce stock only after successful payment
            for (var cartItem : cart.getItems()) {
                productServiceClient.reduceStock(cartItem.getProductId(), cartItem.getQuantity());
            }
            savedOrder.setStatus(Order.Status.CONFIRMED);
            cartService.clearCart(userId);
        } else {
            savedOrder.setStatus(Order.Status.PAYMENT_FAILED);
        }

        Order finalOrder = orderRepository.save(savedOrder);
        OrderDto dto = toDto(finalOrder);
        dto.setPayment(payment);

        if (Order.Status.PAYMENT_FAILED.name().equals(dto.getStatus())) {
            throw new RuntimeException("Payment failed: " + payment.getFailureReason());
        }

        return dto;
    }

    public List<OrderDto> getUserOrders(Long userId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /** Admin: get all orders across all customers. */
    public List<OrderDto> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public OrderDto getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        if (!order.getCustomerId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
        return toDto(order);
    }

    /** Admin: get any order without ownership check. */
    public OrderDto getOrderByIdAsAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        return toDto(order);
    }

    private OrderDto toDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setCustomerId(order.getCustomerId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus().name());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setCreatedAt(order.getCreatedAt());

        List<CartItemDto> items = order.getItems().stream().map(item -> {
            CartItemDto itemDto = new CartItemDto();
            itemDto.setId(item.getId());
            itemDto.setProductId(item.getProductId());
            itemDto.setProductName(item.getProductName());
            itemDto.setPrice(item.getPrice());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            return itemDto;
        }).collect(Collectors.toList());
        dto.setItems(items);

        return dto;
    }
}
