import type { LoginRequest, TokenResponse } from '../types/auth'
import { apiRequest } from './client'

export function loginMember(request: LoginRequest) {
    return apiRequest<TokenResponse>('/members/login', {
        method: 'POST',
        body: request,
        auth: false,
    })
}
