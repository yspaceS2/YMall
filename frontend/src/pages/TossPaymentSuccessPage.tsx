import { CircleAlert, LoaderCircle } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { confirmPayment, getOrder } from '../api/orders'
import { getConfirmationIdempotencyKey } from '../utils/tossPayments'

const pendingApprovals = new Map<string, Promise<void>>()

export function TossPaymentSuccessPage() {
    const { orderId: orderIdParam } = useParams()
    const [searchParams] = useSearchParams()
    const navigate = useNavigate()
    const orderId = Number(orderIdParam)
    const paymentKey = searchParams.get('paymentKey') ?? ''
    const paymentOrderId = searchParams.get('orderId') ?? ''
    const amount = Number(searchParams.get('amount'))
    const isValidCallback = Number.isInteger(orderId)
        && orderId > 0
        && paymentKey.length > 0
        && paymentOrderId.length > 0
        && Number.isInteger(amount)
        && amount > 0
    const [errorMessage, setErrorMessage] = useState('')
    const [retryCount, setRetryCount] = useState(0)

    useEffect(() => {
        if (!isValidCallback) return
        let isActive = true

        function getOrCreateApproval() {
            const existingApproval = pendingApprovals.get(paymentKey)
            if (existingApproval) return existingApproval

            const approval = approvePayment()
            pendingApprovals.set(paymentKey, approval)
            return approval
        }

        async function approvePayment() {
            const order = await getOrder(orderId)
            if (order.paymentOrderId !== paymentOrderId || order.totalAmount !== amount) {
                throw new Error('결제 정보가 주문 정보와 일치하지 않아 승인을 중단했습니다.')
            }

            await confirmPayment(orderId, {
                paymentKey,
                paymentOrderId,
                amount,
                idempotencyKey: getConfirmationIdempotencyKey(paymentKey),
            })
        }

        async function handleApproval() {
            try {
                await getOrCreateApproval()
                pendingApprovals.delete(paymentKey)
                if (isActive) {
                    navigate(`/orders/${orderId}/result`, { replace: true })
                }
            } catch (error) {
                pendingApprovals.delete(paymentKey)
                if (!isActive) return
                setErrorMessage(
                    error instanceof ApiError || error instanceof Error
                        ? error.message
                        : '결제 승인 중 오류가 발생했습니다.',
                )
            }
        }

        void handleApproval()
        return () => {
            isActive = false
        }
    }, [
        amount,
        isValidCallback,
        navigate,
        orderId,
        paymentKey,
        paymentOrderId,
        retryCount,
    ])

    function retry() {
        pendingApprovals.delete(paymentKey)
        setErrorMessage('')
        setRetryCount((current) => current + 1)
    }

    if (!isValidCallback) {
        return (
            <PaymentApprovalError
                message="결제 승인에 필요한 정보가 올바르지 않습니다."
                orderId={Number.isInteger(orderId) && orderId > 0 ? orderId : null}
            />
        )
    }

    if (errorMessage) {
        return <PaymentApprovalError message={errorMessage} orderId={orderId} onRetry={retry} />
    }

    return (
        <section className="mx-auto grid min-h-130 max-w-180 place-content-center justify-items-center px-4 py-16 text-center">
            <LoaderCircle className="mb-6 size-12 animate-spin text-[#71801e]" />
            <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">PAYMENT APPROVAL</p>
            <h1 className="font-serif text-[clamp(38px,7vw,64px)] leading-none tracking-tighter">결제를 승인하고 있습니다</h1>
            <p className="mt-6 text-sm leading-6 text-muted">창을 닫거나 새로고침하지 말고 잠시만 기다려 주세요.</p>
        </section>
    )
}

interface PaymentApprovalErrorProps {
    message: string
    orderId: number | null
    onRetry?: () => void
}

function PaymentApprovalError({ message, orderId, onRetry }: PaymentApprovalErrorProps) {
    return (
        <section className="mx-auto grid min-h-130 max-w-180 place-content-center justify-items-center px-4 py-16 text-center">
            <CircleAlert className="mb-6 size-12 text-[#b23b2f]" />
            <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-[#b23b2f]">APPROVAL FAILED</p>
            <h1 className="font-serif text-[clamp(38px,7vw,64px)] leading-none tracking-tighter">결제 승인에 실패했습니다</h1>
            <p className="mt-6 max-w-130 text-sm leading-6 text-[#b23b2f]" role="alert">{message}</p>
            <div className="mt-8 flex flex-wrap justify-center gap-3">
                {onRetry && (
                    <button className="border-0 bg-ink px-6 py-3 text-xs font-bold text-white" type="button" onClick={onRetry}>
                        승인 다시 시도
                    </button>
                )}
                {orderId !== null && (
                    <Link className="border border-ink px-6 py-3 text-xs font-bold" to={`/orders/${orderId}/payment`}>
                        결제 화면으로
                    </Link>
                )}
                <Link className="px-6 py-3 text-xs underline" to="/orders">주문 내역 보기</Link>
            </div>
        </section>
    )
}
