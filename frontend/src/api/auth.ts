import type { EmailAvailabilityResponse, EmailChangeReauthenticationResponse, EmailChangeVerificationResponse, GoogleOneTapLoginResponse, GoogleOneTapNonceResponse, LoginRequest, MemberAddress, MemberAddressRequest, MemberPasswordChangeRequest, MemberProfile, MemberProfileUpdateRequest, MemberResponse, OAuthAccount, OAuthLinkResponse, OAuthProvider, OAuthSignupRequest, PasswordResetConfirmRequest, PasswordResetRequestResponse, PasswordResetVerificationResponse, SignupEmailVerificationConfirmResponse, SignupEmailVerificationResponse, SignupRequest, TokenResponse } from '../types/auth'
import { apiRequest } from './client'

export function loginMember(request: LoginRequest) {
    return apiRequest<TokenResponse>('/members/login', {
        method: 'POST',
        body: request,
        auth: false,
    })
}

export function logoutMember() {
    return apiRequest<void>('/members/logout', { method: 'POST', auth: false })
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

export function requestSignupEmailVerification(email: string) {
    return apiRequest<SignupEmailVerificationResponse>('/members/signup/email-verifications', {
        method: 'POST',
        body: { email },
        auth: false,
    })
}

export function confirmSignupEmailVerification(requestId: string, email: string, code: string) {
    return apiRequest<SignupEmailVerificationConfirmResponse>(
        '/members/signup/email-verifications/confirm',
        {
            method: 'POST',
            body: { requestId, email, code },
            auth: false,
        },
    )
}

export function startEmailChangeReauthentication(currentPassword?: string) {
    return apiRequest<EmailChangeReauthenticationResponse>('/members/me/email-change/reauthentications', {
        method: 'POST',
        body: { currentPassword },
    })
}

export function confirmEmailChangeReauthentication(requestId: string, code: string) {
    return apiRequest<void>('/members/me/email-change/reauthentications/confirm', {
        method: 'POST',
        body: { requestId, code },
    })
}

export function requestNewEmailVerification(email: string) {
    return apiRequest<EmailChangeVerificationResponse>('/members/me/email-change/verifications', {
        method: 'POST',
        body: { email },
    })
}

export function changeMemberEmail(requestId: string, email: string, code: string) {
    return apiRequest<void>('/members/me/email-change', {
        method: 'PATCH',
        body: { requestId, email, code },
    })
}

export function requestPasswordReset(email: string) {
    return apiRequest<PasswordResetRequestResponse>('/members/password-reset-requests', {
        method: 'POST',
        body: { email },
        auth: false,
    })
}

export function verifyPasswordReset(requestId: string, code: string) {
    return apiRequest<PasswordResetVerificationResponse>('/members/password-reset-verifications', {
        method: 'POST',
        body: { requestId, code },
        auth: false,
    })
}

export function confirmPasswordReset(request: PasswordResetConfirmRequest) {
    return apiRequest<void>('/members/password-resets', {
        method: 'POST',
        body: request,
        auth: false,
    })
}

export function getOAuthAccounts(signal?: AbortSignal) {
    return apiRequest<OAuthAccount[]>('/members/me/oauth-accounts', { signal })
}

export function startOAuthAccountLink(provider: OAuthProvider) {
    return apiRequest<OAuthLinkResponse>(`/members/me/oauth-accounts/${provider.toLowerCase()}/links`, {
        method: 'POST',
    })
}

export function completeOAuthSignup(request: OAuthSignupRequest) {
    return apiRequest<TokenResponse>('/members/oauth2/signup', {
        method: 'POST',
        body: request,
        auth: false,
    })
}

export function requestOAuthEmailVerification(email: string) {
    return apiRequest<void>('/members/oauth2/email-verifications', {
        method: 'POST', body: { email }, auth: false,
    })
}

export function confirmOAuthEmailVerification(email: string, code: string) {
    return apiRequest<void>('/members/oauth2/email-verifications/confirm', {
        method: 'POST', body: { email, code }, auth: false,
    })
}

export function getOAuthAuthorizationUrl(provider: OAuthProvider) {
    const baseUrl = (import.meta.env.VITE_OAUTH2_BASE_URL ?? '').replace(/\/$/, '')
    return `${baseUrl}/oauth2/authorization/${provider.toLowerCase()}`
}

export function requestGoogleOneTapNonce() {
    return apiRequest<GoogleOneTapNonceResponse>('/members/oauth2/google/one-tap/nonces', {
        method: 'POST',
        auth: false,
    })
}

export function loginWithGoogleOneTap(credential: string) {
    return apiRequest<GoogleOneTapLoginResponse>('/members/oauth2/google/one-tap', {
        method: 'POST',
        body: { credential },
        auth: false,
    })
}

export function getMemberAddresses(signal?: AbortSignal) {
    return apiRequest<MemberAddress[]>('/members/me/addresses', { signal })
}

export function createMemberAddress(request: MemberAddressRequest) {
    return apiRequest<MemberAddress>('/members/me/addresses', { method: 'POST', body: request })
}

export function updateMemberAddress(addressId: number, request: MemberAddressRequest) {
    return apiRequest<MemberAddress>(`/members/me/addresses/${addressId}`, { method: 'PUT', body: request })
}

export function deleteMemberAddress(addressId: number) {
    return apiRequest<void>(`/members/me/addresses/${addressId}`, { method: 'DELETE' })
}
