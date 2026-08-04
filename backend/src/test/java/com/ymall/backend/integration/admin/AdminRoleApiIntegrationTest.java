package com.ymall.backend.integration.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.admin.entity.AdminAuditAction;
import com.ymall.backend.admin.entity.AdminGrade;
import com.ymall.backend.admin.repository.AdminAuditLogRepository;
import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.global.security.RefreshTokenService;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.seller.entity.SellerApplication;
import com.ymall.backend.seller.repository.SellerApplicationRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminRoleApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AdminAuditLogRepository auditLogRepository;
    @Autowired private SellerApplicationRepository sellerApplicationRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private RefreshTokenService refreshTokenService;

    private Member superAdmin;
    private Member user;
    private Member seller;
    private String superAdminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        superAdmin = saveMember("super-admin@example.test", MemberRole.ROLE_ADMIN);
        user = saveMember("user@example.test", MemberRole.ROLE_USER);
        seller = saveMember("seller@example.test", MemberRole.ROLE_SELLER);
        superAdminToken = token(superAdmin);
        userToken = token(user);
    }

    @Test
    void superAdminPromotesUserToSupervisorAndInvalidatesExistingAccess() throws Exception {
        mockMvc.perform(patch(
                "/api/admin/members/{memberId}/admin-role",
                user.getId()
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(superAdminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "role":"ROLE_ADMIN",
                      "adminGrade":"SUPERVISOR",
                      "reason":"고객센터 운영 책임자 지정"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.memberId").value(user.getId()))
            .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"))
            .andExpect(jsonPath("$.data.adminGrade").value("SUPERVISOR"))
            .andExpect(jsonPath("$.data.permissions").isArray());

        Member updated = memberRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getAuthVersion()).isEqualTo(1L);
        assertThat(auditLogRepository.findAll()).singleElement().satisfies(audit -> {
            assertThat(audit.getActor().getId()).isEqualTo(superAdmin.getId());
            assertThat(audit.getActorGrade()).isEqualTo(AdminGrade.SUPER_ADMIN);
            assertThat(audit.getAction()).isEqualTo(AdminAuditAction.ADMIN_ROLE_CHANGED);
            assertThat(audit.getBeforeValue()).isEqualTo("ROLE_USER");
            assertThat(audit.getAfterValue()).isEqualTo("ROLE_ADMIN:SUPERVISOR");
            assertThat(audit.getReason()).isEqualTo("고객센터 운영 책임자 지정");
        });
        verify(refreshTokenService).revokeAll(user.getId());

        mockMvc.perform(get("/api/notifications")
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    void returnsCurrentAdminGradeAndPermissions() throws Exception {
        mockMvc.perform(get("/api/admin/authorization")
                .header(HttpHeaders.AUTHORIZATION, bearer(superAdminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.memberId").value(superAdmin.getId()))
            .andExpect(jsonPath("$.data.adminGrade").value("SUPER_ADMIN"))
            .andExpect(jsonPath("$.data.permissions").isArray())
            .andExpect(jsonPath("$.data.permissions").isNotEmpty());
    }

    @Test
    void supervisorCanPromoteUserToManager() throws Exception {
        Member supervisor = adminWithGrade("supervisor@example.test", AdminGrade.SUPERVISOR);

        mockMvc.perform(patch(
                "/api/admin/members/{memberId}/admin-role",
                user.getId()
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(token(supervisor)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "role":"ROLE_ADMIN",
                      "adminGrade":"MANAGER",
                      "reason":"운영 담당자 지정"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.adminGrade").value("MANAGER"));
    }

    @Test
    void supervisorCanDemoteManagerToUser() throws Exception {
        Member supervisor = adminWithGrade("supervisor@example.test", AdminGrade.SUPERVISOR);
        Member manager = adminWithGrade("manager@example.test", AdminGrade.MANAGER);

        changeRole(supervisor, manager, MemberRole.ROLE_USER, null)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role").value("ROLE_USER"))
            .andExpect(jsonPath("$.data.adminGrade").doesNotExist());
    }

    @Test
    void superAdminCanChangeManagerAndSupervisorWithinAllowedRange() throws Exception {
        Member manager = adminWithGrade("manager@example.test", AdminGrade.MANAGER);

        changeRole(superAdmin, manager, MemberRole.ROLE_ADMIN, AdminGrade.SUPERVISOR)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.adminGrade").value("SUPERVISOR"));

        Member supervisor = memberRepository.findById(manager.getId()).orElseThrow();
        changeRole(superAdmin, supervisor, MemberRole.ROLE_ADMIN, AdminGrade.MANAGER)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.adminGrade").value("MANAGER"));
    }

    @Test
    void supervisorCannotPromoteUserToSupervisor() throws Exception {
        Member supervisor = adminWithGrade("supervisor@example.test", AdminGrade.SUPERVISOR);

        changeRole(supervisor, user, MemberRole.ROLE_ADMIN, AdminGrade.SUPERVISOR)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("ADMIN_ROLE_CHANGE_FORBIDDEN"));
    }

    @Test
    void managerCannotChangeAdminRoles() throws Exception {
        Member manager = adminWithGrade("manager@example.test", AdminGrade.MANAGER);

        changeRole(manager, user, MemberRole.ROLE_ADMIN, AdminGrade.MANAGER)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("ADMIN_ROLE_CHANGE_FORBIDDEN"));
    }

    @Test
    void managerCanReadOperationsButCannotPerformSupervisorDecisions() throws Exception {
        Member manager = adminWithGrade("manager-permissions@example.test", AdminGrade.MANAGER);
        String authorization = bearer(token(manager));
        SellerApplication application = sellerApplicationRepository.saveAndFlush(
            new SellerApplication(user, "Permission Test Store", "101-20-39999", null)
        );

        mockMvc.perform(get("/api/admin/members")
                .header(HttpHeaders.AUTHORIZATION, authorization))
            .andExpect(status().isOk());

        mockMvc.perform(patch(
                "/api/admin/seller-applications/{applicationId}",
                application.getId()
            )
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status":"APPROVED"}
                    """))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/settlement-requests/{requestId}/approval", 1L)
                .header(HttpHeaders.AUTHORIZATION, authorization))
            .andExpect(status().isForbidden());
    }

    @Test
    void cannotChangeSelfOrPromoteSeller() throws Exception {
        changeRole(superAdmin, superAdmin, MemberRole.ROLE_USER, null)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("ADMIN_ROLE_CHANGE_FORBIDDEN"));

        changeRole(superAdmin, seller, MemberRole.ROLE_ADMIN, AdminGrade.MANAGER)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("ADMIN_ROLE_CHANGE_INVALID"));
    }

    @Test
    void superAdminCannotChangeAnotherSuperAdmin() throws Exception {
        Member anotherSuperAdmin = adminWithGrade(
            "another-super-admin@example.test",
            AdminGrade.SUPER_ADMIN
        );

        changeRole(superAdmin, anotherSuperAdmin, MemberRole.ROLE_USER, null)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("ADMIN_ROLE_CHANGE_FORBIDDEN"));
    }

    @Test
    void roleChangeRequiresReasonAndRejectsSuperAdminAssignment() throws Exception {
        mockMvc.perform(patch(
                "/api/admin/members/{memberId}/admin-role",
                user.getId()
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(superAdminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"role":"ROLE_ADMIN","adminGrade":"MANAGER","reason":" "}
                    """))
            .andExpect(status().isBadRequest());

        changeRole(superAdmin, user, MemberRole.ROLE_ADMIN, AdminGrade.SUPER_ADMIN)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("ADMIN_ROLE_CHANGE_INVALID"));
    }

    private org.springframework.test.web.servlet.ResultActions changeRole(
        Member actor,
        Member target,
        MemberRole role,
        AdminGrade adminGrade
    ) throws Exception {
        String gradeJson = adminGrade == null ? "null" : "\"" + adminGrade.name() + "\"";
        return mockMvc.perform(patch(
                "/api/admin/members/{memberId}/admin-role",
                target.getId()
            )
            .header(HttpHeaders.AUTHORIZATION, bearer(token(actor)))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"role":"%s","adminGrade":%s,"reason":"권한 변경 테스트"}
                """.formatted(role.name(), gradeJson)));
    }

    private Member adminWithGrade(String email, AdminGrade grade) {
        Member admin = new Member(email, "password", email, MemberRole.ROLE_ADMIN);
        if (grade != AdminGrade.SUPER_ADMIN) {
            admin.changeAdminRole(MemberRole.ROLE_ADMIN, grade);
        }
        return memberRepository.saveAndFlush(admin);
    }

    private Member saveMember(String email, MemberRole role) {
        return memberRepository.saveAndFlush(new Member(email, "password", email, role));
    }

    private String token(Member member) {
        return jwtTokenProvider.createAccessToken(member).accessToken();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
