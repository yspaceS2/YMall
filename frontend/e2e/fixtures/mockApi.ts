import type { Page, Route } from '@playwright/test'

type Role = 'ROLE_USER' | 'ROLE_SELLER' | 'ROLE_ADMIN'
type DashboardMode = 'normal' | 'empty' | 'error'
type SupportStatus = 'WAITING' | 'IN_PROGRESS' | 'ANSWERED' | 'LIVE_REQUESTED' | 'LIVE_OFFERED' | 'LIVE_ACTIVE' | 'CLOSED'

interface MockSupportInquiry {
    inquiryId: number
    requesterType: 'CUSTOMER' | 'SELLER'
    requesterName: string
    category: 'ORDER' | 'SETTLEMENT'
    title: string
    status: SupportStatus
    assignedAdminName: string | null
    createdAt: string
    updatedAt: string
    closedAt: string | null
    messages: Array<{
        messageId: number
        authorId: number
        authorName: string
        authorRole: Role
        type: 'INQUIRY' | 'REPLY' | 'SYSTEM' | 'RESOLUTION'
        content: string
        attachments: Array<never>
        clientMessageId: string | null
        createdAt: string
    }>
}

interface MockApiOptions {
    role?: Role
    sellerDashboardMode?: DashboardMode
    adminDashboardMode?: DashboardMode
}

const product = {
    productId: 1,
    categoryId: 1,
    categoryName: '생활',
    name: '테스트 무선 키보드',
    brand: 'YMALL LAB',
    price: 50000,
    discountPercentage: 10,
    rating: 4.8,
    stock: 20,
    thumbnailUrl: null,
    status: 'APPROVED',
}

const merchandisingProduct = {
    productId: product.productId,
    categoryId: product.categoryId,
    categoryName: product.categoryName,
    name: product.name,
    brand: product.brand,
    price: product.price,
    discountPercentage: product.discountPercentage,
    rating: product.rating,
    thumbnailUrl: product.thumbnailUrl,
    salesQuantity: 25,
}

const groceryCategories = [
    { categoryId: 31, categoryName: '신선식품', categorySlug: 'fresh-food' },
    { categoryId: 32, categoryName: '가공식품', categorySlug: 'processed-food' },
    { categoryId: 33, categoryName: '음료', categorySlug: 'beverage' },
    { categoryId: 34, categoryName: '간편식', categorySlug: 'meal-kit' },
]

const homeMerchandising = {
    categoryBest: [
        {
            categoryId: 1,
            categoryName: '생활',
            categorySlug: 'life',
            products: [merchandisingProduct],
        },
        {
            categoryId: 2,
            categoryName: '가전',
            categorySlug: 'digital',
            products: [{
                ...merchandisingProduct,
                productId: 2,
                categoryId: 2,
                categoryName: '가전',
                name: '테스트 무선 스피커',
            }],
        },
        {
            categoryId: 3,
            categoryName: '패션',
            categorySlug: 'fashion',
            products: [{
                ...merchandisingProduct,
                productId: 6,
                categoryId: 3,
                categoryName: '패션',
                name: '테스트 패션 재킷',
            }],
        },
        {
            categoryId: 4,
            categoryName: '뷰티',
            categorySlug: 'beauty',
            products: [{
                ...merchandisingProduct,
                productId: 7,
                categoryId: 4,
                categoryName: '뷰티',
                name: '테스트 수분 크림',
            }],
        },
        ...[
            { categoryId: 5, categoryName: '식품', productId: 8, name: '테스트 신선 식품' },
            { categoryId: 6, categoryName: '가구·인테리어', productId: 9, name: '테스트 인테리어 소품' },
            { categoryId: 7, categoryName: '자동차·공구', productId: 10, name: '테스트 차량 용품' },
            { categoryId: 8, categoryName: '도서·취미', productId: 11, name: '테스트 취미 상품' },
        ].map((category) => ({
            categoryId: category.categoryId,
            categoryName: category.categoryName,
            categorySlug: `category-${category.categoryId}`,
            products: [{
                ...merchandisingProduct,
                productId: category.productId,
                categoryId: category.categoryId,
                categoryName: category.categoryName,
                name: category.name,
            }],
        })),
    ],
    grocery: groceryCategories.map((category, index) => ({
        ...category,
        products: [
            {
                ...merchandisingProduct,
                productId: 31 + index * 2,
                categoryId: category.categoryId,
                categoryName: category.categoryName,
                name: `테스트 ${category.categoryName} 베스트`,
            },
            {
                ...merchandisingProduct,
                productId: 32 + index * 2,
                categoryId: category.categoryId,
                categoryName: category.categoryName,
                name: `테스트 ${category.categoryName} 추천`,
            },
        ],
    })),
    fashion: [
        { categoryId: 41, categoryName: '여성패션', categorySlug: 'women', productId: 41, name: '테스트 여성 재킷' },
        { categoryId: 42, categoryName: '남성패션', categorySlug: 'men', productId: 42, name: '테스트 남성 셔츠' },
        { categoryId: 43, categoryName: '유아동패션', categorySlug: 'kids', productId: 43, name: '테스트 키즈 셋업' },
        { categoryId: 44, categoryName: '스포츠패션', categorySlug: 'sports', productId: 44, name: '테스트 러닝 재킷' },
    ].map((category) => ({
        categoryId: category.categoryId,
        categoryName: category.categoryName,
        categorySlug: category.categorySlug,
        products: [{
            ...merchandisingProduct,
            productId: category.productId,
            categoryId: category.categoryId,
            categoryName: category.categoryName,
            name: category.name,
        }],
    })),
    newArrivals: Array.from({ length: 8 }, (_, index) => ({
        ...merchandisingProduct,
        productId: 51 + index,
        categoryId: 51 + index,
        categoryName: `신상품 카테고리 ${index + 1}`,
        name: `테스트 신상품 ${index + 1}`,
    })),
}

