package com.ymall.backend.integration.seller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.entity.SellerSettlementAccount;
import com.ymall.backend.seller.repository.SellerProfileRepository;
import com.ymall.backend.seller.repository.SellerSettlementAccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SellerSettlementAccountApiIntegrationTest {

    private static final String CURRENT_PASSWORD = "Test-" + UUID.randomUUID();
    private static final String WRONG_PASSWORD = "Wrong-" + UUID.randomUUID();
    private static final String FIRST_ACCOUNT_NUMBER = "000000000001";
    private static final String SECOND_ACCOUNT_NUMBER = "000000000002";
    private static final String ENCRYPTION_KEY = createEncryptionKey();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SellerProfileRepository sellerProfileRepository;

    @Autowired
    private SellerSettlementAccountRepository settlementAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Member seller;
    private Member otherSeller;
    private Member buyer;
    private SellerProfile sellerProfile;

    @DynamicPropertySource
    static void settlementAccountProperties(DynamicPropertyRegistry registry) {
        registry.add(
            "ymall.settlement-account.encryption-key",
            () -> ENCRYPTION_KEY
        );
    }

    @BeforeEach
    void setUp() {
        seller = saveMember("settlement-seller@example.com", MemberRole.ROLE_SELLER);
        otherSeller = saveMember("other-settlement-seller@example.com", MemberRole.ROLE_SELLER);
        buyer = saveMember("settlement-buyer@example.com", MemberRole.ROLE_USER);
        sellerProfile = sellerProfileRepository.save(new SellerProfile(
            seller,
            "정산 테스트 상점",
            "111-22-33333",
            "정산 계좌 테스트용 판매자"
        ));
        sellerProfileRepository.save(new SellerProfile(
            otherSeller,
            "다른 정산 테스트 상점",
            "444-55-66666",
            "다른 판매자"
        ));
    }

    @Test
    void sellerRegistersAndReadsOnlyMaskedSettlementAccount() throws Exception {
        mockMvc.perform(put("/api/seller/settlement-account")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(seller)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(accountJson("004", "테스트판매자", FIRST_ACCOUNT_NUMBER, CURRENT_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.bankCode").value("004"))
            .andExpect(jsonPath("$.data.bankName").value("KB국민은행"))
            .andExpect(jsonPath("$.data.accountHolder").value("테스트판매자"))
            .andExpect(jsonPath("$.data.maskedAccountNumber").value("****0001"))
            .andExpect(jsonPath("$.data.verificationStatus").value("UNVERIFIED"))
            .andExpect(jsonPath("$.data.accountNumber").doesNotExist());

        SellerSettlementAccount saved = settlementAccountRepository
            .findBySellerProfileId(sellerProfile.getId())
            .orElseThrow();
        assertThat(saved.getAccountNumberCiphertext())
            .doesNotContain(FIRST_ACCOUNT_NUMBER)
            .startsWith("v1:");
        assertThat(saved.getAccountHolderCiphertext())
            .doesNotContain("테스트판매자")
            .startsWith("v1:");

        mockMvc.perform(get("/api/seller/settlement-account")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(seller))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.maskedAccountNumber").value("****0001"))
            .andExpect(jsonPath("$.data.accountNumber").doesNotExist());
    }

    @Test
    void sellerUpdatesOwnSettlementAccountWithoutCreatingDuplicate() throws Exception {
        String sellerToken = token(seller);
        mockMvc.perform(put("/api/seller/settlement-account")
                .header(HttpHeaders.AUTHORIZATION, bearer(sellerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(accountJson("004", "테스트판매자", FIRST_ACCOUNT_NUMBER, CURRENT_PASSWORD)))
            .andExpect(status().isOk());

        Long originalId = settlementAccountRepository
            .findBySellerProfileId(sellerProfile.getId())
            .orElseThrow()
            .getId();

        mockMvc.perform(put("/api/seller/settlement-account")
                .header(HttpHeaders.AUTHORIZATION, bearer(sellerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(accountJson("088", "변경판매자", SECOND_ACCOUNT_NUMBER, CURRENT_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.settlementAccountId").value(originalId))
            .andExpect(jsonPath("$.data.bankName").value("신한은행"))
            .andExpect(jsonPath("$.data.maskedAccountNumber").value("****0002"));

        assertThat(settlementAccountRepository.count()).isEqualTo(1);
    }

    @Test
    void wrongPasswordAndInvalidAccountInputAreRejected() throws Exception {
        String sellerToken = token(seller);
        mockMvc.perform(put("/api/seller/settlement-account")
                .header(HttpHeaders.AUTHORIZATION, bearer(sellerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(accountJson("004", "테스트판매자", FIRST_ACCOUNT_NUMBER, WRONG_PASSWORD)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("CURRENT_PASSWORD_MISMATCH"));

        mockMvc.perform(put("/api/seller/settlement-account")
                .header(HttpHeaders.AUTHORIZATION, bearer(sellerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(accountJson("999", "테스트판매자", "invalid-number", CURRENT_PASSWORD)))
            .andExpect(status().isBadRequest());

        assertThat(settlementAccountRepository.count()).isZero();
    }

    @Test
    void sellerCannotReadAnotherSellersAccountAndBuyerCannotAccessSellerApi()
        throws Exception {
        mockMvc.perform(put("/api/seller/settlement-account")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(seller)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(accountJson("004", "테스트판매자", FIRST_ACCOUNT_NUMBER, CURRENT_PASSWORD)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/seller/settlement-account")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(otherSeller))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code")
                .value("SELLER_SETTLEMENT_ACCOUNT_NOT_FOUND"));

        mockMvc.perform(get("/api/seller/settlement-account")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(buyer))))
            .andExpect(status().isForbidden());
    }

    private Member saveMember(String email, MemberRole role) {
        return memberRepository.save(new Member(
            email,
            passwordEncoder.encode(CURRENT_PASSWORD),
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

    private String accountJson(
        String bankCode,
        String accountHolder,
        String accountNumber,
        String currentPassword
    ) {
        return """
            {
                "bankCode": "%s",
                "accountHolder": "%s",
                "accountNumber": "%s",
                "currentPassword": "%s"
            }
            """.formatted(bankCode, accountHolder, accountNumber, currentPassword);
    }

    private static String createEncryptionKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
