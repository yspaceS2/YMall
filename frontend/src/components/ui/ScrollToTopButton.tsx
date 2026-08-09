import { ArrowUp } from 'lucide-react'
import { useEffect, useState } from 'react'

const SHOW_BUTTON_OFFSET = 320

export function ScrollToTopButton() {
    const [isVisible, setIsVisible] = useState(false)

    useEffect(() => {
        const updateVisibility = () => {
            setIsVisible(window.scrollY > SHOW_BUTTON_OFFSET)
        }

        updateVisibility()
        window.addEventListener('scroll', updateVisibility, { passive: true })

        return () => window.removeEventListener('scroll', updateVisibility)
    }, [])

    const scrollToTop = () => {
        window.scrollTo({ top: 0, behavior: 'smooth' })
    }

    return (
        <button
            className={`fixed right-5 bottom-5 z-40 inline-grid size-12 place-items-center rounded-full border border-line bg-surface text-ink shadow-[0_10px_30px_rgba(0,0,0,.16)] transition-[opacity,transform,visibility] duration-200 hover:-translate-y-1 hover:border-ink focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ink min-[601px]:right-8 min-[601px]:bottom-8 ${
                isVisible
                    ? 'visible translate-y-0 opacity-100'
                    : 'invisible translate-y-3 opacity-0'
            }`}
            type="button"
            aria-label="맨 위로 이동"
            onClick={scrollToTop}
        >
            <ArrowUp className="size-5" aria-hidden="true" />
        </button>
    )
}
