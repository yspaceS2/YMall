import { ChevronLeft, LoaderCircle } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getCart } from '../api/cart'
import { ApiError } from '../api/client'
import { createOrder } from '../api/orders'
import type { CartItem } from '../types/cart'
import { formatPrice, getDiscountedPrice } from '../utils/product'

export function CheckoutPage() {
    const [items, setItems] = useState<CartItem[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [errorMessage, setErrorMessage] = useState('')
    const orderKeyRef = useRef(crypto.randomUUID())
    const navigate = useNavigate()

    useEffect(() => {
        const controller = new AbortController()
        getCart(controller.signal)
            .then((cart) => setItems(cart.items))
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
        if (items.length === 0 || isSubmitting) return
        setIsSubmitting(true)
        setErrorMessage('')
        try {
            const order = await createOrder({ idempotencyKey: orderKeyRef.current })
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

                    <aside className="border border-line p-6">
                        <p className="text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">ORDER SUMMARY</p>
                        <div className="my-6 flex items-baseline justify-between border-b border-ink pb-5">
                            <span className="text-xs font-bold">결제 예정 금액</span>
                            <strong className="text-xl">{formatPrice(totalAmount)}</strong>
                        </div>
                        <button
                            className="grid h-13 w-full place-items-center border-0 bg-ink text-sm font-extrabold text-white disabled:bg-[#aaa]"
                            type="button"
                            disabled={isSubmitting}
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
