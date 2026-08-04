import { ArrowLeft, LoaderCircle } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
    getAdminMember,
    getAdminMembers,
    getAdminOrder,
    getAdminOrders,
    getAdminSeller,
    getAdminSellers,
    type AdminOrderWorkType,
} from '../api/admin'
import { ApiError } from '../api/client'
import {
    ManagementEmpty,
    ManagementListSearch,
    ManagementPageHeader,
    ManagementPagination,
    managementPageClassName,
} from '../components/management/ManagementListUi'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { StatusBadge } from '../components/ui/StatusBadge'
import { AdminMemberRolePanel } from '../components/admin/AdminMemberRolePanel'
import { AdminMemberOperationsPanel } from '../components/admin/AdminMemberOperationsPanel'
import {
    formatAdminGrade,
    formatMemberAuthority,
    formatMemberRole,
} from '../components/admin/adminRolePresentation'
import type {
    AdminMember,
    AdminMemberPage,
    AdminOrder,
    AdminOrderPage,
    AdminSeller,
    AdminSellerPage,
    AdminGrade,
    MemberAccessStatus,
} from '../types/admin'
import { formatKoreanDateTime } from '../utils/dateTime'
import { getOrderItemFulfillmentStatusLabel, getOrderStatusLabel } from '../utils/order'
import { formatPrice } from '../utils/product'

type AdminResource = 'members' | 'sellers' | 'orders'
type AdminResourceItem = AdminMember | AdminSeller | AdminOrder
type AdminResourcePage = AdminMemberPage | AdminSellerPage | AdminOrderPage

