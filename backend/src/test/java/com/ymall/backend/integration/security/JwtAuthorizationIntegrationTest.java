package com.ymall.backend.integration.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(JwtAuthorizationIntegrationTest.TestController.class)
class JwtAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String userToken;
    private String sellerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        userToken = tokenFor("user@example.com", MemberRole.ROLE_USER);
        sellerToken = tokenFor("seller@example.com", MemberRole.ROLE_SELLER);
        adminToken = tokenFor("admin@example.com", MemberRole.ROLE_ADMIN);
    }

    @Test
    void authenticatedUserCanAccessProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/security-test/user")
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
            .andExpect(status().isOk());
    }

    @Test
    void missingTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/security-test/user"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    void invalidTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/security-test/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    void sellerEndpointAllowsSellerAndAdminOnly() throws Exception {
        mockMvc.perform(get("/api/seller/security-test")
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/seller/security-test")
                .header(HttpHeaders.AUTHORIZATION, bearer(sellerToken)))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/seller/security-test")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk());
    }

    @Test
    void adminEndpointAllowsAdminOnly() throws Exception {
        mockMvc.perform(get("/api/admin/security-test")
                .header(HttpHeaders.AUTHORIZATION, bearer(sellerToken)))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/security-test")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk());
    }

    private String tokenFor(String email, MemberRole role) {
        Member member = memberRepository.save(new Member(email, "password", email, role));
        return jwtTokenProvider.createAccessToken(member).accessToken();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @RestController
    static class TestController {

        @GetMapping("/api/security-test/user")
        String user() {
            return "ok";
        }

        @GetMapping("/api/seller/security-test")
        String seller() {
            return "ok";
        }

        @GetMapping("/api/admin/security-test")
        String admin() {
            return "ok";
        }
    }
}
