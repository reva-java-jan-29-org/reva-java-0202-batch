package com.training;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		return http
				// For stateless REST APIs, disable CSRF (POST/PUT/PATCH/DELETE will otherwise
				// be blocked)
				.csrf(csrf -> csrf.disable())

				// Allow everything (no auth)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.GET, "/**").permitAll()
						.anyRequest().authenticated())

				// Optional (doesn't matter if permitAll, but safe to keep consistent)
//				.httpBasic(Customizer.withDefaults())
				
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())

				.build();
	}

}