const resourceMeta = {
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

export function AdminResourceListPage({ resource }: { resource: AdminResource }) {
    const navigate = useNavigate()
    const [searchParams, setSearchParams] = useSearchParams()
    const [pageData, setPageData] = useState<AdminResourcePage | null>(null)
    const [errorMessage, setErrorMessage] = useState('')
    const [isLoading, setIsLoading] = useState(true)
    const page = positiveNumber(searchParams.get('page'), 1)
    const keyword = searchParams.get('keyword')?.trim() ?? ''
    const workType = resource === 'orders'
        ? parseOrderWorkType(searchParams.get('workType'))
        : undefined
    const memberStatus = resource === 'members'
        ? parseMemberAccessStatus(searchParams.get('status'))
        : undefined
    const memberRole = resource === 'members'
        ? parseMemberRole(searchParams.get('role'))
        : undefined
    const adminGrade = resource === 'members'
        ? parseAdminGrade(searchParams.get('adminGrade'))
        : undefined
    const joinedFrom = resource === 'members' ? searchParams.get('joinedFrom') ?? '' : ''
    const joinedTo = resource === 'members' ? searchParams.get('joinedTo') ?? '' : ''
    const meta = resourceMeta[resource]

    useEffect(() => {
        const controller = new AbortController()
        loadList(resource, page, keyword, workType, {
            status: memberStatus,
            role: memberRole,
            adminGrade,
            joinedFrom,
            joinedTo,
        }, controller.signal)
            .then(setPageData)
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(
                    error instanceof ApiError
                        ? error.message
                        : '관리 목록을 불러오지 못했습니다.',
                )
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [adminGrade, joinedFrom, joinedTo, keyword, memberRole, memberStatus, page, resource, workType])

    const items = pageData?.content ?? []

    return (
        <section className={managementPageClassName}>
            <ManagementPageHeader
                eyebrow={meta.eyebrow}
                title={meta.title}
                description={meta.description}
            />
            {resource === 'orders' && (
                <label className="mb-4 grid max-w-60 gap-1.5 text-xs font-bold">
                    처리 업무
                    <select
                        className="h-11 border border-line bg-surface px-3 text-sm font-normal text-ink"
                        value={workType ?? ''}
                        onChange={(event) => {
                            const next = new URLSearchParams(searchParams)
                            const nextWorkType = parseOrderWorkType(event.target.value)
                            if (nextWorkType) next.set('workType', nextWorkType)
                            else next.delete('workType')
                            next.set('page', '1')
                            setSearchParams(next)
                        }}
                    >
                        <option value="">전체 주문</option>
                        <option value="PENDING_REFUND">환불 처리 대기</option>
                        <option value="PENDING_RETURN">반품 처리 대기</option>
                    </select>
                </label>
            )}
            {resource === 'members' && (
                <MemberFilters
                    adminGrade={adminGrade}
                    joinedFrom={joinedFrom}
                    joinedTo={joinedTo}
                    role={memberRole}
                    status={memberStatus}
                />
            )}
            <ManagementListSearch placeholder={meta.searchPlaceholder} />
            {errorMessage && <FeedbackMessage className="mb-5" tone="error">{errorMessage}</FeedbackMessage>}
            {isLoading ? (
                <div className="grid min-h-72 place-items-center">
                    <LoaderCircle className="size-6 animate-spin" aria-label="불러오는 중" />
                </div>
            ) : items.length === 0 ? (
                <ManagementEmpty>{meta.emptyMessage}</ManagementEmpty>
            ) : (
                <div className="overflow-x-auto border-y border-line">
                    <table className="w-full min-w-180 text-left text-sm">
                        <thead className="border-b border-ink text-xs">
                            <tr>
                                {headers(resource).map((header) => (
                                    <th className="p-4" key={header}>{header}</th>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {items.map((item) => {
                                const id = itemId(resource, item)
                                return (
                                    <tr
                                        className="cursor-pointer border-b border-line transition-colors hover:bg-surface"
                                        key={id}
                                        tabIndex={0}
                                        role="link"
                                        onClick={() => navigate(`/admin/${resource}/${id}`, {
                                            state: { returnTo: `/admin/${resource}?${searchParams.toString()}` },
                                        })}
                                        onKeyDown={(event) => {
                                            if (event.key === 'Enter' || event.key === ' ') {
                                                event.preventDefault()
                                                navigate(`/admin/${resource}/${id}`, {
                                                    state: { returnTo: `/admin/${resource}?${searchParams.toString()}` },
                                                })
                                            }
                                        }}
                                    >
                                        {rowCells(resource, item).map((cell, index) => (
                                            <td className={index === 0 ? 'p-4 font-bold' : 'p-4'} key={index}>
                                                {cell}
                                            </td>
                                        ))}
                                    </tr>
                                )
                            })}
                        </tbody>
                    </table>
                </div>
            )}
            <ManagementPagination
                page={pageData?.page ?? page}
                totalPages={pageData?.totalPages ?? 0}
            />
        </section>
    )
}

export function AdminResourceDetailPage({ resource }: { resource: AdminResource }) {
    const location = useLocation()
    const { resourceId } = useParams()
    const id = Number(resourceId)
    const invalidId = !Number.isInteger(id) || id <= 0
    const [item, setItem] = useState<AdminResourceItem | null>(null)
    const [errorMessage, setErrorMessage] = useState('')
    const [isLoading, setIsLoading] = useState(true)
    const meta = resourceMeta[resource]
    const returnTo = typeof location.state === 'object'
        && location.state !== null
        && 'returnTo' in location.state
        && typeof location.state.returnTo === 'string'
        ? location.state.returnTo
        : `/admin/${resource}`

    useEffect(() => {
        if (invalidId) return

        const controller = new AbortController()
        loadDetail(resource, id, controller.signal)
            .then(setItem)
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(
                    error instanceof ApiError
                        ? error.message
                        : '상세 정보를 불러오지 못했습니다.',
                )
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [id, invalidId, resource])

    return (
        <section className={managementPageClassName}>
            <Link
                className="mb-6 inline-flex items-center gap-2 text-xs font-bold underline underline-offset-4"
                to={returnTo}
            >
                <ArrowLeft className="size-4" aria-hidden="true" />
                {meta.title} 목록
            </Link>
            <ManagementPageHeader
                eyebrow={meta.eyebrow}
                title={detailTitle(resource, item)}
                description="목록에서 선택한 항목의 상세 정보입니다."
            />
            {isLoading && !invalidId ? (
                <div className="grid min-h-72 place-items-center">
                    <LoaderCircle className="size-6 animate-spin" aria-label="불러오는 중" />
                </div>
            ) : invalidId || errorMessage || !item ? (
                <FeedbackMessage tone="error">
                    {invalidId ? '잘못된 상세 페이지 주소입니다.' : errorMessage || '상세 정보가 없습니다.'}
                </FeedbackMessage>
            ) : (
                <DetailContent
                    resource={resource}
                    item={item}
                    onMemberChanged={(response) => setItem((current) => {
                        if (!current || resource !== 'members') return current
                        return {
                            ...(current as AdminMember),
                            role: response.role,
                            adminGrade: response.adminGrade,
                        }
                    })}
                    onMemberOperationsChanged={(member) => setItem(member)}
                />
            )}
        </section>
    )
}

function DetailContent({
    resource,
    item,
    onMemberChanged,
    onMemberOperationsChanged,
}: {
    resource: AdminResource
    item: AdminResourceItem
    onMemberChanged: Parameters<typeof AdminMemberRolePanel>[0]['onChanged']
    onMemberOperationsChanged: Parameters<typeof AdminMemberOperationsPanel>[0]['onChanged']
}) {
    if (resource === 'members') {
        const member = item as AdminMember
        return (
            <div className="grid gap-8">
                <DetailGrid columns={2} values={[
                    ['회원번호', String(member.memberId)],
                    ['이름', member.name],
                    ['이메일', member.email],
                    ['계정 유형', formatMemberRole(member.role)],
                    ...(member.role === 'ROLE_ADMIN'
                        ? [['관리자 등급', formatAdminGrade(member.adminGrade)] as [string, string]]
                        : []),
                    ['이용 상태', member.accessStatus === 'ACTIVE' ? '정상' : '이용 제한'],
                    ['최근 로그인', member.lastLoginAt ? formatKoreanDateTime(member.lastLoginAt) : '로그인 기록 없음'],
                    ['주문 건수', `${member.orderCount}건`],
                    ['누적 결제액', formatPrice(member.totalPaidAmount)],
                    ['가입일', formatKoreanDateTime(member.createdAt)],
                ]} />
                <AdminMemberRolePanel member={member} onChanged={onMemberChanged} />
                <AdminMemberOperationsPanel
                    member={member}
                    onChanged={onMemberOperationsChanged}
                />
            </div>
        )
    }

    if (resource === 'sellers') {
        const seller = item as AdminSeller
        return (
            <div className="grid gap-8">
                <DetailGrid columns={2} values={[
                    ['판매자번호', String(seller.sellerProfileId)],
                    ['상점명', seller.storeName],
                    ['회원명', seller.memberName],
                    ['이메일', seller.email],
                    ['사업자번호', seller.businessNumber],
                    ['등록일', formatKoreanDateTime(seller.createdAt)],
                    ['전체 상품', `${seller.productCount}개`],
                    ['승인 대기 상품', `${seller.pendingProductCount}개`],
                    ['주문 건수', `${seller.orderCount}건`],
                    ['거래액', formatPrice(seller.grossSalesAmount)],
                    ['환불 수량', `${seller.refundedQuantity}개`],
                    ['처리 대기 반품', `${seller.pendingReturnCount}건`],
                    ['미처리 문의', `${seller.pendingSupportCount}건`],
                    ['정산 검토 대기', `${seller.pendingSettlementCount}건`],
                    ['가입 신청 상태', seller.applicationStatus ?? '-'],
                    ['신청 검토일', seller.applicationReviewedAt
                        ? formatKoreanDateTime(seller.applicationReviewedAt)
                        : '-'],
                ]} />
                {seller.applicationReviewReason && (
                    <FeedbackMessage tone="info">
                        판매자 신청 검토 사유: {seller.applicationReviewReason}
                    </FeedbackMessage>
                )}
                <section className="border-t-2 border-ink pt-5">
                    <h2 className="mb-5 text-xl font-bold">관련 업무 바로가기</h2>
                    <div className="flex flex-wrap gap-2">
                        <RelatedLink to="/admin/products">상품 심사</RelatedLink>
                        <RelatedLink to="/admin/orders">주문·환불</RelatedLink>
                        <RelatedLink to="/admin/settlement">정산</RelatedLink>
                        <RelatedLink to={`/admin/support?keyword=${encodeURIComponent(seller.memberName)}`}>
                            고객센터
                        </RelatedLink>
                        <RelatedLink to="/admin/seller-applications">가입 신청</RelatedLink>
                    </div>
                </section>
            </div>
        )
    }

    const order = item as AdminOrder
    return (
        <div className="grid gap-8">
            <DetailGrid values={[
                ['주문번호', String(order.orderId)],
                ['구매자', order.memberName],
                ['이메일', order.memberEmail],
                ['상태', getOrderStatusLabel(order.status)],
                ['결제금액', formatPrice(order.totalAmount)],
                ['주문일', formatKoreanDateTime(order.createdAt)],
            ]} />
            <section>
                <h2 className="mb-4 text-lg font-bold">주문 상품</h2>
                <div className="overflow-x-auto border-y border-line">
                    <table className="w-full min-w-150 text-left text-sm">
                        <thead className="border-b border-ink text-xs">
                            <tr>
                                <th className="p-4">상품</th>
                                <th className="p-4">단가</th>
                                <th className="p-4">수량</th>
                                <th className="p-4">환불 수량</th>
                                <th className="p-4">처리 상태</th>
                            </tr>
                        </thead>
                        <tbody>
                            {order.items.map((orderItem) => (
                                <tr className="border-b border-line" key={orderItem.orderItemId}>
                                    <td className="p-4 font-bold">{orderItem.productName}</td>
                                    <td className="p-4">{formatPrice(orderItem.unitPrice)}</td>
                                    <td className="p-4">{orderItem.quantity}</td>
                                    <td className="p-4">{orderItem.refundedQuantity}</td>
                                    <td className="p-4">
                                        {getOrderItemFulfillmentStatusLabel(orderItem.fulfillmentStatus)}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </section>
        </div>
    )
}

function DetailGrid({
    values,
    columns = 1,
}: {
    values: Array<[string, string]>
    columns?: 1 | 2
}) {
    return (
        <dl className={columns === 2
            ? 'grid border-t border-ink min-[901px]:grid-cols-2'
            : 'border-t border-ink'}>
            {values.map(([label, value]) => (
                <div
                    className={columns === 2
                        ? 'grid gap-2 border-b border-line py-4 min-[701px]:grid-cols-[140px_1fr] min-[901px]:pr-6'
                        : 'grid gap-2 border-b border-line py-5 min-[701px]:grid-cols-[180px_1fr]'}
                    key={label}
                >
                    <dt className="text-xs font-bold text-muted">{label}</dt>
                    <dd className="text-sm font-bold">{value}</dd>
                </div>
            ))}
        </dl>
    )
}

function RelatedLink({ to, children }: { to: string; children: string }) {
    return (
        <Link className="border border-line bg-surface px-4 py-3 text-xs font-bold hover:border-ink" to={to}>
            {children}
        </Link>
    )
}

function MemberFilters({
    status,
    role,
    adminGrade,
    joinedFrom,
    joinedTo,
}: {
    status?: MemberAccessStatus
    role?: 'ROLE_USER' | 'ROLE_SELLER' | 'ROLE_ADMIN'
    adminGrade?: AdminGrade
    joinedFrom: string
    joinedTo: string
}) {
    const [searchParams, setSearchParams] = useSearchParams()

    function update(name: string, value: string) {
        const next = new URLSearchParams(searchParams)
        if (value) next.set(name, value)
        else next.delete(name)
        if (name === 'role' && value !== 'ROLE_ADMIN') next.delete('adminGrade')
        next.set('page', '1')
        setSearchParams(next)
    }

    const selectClassName = 'h-11 min-w-36 border border-line bg-surface px-3 text-sm text-ink'
    return (
        <div className="mb-4 flex flex-wrap items-end gap-3">
            <label className="grid gap-1.5 text-xs font-bold text-muted">
                이용 상태
                <select className={selectClassName} value={status ?? ''} onChange={(event) => update('status', event.target.value)}>
                    <option value="">전체</option>
                    <option value="ACTIVE">정상</option>
                    <option value="RESTRICTED">이용 제한</option>
                </select>
            </label>
            <label className="grid gap-1.5 text-xs font-bold text-muted">
                계정 역할
                <select className={selectClassName} value={role ?? ''} onChange={(event) => update('role', event.target.value)}>
                    <option value="">전체</option>
                    <option value="ROLE_USER">회원</option>
                    <option value="ROLE_SELLER">판매자</option>
                    <option value="ROLE_ADMIN">관리자</option>
                </select>
            </label>
            <label className="grid gap-1.5 text-xs font-bold text-muted">
                관리자 등급
                <select
                    className={selectClassName}
                    disabled={role !== 'ROLE_ADMIN'}
                    value={adminGrade ?? ''}
                    onChange={(event) => update('adminGrade', event.target.value)}
                >
                    <option value="">전체</option>
                    <option value="MANAGER">Manager</option>
                    <option value="SUPERVISOR">Supervisor</option>
                    <option value="SUPER_ADMIN">Super Admin</option>
                </select>
            </label>
            <label className="grid gap-1.5 text-xs font-bold text-muted">
                가입 시작일
                <input className={selectClassName} type="date" value={joinedFrom} onChange={(event) => update('joinedFrom', event.target.value)} />
            </label>
            <label className="grid gap-1.5 text-xs font-bold text-muted">
                가입 종료일
                <input className={selectClassName} type="date" value={joinedTo} onChange={(event) => update('joinedTo', event.target.value)} />
            </label>
        </div>
    )
}

function loadList(
    resource: AdminResource,
    page: number,
    keyword: string,
    workType: AdminOrderWorkType | undefined,
    memberFilters: {
        status?: MemberAccessStatus
        role?: 'ROLE_USER' | 'ROLE_SELLER' | 'ROLE_ADMIN'
        adminGrade?: AdminGrade
        joinedFrom: string
        joinedTo: string
    },
    signal: AbortSignal,
): Promise<AdminResourcePage> {
    if (resource === 'members') return getAdminMembers({
        page,
        keyword,
        signal,
        status: memberFilters.status,
        role: memberFilters.role,
        adminGrade: memberFilters.adminGrade,
        joinedFrom: memberFilters.joinedFrom || undefined,
        joinedTo: memberFilters.joinedTo || undefined,
    })
    if (resource === 'sellers') return getAdminSellers({ page, keyword, signal })
    return getAdminOrders({ page, keyword, workType, signal })
}

function parseOrderWorkType(value: string | null): AdminOrderWorkType | undefined {
    return value === 'PENDING_REFUND' || value === 'PENDING_RETURN' ? value : undefined
}

function parseMemberAccessStatus(value: string | null): MemberAccessStatus | undefined {
    return value === 'ACTIVE' || value === 'RESTRICTED' ? value : undefined
}

function parseMemberRole(value: string | null) {
    return value === 'ROLE_USER' || value === 'ROLE_SELLER' || value === 'ROLE_ADMIN'
        ? value
        : undefined
}

function parseAdminGrade(value: string | null): AdminGrade | undefined {
    return value === 'MANAGER' || value === 'SUPERVISOR' || value === 'SUPER_ADMIN'
        ? value
        : undefined
}

function loadDetail(
    resource: AdminResource,
    id: number,
    signal: AbortSignal,
): Promise<AdminResourceItem> {
    if (resource === 'members') return getAdminMember(id, signal)
    if (resource === 'sellers') return getAdminSeller(id, signal)
    return getAdminOrder(id, signal)
}

function headers(resource: AdminResource) {
    if (resource === 'members') {
        return ['회원', '이메일', '권한', '상태', '주문', '누적 결제액', '최근 로그인']
    }
    if (resource === 'sellers') return ['상점명', '회원', '이메일', '사업자번호']
    return ['주문번호', '구매자', '대표 상품', '결제금액', '상태', '주문일']
}

function rowCells(resource: AdminResource, item: AdminResourceItem) {
    if (resource === 'members') {
        const member = item as AdminMember
        return [
            member.name,
            member.email,
            formatMemberAuthority(member),
            <StatusBadge tone={member.accessStatus === 'ACTIVE' ? 'success' : 'danger'}>
                {member.accessStatus === 'ACTIVE' ? '정상' : '이용 제한'}
            </StatusBadge>,
            `${member.orderCount}건`,
            formatPrice(member.totalPaidAmount),
            member.lastLoginAt ? formatKoreanDateTime(member.lastLoginAt) : '-',
        ]
    }
    if (resource === 'sellers') {
        const seller = item as AdminSeller
        return [seller.storeName, seller.memberName, seller.email, seller.businessNumber]
    }
    const order = item as AdminOrder
    const firstProduct = order.items[0]?.productName ?? '-'
    const productLabel = order.items.length > 1
        ? `${firstProduct} 외 ${order.items.length - 1}개`
        : firstProduct
    return [
        String(order.orderId),
        order.memberName,
        productLabel,
        formatPrice(order.totalAmount),
        <StatusBadge tone={adminOrderStatusTone(order.status)}>
            {getOrderStatusLabel(order.status)}
        </StatusBadge>,
        formatKoreanDateTime(order.createdAt),
    ]
}

function adminOrderStatusTone(status: AdminOrder['status']) {
    if (status === 'DELIVERED') return 'success' as const
    if (status === 'PAYMENT_FAILED') return 'danger' as const
    if (status === 'PENDING_PAYMENT' || status === 'PARTIALLY_REFUNDED') return 'warning' as const
    if (status === 'CANCELED' || status === 'REFUNDED') return 'neutral' as const
    return 'info' as const
}

function itemId(resource: AdminResource, item: AdminResourceItem) {
    if (resource === 'members') return (item as AdminMember).memberId
    if (resource === 'sellers') return (item as AdminSeller).sellerProfileId
    return (item as AdminOrder).orderId
}

function detailTitle(resource: AdminResource, item: AdminResourceItem | null) {
    if (!item) return resourceMeta[resource].title
    if (resource === 'members') return (item as AdminMember).name
    if (resource === 'sellers') return (item as AdminSeller).storeName
    return `주문 #${(item as AdminOrder).orderId}`
}

function positiveNumber(value: string | null, fallback: number) {
    const parsed = Number(value)
    return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback
}
