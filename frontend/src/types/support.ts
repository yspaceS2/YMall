import type { MemberRole } from './auth'

export type SupportRequesterType = 'CUSTOMER' | 'SELLER'
export type SupportInquiryStatus =
    | 'WAITING'
    | 'IN_PROGRESS'
    | 'ANSWERED'
    | 'LIVE_REQUESTED'
    | 'LIVE_OFFERED'
    | 'LIVE_ACTIVE'
    | 'CLOSED'
export type SupportInquiryCategory =
    | 'ORDER'
    | 'PAYMENT'
    | 'CANCEL_REFUND'
    | 'DELIVERY'
    | 'ACCOUNT'
    | 'PRODUCT_APPROVAL'
    | 'SETTLEMENT'
    | 'SELLER_PERMISSION'
    | 'POLICY'
    | 'SERVICE'
export type SupportMessageType = 'INQUIRY' | 'REPLY' | 'LIVE_CHAT' | 'SYSTEM' | 'RESOLUTION'
export type SupportChatStatus = 'WAITING' | 'ACTIVE' | 'ENDED' | 'EXPIRED' | 'REJECTED'

export interface SupportInquirySummary {
    inquiryId: number
    requesterType: SupportRequesterType
    requesterName: string
    category: SupportInquiryCategory
    title: string
    status: SupportInquiryStatus
    assignedAdminName: string | null
    createdAt: string
    updatedAt: string
    closedAt: string | null
}

export interface SupportMessage {
    messageId: number
    authorId: number
    authorName: string
    authorRole: MemberRole
    type: SupportMessageType
    content: string
    attachments: SupportAttachment[]
    clientMessageId: string | null
    readAt: string | null
    createdAt: string
}

export interface SupportAttachment {
    attachmentId: number
    fileName: string
    contentType: string
    fileSize: number
    downloadUrl: string
}

export interface SupportChatSession {
    sessionId: number
    adminId: number | null
    adminName: string | null
    initiatedBy: 'USER_REQUEST' | 'ADMIN_OFFER'
    status: SupportChatStatus
    startedAt: string | null
    endedAt: string | null
    expiresAt: string | null
}

export interface SupportInquiryDetail {
    inquiry: SupportInquirySummary
    relatedOrderId: number | null
    relatedProductId: number | null
    relatedSettlementId: number | null
    chatSession: SupportChatSession | null
    messages: SupportMessage[]
}

export interface SupportInquiryCreateRequest {
    category: SupportInquiryCategory
    title: string
    content: string
    relatedOrderId?: number
    relatedProductId?: number
    relatedSettlementId?: number
}

export interface RealtimeEvent {
    type: string
    resource: string
    resourceId: number | null
    occurredAt: string
}
