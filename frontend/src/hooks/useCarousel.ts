import { useCallback, useEffect, useRef, useState, type TouchEvent } from 'react'
import { useCarouselPause } from './useCarouselPause'

export type CarouselDirection = 'forward' | 'backward'

interface UseCarouselOptions {
    slideCount: number
    intervalMs: number
}

const SWIPE_THRESHOLD_PX = 48

export function useCarousel({ slideCount, intervalMs }: UseCarouselOptions) {
    const [activeIndex, setActiveIndex] = useState(0)
    const [previousIndex, setPreviousIndex] = useState<number | null>(null)
    const [direction, setDirection] = useState<CarouselDirection>('forward')
    const touchStartX = useRef<number | null>(null)
    const { isPaused, isReducedMotion, isUserPaused, toggleUserPaused, interactionProps } = useCarouselPause()
    const canNavigate = slideCount > 1
    const safeActiveIndex = slideCount > 0 ? activeIndex % slideCount : 0

    const moveTo = useCallback((nextIndex: number, requestedDirection: CarouselDirection) => {
        if (!canNavigate) {
            return
        }

        const normalizedIndex = (nextIndex + slideCount) % slideCount
        if (normalizedIndex === safeActiveIndex) {
            return
        }

        setPreviousIndex(isReducedMotion ? null : safeActiveIndex)
        setDirection(requestedDirection)
        setActiveIndex(normalizedIndex)
    }, [canNavigate, isReducedMotion, safeActiveIndex, slideCount])

    const showPrevious = useCallback(() => {
        moveTo(safeActiveIndex - 1, 'backward')
    }, [moveTo, safeActiveIndex])

    const showNext = useCallback(() => {
        moveTo(safeActiveIndex + 1, 'forward')
    }, [moveTo, safeActiveIndex])

    const showSlide = useCallback((index: number) => {
        moveTo(index, index < safeActiveIndex ? 'backward' : 'forward')
    }, [moveTo, safeActiveIndex])

    useEffect(() => {
        if (isPaused || !canNavigate) {
            return
        }

        const timeoutId = window.setTimeout(showNext, intervalMs)
        return () => window.clearTimeout(timeoutId)
    }, [canNavigate, intervalMs, isPaused, safeActiveIndex, showNext])

    const handleTouchStart = (event: TouchEvent<HTMLElement>) => {
        touchStartX.current = event.touches[0]?.clientX ?? null
    }

    const handleTouchEnd = (event: TouchEvent<HTMLElement>) => {
        if (touchStartX.current === null) {
            return
        }

        const endX = event.changedTouches[0]?.clientX ?? touchStartX.current
        const distance = endX - touchStartX.current
        touchStartX.current = null

        if (Math.abs(distance) < SWIPE_THRESHOLD_PX) {
            return
        }

        if (distance < 0) {
            showNext()
        } else {
            showPrevious()
        }
    }

    return {
        activeIndex: safeActiveIndex,
        previousIndex,
        direction,
        canNavigate,
        isReducedMotion,
        isUserPaused,
        showPrevious,
        showNext,
        showSlide,
        toggleUserPaused,
        finishTransition: () => setPreviousIndex(null),
        interactionProps: {
            ...interactionProps,
            onTouchStart: handleTouchStart,
            onTouchEnd: handleTouchEnd,
        },
    }
}

interface CarouselSlideMotionOptions {
    index: number
    activeIndex: number
    previousIndex: number | null
    direction: CarouselDirection
    isReducedMotion: boolean
}

export function getCarouselSlideMotionClass({
    index,
    activeIndex,
    previousIndex,
    direction,
    isReducedMotion,
}: CarouselSlideMotionOptions) {
    if (index === activeIndex) {
        if (previousIndex === null || isReducedMotion) {
            return 'relative'
        }
        return direction === 'forward'
            ? 'relative carousel-slide-in-forward'
            : 'relative carousel-slide-in-backward'
    }

    if (index === previousIndex && !isReducedMotion) {
        return direction === 'forward'
            ? 'absolute inset-0 carousel-slide-out-forward'
            : 'absolute inset-0 carousel-slide-out-backward'
    }

    return 'hidden'
}
