import { LoaderCircle } from 'lucide-react'

export function RouteLoadingFallback() {
    return (
        <div className="grid min-h-72 place-items-center" role="status">
            <LoaderCircle className="size-6 animate-spin" aria-hidden="true" />
            <span className="sr-only">페이지를 불러오는 중</span>
        </div>
    )
}
