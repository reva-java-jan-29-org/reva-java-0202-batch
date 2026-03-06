package com.training.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity			//?
@EnableMethodSecurity		//?
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		
		http
        	// Disable CSRF — not needed for stateless REST APIs
        	// CSRF is for browser-based form submissions with session cookies
        	.csrf(AbstractHttpConfigurer::disable)
						
			.authorizeHttpRequests(auth -> auth

	                // ── Public endpoints — no authentication required ─────────────
	                .requestMatchers("/api/auth/**").permitAll()             // login, register
	                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()  // browse categories
	                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()    // browse products
	                
	                // ── Admin-only endpoints ──────────────────────────────────────
	                .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
	                .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
	                .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")
	                
	                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
	                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
	                .requestMatchers(HttpMethod.PATCH, "/api/products/**").hasRole("ADMIN")
	                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

	                // ── Everything else requires at least authentication ──────────
	                .anyRequest().authenticated()
			)
			
			// Stateless session — Spring Security will NOT create HTTP sessions
            // Each request must carry its own JWT
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Register our custom authentication provider (DAO-based)
            .authenticationProvider(authenticationProvider())

            // Add JWT filter BEFORE Spring's built-in UsernamePasswordAuthenticationFilter
            // This ensures JWT is checked before any other authentication attempt
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
			
			.httpBasic(basic -> basic.disable())
			.formLogin(formlogin -> formlogin.disable());
		
		
		return http.build();
	}
	
	@Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        // Tell Spring Security how to load users
        authProvider.setUserDetailsService(userDetailsService);
        // Tell Spring Security how to verify passwords (BCrypt)
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // ── Authentication Manager ─────────────────────────────────────────────

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    // ── Password Encoder ───────────────────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
