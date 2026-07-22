import type { ReactNode } from 'react'

interface AuthPageLayoutProps {
    eyebrow: string
    title: ReactNode
    description?: ReactNode
    children: ReactNode
    asideEyebrow: string
    asideTitle: ReactNode
    asideDescription?: ReactNode
    asideClassName?: string
    contentClassName?: string
}

export function AuthPageLayout({
    eyebrow,
    title,
    description,
    children,
    asideEyebrow,
    asideTitle,
    asideDescription,
    asideClassName = 'bg-[radial-gradient(circle_at_75%_22%,rgba(217,255,67,.95),transparent_21%),linear-gradient(145deg,#d9ddc8,#f1f0e8_58%,#c8cfab)]',
    contentClassName = 'max-w-125',
}: AuthPageLayoutProps) {
    return (
        <section className="grid min-h-[calc(100vh-76px)] grid-cols-1 min-[901px]:grid-cols-[minmax(0,1fr)_minmax(390px,1fr)]">
            <div className={`mx-auto w-[calc(100%-40px)] py-14 min-[601px]:w-[calc(100%-48px)] min-[601px]:py-20 ${contentClassName}`}>
                <p className="mb-4.5 text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">{eyebrow}</p>
                <h1 className="m-0 font-serif text-[clamp(38px,5vw,62px)] leading-[1.02] font-medium tracking-[-.05em]">{title}</h1>
                {description && <p className="mt-5 mb-10 text-sm leading-7 text-muted">{description}</p>}
                {children}
            </div>
            <aside className={`flex min-h-72 flex-col justify-end overflow-hidden p-5 text-ink min-[601px]:min-h-90 min-[601px]:p-[clamp(40px,7vw,100px)] ${asideClassName}`} aria-hidden="true">
                <span className="mb-4.5 text-[11px] font-extrabold tracking-[.2em]">{asideEyebrow}</span>
                <strong className="font-serif text-[clamp(46px,6vw,86px)] leading-[.9] font-medium tracking-[-.06em]">{asideTitle}</strong>
                {asideDescription && <p className="mt-8 max-w-80 text-sm leading-7 opacity-75">{asideDescription}</p>}
            </aside>
        </section>
    )
}