const cartItem = {
    cartItemId: 1,
    productId: 1,
    productName: product.name,
    thumbnailUrl: null,
    price: product.price,
    discountPercentage: product.discountPercentage,
    stock: product.stock,
    productStatus: product.status,
    quantity: 1,
}

const address = {
    addressId: 1,
    addressName: '테스트 배송지',
    recipientName: '테스트 사용자',
    recipientPhone: '01000000000',
    postalCode: '00000',
    roadAddress: '테스트로 1',
    detailAddress: '테스트동 101호',
    isDefault: true,
}

const dashboardTrend = [
    { date: '2026-07-27', netSalesAmount: 280000, orderCount: 8, salesQuantity: 12 },
    { date: '2026-07-28', netSalesAmount: 410000, orderCount: 11, salesQuantity: 17 },
    { date: '2026-07-29', netSalesAmount: 350000, orderCount: 9, salesQuantity: 14 },
    { date: '2026-07-30', netSalesAmount: 620000, orderCount: 16, salesQuantity: 24 },
    { date: '2026-07-31', netSalesAmount: 540000, orderCount: 14, salesQuantity: 20 },
    { date: '2026-08-01', netSalesAmount: 760000, orderCount: 19, salesQuantity: 29 },
    { date: '2026-08-02', netSalesAmount: 690000, orderCount: 17, salesQuantity: 26 },
]

const sellerDashboardStatistics = {
    period: { period: '30d', from: '2026-07-04', to: '2026-08-02', interval: 'DAY' },
    netSalesAmount: 3650000,
    orderCount: 94,
    salesQuantity: 142,
    trend: dashboardTrend,
    orderStatusCounts: [
        { status: 'PAID', count: 18 },
        { status: 'PREPARING', count: 12 },
        { status: 'SHIPPED', count: 21 },
        { status: 'DELIVERED', count: 39 },
        { status: 'PARTIALLY_REFUNDED', count: 4 },
    ],
    topProducts: [
        { productId: 1, productName: '리브드 코튼 카디건', salesQuantity: 38, netSalesAmount: 1216000 },
        { productId: 2, productName: '시그니처 와이드 데님', salesQuantity: 31, netSalesAmount: 1085000 },
        { productId: 3, productName: '데일리 레더 로퍼', salesQuantity: 24, netSalesAmount: 816000 },
        { productId: 4, productName: '소프트 울 머플러', salesQuantity: 19, netSalesAmount: 418000 },
    ],
    settlement: { availableAmount: 2180000, processingAmount: 740000, completedAmount: 12680000 },
    pendingTasks: { orders: 12, returns: 3, questions: 7 },
    generatedAt: '2026-08-02T14:00:00+09:00',
}

