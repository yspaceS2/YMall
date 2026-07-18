import type { EmailAvailabilityResponse, LoginRequest, MemberPasswordChangeRequest, MemberProfile, MemberProfileUpdateRequest, MemberResponse, SignupRequest, TokenResponse } from '../types/auth'
import { apiRequest } from './client'

export function loginMember(request: LoginRequest) {
    return apiRequest<TokenResponse>('/members/login', {
        method: 'POST',
        body: request,
        auth: false,
    })
}

export function signupMember(request: SignupRequest) {
    return apiRequest<MemberResponse>('/members/signup', {
        method: 'POST',
        body: request,
        auth: false,
    })
}

export function checkEmailAvailability(email: string) {
    const query = new URLSearchParams({ email })
    return apiRequest<EmailAvailabilityResponse>(`/members/email-availability?${query}`, {
        auth: false,
    })
}

export function getMemberProfile(signal?: AbortSignal) {
    return apiRequest<MemberProfile>('/members/me', { signal })
}

export function updateMemberProfile(request: MemberProfileUpdateRequest) {
    return apiRequest<MemberProfile>('/members/me', {
        method: 'PUT',
        body: request,
    })
}

export function changeMemberPassword(request: MemberPasswordChangeRequest) {
    return apiRequest<void>('/members/me/password', {
        method: 'PATCH',
        body: request,
    })
}
