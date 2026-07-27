import { Heart } from 'lucide-react'
import { useEffect, useState } from 'react'

import { ApiError } from '../../api/client'
import {
    addWishlistProduct,
    getWishlistStatus,
    removeWishlistProduct,
} from '../../api/wishlist'

interface ProductWishlistButtonProps {
    productId: number
    isAuthenticated: boolean
    onLoginRequired: () => void
    onError: (message: string) => void
}

export function ProductWishlistButton({
    productId,
    isAuthenticated,
    onLoginRequired,
    onError,
}: ProductWishlistButtonProps) {
    const [status, setStatus] = useState<{
        productId: number
        wished: boolean
    } | null>(null)
    const [isUpdating, setIsUpdating] = useState(false)
    const isReady = !isAuthenticated || status?.productId === productId
    const isWished = isAuthenticated
        && status?.productId === productId
        && status.wished

    useEffect(() => {
        if (!isAuthenticated) {
            return
        }

        const controller = new AbortController()
        getWishlistStatus(productId, controller.signal)
            .then((response) => {
                setStatus(response)
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                onError(error instanceof ApiError
                    ? error.message
                    : '찜 상태를 불러오지 못했습니다.')
                setStatus({ productId, wished: false })
            })
        return () => controller.abort()
    }, [isAuthenticated, onError, productId])

    async function toggle() {
        if (!isAuthenticated) {
            onLoginRequired()
            return
        }
        if (!isReady || isUpdating) return

        setIsUpdating(true)
        onError('')
        try {
            if (isWished) {
                await removeWishlistProduct(productId)
                setStatus({ productId, wished: false })
            } else {
                await addWishlistProduct(productId)
                setStatus({ productId, wished: true })
            }
        } catch (error) {
            onError(error instanceof ApiError
                ? error.message
                : '찜 상태를 변경하지 못했습니다.')
        } finally {
            setIsUpdating(false)
        }
    }

    return (
        <button
            className={`h-13.5 border border-ink font-extrabold transition-colors disabled:opacity-50 ${
                isWished ? 'bg-ink text-surface' : 'bg-transparent text-ink'
            }`}
            type="button"
            aria-pressed={isWished}
            disabled={isAuthenticated && (!isReady || isUpdating)}
            onClick={() => void toggle()}
        >
            <Heart
                className="mr-1 inline size-4"
                fill={isWished ? 'currentColor' : 'none'}
                aria-hidden="true"
            />
            {isUpdating ? '처리 중...' : isWished ? '찜 해제' : '찜하기'}
        </button>
    )
}