const adminDashboardStatistics = {
    period: { period: '30d', from: '2026-07-04', to: '2026-08-02', interval: 'DAY' },
    netTransactionAmount: 128450000,
    orderCount: 2841,
    salesQuantity: 4268,
    transactionTrend: dashboardTrend.map((point) => ({
        ...point,
        netSalesAmount: point.netSalesAmount * 28,
        orderCount: point.orderCount * 24,
        salesQuantity: point.salesQuantity * 22,
    })),
    registrationTrend: Array.from({ length: 30 }, (_, index) => ({
        date: new Date(Date.UTC(2026, 6, 4 + index)).toISOString().slice(0, 10),
        members: index % 6 === 0 ? 18 + index : index % 4,
        sellers: index % 9 === 0 ? 2 + Math.floor(index / 9) : 0,
    })),
    categorySales: [
        { categoryId: 1, categoryName: '패션', netSalesAmount: 38400000, salesQuantity: 1028 },
        { categoryId: 2, categoryName: '가전·디지털', netSalesAmount: 31200000, salesQuantity: 486 },
        { categoryId: 3, categoryName: '식품', netSalesAmount: 24800000, salesQuantity: 1392 },
        { categoryId: 4, categoryName: '생활·주방', netSalesAmount: 18600000, salesQuantity: 794 },
        { categoryId: 5, categoryName: '뷰티', netSalesAmount: 11200000, salesQuantity: 568 },
        { categoryId: 6, categoryName: '도서·취미', netSalesAmount: 8400000, salesQuantity: 412 },
        { categoryId: 7, categoryName: '자동차·공구', netSalesAmount: 6200000, salesQuantity: 238 },
        { categoryId: 8, categoryName: '가구·인테리어', netSalesAmount: 0, salesQuantity: 0 },
    ],
    topProducts: sellerDashboardStatistics.topProducts,
    pendingTasks: { products: 14, sellers: 6, refunds: 9, returns: 11, settlements: 8, support: 4 },
    generatedAt: '2026-08-02T14:00:00+09:00',
}

const baseOrder = {
    orderId: 9001,
    paymentOrderId: 'ymall-test-order-9001',
    status: 'PENDING_PAYMENT',
    totalAmount: 45000,
    items: [{
        orderItemId: 1,
        productId: 1,
        productName: product.name,
        unitPrice: 45000,
        quantity: 1,
        refundedQuantity: 0,
        totalPrice: 45000,
        fulfillmentStatus: 'PENDING',
    }],
    deliveryAddress: {
        recipientName: address.recipientName,
        recipientPhone: address.recipientPhone,
        postalCode: address.postalCode,
        roadAddress: address.roadAddress,
        detailAddress: address.detailAddress,
    },
    refundSupported: false,
    createdAt: '2026-07-26T12:00:00+09:00',
}

const initialNotifications = [
    {
        notificationId: 1,
        type: 'ORDER_CREATED',
        title: '주문이 생성되었습니다',
        message: '테스트 주문의 결제를 진행해 주세요.',
        targetUrl: '/orders/9001/result',
        readAt: null,
        createdAt: '2026-07-26T12:01:00+09:00',
    },
    {
        notificationId: 2,
        type: 'PAYMENT_COMPLETED',
        title: '결제가 완료되었습니다',
        message: '테스트 주문의 결제가 완료되었습니다.',
        targetUrl: '/orders/9001/result',
        readAt: null,
        createdAt: '2026-07-26T12:02:00+09:00',
    },
]

