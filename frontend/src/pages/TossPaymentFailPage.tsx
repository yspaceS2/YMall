import { Ban, CircleX } from 'lucide-react'
import { Link, useParams, useSearchParams } from 'react-router-dom'

const USER_CANCELED_CODE = 'PAY_PROCESS_CANCELED'

export function TossPaymentFailPage() {
    const { orderId: orderIdParam } = useParams()
    const [searchParams] = useSearchParams()
    const orderId = Number(orderIdParam)
    const isValidOrderId = Number.isInteger(orderId) && orderId > 0
    const errorCode = searchParams.get('code') ?? ''
    const providerMessage = searchParams.get('message') ?? ''
    const isCanceled = errorCode === USER_CANCELED_CODE
    const Icon = isCanceled ? Ban : CircleX
    const title = isCanceled ? '결제를 취소했습니다' : '결제 인증에 실패했습니다'
    const message = isCanceled
        ? '주문은 결제 대기 상태로 유지됩니다. 원할 때 다시 결제할 수 있습니다.'
        : providerMessage || '결제 정보를 확인한 뒤 다시 시도해 주세요.'

    return (
        <section className="mx-auto grid min-h-130 max-w-180 place-content-center justify-items-center px-4 py-16 text-center">
            <Icon className={`mb-6 size-12 ${isCanceled ? 'text-muted' : 'text-danger'}`} />
            <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-danger">
                {isCanceled ? 'PAYMENT CANCELED' : 'PAYMENT FAILED'}
            </p>
            <h1 className="font-serif text-[clamp(38px,7vw,64px)] leading-none tracking-tighter">{title}</h1>
            <p className="mt-6 max-w-130 text-sm leading-6 text-muted">{message}</p>
            {errorCode && <p className="mt-2 text-[11px] text-muted">오류 코드: {errorCode}</p>}
            <div className="mt-8 flex flex-wrap justify-center gap-3">
                {isValidOrderId && (
                    <Link className="bg-ink px-6 py-3 text-xs font-bold text-white" to={`/orders/${orderId}/payment`}>
                        결제 다시 시도
                    </Link>
                )}
                <Link className="border border-ink px-6 py-3 text-xs font-bold" to="/orders">주문 내역 보기</Link>
                <Link className="px-6 py-3 text-xs underline" to="/">쇼핑 계속하기</Link>
            </div>
        </section>
    )
}
