import { createContext } from 'react'
import type { LoginRequest, MemberRole } from '../types/auth'

export interface AuthContextValue {
    isAuthenticated: boolean
    role: MemberRole | null
    login: (request: LoginRequest) => Promise<void>
    completeOAuthLogin: (accessToken: string) => void
    logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
