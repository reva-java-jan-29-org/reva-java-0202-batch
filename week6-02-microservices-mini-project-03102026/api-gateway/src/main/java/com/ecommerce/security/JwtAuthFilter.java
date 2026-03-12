package com.ecommerce.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;


@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtService jwtUtil;

    // Public endpoints that do not require JWT
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/register",
            "/api/auth/login"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        // Allow public endpoints without auth
        if (isPublicEndpoint(path, method)) {
            return chain.filter(exchange);
        }

        // Check for Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        System.out.println(authHeader);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
        	System.out.println("The token is invalid");
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        // Extract user info and add to headers
        System.out.println("Now the token is valid....");
        Claims claims = jwtUtil.extractAllClaims(token);
        String username = claims.getSubject();
        Long userId = claims.get("userId", Long.class);

        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId != null ? userId.toString() : "")
                .header("X-Username", username != null ? username : "")
                .build();
        
        
        System.out.println("Routing the request to the respected servie....");
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private boolean isPublicEndpoint(String path, String method) {
        // Public: register and login
        if (PUBLIC_ENDPOINTS.contains(path)) {
            return true;
        }
        // Public: GET requests to /api/products/**
        if (method.equals("GET") && path.startsWith("/api/products")) {
            return true;
        }
        return false;
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
