package com.ymall.backend.member.dto;

import com.ymall.backend.member.entity.OAuthProvider;

public record OAuthAccountResponse(OAuthProvider provider) {
}
