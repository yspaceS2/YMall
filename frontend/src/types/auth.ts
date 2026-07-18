export interface LoginRequest {
  email: string
  password: string
}

export interface SignupRequest {
  email: string
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

export type MemberRole = 'ROLE_USER' | 'ROLE_SELLER' | 'ROLE_ADMIN'

export interface TokenResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}
