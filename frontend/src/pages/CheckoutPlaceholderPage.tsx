import { ChevronLeft } from 'lucide-react'
import { Link } from 'react-router-dom'

export function CheckoutPlaceholderPage() {
    return (
        <section className="grid min-h-[calc(100vh-76px)] place-content-center justify-items-center px-6 py-20 text-center">
            <p className="mb-2.5 text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">NEXT STEP</p>
            <h1 className="m-0 font-serif text-[clamp(36px,5vw,64px)] leading-none font-medium tracking-[-.05em]">주문 기능을 준비하고 있습니다.</h1>
            <span className="my-6 max-w-140 leading-7 text-muted">장바구니의 주문 이동 흐름까지 연결됐습니다. 주문서와 결제는 다음 작업에서 이어집니다.</span>
            <Link className="flex items-center gap-1 text-xs underline" to="/cart"><ChevronLeft className="size-4" /> 장바구니로 돌아가기</Link>
        </section>
    )
}
