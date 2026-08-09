import { createContext } from 'react'
import type { LoginRequest, MemberRole } from '../types/auth'

export interface AuthContextValue {
    isAuthenticated: boolean
    isLoggingOut?: boolean
    role: MemberRole | null
    login: (request: LoginRequest) => Promise<MemberRole | null>
    completeOAuthLogin: (accessToken: string) => MemberRole | null
    logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
