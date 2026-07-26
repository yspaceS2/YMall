package com.ymall.backend.member.event;

public record MemberEmailChangedEvent(
    Long memberId,
    String previousEmail,
    String newEmail
) {
}
