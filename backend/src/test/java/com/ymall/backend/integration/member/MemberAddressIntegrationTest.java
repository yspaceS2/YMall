package com.ymall.backend.integration.member;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberAddress;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberAddressRepository;
import com.ymall.backend.member.repository.MemberRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberAddressIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired MemberRepository memberRepository;
    @Autowired MemberAddressRepository addressRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    private Member member;
    private String authorization;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(new Member("address@example.com", "password", "사용자", MemberRole.ROLE_USER));
        authorization = "Bearer " + jwtTokenProvider.createAccessToken(member).accessToken();
    }

    @Test
    void firstAddressBecomesDefaultAndOnlyOneDefaultIsMaintained() throws Exception {
        createAddress("집", false).andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.isDefault").value(true));
        createAddress("회사", true).andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.isDefault").value(true));

        mockMvc.perform(get("/api/members/me/addresses").header(HttpHeaders.AUTHORIZATION, authorization))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].addressName").value("회사"))
            .andExpect(jsonPath("$.data[0].isDefault").value(true))
            .andExpect(jsonPath("$.data[1].isDefault").value(false));
    }

    @Test
    void cannotUpdateOrDeleteAnotherMembersAddress() throws Exception {
        Member other = memberRepository.save(new Member("other@example.com", "password", "다른 사용자", MemberRole.ROLE_USER));
        MemberAddress address = addressRepository.save(new MemberAddress(other, "집", "수령인", "01012345678",
            "00000", "테스트로 123", "101동", true));

        mockMvc.perform(put("/api/members/me/addresses/{addressId}", address.getId())
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON).content(addressJson("변경", false)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("MEMBER_ADDRESS_NOT_FOUND"));
        mockMvc.perform(delete("/api/members/me/addresses/{addressId}", address.getId())
                .header(HttpHeaders.AUTHORIZATION, authorization))
            .andExpect(status().isNotFound());
    }

    @Test
    void deletingDefaultPromotesRemainingAddress() throws Exception {
        MemberAddress first = addressRepository.save(new MemberAddress(member, "집", "수령인", "01012345678",
            "00000", "테스트로 123", "101동", true));
        MemberAddress second = addressRepository.save(new MemberAddress(member, "회사", "수령인", "01012345678",
            "00000", "테스트로 123", "202호", false));

        mockMvc.perform(delete("/api/members/me/addresses/{addressId}", first.getId())
                .header(HttpHeaders.AUTHORIZATION, authorization))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/members/me/addresses").header(HttpHeaders.AUTHORIZATION, authorization))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].addressId").value(second.getId()))
            .andExpect(jsonPath("$.data[0].isDefault").value(true));
    }

    private org.springframework.test.web.servlet.ResultActions createAddress(String name, boolean isDefault)
        throws Exception {
        return mockMvc.perform(post("/api/members/me/addresses")
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content(addressJson(name, isDefault)));
    }

    private String addressJson(String name, boolean isDefault) {
        return """
            {"addressName":"%s","recipientName":"수령인","recipientPhone":"010-1234-5678",
             "postalCode":"00000","roadAddress":"테스트로 123","detailAddress":"101동","isDefault":%s}
            """.formatted(name, isDefault);
    }
}
