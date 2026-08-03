import type { PageResponse } from '../types/api'
import type {
    SupportInquiryCreateRequest,
    SupportInquiryDetail,
    SupportInquiryStatus,
    SupportInquirySummary,
    SupportMessage,
} from '../types/support'
import { apiRequest } from './client'

export function getSupportInquiries(
    admin: boolean,
    page = 1,
    status?: SupportInquiryStatus,
    keyword = '',
    signal?: AbortSignal,
) {
    const parameters = new URLSearchParams({ page: String(page), size: '30' })
    if (status) parameters.set('status', status)
    if (keyword.trim()) parameters.set('keyword', keyword.trim())
    return apiRequest<PageResponse<SupportInquirySummary>>(
        `${admin ? '/admin' : ''}/support/inquiries?${parameters}`,
        { signal },
    )
}

export function getSupportInquiry(admin: boolean, inquiryId: number, signal?: AbortSignal) {
    return apiRequest<SupportInquiryDetail>(
        `${admin ? '/admin' : ''}/support/inquiries/${inquiryId}`,
        { signal },
    )
}

export function createSupportInquiry(request: SupportInquiryCreateRequest) {
    return apiRequest<SupportInquiryDetail>('/support/inquiries', {
        method: 'POST',
        body: request,
    })
}

export function addSupportMessage(
    admin: boolean,
    inquiryId: number,
    content: string,
    files: File[] = [],
) {
    if (files.length > 0) {
        const formData = new FormData()
        formData.set('clientMessageId', crypto.randomUUID())
        formData.set('content', content)
        files.forEach((file) => formData.append('files', file))
        return apiRequest<SupportMessage>(
            `${admin ? '/admin' : ''}/support/inquiries/${inquiryId}/messages`,
            { method: 'POST', body: formData },
        )
    }
    return apiRequest<SupportMessage>(
        `${admin ? '/admin' : ''}/support/inquiries/${inquiryId}/messages`,
        {
            method: 'POST',
            body: { content, clientMessageId: crypto.randomUUID() },
        },
    )
}

export function requestLiveSupport(inquiryId: number) {
    return supportAction(false, inquiryId, 'live-requests')
}

export function offerLiveSupport(inquiryId: number) {
    return supportAction(true, inquiryId, 'live-offers')
}

export function acceptLiveSupport(admin: boolean, inquiryId: number) {
    return supportAction(admin, inquiryId, 'live-requests/accept')
}

export function rejectLiveSupport(admin: boolean, inquiryId: number) {
    return supportAction(admin, inquiryId, 'live-requests/reject')
}

export function cancelLiveSupport(admin: boolean, inquiryId: number) {
    return supportAction(admin, inquiryId, 'live-requests/cancel')
}

export function endLiveSupport(admin: boolean, inquiryId: number) {
    return supportAction(admin, inquiryId, 'live-requests/end')
}

export function closeSupportInquiry(inquiryId: number, content: string) {
    return apiRequest<SupportInquiryDetail>(`/admin/support/inquiries/${inquiryId}/close`, {
        method: 'POST',
        body: { content },
    })
}

export function getPendingSupportCount(signal?: AbortSignal) {
    return apiRequest<{ count: number }>('/admin/support/inquiries/pending-count', { signal })
}

function supportAction(admin: boolean, inquiryId: number, action: string) {
    return apiRequest<SupportInquiryDetail>(
        `${admin ? '/admin' : ''}/support/inquiries/${inquiryId}/${action}`,
        { method: 'POST' },
    )
}
