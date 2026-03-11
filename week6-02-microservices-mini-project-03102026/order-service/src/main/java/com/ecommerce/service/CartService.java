package com.ecommerce.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecommerce.dto.CartDto;
import com.ecommerce.dto.CartItemDto;
import com.ecommerce.dto.CartItemRequest;
import com.ecommerce.dto.ProductDto;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.feign.ProductServiceClient;
import com.ecommerce.repository.CartRepository;

@Service
public class CartService {

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private ProductServiceClient productServiceClient;

	public CartDto getCart(Long userId) {
		Cart cart = cartRepository.findByCustomerId(userId).orElseGet(() -> createNewCart(userId));
		return toDto(cart);
	}

	public CartDto addToCart(Long userId, CartItemRequest request) {
		
		// Fetch product info via OpenFeign
		ProductDto product = productServiceClient.getProductById(request.getProductId());
		

		Cart cart = cartRepository.findByCustomerId(userId).orElseGet(() -> createNewCart(userId));

		// Check if product already in cart
		Optional<CartItem> existingItem = cart.getItems().stream()
				.filter(item -> item.getProductId().equals(request.getProductId())).findFirst();

		if (existingItem.isPresent()) {
			existingItem.get().setQuantity(existingItem.get().getQuantity() + request.getQuantity());
		} else {
			CartItem newItem = new CartItem();
			newItem.setCart(cart);
			newItem.setProductId(product.getId());
			newItem.setProductName(product.getName());
			newItem.setPrice(product.getPrice());
			newItem.setQuantity(request.getQuantity());
			cart.getItems().add(newItem);
		}

		return toDto(cartRepository.save(cart));
	}

	public CartDto removeFromCart(Long userId, Long itemId) {
		Cart cart = cartRepository.findByCustomerId(userId)
				.orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));
		cart.getItems().removeIf(item -> item.getId().equals(itemId));
		return toDto(cartRepository.save(cart));
	}

	public CartDto updateCartItem(Long userId, Long itemId, int quantity) {
		Cart cart = cartRepository.findByCustomerId(userId)
				.orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));
		cart.getItems().stream().filter(item -> item.getId().equals(itemId)).findFirst()
				.ifPresent(item -> item.setQuantity(quantity));
		return toDto(cartRepository.save(cart));
	}

	public void clearCart(Long userId) {
		Cart cart = cartRepository.findByCustomerId(userId).orElse(null);
		if (cart != null) {
			cart.getItems().clear();
			cartRepository.save(cart);
		}
	}

	private Cart createNewCart(Long userId) {
		Cart cart = new Cart();
		cart.setCustomerId(userId);
		return cartRepository.save(cart);
	}

	private CartDto toDto(Cart cart) {
		CartDto dto = new CartDto();
		dto.setId(cart.getId());
		dto.setCustomerId(cart.getCustomerId());
		List<CartItemDto> itemDtos = cart.getItems().stream().map(this::toItemDto).collect(Collectors.toList());
		dto.setItems(itemDtos);
		BigDecimal total = itemDtos.stream().map(CartItemDto::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
		dto.setTotalAmount(total);
		return dto;
	}

	private CartItemDto toItemDto(CartItem item) {
		CartItemDto dto = new CartItemDto();
		dto.setId(item.getId());
		dto.setProductId(item.getProductId());
		dto.setProductName(item.getProductName());
		dto.setPrice(item.getPrice());
		dto.setQuantity(item.getQuantity());
		dto.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
		return dto;
	}
}
