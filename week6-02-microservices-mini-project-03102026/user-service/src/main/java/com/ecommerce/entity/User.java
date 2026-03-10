package com.ecommerce.entity;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@MappedSuperclass
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; 
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    
 // ── UserDetails interface methods ──────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security expects role names prefixed with "ROLE_"
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));		
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;  // Simplification: accounts never expire
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;  // Simplification: accounts never get locked
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;  // Simplification: credentials never expire
    }

    @Override
    public boolean isEnabled() {
        return true;  // Simplification: all accounts are enabled
    }
    
    
}
