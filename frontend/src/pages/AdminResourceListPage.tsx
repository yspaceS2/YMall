import { LoaderCircle } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import {
    getAdminMembers,
    getAdminOrders,
    getAdminSellers,
    type AdminOrderWorkType,
} from '../api/admin'
import { getApiErrorMessage, isAbortError } from '../api/errors'
import { formatMemberAuthority } from '../components/admin/adminRolePresentation'
import {
    ManagementEmpty,
    ManagementListSearch,
    ManagementPageHeader,
    ManagementPagination,
    managementPageClassName,
} from '../components/management/ManagementListUi'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { StatusBadge } from '../components/ui/StatusBadge'
import type {
    AdminGrade,
    AdminMember,
    AdminMemberPage,
    AdminOrder,
    AdminOrderPage,
    AdminSeller,
    AdminSellerPage,
    MemberAccessStatus,
} from '../types/admin'
import { formatKoreanDateTime } from '../utils/dateTime'
import { getOrderStatusLabel } from '../utils/order'
import { formatPrice } from '../utils/product'
import { parsePositiveInteger } from '../utils/searchParams'
import { adminResourceMeta, type AdminResource } from './adminResourceConfig'

type AdminResourceItem = AdminMember | AdminSeller | AdminOrder
type AdminResourcePage = AdminMemberPage | AdminSellerPage | AdminOrderPage

export function AdminResourceListPage({ resource }: { resource: AdminResource }) {
    const navigate = useNavigate()
    const [searchParams, setSearchParams] = useSearchParams()
    const [pageData, setPageData] = useState<AdminResourcePage | null>(null)
    const [errorMessage, setErrorMessage] = useState('')
    const [isLoading, setIsLoading] = useState(true)
    const page = parsePositiveInteger(searchParams.get('page'), 1)
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
    const meta = adminResourceMeta[resource]

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
                if (isAbortError(error)) return
                setErrorMessage(getApiErrorMessage(error, '관리 목록을 불러오지 못했습니다.'))
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
