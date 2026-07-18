package com.ymall.backend.global.config;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.security.JwtAccessDeniedHandler;
import com.ymall.backend.global.security.JwtAuthenticationEntryPoint;
import com.ymall.backend.global.security.JwtAuthenticationFilter;
import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.global.security.SecurityErrorResponseWriter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * JWT 기반 무상태 인증을 적용하고 공개 조회, 판매자 운영, 관리자 API의 권한을 분리한다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtTokenProvider jwtTokenProvider,
        SecurityErrorResponseWriter responseWriter
    ) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(POST, "/api/members/signup", "/api/members/login").permitAll()
                .requestMatchers(GET, "/api/members/email-availability").permitAll()
                .requestMatchers(GET, "/api/products/**", "/api/categories/**", "/images/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/seller/**").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(POST, "/api/files/images").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(POST, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(PUT, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(DELETE, "/api/products/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider, responseWriter),
                UsernamePasswordAuthenticationFilter.class
            )
            .build();
    }
}
