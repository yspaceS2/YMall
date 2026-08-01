import { Check, LoaderCircle, PackageSearch, ReceiptText, Store, Users, X } from 'lucide-react'
import { useEffect, useState, type ReactNode } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
    getAdminMembers,
    getAdminOrders,
    getAdminRefunds,
    getAdminSellers,
    getAdminProducts,
    requestAdminRefund,
    updateAdminProductStatus,
} from '../api/admin'
import { ApiError } from '../api/client'
import { RefundDialog } from '../components/RefundDialog'
import { AdminSettlementPanel } from '../components/admin/AdminSettlementPanel'
import { AdminSellerApplicationPanel } from '../components/admin/AdminSellerApplicationPanel'
import type { AdminMember, AdminOrder, AdminProduct, AdminSeller } from '../types/admin'
import type { PaymentRefund, PaymentRefundRequest } from '../types/order'
import { formatKoreanDateTime } from '../utils/dateTime'
import { formatPrice } from '../utils/product'
import { useToast } from '../toast/useToast'

type AdminSection = 'products' | 'members' | 'sellers' | 'orders'
type AdminView = 'dashboard' | 'settlement' | 'seller-applications' | AdminSection

const adminViews: AdminView[] = [
    'dashboard',
    'settlement',
    'products',
    'members',
    'sellers',
    'seller-applications',
    'orders',
]

