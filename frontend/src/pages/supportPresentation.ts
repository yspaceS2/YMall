import type { SupportInquiryCategory, SupportInquiryStatus } from '../types/support'

export const STATUS_LABELS: Record<SupportInquiryStatus, string> = {
    WAITING: '답변 대기',
    IN_PROGRESS: '처리 중',
    ANSWERED: '답변 완료',
    LIVE_REQUESTED: '상담 요청',
    LIVE_OFFERED: '상담 제안',
    LIVE_ACTIVE: '상담 중',
    CLOSED: '처리 완료',
}

export const CATEGORY_LABELS: Record<SupportInquiryCategory, string> = {
    ORDER: '주문',
    PAYMENT: '결제',
    CANCEL_REFUND: '취소·환불',
    DELIVERY: '배송',
    ACCOUNT: '계정',
    PRODUCT_APPROVAL: '상품 승인',
    SETTLEMENT: '정산',
    SELLER_PERMISSION: '판매 권한',
    POLICY: '운영 정책',
    SERVICE: '서비스 이용',
}
