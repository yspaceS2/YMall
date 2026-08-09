import { ChevronLeft, ChevronRight, Search } from 'lucide-react'
import { useState, type FormEvent, type ReactNode } from 'react'
import { Link, useSearchParams } from 'react-router-dom'

export function ManagementPageHeader({
    eyebrow,
    title,
    description,
    action,
}: {
    eyebrow: string
    title: string
    description?: string
    action?: ReactNode
}) {
    return (
        <header className="mb-8 flex flex-wrap items-end justify-between gap-5">
            <div>
                <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-accent">
                    {eyebrow}
                </p>
                <h1 className="font-serif text-[clamp(36px,5vw,56px)] leading-none tracking-tighter">
                    {title}
                </h1>
                {description && <p className="mt-3 text-sm text-muted">{description}</p>}
            </div>
            {action}
        </header>
    )
}

export function ManagementListSearch({
    placeholder,
}: {
    placeholder: string
}) {
    const [searchParams, setSearchParams] = useSearchParams()
    const [value, setValue] = useState(searchParams.get('keyword') ?? '')

    function submit(event: FormEvent) {
        event.preventDefault()
        const next = new URLSearchParams(searchParams)
        const keyword = value.trim()
        if (keyword) next.set('keyword', keyword)
        else next.delete('keyword')
        next.set('page', '1')
        setSearchParams(next)
    }

    return (
        <form className="mb-6 flex max-w-170" onSubmit={submit} role="search">
            <label className="relative flex-1">
                <span className="sr-only">검색어</span>
                <Search
                    className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted"
                    aria-hidden="true"
                />
                <input
                    className="h-12 w-full border border-line bg-surface pl-11 pr-4 text-sm text-ink outline-0 focus:border-ink"
                    value={value}
                    onChange={(event) => setValue(event.target.value)}
                    placeholder={placeholder}
                />
            </label>
            <button className="h-12 bg-ink px-6 text-xs font-bold text-white" type="submit">
                검색
            </button>
        </form>
    )
}

export function ManagementPagination({
    page,
    totalPages,
}: {
    page: number
    totalPages: number
}) {
    const [searchParams] = useSearchParams()
    if (totalPages <= 1) return null

    const pageNumbers = Array.from(
        { length: Math.min(totalPages, 5) },
        (_, index) => {
            const start = Math.max(1, Math.min(page - 2, totalPages - 4))
            return start + index
        },
    )

    const pageLink = (target: number) => {
        const next = new URLSearchParams(searchParams)
        next.set('page', String(target))
        return `?${next.toString()}`
    }

    return (
        <nav className="mt-8 flex items-center justify-center gap-1" aria-label="페이지 이동">
            <PaginationLink
                to={pageLink(Math.max(1, page - 1))}
                disabled={page <= 1}
                label="이전 페이지"
            >
                <ChevronLeft className="size-4" />
            </PaginationLink>
            {pageNumbers.map((pageNumber) => (
                <Link
                    className={[
                        'grid size-10 place-items-center border text-xs font-bold',
                        pageNumber === page
                            ? 'border-ink bg-ink text-white'
                            : 'border-line bg-surface hover:border-ink',
                    ].join(' ')}
                    key={pageNumber}
                    to={pageLink(pageNumber)}
                    aria-current={pageNumber === page ? 'page' : undefined}
                >
                    {pageNumber}
                </Link>
            ))}
            <PaginationLink
                to={pageLink(Math.min(totalPages, page + 1))}
                disabled={page >= totalPages}
                label="다음 페이지"
            >
                <ChevronRight className="size-4" />
            </PaginationLink>
        </nav>
    )
}

function PaginationLink({
    to,
    disabled,
    label,
    children,
}: {
    to: string
    disabled: boolean
    label: string
    children: ReactNode
}) {
    return disabled ? (
        <span
            className="grid size-10 place-items-center border border-line text-muted opacity-40"
            aria-disabled="true"
            aria-label={label}
        >
            {children}
        </span>
    ) : (
        <Link
            className="grid size-10 place-items-center border border-line bg-surface hover:border-ink"
            to={to}
            aria-label={label}
        >
            {children}
        </Link>
    )
}

export function ManagementEmpty({ children }: { children: ReactNode }) {
    return (
        <div className="grid min-h-44 place-items-center border border-dashed border-line bg-surface p-6 text-sm text-muted">
            {children}
        </div>
    )
}

export const managementPageClassName =
    'mx-auto max-w-350 px-4 py-10 min-[601px]:px-8 min-[601px]:py-14'
