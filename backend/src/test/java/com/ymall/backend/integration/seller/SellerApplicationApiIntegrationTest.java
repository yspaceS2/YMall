package com.ymall.backend.integration.seller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import jakarta.persistence.EntityManager;

import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.admin.entity.AdminGrade;
import com.ymall.backend.global.security.RefreshTokenService;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.seller.entity.SellerApplication;
import com.ymall.backend.seller.repository.SellerApplicationRepository;
import com.ymall.backend.seller.repository.SellerProfileRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SellerApplicationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SellerApplicationRepository sellerApplicationRepository;

    @Autowired
    private SellerProfileRepository sellerProfileRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    private Member applicant;
    private Member admin;
    private Member otherUser;
    private String applicantToken;
    private String adminToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() {
        applicant = saveMember("seller-applicant@example.com", MemberRole.ROLE_USER);
        admin = saveMember("seller-application-admin@example.com", MemberRole.ROLE_ADMIN);
        otherUser = saveMember("seller-application-user@example.com", MemberRole.ROLE_USER);
        applicantToken = token(applicant);
        adminToken = token(admin);
        otherUserToken = token(otherUser);
    }

    @Test
    void memberAppliesAndAdminApprovesSellerApplication() throws Exception {
        apply(applicantToken, "YMall Store", "101-20-30001")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.storeName").value("YMall Store"));

        SellerApplication application = sellerApplicationRepository
            .findByMemberId(applicant.getId())
            .orElseThrow();

        mockMvc.perform(get("/api/admin/seller-applications")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].sellerApplicationId")
                .value(application.getId()));

        mockMvc.perform(patch(
                "/api/admin/seller-applications/{sellerApplicationId}",
                application.getId()
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "status": "APPROVED"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("APPROVED"));

        entityManager.flush();
        entityManager.clear();

        Member promotedMember = memberRepository.findById(applicant.getId()).orElseThrow();
        assertThat(promotedMember.getRole()).isEqualTo(MemberRole.ROLE_SELLER);
        assertThat(sellerProfileRepository.findByMemberId(applicant.getId()))
            .isPresent()
            .get()
            .extracting(profile -> profile.getBusinessNumber())
            .isEqualTo("101-20-30001");
        verify(refreshTokenService).revokeAll(applicant.getId());
    }

    @Test
    void managerRequestsRevisionAndApplicantResubmits() throws Exception {
        apply(applicantToken, "Revision Store", "101-20-30101")
            .andExpect(status().isCreated());
        SellerApplication application = sellerApplicationRepository
            .findByMemberId(applicant.getId())
            .orElseThrow();
        Member manager = saveMember("seller-review-manager@example.com", MemberRole.ROLE_ADMIN);
        manager.changeAdminRole(MemberRole.ROLE_ADMIN, AdminGrade.MANAGER);
        memberRepository.save(manager);

        mockMvc.perform(patch(
                "/api/admin/seller-applications/{applicationId}",
                application.getId()
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(token(manager)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "status": "NEEDS_REVISION",
                        "rejectionReason": "사업자 서류 설명을 보완해 주세요."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("NEEDS_REVISION"));

        apply(applicantToken, "Revised Store", "101-20-30101")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.storeName").value("Revised Store"));
    }

    @Test
    void rejectedApplicationCanBeResubmitted() throws Exception {
        apply(applicantToken, "First Store", "101-20-30002")
            .andExpect(status().isCreated());
        Long applicationId = sellerApplicationRepository.findByMemberId(applicant.getId())
            .orElseThrow()
            .getId();

        mockMvc.perform(patch(
                "/api/admin/seller-applications/{sellerApplicationId}",
                applicationId
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "status": "REJECTED",
                        "rejectionReason": "사업자 정보를 다시 확인해 주세요."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REJECTED"))
            .andExpect(jsonPath("$.data.rejectionReason")
                .value("사업자 정보를 다시 확인해 주세요."));

        apply(applicantToken, "Updated Store", "101-20-30003")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.sellerApplicationId").value(applicationId))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.rejectionReason").doesNotExist());
    }

    @Test
    void regularMemberCannotReviewSellerApplication() throws Exception {
        apply(applicantToken, "Protected Store", "101-20-30004")
            .andExpect(status().isCreated());
        Long applicationId = sellerApplicationRepository.findByMemberId(applicant.getId())
            .orElseThrow()
            .getId();

        mockMvc.perform(patch(
                "/api/admin/seller-applications/{sellerApplicationId}",
                applicationId
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(otherUserToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "status": "APPROVED"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    private org.springframework.test.web.servlet.ResultActions apply(
        String accessToken,
        String storeName,
        String businessNumber
    ) throws Exception {
        return mockMvc.perform(post("/api/members/seller-application")
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "storeName": "%s",
                    "businessNumber": "%s",
                    "description": "판매자 신청 통합 테스트"
                }
                """.formatted(storeName, businessNumber)));
    }

    private Member saveMember(String email, MemberRole role) {
        return memberRepository.save(new Member(
            email,
            "encoded-password",
            "테스트 회원",
            role
        ));
    }

    private String token(Member member) {
        return jwtTokenProvider.createAccessToken(member).accessToken();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
