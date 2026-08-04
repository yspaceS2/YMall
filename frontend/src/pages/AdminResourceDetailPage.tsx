import { ArrowLeft, LoaderCircle } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import {
    getAdminMember,
    getAdminOrder,
    getAdminSeller,
} from '../api/admin'
import { ApiError } from '../api/client'
import {
    ManagementPageHeader,
    managementPageClassName,
} from '../components/management/ManagementListUi'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { AdminMemberRolePanel } from '../components/admin/AdminMemberRolePanel'
import { AdminMemberOperationsPanel } from '../components/admin/AdminMemberOperationsPanel'
import {
    formatAdminGrade,
    formatMemberRole,
} from '../components/admin/adminRolePresentation'
import type {
    AdminMember,
    AdminOrder,
    AdminSeller,
} from '../types/admin'
import { formatKoreanDateTime } from '../utils/dateTime'
import { getOrderItemFulfillmentStatusLabel, getOrderStatusLabel } from '../utils/order'
import { formatPrice } from '../utils/product'
import { adminResourceMeta, type AdminResource } from './adminResourceConfig'

type AdminResourceItem = AdminMember | AdminSeller | AdminOrder

export function AdminResourceDetailPage({ resource }: { resource: AdminResource }) {
    const location = useLocation()
    const { resourceId } = useParams()
    const id = Number(resourceId)
    const invalidId = !Number.isInteger(id) || id <= 0
    const [item, setItem] = useState<AdminResourceItem | null>(null)
    const [errorMessage, setErrorMessage] = useState('')
    const [isLoading, setIsLoading] = useState(true)
    const meta = adminResourceMeta[resource]
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

function loadDetail(
    resource: AdminResource,
    id: number,
    signal: AbortSignal,
): Promise<AdminResourceItem> {
    if (resource === 'members') return getAdminMember(id, signal)
    if (resource === 'sellers') return getAdminSeller(id, signal)
    return getAdminOrder(id, signal)
}

function detailTitle(resource: AdminResource, item: AdminResourceItem | null) {
    if (!item) return adminResourceMeta[resource].title
    if (resource === 'members') return (item as AdminMember).name
    if (resource === 'sellers') return (item as AdminSeller).storeName
    return `주문 #${(item as AdminOrder).orderId}`
}