export async function installMockApi(page: Page, options: MockApiOptions = {}) {
    const defaultRole = options.role ?? 'ROLE_USER'
    const state = {
        cartItems: [] as typeof cartItem[],
        notifications: initialNotifications.map((notification) => ({ ...notification })),
        order: { ...baseOrder },
        currentEmail: 'member@example.test',
        currentRole: defaultRole,
        dashboardPeriods: {
            seller: [] as string[],
            admin: [] as string[],
        },
        supportInquiries: [{
            inquiryId: 31,
            requesterType: 'CUSTOMER',
            requesterName: '테스트 회원',
            category: 'ORDER',
            title: '배송 상태를 확인해 주세요',
            status: 'WAITING',
            assignedAdminName: '상담 관리자',
            createdAt: '2026-08-03T09:00:00+09:00',
            updatedAt: '2026-08-03T09:00:00+09:00',
            closedAt: null,
            messages: [{
                messageId: 1,
                authorId: 101,
                authorName: '테스트 회원',
                authorRole: 'ROLE_USER',
                type: 'INQUIRY',
                content: '배송 준비 상태가 오래 지속되고 있습니다.',
                attachments: [],
                clientMessageId: 'e2e-support-message-1',
                createdAt: '2026-08-03T09:00:00+09:00',
            }],
        }] as MockSupportInquiry[],
    }

    await page.routeWebSocket('**/ws', (webSocket) => {
        webSocket.onMessage((message) => {
            if (String(message).startsWith('CONNECT')) {
                webSocket.send('CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\0')
            }
        })
    })

    await page.route('**/api/**', async (route) => {
        const request = route.request()
        const url = new URL(request.url())
        if (!url.pathname.startsWith('/api/')) {
            return route.continue()
        }
        const path = url.pathname.replace(/^\/api/, '')
        const method = request.method()

        if (path === '/members/tokens/refresh' && method === 'POST') {
            return error(route, 401, 'INVALID_TOKEN', '인증 정보가 없습니다.')
        }
        if (path === '/members/login' && method === 'POST') {
            const body = request.postDataJSON() as { email: string }
            const role = roleForEmail(body.email, defaultRole)
            state.currentEmail = body.email
            state.currentRole = role
            return ok(route, {
                accessToken: createJwt(role),
                tokenType: 'Bearer',
                expiresIn: 3600,
            })
        }
        if (path === '/members/logout' && method === 'POST') {
            return noContent(route)
        }
        if (path === '/members/me' && method === 'GET') {
            return ok(route, {
                memberId: 101,
                email: state.currentEmail,
                name: '테스트 회원',
                phone: '01000000000',
                hasPassword: true,
                role: state.currentRole,
                createdAt: '2026-07-26T12:00:00+09:00',
            })
        }
        if (path === '/admin/authorization' && method === 'GET') {
            return ok(route, {
                memberId: 101,
                adminGrade: 'SUPER_ADMIN',
                permissions: [
                    'DASHBOARD_READ',
                    'MEMBER_READ',
                    'MEMBER_RESTRICT_LIMITED',
                    'MEMBER_RESTRICT_ALL',
                    'SELLER_READ',
                    'SELLER_APPLICATION_REVIEW',
                    'SELLER_APPLICATION_DECIDE',
                    'SUPPORT_REPLY',
                    'PRODUCT_REVIEW',
                    'REFUND_STANDARD',
                    'REFUND_ALL',
                    'SETTLEMENT_REVIEW',
                    'SETTLEMENT_APPROVE',
                    'TASK_SELF',
                    'TASK_ASSIGN',
                    'CATEGORY_READ',
                    'CATEGORY_MANAGE_PARTIAL',
                    'CATEGORY_MANAGE_ALL',
                    'ADMIN_MANAGER_MANAGE',
                    'ADMIN_ALL_MANAGE',
                    'AUDIT_OWN_READ',
                    'AUDIT_ALL_READ',
                ],
            })
        }
        if (path === '/members/me/oauth-accounts' && method === 'GET') {
            return ok(route, [])
        }
        if (path === '/members/email-availability' && method === 'GET') {
            return ok(route, { available: true })
        }
        if (path === '/members/signup/email-verifications' && method === 'POST') {
            return ok(route, {
                requestId: 'e2e-signup-email-request',
                expiresIn: 300,
            })
        }
        if (path === '/members/signup/email-verifications/confirm' && method === 'POST') {
            return ok(route, {
                verificationToken: 'e2e-signup-verification-token',
                expiresIn: 600,
            })
        }
        if (path === '/members/signup' && method === 'POST') {
            const body = request.postDataJSON() as {
                email: string
                emailVerificationToken?: string
                name: string
                phone: string
            }
            if (body.emailVerificationToken !== 'e2e-signup-verification-token') {
                return error(route, 400, 'SIGNUP_EMAIL_VERIFICATION_REQUIRED', '이메일 인증이 필요합니다.')
            }
            return ok(route, {
                memberId: 101,
                email: body.email,
                name: body.name,
                phone: body.phone,
                role: 'ROLE_USER',
                createdAt: '2026-07-26T12:00:00+09:00',
            }, 201)
        }
        if (path === '/seller/profile' && method === 'GET') {
            return ok(route, {
                sellerProfileId: 10,
                storeName: '모브 셀렉트',
                businessNumber: '0000000000',
                description: '취향을 담은 데일리웨어',
            })
        }
        if (path === '/seller/products' && method === 'GET') {
            return ok(route, pageResponse([]))
        }
        if (path === '/seller/orders' && method === 'GET') {
            return ok(route, pageResponse([]))
        }
        if (path === '/seller/settlement-requests' && method === 'GET') {
            return ok(route, pageResponse([]))
        }
        if (path === '/seller/dashboard/statistics' && method === 'GET') {
            const period = url.searchParams.get('period') ?? '30d'
            state.dashboardPeriods.seller.push(period)
            if (options.sellerDashboardMode === 'error') {
                return error(route, 503, 'DASHBOARD_LOAD_FAILED', '판매자 대시보드 통계를 불러오지 못했습니다.')
            }
            const statistics = options.sellerDashboardMode === 'empty'
                ? {
                    ...sellerDashboardStatistics,
                    netSalesAmount: 0,
                    orderCount: 0,
                    salesQuantity: 0,
                    trend: [],
                    orderStatusCounts: [],
                    topProducts: [],
                    settlement: { availableAmount: 0, processingAmount: 0, completedAmount: 0 },
                    pendingTasks: { orders: 0, returns: 0, questions: 0 },
                }
                : sellerDashboardStatistics
            return ok(route, {
                ...statistics,
                period: { ...statistics.period, period },
            })
        }
        if (path === '/admin/products' && method === 'GET') {
            return ok(route, pageResponse([]))
        }
        if (path === '/admin/members' && method === 'GET') {
            return ok(route, pageResponse([]))
        }
        if (path === '/admin/sellers' && method === 'GET') {
            return ok(route, pageResponse([]))
        }
        if (path === '/admin/orders' && method === 'GET') {
            return ok(route, pageResponse([]))
        }
        if (path === '/admin/settlement-requests' && method === 'GET') {
            return ok(route, pageResponse([]))
        }
        if (path === '/admin/dashboard/statistics' && method === 'GET') {
            const period = url.searchParams.get('period') ?? '30d'
            state.dashboardPeriods.admin.push(period)
            if (options.adminDashboardMode === 'error') {
                return error(route, 503, 'DASHBOARD_LOAD_FAILED', '관리자 대시보드 통계를 불러오지 못했습니다.')
            }
            const statistics = options.adminDashboardMode === 'empty'
                ? {
                    ...adminDashboardStatistics,
                    netTransactionAmount: 0,
                    orderCount: 0,
                    salesQuantity: 0,
                    transactionTrend: [],
                    registrationTrend: [],
                    categorySales: [],
                    topProducts: [],
                    pendingTasks: { products: 0, sellers: 0, refunds: 0, returns: 0, settlements: 0, support: 0 },
                }
                : adminDashboardStatistics
            return ok(route, {
                ...statistics,
                period: { ...statistics.period, period },
            })
        }
        if (path === '/admin/support/inquiries/pending-count' && method === 'GET') {
            return ok(route, {
                count: state.supportInquiries.filter((item) =>
                    item.status === 'WAITING' || item.status === 'LIVE_REQUESTED').length,
            })
        }
        if ((path === '/support/inquiries' || path === '/admin/support/inquiries') && method === 'GET') {
            const status = url.searchParams.get('status')
            const keyword = (url.searchParams.get('keyword') ?? '').toLowerCase()
            const content = state.supportInquiries
                .filter((item) => !status || item.status === status)
                .filter((item) => !keyword
                    || item.title.toLowerCase().includes(keyword)
                    || item.requesterName.toLowerCase().includes(keyword)
                    || (item.assignedAdminName ?? '').toLowerCase().includes(keyword))
                .map(supportSummary)
            return ok(route, pageResponse(content, 30))
        }
        if (path === '/support/inquiries' && method === 'POST') {
            const body = request.postDataJSON() as { category: 'ORDER'; title: string; content: string }
            const now = new Date().toISOString()
            const inquiry: MockSupportInquiry = {
                inquiryId: 32,
                requesterType: state.currentRole === 'ROLE_SELLER' ? 'SELLER' : 'CUSTOMER',
                requesterName: '테스트 회원',
                category: body.category,
                title: body.title,
                status: 'WAITING',
                assignedAdminName: null,
                createdAt: now,
                updatedAt: now,
                closedAt: null,
                messages: [{
                    messageId: 2,
                    authorId: 101,
                    authorName: '테스트 회원',
                    authorRole: state.currentRole,
                    type: 'INQUIRY',
                    content: body.content,
                    attachments: [],
                    clientMessageId: 'e2e-support-message-2',
                    createdAt: now,
                }],
            }
            state.supportInquiries.unshift(inquiry)
            return ok(route, supportDetail(inquiry, false), 201)
        }
        const supportDetailMatch = path.match(/^\/(?:admin\/)?support\/inquiries\/(\d+)$/)
        if (supportDetailMatch && method === 'GET') {
            const inquiry = state.supportInquiries.find((item) => item.inquiryId === Number(supportDetailMatch[1]))
            return inquiry
                ? ok(route, supportDetail(inquiry, path.startsWith('/admin/')))
                : error(route, 404, 'SUPPORT_INQUIRY_NOT_FOUND', '문의를 찾을 수 없습니다.')
        }
        const supportMessageMatch = path.match(/^\/(admin\/)?support\/inquiries\/(\d+)\/messages$/)
        if (supportMessageMatch && method === 'POST') {
            const inquiry = state.supportInquiries.find((item) => item.inquiryId === Number(supportMessageMatch[2]))
            if (!inquiry) return error(route, 404, 'SUPPORT_INQUIRY_NOT_FOUND', '문의를 찾을 수 없습니다.')
            const body = request.postDataJSON() as { content: string; clientMessageId: string }
            const message = {
                messageId: inquiry.messages.length + 1,
                authorId: 101,
                authorName: supportMessageMatch[1] ? '상담 관리자' : '테스트 회원',
                authorRole: (supportMessageMatch[1] ? 'ROLE_ADMIN' : state.currentRole) as Role,
                type: (supportMessageMatch[1] ? 'REPLY' : 'INQUIRY') as 'REPLY' | 'INQUIRY',
                content: body.content,
                attachments: [],
                clientMessageId: body.clientMessageId,
                createdAt: new Date().toISOString(),
            }
            inquiry.messages.push(message)
            inquiry.status = supportMessageMatch[1] ? 'ANSWERED' : 'WAITING'
            inquiry.updatedAt = message.createdAt
            return ok(route, message)
        }
        const supportCloseMatch = path.match(/^\/admin\/support\/inquiries\/(\d+)\/close$/)
        if (supportCloseMatch && method === 'POST') {
            const inquiry = state.supportInquiries.find((item) => item.inquiryId === Number(supportCloseMatch[1]))
            if (!inquiry) return error(route, 404, 'SUPPORT_INQUIRY_NOT_FOUND', '문의를 찾을 수 없습니다.')
            const body = request.postDataJSON() as { content: string }
            const now = new Date().toISOString()
            inquiry.messages.push({
                messageId: inquiry.messages.length + 1,
                authorId: 101,
                authorName: '상담 관리자',
                authorRole: 'ROLE_ADMIN',
                type: 'RESOLUTION',
                content: body.content,
                attachments: [],
                clientMessageId: null,
                createdAt: now,
            })
            inquiry.status = 'CLOSED'
            inquiry.closedAt = now
            inquiry.updatedAt = now
            return ok(route, supportDetail(inquiry))
        }
        if (path === '/notifications/unread-count' && method === 'GET') {
            return ok(route, {
                unreadCount: state.notifications.filter((item) => item.readAt === null).length,
            })
        }
        if (path === '/notifications' && method === 'GET') {
            return ok(route, pageResponse(state.notifications))
        }
        if (/^\/notifications\/\d+\/read$/.test(path) && method === 'PATCH') {
            const notificationId = Number(path.split('/')[2])
            const notification = state.notifications.find((item) => item.notificationId === notificationId)
            if (!notification) return error(route, 404, 'NOTIFICATION_NOT_FOUND', '알림이 없습니다.')
            notification.readAt = new Date().toISOString()
            return ok(route, notification)
        }
        if (path === '/notifications/read-all' && method === 'PATCH') {
            let updatedCount = 0
            state.notifications.forEach((notification) => {
                if (notification.readAt === null) {
                    notification.readAt = new Date().toISOString()
                    updatedCount += 1
                }
            })
            return ok(route, { updatedCount })
        }
        if (path === '/categories' && method === 'GET') {
            return ok(route, [{ categoryId: 1, name: '생활', slug: 'life' }])
        }
        if (path === '/home/merchandising' && method === 'GET') {
            return ok(route, homeMerchandising)
        }
        if ((path === '/products' || path === '/products/search') && method === 'GET') {
            return ok(route, pageResponse([product], 12))
        }
        if (path === '/products/1' && method === 'GET') {
            return ok(route, {
                ...product,
                category: { categoryId: 1, name: '생활', slug: 'life' },
                description: '브라우저 자동화 검증용 상품입니다.',
                images: [],
            })
        }
        if (path === '/products/1/reviews' && method === 'GET') {
            return ok(route, pageResponse([], 10))
        }
        if (path === '/products/1/review-summary' && method === 'GET') {
            return ok(route, {
                available: false,
                reviewCount: 0,
                pros: [],
                cons: [],
                commonOpinions: [],
                modelVersion: null,
                generatedAt: null,
            })
        }
        if (path === '/cart/items' && method === 'POST') {
            const body = request.postDataJSON() as { quantity: number }
            state.cartItems = [{ ...cartItem, quantity: body.quantity }]
            return ok(route, state.cartItems[0], 201)
        }
        if (path === '/cart' && method === 'GET') {
            return ok(route, { items: state.cartItems })
        }
        if (path === '/members/me/addresses' && method === 'GET') {
            return ok(route, [address])
        }
        if (path === '/orders' && method === 'POST') {
            state.order = { ...baseOrder }
            return ok(route, state.order, 201)
        }
        if (path === '/orders/9001' && method === 'GET') {
            return ok(route, state.order)
        }
        if (path === '/orders/9001/cancellations' && method === 'POST') {
            state.order = { ...state.order, status: 'CANCELED' }
            return ok(route, state.order)
        }

        return error(route, 404, 'E2E_MOCK_NOT_FOUND', `${method} ${path} 가짜 응답이 없습니다.`)
    })

    return state
}

