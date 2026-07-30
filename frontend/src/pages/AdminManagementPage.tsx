import { LoaderCircle, PackageSearch, ReceiptText, Store, Users } from 'lucide-react'
import { useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import {
    getAdminMembers,
    getAdminOrders,
    getAdminRefunds,
    getAdminSellers,
    getPendingProducts,
    requestAdminRefund,
} from '../api/admin'
import { ApiError } from '../api/client'
import { RefundDialog } from '../components/RefundDialog'
import { AdminSettlementPanel } from '../components/admin/AdminSettlementPanel'
import type { AdminMember, AdminOrder, AdminProduct, AdminSeller } from '../types/admin'
import type { PaymentRefund, PaymentRefundRequest } from '../types/order'
import { formatKoreanDateTime } from '../utils/dateTime'
import { formatPrice } from '../utils/product'

type AdminSection = 'products' | 'members' | 'sellers' | 'orders'

export function AdminManagementPage() {
    const [products, setProducts] = useState<AdminProduct[]>([])
    const [members, setMembers] = useState<AdminMember[]>([])
    const [sellers, setSellers] = useState<AdminSeller[]>([])
    const [orders, setOrders] = useState<AdminOrder[]>([])
    const [totals, setTotals] = useState({ products: 0, members: 0, sellers: 0, orders: 0 })
    const [nextPages, setNextPages] = useState({ products: 2, members: 2, sellers: 2, orders: 2 })
    const [hasMore, setHasMore] = useState({ products: false, members: false, sellers: false, orders: false })
    const [isLoading, setIsLoading] = useState(true)
    const [loadingSection, setLoadingSection] = useState<AdminSection | null>(null)
    const [message, setMessage] = useState('')
    const [errorMessage, setErrorMessage] = useState('')
    const [refundOrder, setRefundOrder] = useState<AdminOrder | null>(null)
    const [refunds, setRefunds] = useState<PaymentRefund[]>([])
    const [isLoadingRefunds, setIsLoadingRefunds] = useState(false)
    const [isRefunding, setIsRefunding] = useState(false)
    const [refundError, setRefundError] = useState('')

    useEffect(() => {
        const controller = new AbortController()
        Promise.all([
            getPendingProducts({ signal: controller.signal }),
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

    async function loadMore(section: AdminSection) {
        if (!hasMore[section] || loadingSection !== null) return
        setLoadingSection(section)
        setErrorMessage('')
        try {
            const page = nextPages[section]
            if (section === 'products') {
                const response = await getPendingProducts({ page })
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
            setMessage('환불 요청이 처리되었습니다.')
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
        <section className="mx-auto max-w-300 px-4 py-12 min-[601px]:px-8 min-[601px]:py-18">
            <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">ADMIN CENTER</p>
            <h1 className="mb-8 font-serif text-[clamp(40px,6vw,64px)] leading-none tracking-tighter">관리자 운영</h1>
            {message && <p className="mb-5 border border-[#cad39b] bg-[#f4f6e8] p-3 text-sm">{message}</p>}
            {errorMessage && <p className="mb-5 border border-[#e2b9b4] bg-[#fff5f3] p-3 text-sm text-[#a22e24]" role="alert">{errorMessage}</p>}

            <div className="mb-10 grid gap-3 min-[601px]:grid-cols-2 min-[901px]:grid-cols-4">
                <Summary label="승인 대기" value={totals.products} />
                <Summary label="회원" value={totals.members} />
                <Summary label="판매자" value={totals.sellers} />
                <Summary label="주문" value={totals.orders} />
            </div>

            <div className="grid gap-10">
                <AdminSettlementPanel />
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
                                    <Link
                                        className="flex h-10 items-center bg-ink px-4 text-xs font-bold text-white"
                                        to={`/admin/products/${product.productId}`}
                                    >
                                        검수하기
                                    </Link>
                                </article>
                            ))}
                        </div>
                    )}
                    {hasMore.products && <LoadMoreButton loading={loadingSection === 'products'} onClick={() => loadMore('products')} />}
                </Panel>

                <Panel icon={<Users />} title="회원">
                    <div className="overflow-x-auto"><table className="w-full min-w-150 text-left text-sm"><thead className="border-b border-ink text-xs"><tr><th className="p-3">이름</th><th className="p-3">이메일</th><th className="p-3">권한</th><th className="p-3">가입일</th></tr></thead><tbody>{members.map((member) => <tr className="border-b border-line" key={member.memberId}><td className="p-3 font-bold">{member.name}</td><td className="p-3">{member.email}</td><td className="p-3">{member.role}</td><td className="p-3">{formatDate(member.createdAt)}</td></tr>)}</tbody></table></div>
                    {hasMore.members && <LoadMoreButton loading={loadingSection === 'members'} onClick={() => loadMore('members')} />}
                </Panel>

                <Panel icon={<Store />} title="판매자">
                    <div className="grid gap-3">{sellers.length === 0 ? <Empty>등록된 판매자가 없습니다.</Empty> : sellers.map((seller) => <article className="border border-line p-4" key={seller.sellerProfileId}><strong>{seller.storeName}</strong><p className="mt-1 text-xs text-muted">{seller.memberName} · {seller.email} · 사업자번호 {seller.businessNumber}</p></article>)}</div>
                    {hasMore.sellers && <LoadMoreButton loading={loadingSection === 'sellers'} onClick={() => loadMore('sellers')} />}
                </Panel>

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
                                        <span className="bg-[#eef0df] px-3 py-1 text-xs font-bold text-[#66751c]">
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
