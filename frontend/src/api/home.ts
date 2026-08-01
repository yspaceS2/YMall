import type { HomeMerchandising } from '../types/home'
import { apiRequest } from './client'

export function getHomeMerchandising(signal?: AbortSignal) {
    return apiRequest<HomeMerchandising>('/home/merchandising', {
        auth: false,
        signal,
    })
}