export async function loginThroughUi(page: Page, email = 'member@example.test') {
    await page.goto('/login')
    await page.getByLabel('이메일').fill(email)
    await page.getByLabel('비밀번호').fill('Test1234!')
    await page.getByRole('button', { name: '로그인' }).click()
    await page.waitForURL('/')
}

function roleForEmail(email: string, fallback: Role): Role {
    if (email.startsWith('admin')) return 'ROLE_ADMIN'
    if (email.startsWith('seller')) return 'ROLE_SELLER'
    return fallback
}

function createJwt(role: Role) {
    const encode = (value: object) => Buffer.from(JSON.stringify(value)).toString('base64url')
    const now = Math.floor(Date.now() / 1000)
    return `${encode({ alg: 'none', typ: 'JWT' })}.${encode({
        sub: '101',
        email: 'member@example.test',
        role,
        iat: now,
        exp: now + 3600,
    })}.e2e`
}

function pageResponse<T>(content: T[], size = 20) {
    return {
        content,
        page: 1,
        size,
        totalElements: content.length,
        totalPages: content.length === 0 ? 0 : 1,
        hasNext: false,
        hasPrevious: false,
    }
}

function supportDetail(inquiry: MockSupportInquiry, includeResolution = true) {
    return {
        inquiry: supportSummary(inquiry),
        relatedOrderId: null,
        relatedProductId: null,
        relatedSettlementId: null,
        chatSession: null,
        messages: inquiry.messages.filter((message) => includeResolution || message.type !== 'RESOLUTION'),
    }
}

function supportSummary(inquiry: MockSupportInquiry) {
    return {
        inquiryId: inquiry.inquiryId,
        requesterType: inquiry.requesterType,
        requesterName: inquiry.requesterName,
        category: inquiry.category,
        title: inquiry.title,
        status: inquiry.status,
        assignedAdminName: inquiry.assignedAdminName,
        createdAt: inquiry.createdAt,
        updatedAt: inquiry.updatedAt,
        closedAt: inquiry.closedAt,
    }
}

function ok(route: Route, data: unknown, status = 200) {
    return route.fulfill({
        status,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data, message: '요청이 성공했습니다.' }),
    })
}

function error(route: Route, status: number, code: string, message: string) {
    return route.fulfill({
        status,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, error: { code, message } }),
    })
}

function noContent(route: Route) {
    return route.fulfill({ status: 204 })
}
