package com.ymall.backend.integration.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.support.dto.SupportMessageCreateRequest;
import com.ymall.backend.support.entity.SupportInquiry;
import com.ymall.backend.support.repository.SupportInquiryRepository;
import com.ymall.backend.support.repository.SupportMessageRepository;
import com.ymall.backend.support.service.SupportService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SupportApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SupportInquiryRepository inquiryRepository;

    @Autowired
    private SupportMessageRepository messageRepository;

    @Autowired
    private SupportService supportService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Member customer;
    private Member otherCustomer;
    private Member admin;
    private String customerToken;
    private String otherCustomerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        customer = memberRepository.save(new Member(
            "support-customer@example.test", "encoded", "문의 고객", MemberRole.ROLE_USER
        ));
        otherCustomer = memberRepository.save(new Member(
            "other-customer@example.test", "encoded", "다른 고객", MemberRole.ROLE_USER
        ));
        admin = memberRepository.save(new Member(
            "support-admin@example.test", "encoded", "상담 관리자", MemberRole.ROLE_ADMIN
        ));
        customerToken = jwtTokenProvider.createAccessToken(customer).accessToken();
        otherCustomerToken = jwtTokenProvider.createAccessToken(otherCustomer).accessToken();
        adminToken = jwtTokenProvider.createAccessToken(admin).accessToken();
    }

    @Test
    void inquiryOwnershipLiveTransitionIdempotencyAndCloseAreEnforced() throws Exception {
        mockMvc.perform(post("/api/support/inquiries")
                .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "category": "ORDER",
                      "title": "주문 상태를 확인해 주세요",
                      "content": "배송 상태가 바뀌지 않습니다."
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.inquiry.status").value("WAITING"))
            .andExpect(jsonPath("$.data.messages[0].type").value("INQUIRY"));

        SupportInquiry inquiry = inquiryRepository.findAll().get(0);

        mockMvc.perform(get("/api/support/inquiries/{inquiryId}", inquiry.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(otherCustomerToken)))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/support/inquiries/{inquiryId}/live-requests", inquiry.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.inquiry.status").value("LIVE_REQUESTED"))
            .andExpect(jsonPath("$.data.chatSession.status").value("WAITING"));

        mockMvc.perform(post("/api/support/inquiries/{inquiryId}/messages", inquiry.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"clientMessageId":"%s","content":"상담 대기 중 추가 메시지"}
                    """.formatted(UUID.randomUUID())))
            .andExpect(status().isConflict());

        mockMvc.perform(post(
                "/api/support/inquiries/{inquiryId}/live-requests/cancel",
                inquiry.getId()
            ).header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.inquiry.status").value("WAITING"))
            .andExpect(jsonPath("$.data.chatSession.status").value("REJECTED"));

        mockMvc.perform(post("/api/support/inquiries/{inquiryId}/live-requests", inquiry.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.inquiry.status").value("LIVE_REQUESTED"));

        mockMvc.perform(post(
                "/api/admin/support/inquiries/{inquiryId}/live-requests/accept",
                inquiry.getId()
            ).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.inquiry.status").value("LIVE_ACTIVE"))
            .andExpect(jsonPath("$.data.chatSession.status").value("ACTIVE"));

        mockMvc.perform(get("/api/admin/support/inquiries")
                .param("keyword", admin.getName())
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1));

        UUID clientMessageId = UUID.randomUUID();
        MemberPrincipal customerPrincipal = new MemberPrincipal(
            customer.getId(), customer.getEmail(), customer.getRole()
        );
        long before = messageRepository.countByInquiryId(inquiry.getId());
        supportService.addMessage(
            customerPrincipal,
            inquiry.getId(),
            new SupportMessageCreateRequest(clientMessageId, "실시간 상담 메시지"),
            true
        );
        supportService.addMessage(
            customerPrincipal,
            inquiry.getId(),
            new SupportMessageCreateRequest(clientMessageId, "중복 전송"),
            true
        );
        assertThat(messageRepository.countByInquiryId(inquiry.getId())).isEqualTo(before + 1);

        mockMvc.perform(post(
                "/api/admin/support/inquiries/{inquiryId}/live-requests/end",
                inquiry.getId()
            ).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.chatSession.status").value("ENDED"));

        mockMvc.perform(post("/api/admin/support/inquiries/{inquiryId}/close", inquiry.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"배송 상태 동기화를 완료했습니다.\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.inquiry.status").value("CLOSED"))
            .andExpect(jsonPath("$.data.messages[-1:].type").value("RESOLUTION"));

        mockMvc.perform(post("/api/support/inquiries/{inquiryId}/messages", inquiry.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"clientMessageId":"%s","content":"종료 후 메시지"}
                    """.formatted(UUID.randomUUID())))
            .andExpect(status().isConflict());

        mockMvc.perform(get("/api/support/inquiries/{inquiryId}", inquiry.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.inquiry.status").value("CLOSED"))
            .andExpect(jsonPath("$.data.messages[?(@.type == 'RESOLUTION')]").isEmpty());

        mockMvc.perform(get("/api/admin/support/inquiries/{inquiryId}", inquiry.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.messages[?(@.type == 'RESOLUTION')]").isNotEmpty());

        mockMvc.perform(post("/api/support/inquiries/{inquiryId}/reopen", inquiry.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
            .andExpect(status().isNotFound());
    }

    @Test
    void adminLiveOfferCanBeRejectedOrAcceptedOnlyByInquiryOwner() throws Exception {
        mockMvc.perform(post("/api/support/inquiries")
                .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "category": "SERVICE",
                      "title": "실시간 상담을 제안해 주세요",
                      "content": "상담 가능 여부를 확인하고 싶습니다."
                    }
                    """))
            .andExpect(status().isCreated());

        SupportInquiry inquiry = inquiryRepository.findAll().get(0);

        mockMvc.perform(post(
                "/api/admin/support/inquiries/{inquiryId}/live-offers",
                inquiry.getId()
            ).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.inquiry.status").value("LIVE_OFFERED"))
            .andExpect(jsonPath("$.data.chatSession.status").value("WAITING"))
            .andExpect(jsonPath("$.data.chatSession.initiatedBy").value("ADMIN_OFFER"));

        mockMvc.perform(post(
                "/api/support/inquiries/{inquiryId}/live-requests/accept",
                inquiry.getId()
            ).header(HttpHeaders.AUTHORIZATION, bearer(otherCustomerToken)))
            .andExpect(status().isNotFound());

        mockMvc.perform(post(
                "/api/support/inquiries/{inquiryId}/live-requests/reject",
                inquiry.getId()
            ).header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.inquiry.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.data.chatSession.status").value("REJECTED"));

        mockMvc.perform(post(
                "/api/admin/support/inquiries/{inquiryId}/live-offers",
                inquiry.getId()
            ).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk());

        mockMvc.perform(post(
                "/api/support/inquiries/{inquiryId}/live-requests/accept",
                inquiry.getId()
            ).header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.inquiry.status").value("LIVE_ACTIVE"))
            .andExpect(jsonPath("$.data.chatSession.status").value("ACTIVE"));
    }

    @Test
    void attachmentIsStoredAndOnlyInquiryParticipantsCanDownloadIt() throws Exception {
        mockMvc.perform(post("/api/support/inquiries")
                .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "category": "ORDER",
                      "title": "첨부파일 확인 요청",
                      "content": "스크린샷을 첨부하겠습니다."
                    }
                    """))
            .andExpect(status().isCreated());

        SupportInquiry inquiry = inquiryRepository.findAll().get(0);
        MockMultipartFile screenshot = new MockMultipartFile(
            "files",
            "order-screen.png",
            MediaType.IMAGE_PNG_VALUE,
            new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x00
            }
        );

        MvcResult uploadResult = mockMvc.perform(multipart(
                "/api/support/inquiries/{inquiryId}/messages",
                inquiry.getId()
            )
                .file(screenshot)
                .param("clientMessageId", UUID.randomUUID().toString())
                .param("content", "주문 화면입니다.")
                .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.attachments[0].fileName").value("order-screen.png"))
            .andExpect(jsonPath("$.data.attachments[0].contentType").value(MediaType.IMAGE_PNG_VALUE))
            .andReturn();

        String response = uploadResult.getResponse().getContentAsString();
        String attachmentPath = com.jayway.jsonpath.JsonPath.read(
            response,
            "$.data.attachments[0].downloadUrl"
        );

        mockMvc.perform(get(attachmentPath)
                .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
            .andExpect(status().isOk())
            .andExpect(result -> assertThat(result.getResponse().getContentType())
                .isEqualTo(MediaType.IMAGE_PNG_VALUE));

        mockMvc.perform(get(attachmentPath)
                .header(HttpHeaders.AUTHORIZATION, bearer(otherCustomerToken)))
            .andExpect(status().isNotFound());

        mockMvc.perform(get(attachmentPath)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
