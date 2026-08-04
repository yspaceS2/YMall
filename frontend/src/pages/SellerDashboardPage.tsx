import { LoaderCircle } from 'lucide-react'
import { useEffect, useState } from 'react'
import { ApiError } from '../api/client'
import { getSellerProfile } from '../api/seller'
import { SellerDashboard } from '../components/dashboard/SellerDashboard'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'

export function SellerDashboardPage() {
    const [hasProfile, setHasProfile] = useState(false)
    const [isLoading, setIsLoading] = useState(true)
    const [errorMessage, setErrorMessage] = useState('')

    useEffect(() => {
        const controller = new AbortController()
        void getSellerProfile(controller.signal).then(() => {
            setHasProfile(true)
        }).catch((error: unknown) => {
            if (error instanceof Error && error.name === 'AbortError') return
            if (error instanceof ApiError && error.code === 'SELLER_PROFILE_NOT_FOUND') return
            setErrorMessage(error instanceof ApiError ? error.message : '판매자 정보를 불러오지 못했습니다.')
        }).finally(() => {
            if (!controller.signal.aborted) setIsLoading(false)
        })
        return () => controller.abort()
    }, [])

    if (isLoading) {
        return <div className="grid min-h-100 place-content-center"><LoaderCircle className="size-6 animate-spin" /></div>
    }

    return (
        <section className="mx-auto max-w-350 px-4 py-3 min-[601px]:px-8" id="management-overview">
            {errorMessage
                ? <FeedbackMessage tone="error">{errorMessage}</FeedbackMessage>
                : hasProfile
                    ? <SellerDashboard />
                    : <FeedbackMessage tone="error">판매자 정보를 등록하면 통계를 확인할 수 있습니다.</FeedbackMessage>}
        </section>
    )
}
