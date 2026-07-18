package com.ymall.backend.member.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.member.dto.EmailAvailabilityResponse;
import com.ymall.backend.member.dto.MemberLoginRequest;
import com.ymall.backend.member.dto.MemberPasswordChangeRequest;
import com.ymall.backend.member.dto.MemberProfileResponse;
import com.ymall.backend.member.dto.MemberProfileUpdateRequest;
import com.ymall.backend.member.dto.MemberResponse;
import com.ymall.backend.member.dto.MemberSignupRequest;
import com.ymall.backend.member.dto.TokenResponse;
import com.ymall.backend.member.service.MemberService;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ApiResponse<MemberProfileResponse> getProfile(
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return ApiResponse.success(memberService.getProfile(principal.memberId()), "회원 정보를 조회했습니다.");
    }

    @PutMapping("/me")
    public ApiResponse<MemberProfileResponse> updateProfile(
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @RequestBody MemberProfileUpdateRequest request
    ) {
        return ApiResponse.success(
            memberService.updateProfile(principal.memberId(), request),
            "회원 정보가 수정되었습니다."
        );
    }

    @PatchMapping("/me/password")
    public ApiResponse<Void> changePassword(
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @RequestBody MemberPasswordChangeRequest request
    ) {
        memberService.changePassword(principal.memberId(), request);
        return ApiResponse.success(null, "비밀번호가 변경되었습니다.");
    }

    @GetMapping("/email-availability")
    public ApiResponse<EmailAvailabilityResponse> checkEmailAvailability(
        @RequestParam @NotBlank @Email String email
    ) {
        return ApiResponse.success(
            memberService.checkEmailAvailability(email),
            "이메일 중복 확인이 완료되었습니다."
        );
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<MemberResponse>> signup(
        @Valid @RequestBody MemberSignupRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(memberService.signup(request), "회원가입이 완료되었습니다."));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody MemberLoginRequest request) {
        return ApiResponse.success(memberService.login(request), "로그인에 성공했습니다.");
    }
}
