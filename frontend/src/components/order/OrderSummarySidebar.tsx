import { Link } from 'react-router-dom'
import type { Order } from '../../types/order'
import { formatPrice } from '../../utils/product'

interface OrderSummarySidebarProps {
    order: Order
    onOpenRefund: () => void
}

export function OrderSummarySidebar({ order, onOpenRefund }: OrderSummarySidebarProps) {
    const canPay = order.status === 'PENDING_PAYMENT' || order.status === 'PAYMENT_FAILED'

    return (
        <aside className="grid content-start gap-6">
            <section className="border border-line bg-surface p-5">
                <h2 className="text-sm font-extrabold">결제 정보</h2>
                <dl className="mt-5 grid gap-3 text-xs">
                    <div className="flex justify-between gap-4 text-muted">
                        <dt>상품금액</dt>
                        <dd className="text-ink">{formatPrice(order.productAmount)}</dd>
                    </div>
                    <div className="flex justify-between gap-4 text-muted">
                        <dt>배송비</dt>
                        <dd className="text-ink">{formatPrice(order.shippingFee)}</dd>
                    </div>
                    <div className="flex justify-between gap-4 border-t border-line pt-4 text-sm font-extrabold">
                        <dt>총 결제금액</dt>
                        <dd>{formatPrice(order.totalAmount)}</dd>
                    </div>
                </dl>
                <div className="mt-5 grid gap-2">
                    {canPay && (
                        <Link className="grid h-11 place-items-center bg-ink text-xs font-bold text-white" to={`/orders/${order.orderId}/payment`}>
                            결제 계속하기
                        </Link>
                    )}
                    {order.refundSupported
                        && order.status !== 'DELIVERED'
                        && order.status !== 'SHIPPED'
                        && (
                        <button className="h-11 border border-ink text-xs font-bold" type="button" onClick={onOpenRefund}>
                            환불 신청·내역
                        </button>
                    )}
                </div>
            </section>

            {order.deliveryAddress && (
                <section className="border border-line bg-surface p-5">
                    <h2 className="text-sm font-extrabold">배송지 정보</h2>
                    <dl className="mt-5 grid gap-3 text-xs leading-5">
                        <div>
                            <dt className="text-muted">받는 분</dt>
                            <dd className="mt-1">{order.deliveryAddress.recipientName}</dd>
                        </div>
                        <div>
                            <dt className="text-muted">연락처</dt>
                            <dd className="mt-1">{order.deliveryAddress.recipientPhone}</dd>
                        </div>
                        <div>
                            <dt className="text-muted">주소</dt>
                            <dd className="mt-1">
                                ({order.deliveryAddress.postalCode}) {order.deliveryAddress.roadAddress} {order.deliveryAddress.detailAddress}
                            </dd>
                        </div>
                    </dl>
                </section>
            )}
        </aside>
    )
}
