import { StatusBadge } from '../components/ui/StatusBadge'
import type { ProductQuestionStatus } from '../types/productQuestion'
import { resolveImageUrl } from '../utils/product'

export function ProductQuestionThumbnail({ name, url }: { name: string; url: string | null }) {
    return (
        <div className="size-14 shrink-0 overflow-hidden border border-line bg-surface">
            {url ? (
                <img
                    alt=""
                    className="size-full object-cover"
                    loading="lazy"
                    src={resolveImageUrl(url)}
                />
            ) : (
                <div className="grid size-full place-items-center text-[9px] font-bold tracking-[.12em] text-muted">
                    YMALL
                </div>
            )}
            <span className="sr-only">{name}</span>
        </div>
    )
}

export function ProductQuestionStatusBadge({ status }: { status: ProductQuestionStatus }) {
    return (
        <StatusBadge tone={status === 'ANSWERED' ? 'success' : 'warning'}>
            {status === 'ANSWERED' ? '답변 완료' : '답변 대기'}
        </StatusBadge>
    )
}
