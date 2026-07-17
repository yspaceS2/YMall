export interface LoginRequest {
  email: string
  password: string
}

export type MemberRole = 'ROLE_USER' | 'ROLE_SELLER' | 'ROLE_ADMIN'

export interface TokenResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}
