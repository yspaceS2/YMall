package com.ymall.backend.seller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SellerSettlementAccountUpsertRequest(
    @NotBlank
    @Pattern(regexp = "\\d{3}", message = "은행 코드는 숫자 3자리여야 합니다.")
    String bankCode,

    @NotBlank
    @Pattern(
        regexp = "^[가-힣A-Za-z][가-힣A-Za-z .'-]{0,48}[가-힣A-Za-z]$",
        message = "예금주명은 한글 또는 영문 2~50자로 입력해 주세요."
    )
    String accountHolder,

    @NotBlank
    @Pattern(regexp = "\\d{8,20}", message = "계좌번호는 숫자 8~20자리로 입력해 주세요.")
    String accountNumber,

    @NotBlank
    @Size(min = 8, max = 100, message = "현재 비밀번호를 확인해 주세요.")
    String currentPassword
) {
}
