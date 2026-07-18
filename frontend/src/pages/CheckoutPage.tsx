import { ChevronLeft, LoaderCircle } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getCart } from '../api/cart'
import { ApiError } from '../api/client'
import { createOrder } from '../api/orders'
import { getMemberAddresses } from '../api/auth'
import type { CartItem } from '../types/cart'
import type { MemberAddress } from '../types/auth'
import { formatPrice, getDiscountedPrice } from '../utils/product'

export function CheckoutPage() {
    const [items, setItems] = useState<CartItem[]>([])
    const [addresses, setAddresses] = useState<MemberAddress[]>([])
    const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [errorMessage, setErrorMessage] = useState('')
    const orderKeyRef = useRef(crypto.randomUUID())
    const navigate = useNavigate()

    useEffect(() => {
        const controller = new AbortController()
        Promise.all([getCart(controller.signal), getMemberAddresses(controller.signal)])
            .then(([cart, memberAddresses]) => {
                setItems(cart.items)
                setAddresses(memberAddresses)
                setSelectedAddressId((memberAddresses.find((address) => address.isDefault) ?? memberAddresses[0])?.addressId ?? null)
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(error instanceof ApiError ? error.message : '장바구니를 불러오지 못했습니다.')
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [])

    const totalAmount = useMemo(
        () => items.reduce(
            (total, item) => total
                + getDiscountedPrice(item.price, item.discountPercentage) * item.quantity,
            0,
        ),
        [items],
    )

    async function submitOrder() {
        if (items.length === 0 || selectedAddressId === null || isSubmitting) return
        setIsSubmitting(true)
        setErrorMessage('')
        try {
            const order = await createOrder({ idempotencyKey: orderKeyRef.current, addressId: selectedAddressId })
            navigate(`/orders/${order.orderId}/payment`)
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '주문을 생성하지 못했습니다.')
        } finally {
            setIsSubmitting(false)
        }
    }

    if (isLoading) {
        return <div className="grid min-h-100 place-content-center text-sm text-muted">주문서를 준비하고 있습니다.</div>
    }

    return (
        <section className="mx-auto max-w-300 px-4 py-14 min-[601px]:px-8 min-[601px]:py-20">
            <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">CHECKOUT</p>
            <h1 className="mb-10 font-serif text-[clamp(42px,6vw,68px)] leading-none tracking-tighter">주문서</h1>

            {errorMessage && <p className="mb-5 text-sm text-[#b23b2f]" role="alert">{errorMessage}</p>}

            {items.length === 0 ? (
                <div className="border-y border-line py-16 text-center">
                    <strong>주문할 상품이 없습니다.</strong>
                    <Link className="mt-5 flex items-center justify-center gap-1 text-xs underline" to="/cart">
                        <ChevronLeft className="size-4" /> 장바구니로 돌아가기
                    </Link>
                </div>
            ) : (
                <div className="grid gap-10 min-[901px]:grid-cols-[minmax(0,1fr)_340px]">
                    <div>
                        <div className="mb-8 border-t border-ink">
                            <h2 className="border-b border-line py-4 font-serif text-2xl">배송지</h2>
                            {addresses.length === 0 ? (
                                <div className="border-b border-line py-6 text-sm">
                                    <p>주문 전에 배송지를 등록해 주세요.</p>
                                    <Link className="mt-3 inline-block text-xs underline" to="/mypage">배송지 등록하기</Link>
                                </div>
                            ) : addresses.map((address) => (
                                <label className="flex cursor-pointer gap-3 border-b border-line py-4" key={address.addressId}>
                                    <input type="radio" name="deliveryAddress" checked={selectedAddressId === address.addressId} onChange={() => setSelectedAddressId(address.addressId)} />
                                    <span>
                                        <strong className="text-sm">{address.addressName}{address.isDefault ? ' · 기본 배송지' : ''}</strong>
                                        <span className="mt-1 block text-xs">{address.recipientName} · {address.recipientPhone}</span>
                                        <span className="mt-1 block text-xs text-muted">[{address.postalCode}] {address.roadAddress} {address.detailAddress}</span>
                                    </span>
                                </label>
                            ))}
                        </div>
                        <div className="border-t border-ink">
                        {items.map((item) => {
                            const unitPrice = getDiscountedPrice(item.price, item.discountPercentage)
                            return (
                                <article className="flex items-center justify-between gap-5 border-b border-line py-5" key={item.cartItemId}>
                                    <div>
                                        <strong className="block text-sm">{item.productName}</strong>
                                        <span className="mt-1 block text-xs text-muted">{item.quantity}개 × {formatPrice(unitPrice)}</span>
                                    </div>
                                    <b className="text-sm">{formatPrice(unitPrice * item.quantity)}</b>
                                </article>
                            )
                        })}
                        </div>
                    </div>

                    <aside className="border border-line p-6">
                        <p className="text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">ORDER SUMMARY</p>
                        <div className="my-6 flex items-baseline justify-between border-b border-ink pb-5">
                            <span className="text-xs font-bold">결제 예정 금액</span>
                            <strong className="text-xl">{formatPrice(totalAmount)}</strong>
                        </div>
                        <button
                            className="grid h-13 w-full place-items-center border-0 bg-ink text-sm font-extrabold text-white disabled:bg-[#aaa]"
                            type="button"
                            disabled={isSubmitting || selectedAddressId === null}
                            onClick={submitOrder}
                        >
                            {isSubmitting ? <LoaderCircle className="size-5 animate-spin" /> : '주문 생성 후 결제하기'}
                        </button>
                        <p className="mt-3 text-[11px] leading-5 text-muted">현재 결제 단계는 포트폴리오 검증을 위한 모의 결제입니다.</p>
                    </aside>
                </div>
            )}
        </section>
    )
}
