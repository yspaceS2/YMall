package com.ymall.backend.member.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.member.dto.EmailAvailabilityResponse;
import com.ymall.backend.member.dto.MemberLoginRequest;
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
