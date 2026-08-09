import { useEffect, useState, type FocusEvent } from 'react'

const REDUCED_MOTION_QUERY = '(prefers-reduced-motion: reduce)'

function prefersReducedMotion() {
    return window.matchMedia?.(REDUCED_MOTION_QUERY).matches ?? false
}

export function useCarouselPause() {
    const [isReducedMotion, setIsReducedMotion] = useState(prefersReducedMotion)
    const [isUserPaused, setIsUserPaused] = useState(prefersReducedMotion)
    const [isHovered, setIsHovered] = useState(false)
    const [hasFocus, setHasFocus] = useState(false)

    useEffect(() => {
        const mediaQuery = window.matchMedia?.(REDUCED_MOTION_QUERY)
        if (!mediaQuery) {
            return
        }

        const handleChange = (event: MediaQueryListEvent) => {
            setIsReducedMotion(event.matches)
            if (event.matches) {
                setIsUserPaused(true)
            }
        }

        mediaQuery.addEventListener('change', handleChange)
        return () => mediaQuery.removeEventListener('change', handleChange)
    }, [])

    const handleBlurCapture = (event: FocusEvent<HTMLElement>) => {
        if (!event.currentTarget.contains(event.relatedTarget as Node | null)) {
            setHasFocus(false)
        }
    }

    return {
        isPaused: isUserPaused || isHovered || hasFocus,
        isReducedMotion,
        isUserPaused,
        toggleUserPaused: () => setIsUserPaused((paused) => !paused),
        interactionProps: {
            onMouseEnter: () => setIsHovered(true),
            onMouseLeave: () => setIsHovered(false),
            onFocusCapture: () => setHasFocus(true),
            onBlurCapture: handleBlurCapture,
        },
    }
}
