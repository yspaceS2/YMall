export interface LoginRequest {
  email: string
  password: string
}

export interface SignupRequest {
  email: string
  emailVerificationToken: string
  password: string
  passwordConfirmation: string
  name: string
  phone: string
}

export interface MemberResponse {
  memberId: number
  email: string
  name: string
  phone: string
  role: MemberRole
  createdAt: string
}

export interface EmailAvailabilityResponse {
  available: boolean
}

export interface SignupEmailVerificationResponse {
  requestId: string
  expiresIn: number
}

export interface SignupEmailVerificationConfirmResponse {
  verificationToken: string
  expiresIn: number
}

export interface MemberProfile {
  memberId: number
  email: string
  name: string
  phone: string | null
  hasPassword: boolean
  role: MemberRole
  createdAt: string
}

export interface MemberProfileUpdateRequest {
  name: string
  phone: string
}

export interface MemberPasswordChangeRequest {
  currentPassword: string
  newPassword: string
  newPasswordConfirmation: string
}

export interface EmailChangeReauthenticationResponse {
  verificationRequired: boolean
  requestId: string | null
  maskedEmail: string | null
  expiresIn: number
}

export interface EmailChangeVerificationResponse {
  requestId: string
  expiresIn: number
}

export interface PasswordResetRequestResponse {
  requestId: string
}

export interface PasswordResetVerificationResponse {
  resetToken: string
  expiresIn: number
}

export interface PasswordResetConfirmRequest {
  resetToken: string
  newPassword: string
  newPasswordConfirmation: string
}

export interface MemberAddress {
  addressId: number
  addressName: string
  recipientName: string
  recipientPhone: string
  postalCode: string
  roadAddress: string
  detailAddress: string
  isDefault: boolean
}

export type MemberAddressRequest = Omit<MemberAddress, 'addressId'>

export type MemberRole = 'ROLE_USER' | 'ROLE_SELLER' | 'ROLE_ADMIN'

export type OAuthProvider = 'GOOGLE' | 'KAKAO' | 'NAVER'

export interface OAuthAccount {
  provider: OAuthProvider
}

export interface OAuthLinkResponse {
  authorizationUrl: string
}

export interface OAuthSignupRequest {
  email: string
  name: string
  phone: string
}

export interface TokenResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export interface GoogleOneTapNonceResponse {
  clientId: string
  nonce: string
  expiresIn: number
}

export interface GoogleOneTapLoginResponse {
  signupRequired: boolean
  token: TokenResponse | null
}
