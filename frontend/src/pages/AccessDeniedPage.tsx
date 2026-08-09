import { ArrowLeft, ShieldX } from 'lucide-react'
import { Link, useLocation } from 'react-router-dom'

interface AccessDeniedLocationState {
    from?: string
}

export function AccessDeniedPage() {
    const location = useLocation()
    const state = location.state as AccessDeniedLocationState | null

    return (
        <section className="mx-auto grid min-h-[calc(100vh-76px)] w-[calc(100%-40px)] max-w-180 place-content-center py-16 text-center">
            <ShieldX className="mx-auto size-12 text-danger" aria-hidden="true" />
            <p className="mt-6 text-[11px] font-extrabold tracking-[.18em] text-danger">ACCESS DENIED</p>
            <h1 className="mt-3 font-serif text-[clamp(38px,6vw,60px)] leading-none tracking-[-.05em]">접근 권한이 없습니다.</h1>
            <p className="mx-auto mt-5 max-w-120 text-sm leading-7 text-muted">
                현재 계정으로는 요청한 화면을 이용할 수 없습니다.
                {state?.from && <span className="mt-1 block text-xs">요청 경로: {state.from}</span>}
            </p>
            <Link className="mx-auto mt-8 inline-flex h-12 items-center gap-2 bg-ink px-7 text-sm font-bold text-white" to="/">
                <ArrowLeft className="size-4" aria-hidden="true" />
                쇼핑 계속하기
            </Link>
        </section>
    )
}
