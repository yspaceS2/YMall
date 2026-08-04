export type AdminResource = 'members' | 'sellers' | 'orders'

export const adminResourceMeta = {
    members: {
        eyebrow: 'MEMBER MANAGEMENT',
        title: '회원 관리',
        description: '회원 이름과 이메일로 검색하고 상세 정보를 확인합니다.',
        searchPlaceholder: '회원 이름 또는 이메일 검색',
        emptyMessage: '조회된 회원이 없습니다.',
    },
    sellers: {
        eyebrow: 'SELLER MANAGEMENT',
        title: '판매자 관리',
        description: '상점명, 회원 정보, 사업자번호로 판매자를 검색합니다.',
        searchPlaceholder: '상점명, 판매자명, 이메일 또는 사업자번호 검색',
        emptyMessage: '조회된 판매자가 없습니다.',
    },
    orders: {
        eyebrow: 'ORDER MANAGEMENT',
        title: '주문 관리',
        description: '주문번호, 구매자 또는 상품명으로 주문을 검색합니다.',
        searchPlaceholder: '주문번호, 구매자 또는 상품명 검색',
        emptyMessage: '조회된 주문이 없습니다.',
    },
} satisfies Record<AdminResource, {
    eyebrow: string
    title: string
    description: string
    searchPlaceholder: string
    emptyMessage: string
}>
