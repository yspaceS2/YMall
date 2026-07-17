import { Check, LoaderCircle, PackageSearch, ReceiptText, Store, Users, X } from 'lucide-react'
import { useEffect, useState, type ReactNode } from 'react'
import {
    getAdminMembers,
    getAdminOrders,
    getAdminSellers,
    getPendingProducts,
    updateAdminProductStatus,
} from '../api/admin'
import { ApiError } from '../api/client'
import type { AdminMember, AdminOrder, AdminProduct, AdminSeller } from '../types/admin'
import { formatPrice } from '../utils/product'

export function AdminManagementPage() {
    const [products, setProducts] = useState<AdminProduct[]>([])
    const [members, setMembers] = useState<AdminMember[]>([])
    const [sellers, setSellers] = useState<AdminSeller[]>([])
    const [orders, setOrders] = useState<AdminOrder[]>([])
    const [totals, setTotals] = useState({ products: 0, members: 0, sellers: 0, orders: 0 })
    const [isLoading, setIsLoading] = useState(true)
    const [processingProductId, setProcessingProductId] = useState<number | null>(null)
    const [message, setMessage] = useState('')
    const [errorMessage, setErrorMessage] = useState('')

    useEffect(() => {
        const controller = new AbortController()
        Promise.all([
            getPendingProducts(controller.signal),
            getAdminMembers(controller.signal),
            getAdminSellers(controller.signal),
            getAdminOrders(controller.signal),
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
        setMessage('')
        setErrorMessage('')
        try {
            await updateAdminProductStatus(product.productId, status)
            setProducts((current) => current.filter((item) => item.productId !== product.productId))
            setTotals((current) => ({ ...current, products: Math.max(current.products - 1, 0) }))
            setMessage(`'${product.name}' 상품을 ${status === 'APPROVED' ? '승인' : '반려'}했습니다.`)
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '상품 상태를 변경하지 못했습니다.')
        } finally {
            setProcessingProductId(null)
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
                                        <button className="flex h-10 items-center gap-1.5 border border-[#a22e24] px-4 text-xs font-bold text-[#a22e24] disabled:opacity-50" type="button" disabled={processingProductId === product.productId} onClick={() => changeProductStatus(product, 'REJECTED')}>
                                            <X className="size-4" />반려
                                        </button>
                                    </div>
                                </article>
                            ))}
                        </div>
                    )}
                </Panel>

                <Panel icon={<Users />} title="회원">
                    <div className="overflow-x-auto"><table className="w-full min-w-150 text-left text-sm"><thead className="border-b border-ink text-xs"><tr><th className="p-3">이름</th><th className="p-3">이메일</th><th className="p-3">권한</th><th className="p-3">가입일</th></tr></thead><tbody>{members.map((member) => <tr className="border-b border-line" key={member.memberId}><td className="p-3 font-bold">{member.name}</td><td className="p-3">{member.email}</td><td className="p-3">{member.role}</td><td className="p-3">{formatDate(member.createdAt)}</td></tr>)}</tbody></table></div>
                </Panel>

                <Panel icon={<Store />} title="판매자">
                    <div className="grid gap-3">{sellers.length === 0 ? <Empty>등록된 판매자가 없습니다.</Empty> : sellers.map((seller) => <article className="border border-line p-4" key={seller.sellerProfileId}><strong>{seller.storeName}</strong><p className="mt-1 text-xs text-muted">{seller.memberName} · {seller.email} · 사업자번호 {seller.businessNumber}</p></article>)}</div>
                </Panel>

                <Panel icon={<ReceiptText />} title="주문">
                    <div className="grid gap-3">{orders.length === 0 ? <Empty>주문 내역이 없습니다.</Empty> : orders.map((order) => <article className="border border-line p-4" key={order.orderId}><div className="flex flex-wrap items-center justify-between gap-2"><strong>주문 #{order.orderId}</strong><span className="bg-[#eef0df] px-3 py-1 text-xs font-bold text-[#66751c]">{order.status}</span></div><p className="mt-2 text-xs text-muted">{order.memberName} · {order.memberEmail} · {formatDate(order.createdAt)}</p><p className="mt-3 text-sm">{order.items.map((item) => `${item.productName} × ${item.quantity}`).join(', ')}</p><p className="mt-2 font-bold">{formatPrice(order.totalAmount)}</p></article>)}</div>
                </Panel>
            </div>
        </section>
    )
}

function Summary({ label, value }: { label: string; value: number }) {
    return <div className="border border-line p-4"><p className="text-xs text-muted">{label}</p><strong className="mt-2 block text-3xl">{value}</strong></div>
}

function Panel({ icon, title, children }: { icon: ReactNode; title: string; children: ReactNode }) {
    return <section className="border-t-2 border-ink pt-5"><h2 className="mb-6 flex items-center gap-2 text-xl font-bold">{icon}{title}</h2>{children}</section>
}

function Empty({ children }: { children: ReactNode }) {
    return <p className="text-sm text-muted">{children}</p>
}

function formatDate(value: string) {
    return new Date(value).toLocaleString('ko-KR')
}
