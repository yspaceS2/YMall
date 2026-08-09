package com.ymall.backend.integration.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.admin.entity.AdminGrade;
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
    private String managerToken;
    private String supervisorToken;

    @BeforeEach
    void setUp() {
        userToken = tokenFor("user@example.com", MemberRole.ROLE_USER);
        sellerToken = tokenFor("seller@example.com", MemberRole.ROLE_SELLER);
        adminToken = tokenFor("admin@example.com", MemberRole.ROLE_ADMIN);
        managerToken = tokenForAdmin("manager@example.com", AdminGrade.MANAGER);
        supervisorToken = tokenForAdmin("supervisor@example.com", AdminGrade.SUPERVISOR);
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

    @Test
    void managerCanReadDashboardButCannotManageCategoriesOrApproveSettlements() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/security-test")
                .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
            .andExpect(status().isOk());

        mockMvc.perform(put("/api/admin/categories/security-test")
                .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));

        mockMvc.perform(patch("/api/admin/settlement-requests/security-test/approval")
                .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void supervisorCanPartiallyManageCategoriesAndApproveSettlementsButCannotDeleteCategories()
        throws Exception {
        mockMvc.perform(put("/api/admin/categories/security-test")
                .header(HttpHeaders.AUTHORIZATION, bearer(supervisorToken)))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/settlement-requests/security-test/approval")
                .header(HttpHeaders.AUTHORIZATION, bearer(supervisorToken)))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/admin/categories/security-test")
                .header(HttpHeaders.AUTHORIZATION, bearer(supervisorToken)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void superAdminCanUseCategoryDeletionPermission() throws Exception {
        mockMvc.perform(delete("/api/admin/categories/security-test")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk());
    }

    private String tokenFor(String email, MemberRole role) {
        Member member = memberRepository.save(new Member(email, "password", email, role));
        return jwtTokenProvider.createAccessToken(member).accessToken();
    }

    private String tokenForAdmin(String email, AdminGrade grade) {
        Member member = new Member(email, "password", email, MemberRole.ROLE_ADMIN);
        member.changeAdminRole(MemberRole.ROLE_ADMIN, grade);
        return jwtTokenProvider.createAccessToken(memberRepository.save(member)).accessToken();
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

        @GetMapping("/api/admin/dashboard/security-test")
        String dashboard() {
            return "ok";
        }

        @PutMapping("/api/admin/categories/security-test")
        String updateCategory() {
            return "ok";
        }

        @DeleteMapping("/api/admin/categories/security-test")
        String deleteCategory() {
            return "ok";
        }

        @PatchMapping("/api/admin/settlement-requests/security-test/approval")
        String approveSettlement() {
            return "ok";
        }
    }
}
