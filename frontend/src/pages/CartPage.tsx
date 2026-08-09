import { ChevronLeft, Minus, Plus, Trash2 } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { deleteCartItem, getCart, updateCartItemQuantity } from '../api/cart'
import { ApiError } from '../api/client'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { PageState } from '../components/ui/PageState'
import { StatusBadge } from '../components/ui/StatusBadge'
import type { CartItem } from '../types/cart'
import { formatPrice, getDiscountedPrice, resolveImageUrl } from '../utils/product'

export function CartPage() {
    const [items, setItems] = useState<CartItem[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [errorMessage, setErrorMessage] = useState('')
    const [pendingItemId, setPendingItemId] = useState<number | null>(null)

    useEffect(() => {
        const controller = new AbortController()

        getCart(controller.signal)
            .then((cart) => {
                setItems(cart.items)
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') {
                    return
                }
                setErrorMessage(
                    error instanceof ApiError
                        ? error.message
                        : '장바구니를 불러오지 못했습니다.',
                )
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setIsLoading(false)
                }
            })

        return () => controller.abort()
    }, [])

    async function retryCart() {
        setIsLoading(true)
        setErrorMessage('')
        try {
            const cart = await getCart()
            setItems(cart.items)
        } catch (error) {
            setErrorMessage(
                error instanceof ApiError ? error.message : '장바구니를 불러오지 못했습니다.',
            )
        } finally {
            setIsLoading(false)
        }
    }

    const totalQuantity = useMemo(
        () => items.reduce((total, item) => total + item.quantity, 0),
        [items],
    )
    const totalPrice = useMemo(
        () => items.reduce(
            (total, item) => total
                + getDiscountedPrice(item.price, item.discountPercentage) * item.quantity,
            0,
        ),
        [items],
    )
    const totalShippingFee = useMemo(
        () => items.reduce((total, item) => total + item.shippingFee, 0),
        [items],
    )
    const hasUnavailableItems = useMemo(
        () => items.some((item) => (
            item.productStatus !== 'APPROVED'
            || item.stock < item.quantity
        )),
        [items],
    )

    async function changeQuantity(item: CartItem, quantity: number) {
        if (quantity < 1 || quantity > item.stock || pendingItemId !== null) {
            return
        }

        setPendingItemId(item.cartItemId)
        setErrorMessage('')
        try {
            const updatedItem = await updateCartItemQuantity(item.cartItemId, { quantity })
            setItems((currentItems) => currentItems.map((currentItem) => (
                currentItem.cartItemId === updatedItem.cartItemId ? updatedItem : currentItem
            )))
        } catch (error) {
            setErrorMessage(
                error instanceof ApiError ? error.message : '수량을 변경하지 못했습니다.',
            )
        } finally {
            setPendingItemId(null)
        }
    }

    async function removeItem(cartItemId: number) {
        if (pendingItemId !== null) {
            return
        }

        setPendingItemId(cartItemId)
        setErrorMessage('')
        try {
            await deleteCartItem(cartItemId)
            setItems((currentItems) => currentItems.filter(
                (item) => item.cartItemId !== cartItemId,
            ))
        } catch (error) {
            setErrorMessage(
                error instanceof ApiError ? error.message : '상품을 삭제하지 못했습니다.',
            )
        } finally {
            setPendingItemId(null)
        }
    }

    if (isLoading) {
        return <PageState variant="loading" title="장바구니를 불러오는 중입니다" description="잠시만 기다려 주세요." />
    }

    if (errorMessage && items.length === 0) {
        return <PageState variant="error" title="장바구니를 불러오지 못했습니다" description={errorMessage} action={<button className="border border-ink bg-white px-5 py-2.5 text-xs font-bold" type="button" onClick={retryCart}>다시 시도</button>} />
    }

    return (
        <section className="mx-auto max-w-360 px-4 pt-12 pb-20 min-[601px]:px-[clamp(20px,5vw,72px)] min-[601px]:pt-18 min-[601px]:pb-27.5">
            <div className="mb-10.5 flex flex-col items-start gap-3.5 border-b border-ink pb-6 min-[601px]:flex-row min-[601px]:items-end min-[601px]:justify-between">
                <div>
                    <p className="mb-2.5 text-[11px] font-extrabold tracking-[.18em] text-accent">YOUR SHOPPING BAG</p>
                    <h1 className="m-0 font-serif text-[clamp(42px,5vw,66px)] leading-none font-medium tracking-[-.05em]">장바구니</h1>
                </div>
                <span className="text-[13px] text-muted">{totalQuantity}개 상품</span>
            </div>

            {errorMessage && <FeedbackMessage className="mb-4.5" tone="error">{errorMessage}</FeedbackMessage>}

            {items.length === 0 ? (
                <PageState variant="empty" title="장바구니가 비어 있습니다" description="마음에 드는 상품을 담아보세요." action={<Link className="flex items-center gap-1 border border-ink bg-white px-5 py-2.5 text-xs font-bold" to="/"><ChevronLeft className="size-4" /> 상품 둘러보기</Link>} />
            ) : (
                <div className="grid grid-cols-1 items-start gap-10 min-[1050px]:grid-cols-[minmax(0,1fr)_340px] min-[1050px]:gap-[clamp(40px,6vw,90px)]">
                    <div className="border-t border-line">
                        {items.map((item) => {
                            const discountedPrice = getDiscountedPrice(
                                item.price,
                                item.discountPercentage,
                            )
                            const isAvailable = item.productStatus === 'APPROVED' && item.stock > 0
                            const isPending = pendingItemId === item.cartItemId

                            return (
                                <article className="grid grid-cols-[92px_minmax(0,1fr)] gap-4 border-b border-line py-6 min-[601px]:grid-cols-[130px_minmax(0,1fr)_auto] min-[601px]:gap-6" key={item.cartItemId}>
                                    <Link className="aspect-[.82] overflow-hidden bg-subtle" to={`/products/${item.productId}`}>
                                        {item.thumbnailUrl ? (
                                            <img
                                                className="size-full object-cover"
                                                src={resolveImageUrl(item.thumbnailUrl)}
                                                alt={item.productName}
                                            />
                                        ) : (
                                            <span className="grid size-full place-items-center font-serif text-[13px] font-bold tracking-[.14em] text-muted">YMALL</span>
                                        )}
                                    </Link>
                                    <div className="flex min-w-0 flex-col justify-between">
                                        <div className="flex flex-col items-start gap-1.5">
                                            {!isAvailable && <StatusBadge tone="danger">구매 불가</StatusBadge>}
                                            <Link className="text-[15px] font-bold" to={`/products/${item.productId}`}>{item.productName}</Link>
                                            {item.discountPercentage > 0 && (
                                                <del className="text-[11px] text-muted">{formatPrice(item.price)}</del>
                                            )}
                                            <strong className="text-[13px]">{formatPrice(discountedPrice)}</strong>
                                        </div>
                                        <div className="flex flex-col items-start gap-2.5 min-[601px]:flex-row min-[601px]:items-center min-[601px]:gap-4.5">
                                            <div className="flex items-center border border-line" aria-label={`${item.productName} 수량`}>
                                                <button
                                                    className="grid size-8 place-items-center border-0 bg-transparent disabled:opacity-35"
                                                    type="button"
                                                    aria-label="수량 줄이기"
                                                    disabled={isPending || item.quantity <= 1}
                                                    onClick={() => changeQuantity(item, item.quantity - 1)}
                                                >
                                                    <Minus className="size-3.5" />
                                                </button>
                                                <b className="min-w-7.5 text-center text-xs">{item.quantity}</b>
                                                <button
                                                    className="grid size-8 place-items-center border-0 bg-transparent disabled:opacity-35"
                                                    type="button"
                                                    aria-label="수량 늘리기"
                                                    disabled={isPending || !isAvailable || item.quantity >= item.stock}
                                                    onClick={() => changeQuantity(item, item.quantity + 1)}
                                                >
                                                    <Plus className="size-3.5" />
                                                </button>
                                            </div>
                                            <button
                                                className="flex items-center gap-1 border-0 bg-transparent p-0 text-[11px] text-muted disabled:opacity-35"
                                                type="button"
                                                aria-label={`${item.productName} 삭제`}
                                                disabled={isPending}
                                                onClick={() => removeItem(item.cartItemId)}
                                            >
                                                <Trash2 className="size-3.5" /> 삭제
                                            </button>
                                        </div>
                                    </div>
                                    <strong className="col-start-2 justify-self-end self-center whitespace-nowrap min-[601px]:col-start-auto min-[601px]:text-base">
                                        {formatPrice(discountedPrice * item.quantity)}
                                    </strong>
                                </article>
                            )
                        })}
                    </div>

                    <aside className="top-25 border border-line bg-white p-5.5 min-[1050px]:sticky min-[1050px]:p-7">
                        <p className="mb-2.5 text-[11px] font-extrabold tracking-[.18em] text-accent">ORDER SUMMARY</p>
                        <dl className="my-6">
                            <div className="flex justify-between py-2 text-xs text-muted"><dt>상품 수량</dt><dd className="m-0 text-ink">{totalQuantity}개</dd></div>
                            <div className="flex justify-between py-2 text-xs text-muted"><dt>상품 금액</dt><dd className="m-0 text-ink">{formatPrice(totalPrice)}</dd></div>
                            <div className="flex justify-between py-2 text-xs text-muted"><dt>배송비</dt><dd className="m-0 text-ink">{totalShippingFee === 0 ? '무료' : formatPrice(totalShippingFee)}</dd></div>
                            <div className="mt-3.5 flex items-baseline justify-between border-t border-ink pt-5 text-xs font-extrabold">
                                <dt>결제 예정 금액</dt>
                                <dd className="m-0 text-xl">{formatPrice(totalPrice + totalShippingFee)}</dd>
                            </div>
                        </dl>
                        {hasUnavailableItems ? (
                            <>
                                <span className="grid h-13 cursor-not-allowed place-items-center bg-disabled text-[13px] font-extrabold text-muted" aria-disabled="true">
                                    주문서 작성
                                </span>
                                <p className="mt-2.5 text-[11px] leading-4 text-danger">
                                    구매 불가 상품을 삭제하거나 수량을 조정해 주세요.
                                </p>
                            </>
                        ) : (
                            <Link className="grid h-13 place-items-center bg-ink text-[13px] font-extrabold text-white" to="/checkout">주문서 작성</Link>
                        )}
                        <Link className="mt-3.5 block text-center text-[11px] text-muted underline" to="/">쇼핑 계속하기</Link>
                    </aside>
                </div>
            )}
        </section>
    )
}
