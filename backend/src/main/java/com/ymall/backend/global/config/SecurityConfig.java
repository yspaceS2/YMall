package com.ymall.backend.global.config;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.security.CustomOAuth2UserService;
import com.ymall.backend.global.security.CustomOidcUserService;
import com.ymall.backend.global.security.JwtAccessDeniedHandler;
import com.ymall.backend.global.security.JwtAuthenticationEntryPoint;
import com.ymall.backend.global.security.JwtAuthenticationFilter;
import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.global.security.MemberPrincipalResolver;
import com.ymall.backend.global.security.OAuth2AuthenticationFailureHandler;
import com.ymall.backend.global.security.OAuth2AuthenticationSuccessHandler;
import com.ymall.backend.global.security.SecurityErrorResponseWriter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final Set<String> ALLOWED_HTTP_METHODS = Set.of(
        "GET",
        "HEAD",
        "POST",
        "PUT",
        "PATCH",
        "DELETE",
        "OPTIONS"
    );

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final CustomOAuth2UserService oAuth2UserService;
    private final CustomOidcUserService oidcUserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2FailureHandler;

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * JWT 기반 무상태 인증을 적용하고 공개 조회, 판매자 운영, 관리자 API의 권한을 분리한다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtTokenProvider jwtTokenProvider,
        MemberPrincipalResolver principalResolver,
        SecurityErrorResponseWriter responseWriter
    ) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oAuth2UserService)
                    .oidcUserService(oidcUserService)
                )
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(request -> !ALLOWED_HTTP_METHODS.contains(request.getMethod()))
                    .denyAll()
                .requestMatchers(
                    POST,
                    "/api/members/signup",
                    "/api/members/signup/email-verifications",
                    "/api/members/signup/email-verifications/confirm",
                    "/api/members/login",
                    "/api/members/tokens/refresh",
                    "/api/members/logout",
                    "/api/members/password-reset-requests",
                    "/api/members/password-reset-verifications",
                    "/api/members/password-resets",
                    "/api/members/oauth2/signup",
                    "/api/members/oauth2/email-verifications",
                    "/api/members/oauth2/email-verifications/confirm",
                    "/api/members/oauth2/google/one-tap",
                    "/api/members/oauth2/google/one-tap/nonces"
                ).permitAll()
                .requestMatchers(GET, "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(GET, "/actuator/prometheus").permitAll()
                .requestMatchers(GET, "/api/members/email-availability").permitAll()
                .requestMatchers(POST, "/api/payments/webhooks/toss").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/ws", "/ws/**").permitAll()
                .requestMatchers(
                    GET,
                    "/api/products/**",
                    "/api/categories/**",
                    "/api/home/**",
                    "/images/**"
                ).permitAll()
                .requestMatchers(POST, "/api/products/*/questions").authenticated()
                .requestMatchers(GET, "/api/admin/dashboard/**")
                    .hasAuthority("DASHBOARD_READ")
                .requestMatchers(GET, "/api/admin/members", "/api/admin/members/*")
                    .hasAuthority("MEMBER_READ")
                .requestMatchers(GET, "/api/admin/members/*/audit-logs")
                    .hasAnyAuthority("AUDIT_OWN_READ", "AUDIT_ALL_READ")
                .requestMatchers(PATCH, "/api/admin/members/*/restriction")
                    .hasAnyAuthority("MEMBER_RESTRICT_LIMITED", "MEMBER_RESTRICT_ALL")
                .requestMatchers(POST, "/api/admin/members/*/sessions/revoke")
                    .hasAnyAuthority("MEMBER_RESTRICT_LIMITED", "MEMBER_RESTRICT_ALL")
                .requestMatchers(PATCH, "/api/admin/members/*/admin-role")
                    .hasRole("ADMIN")
                .requestMatchers(GET, "/api/admin/sellers", "/api/admin/sellers/*")
                    .hasAuthority("SELLER_READ")
                .requestMatchers(GET, "/api/admin/seller-applications/**")
                    .hasAuthority("SELLER_APPLICATION_REVIEW")
                .requestMatchers(PATCH, "/api/admin/seller-applications/*")
                    .hasAuthority("SELLER_APPLICATION_REVIEW")
                .requestMatchers("/api/admin/products/**", "/api/admin/product-change-requests/**")
                    .hasAuthority("PRODUCT_REVIEW")
                .requestMatchers(GET, "/api/admin/categories/**")
                    .hasAuthority("CATEGORY_READ")
                .requestMatchers(POST, "/api/admin/categories")
                    .hasAuthority("CATEGORY_MANAGE_ALL")
                .requestMatchers(PUT, "/api/admin/categories/*")
                    .hasAnyAuthority("CATEGORY_MANAGE_PARTIAL", "CATEGORY_MANAGE_ALL")
                .requestMatchers(DELETE, "/api/admin/categories/*")
                    .hasAuthority("CATEGORY_MANAGE_ALL")
                .requestMatchers(GET, "/api/admin/orders/**")
                    .hasAuthority("REFUND_STANDARD")
                .requestMatchers(POST, "/api/admin/orders/*/refunds")
                    .hasAuthority("REFUND_STANDARD")
                .requestMatchers(GET, "/api/admin/settlement-requests/**")
                    .hasAuthority("SETTLEMENT_REVIEW")
                .requestMatchers(PATCH, "/api/admin/settlement-requests/*/approval")
                    .hasAuthority("SETTLEMENT_APPROVE")
                .requestMatchers(PATCH, "/api/admin/settlement-requests/*/rejection")
                    .hasAuthority("SETTLEMENT_APPROVE")
                .requestMatchers(POST, "/api/admin/settlement-requests/*/mock-payments")
                    .hasAuthority("SETTLEMENT_APPROVE")
                .requestMatchers("/api/admin/support/**").hasAuthority("SUPPORT_REPLY")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/seller/**").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(POST, "/api/files/images").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(POST, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(PUT, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(DELETE, "/api/products/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider, principalResolver, responseWriter),
                UsernamePasswordAuthenticationFilter.class
            )
            .build();
    }
}