export function AdminManagementPage({ section }: { section?: AdminView } = {}) {
    const { showToast } = useToast()
    const [searchParams] = useSearchParams()
    const requestedSection = searchParams.get('section')
    const activeSection = section ?? (adminViews.includes(requestedSection as AdminView)
        ? requestedSection as AdminView
        : 'dashboard')
    const [products, setProducts] = useState<AdminProduct[]>([])
    const [members, setMembers] = useState<AdminMember[]>([])
    const [sellers, setSellers] = useState<AdminSeller[]>([])
    const [orders, setOrders] = useState<AdminOrder[]>([])
    const [totals, setTotals] = useState({ products: 0, members: 0, sellers: 0, orders: 0 })
    const [nextPages, setNextPages] = useState({ products: 2, members: 2, sellers: 2, orders: 2 })
    const [hasMore, setHasMore] = useState({ products: false, members: false, sellers: false, orders: false })
    const [isLoading, setIsLoading] = useState(true)
    const [loadingSection, setLoadingSection] = useState<AdminSection | null>(null)
    const [processingProductId, setProcessingProductId] = useState<number | null>(null)
    const [errorMessage, setErrorMessage] = useState('')
    const [refundOrder, setRefundOrder] = useState<AdminOrder | null>(null)
    const [refunds, setRefunds] = useState<PaymentRefund[]>([])
    const [isLoadingRefunds, setIsLoadingRefunds] = useState(false)
    const [isRefunding, setIsRefunding] = useState(false)
    const [refundError, setRefundError] = useState('')

    useEffect(() => {
        const controller = new AbortController()
        Promise.all([
            getAdminProducts({ status: 'PENDING', signal: controller.signal }),
            getAdminMembers({ signal: controller.signal }),
            getAdminSellers({ signal: controller.signal }),
            getAdminOrders({ signal: controller.signal }),
        ]).then(([productPage, memberPage, sellerPage, orderPage]) => {
            setProducts(productPage.content)
            setMembers(memberPage.content)
            setSellers(sellerPage.content)
            setOrders(orderPage.content)
            setTotals({
                products: productPage.totalElements,
                members: memberPage.totalElements,
                sellers: sellerPage.totalElements,
                orders: orderPage.totalElements,
            })
            setHasMore({
                products: productPage.hasNext,
                members: memberPage.hasNext,
                sellers: sellerPage.hasNext,
                orders: orderPage.hasNext,
            })
        }).catch((error: unknown) => {
            if (error instanceof Error && error.name === 'AbortError') return
            setErrorMessage(error instanceof ApiError ? error.message : '관리자 운영 정보를 불러오지 못했습니다.')
        }).finally(() => {
            if (!controller.signal.aborted) setIsLoading(false)
        })

        return () => controller.abort()
    }, [])

    async function changeProductStatus(
        product: AdminProduct,
        status: 'APPROVED' | 'REJECTED',
    ) {
        setProcessingProductId(product.productId)
        setErrorMessage('')
        try {
            await updateAdminProductStatus(product.productId, status)
            const productPage = await getAdminProducts({ status: 'PENDING' })
            setProducts(productPage.content)
            setTotals((current) => ({ ...current, products: productPage.totalElements }))
            setHasMore((current) => ({ ...current, products: productPage.hasNext }))
            setNextPages((current) => ({ ...current, products: 2 }))
            showToast(
                `'${product.name}' 상품을 ${status === 'APPROVED' ? '승인' : '반려'}했습니다.`,
                'success',
            )
        } catch (error) {
            showToast(
                error instanceof ApiError ? error.message : '상품 상태를 변경하지 못했습니다.',
                'error',
            )
        } finally {
            setProcessingProductId(null)
        }
    }

    async function loadMore(section: AdminSection) {
        if (!hasMore[section] || loadingSection !== null) return
        setLoadingSection(section)
        setErrorMessage('')
        try {
            const page = nextPages[section]
            if (section === 'products') {
                const response = await getAdminProducts({ page, status: 'PENDING' })
                setProducts((current) => [...current, ...response.content])
                setHasMore((current) => ({ ...current, products: response.hasNext }))
            } else if (section === 'members') {
                const response = await getAdminMembers({ page })
                setMembers((current) => [...current, ...response.content])
                setHasMore((current) => ({ ...current, members: response.hasNext }))
            } else if (section === 'sellers') {
                const response = await getAdminSellers({ page })
                setSellers((current) => [...current, ...response.content])
                setHasMore((current) => ({ ...current, sellers: response.hasNext }))
            } else {
                const response = await getAdminOrders({ page })
                setOrders((current) => [...current, ...response.content])
                setHasMore((current) => ({ ...current, orders: response.hasNext }))
            }
            setNextPages((current) => ({ ...current, [section]: current[section] + 1 }))
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '목록을 추가로 불러오지 못했습니다.')
        } finally {
            setLoadingSection(null)
        }
    }

    async function openRefundDialog(order: AdminOrder) {
        setRefundOrder(order)
        setRefunds([])
        setRefundError('')
        setIsLoadingRefunds(true)
        try {
            setRefunds(await getAdminRefunds(order.orderId))
        } catch (error) {
            setRefundError(error instanceof ApiError
                ? error.message
                : '환불 내역을 불러오지 못했습니다.')
        } finally {
            setIsLoadingRefunds(false)
        }
    }

    async function submitRefund(request: PaymentRefundRequest) {
        if (!refundOrder) return false
        setRefundError('')
        setIsRefunding(true)
        try {
            await requestAdminRefund(refundOrder.orderId, request)
            const [refundHistory, orderPage] = await Promise.all([
                getAdminRefunds(refundOrder.orderId),
                getAdminOrders(),
            ])
            setRefunds(refundHistory)
            setOrders(orderPage.content)
            setRefundOrder(
                orderPage.content.find((order) =>
                    order.orderId === refundOrder.orderId
                ) ?? refundOrder,
            )
            showToast('환불 요청이 처리되었습니다.', 'success')
            return true
        } catch (error) {
            setRefundError(error instanceof ApiError
                ? error.message
                : '환불 요청을 처리하지 못했습니다.')
            return false
        } finally {
            setIsRefunding(false)
        }
    }

    if (isLoading) {
        return <div className="grid min-h-100 place-content-center"><LoaderCircle className="size-6 animate-spin" /></div>
    }

    return (
        <section
            className="mx-auto max-w-350 px-4 py-10 min-[601px]:px-8 min-[601px]:py-14"
            id="management-overview"
        >
            <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-[#71801e] dark:text-[#c9db72]">ADMIN CENTER</p>
            <h1 className="mb-8 font-serif text-[clamp(40px,6vw,64px)] leading-none tracking-tighter">관리자 운영</h1>
            {errorMessage && <p className="mb-5 border border-[#e2b9b4] bg-[#fff5f3] p-3 text-sm text-[#a22e24] dark:border-[#7d4039] dark:bg-[#351915] dark:text-[#ffb7ae]" role="alert">{errorMessage}</p>}

            <div className="grid gap-10">
                {activeSection === 'dashboard' && (
                    <>
                        <div className="grid gap-3 min-[601px]:grid-cols-2 min-[901px]:grid-cols-4">
                            <Summary label="승인 대기" value={totals.products} />
                            <Summary label="회원" value={totals.members} />
                            <Summary label="판매자" value={totals.sellers} />
                            <Summary label="주문" value={totals.orders} />
                        </div>
                        <section className="grid min-h-72 place-content-center border border-dashed border-line bg-surface px-6 text-center">
                            <p className="text-xs font-extrabold tracking-[.16em] text-muted">
                                DASHBOARD VISUALIZATION
                            </p>
                            <h2 className="mt-3 text-xl font-bold">운영 지표를 보여줄 영역</h2>
                            <p className="mt-2 text-sm text-muted">
                                매출, 주문, 회원 추이의 집계 기준을 정한 뒤 그래프를 연결합니다.
                            </p>
                        </section>
                    </>
                )}

                {activeSection === 'settlement' && (
                <div>
                    <AdminSettlementPanel />
                </div>
                )}

                {activeSection === 'products' && (
                <Panel icon={<PackageSearch />} title="상품 승인 대기">
                    {products.length === 0 ? (
                        <Empty>승인을 기다리는 상품이 없습니다.</Empty>
                    ) : (
                        <div className="grid gap-3">
                            {products.map((product) => (
                                <article className="flex flex-wrap items-center justify-between gap-4 border border-line p-4" key={product.productId}>
                                    <div>
                                        <strong>{product.name}</strong>
                                        <p className="mt-1 text-xs text-muted">
                                            {product.storeName ?? '관리자 등록'} · {product.categoryName} · {formatPrice(product.price)} · 재고 {product.stock}
                                        </p>
                                    </div>
                                    <div className="flex gap-2">
                                        <button className="flex h-10 items-center gap-1.5 bg-ink px-4 text-xs font-bold text-white disabled:opacity-50" type="button" disabled={processingProductId === product.productId} onClick={() => changeProductStatus(product, 'APPROVED')}>
                                            <Check className="size-4" />승인
                                        </button>
                                        <button className="flex h-10 items-center gap-1.5 border border-[#a22e24] px-4 text-xs font-bold text-[#a22e24] dark:border-[#ff8e84] dark:text-[#ffb7ae] disabled:opacity-50" type="button" disabled={processingProductId === product.productId} onClick={() => changeProductStatus(product, 'REJECTED')}>
                                            <X className="size-4" />반려
                                        </button>
                                    </div>
                                </article>
                            ))}
                        </div>
                    )}
                    {hasMore.products && <LoadMoreButton loading={loadingSection === 'products'} onClick={() => loadMore('products')} />}
                </Panel>
                )}

                {activeSection === 'members' && (
                <Panel icon={<Users />} title="회원">
                    <div className="overflow-x-auto"><table className="w-full min-w-150 text-left text-sm"><thead className="border-b border-ink text-xs"><tr><th className="p-3">이름</th><th className="p-3">이메일</th><th className="p-3">권한</th><th className="p-3">가입일</th></tr></thead><tbody>{members.map((member) => <tr className="border-b border-line" key={member.memberId}><td className="p-3 font-bold">{member.name}</td><td className="p-3">{member.email}</td><td className="p-3">{member.role}</td><td className="p-3">{formatDate(member.createdAt)}</td></tr>)}</tbody></table></div>
                    {hasMore.members && <LoadMoreButton loading={loadingSection === 'members'} onClick={() => loadMore('members')} />}
                </Panel>
                )}

                {activeSection === 'sellers' && (
                <Panel icon={<Store />} title="판매자">
                    <div className="grid gap-3">{sellers.length === 0 ? <Empty>등록된 판매자가 없습니다.</Empty> : sellers.map((seller) => <article className="border border-line p-4" key={seller.sellerProfileId}><strong>{seller.storeName}</strong><p className="mt-1 text-xs text-muted">{seller.memberName} · {seller.email} · 사업자번호 {seller.businessNumber}</p></article>)}</div>
                    {hasMore.sellers && <LoadMoreButton loading={loadingSection === 'sellers'} onClick={() => loadMore('sellers')} />}
                </Panel>
                )}

                {activeSection === 'seller-applications' && (
                    <AdminSellerApplicationPanel />
                )}

                {activeSection === 'orders' && (
                <Panel icon={<ReceiptText />} title="주문">
                    <div className="grid gap-3">
                        {orders.length === 0 ? (
                            <Empty>주문 내역이 없습니다.</Empty>
                        ) : orders.map((order) => {
                            const canOpenRefund = order.refundSupported
                                && (order.status === 'PAID'
                                    || order.status === 'PARTIALLY_REFUNDED'
                                    || order.status === 'REFUNDED')
                            return (
                                <article className="border border-line p-4" key={order.orderId}>
                                    <div className="flex flex-wrap items-center justify-between gap-2">
                                        <strong>주문 #{order.orderId}</strong>
                                        <span className="bg-[#eef0df] px-3 py-1 text-xs font-bold text-[#66751c] dark:bg-[#29301f] dark:text-[#d3e78a]">
                                            {order.status}
                                        </span>
                                    </div>
                                    <p className="mt-2 text-xs text-muted">
                                        {order.memberName} · {order.memberEmail}
                                        {' · '}{formatDate(order.createdAt)}
                                    </p>
                                    <ul className="mt-3 grid gap-1 text-sm">
                                        {order.items.map((item) => (
                                            <li key={item.orderItemId}>
                                                {item.productName} × {item.quantity}
                                                {item.refundedQuantity > 0
                                                    ? ` · 환불 ${item.refundedQuantity}개`
                                                    : ''}
                                            </li>
                                        ))}
                                    </ul>
                                    <div className="mt-3 flex items-center justify-between gap-3">
                                        <b>{formatPrice(order.totalAmount)}</b>
                                        {canOpenRefund && (
                                            <button
                                                className="h-10 border border-ink px-4 text-xs font-bold"
                                                type="button"
                                                onClick={() => void openRefundDialog(order)}
                                            >
                                                환불 처리·내역
                                            </button>
                                        )}
                                    </div>
                                </article>
                            )
                        })}
                    </div>
                    {hasMore.orders && <LoadMoreButton loading={loadingSection === 'orders'} onClick={() => loadMore('orders')} />}
                </Panel>
                )}
            </div>
            <RefundDialog
                key={refundOrder?.orderId ?? 'closed'}
                open={refundOrder !== null}
                orderId={refundOrder?.orderId ?? null}
                items={refundOrder?.items ?? []}
                refunds={refunds}
                isLoadingHistory={isLoadingRefunds}
                isSubmitting={isRefunding}
                errorMessage={refundError}
                onClose={() => {
                    if (!isRefunding) setRefundOrder(null)
                }}
                onSubmit={submitRefund}
            />
        </section>
    )
}

function Summary({ label, value }: { label: string; value: number }) {
    return <div className="border border-line p-4"><p className="text-xs text-muted">{label}</p><strong className="mt-2 block text-3xl">{value}</strong></div>
}

function Panel({ icon, title, children }: { icon: ReactNode; title: string; children: ReactNode }) {
    return <section className="min-w-0 border-t-2 border-ink pt-5"><h2 className="mb-6 flex items-center gap-2 text-xl font-bold">{icon}{title}</h2>{children}</section>
}

function Empty({ children }: { children: ReactNode }) {
    return <p className="text-sm text-muted">{children}</p>
}

function LoadMoreButton({ loading, onClick }: { loading: boolean; onClick: () => void }) {
    return (
        <button className="mx-auto mt-5 grid h-10 min-w-32 place-items-center border border-ink px-5 text-xs font-bold disabled:opacity-50" type="button" disabled={loading} onClick={onClick}>
            {loading ? <LoaderCircle className="size-4 animate-spin" /> : '더 보기'}
        </button>
    )
}

function formatDate(value: string) {
    return formatKoreanDateTime(value)
}
