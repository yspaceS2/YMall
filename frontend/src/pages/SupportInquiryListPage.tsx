import { ArrowLeft, LoaderCircle, Plus } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import {
    createSupportInquiry,
    getSupportInquiries,
} from '../api/support'
import { useAuth } from '../auth/useAuth'
import {
    ManagementEmpty,
    ManagementListSearch,
    ManagementPageHeader,
    ManagementPagination,
    managementPageClassName,
} from '../components/management/ManagementListUi'
import { PageState } from '../components/ui/PageState'
import { StatusBadge, type StatusBadgeTone } from '../components/ui/StatusBadge'
import { REALTIME_EVENT } from '../realtime/RealtimeProvider'
import type { PageResponse } from '../types/api'
import type {
    SupportInquiryStatus,
    SupportInquirySummary,
} from '../types/support'
import { formatKoreanDateTime } from '../utils/dateTime'
import { InquiryCreateForm } from './SupportCenterPage'
import { supportBasePath } from './supportInquiryRouting'
import { CATEGORY_LABELS, STATUS_LABELS } from './supportPresentation'

const SUPPORT_STATUSES = new Set<SupportInquiryStatus>(Object.keys(STATUS_LABELS) as SupportInquiryStatus[])

const supportStatusTones: Record<SupportInquiryStatus, StatusBadgeTone> = {
    WAITING: 'warning',
    IN_PROGRESS: 'info',
    ANSWERED: 'success',
    LIVE_REQUESTED: 'warning',
    LIVE_OFFERED: 'info',
    LIVE_ACTIVE: 'success',
    CLOSED: 'neutral',
}

export function SupportInquiryListPage({ admin = false }: { admin?: boolean }) {
    const { role } = useAuth()
    const navigate = useNavigate()
    const [searchParams, setSearchParams] = useSearchParams()
    const [pageData, setPageData] = useState<PageResponse<SupportInquirySummary> | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const [submitting, setSubmitting] = useState(false)
    const page = positiveNumber(searchParams.get('page'), 1)
    const keyword = searchParams.get('keyword')?.trim() ?? ''
    const status = parseStatus(searchParams.get('status'))
    const creating = searchParams.get('create') === 'true'
    const basePath = supportBasePath(admin, role)

    const load = useCallback(async (signal?: AbortSignal) => {
        try {
            setPageData(await getSupportInquiries(
                admin,
                page,
                admin ? status || undefined : undefined,
                admin ? keyword : '',
                signal,
            ))
            setError('')
        } catch (loadError) {
            if (loadError instanceof Error && loadError.name === 'AbortError') return
            setError(loadError instanceof Error ? loadError.message : '문의 목록을 불러오지 못했습니다.')
        } finally {
            if (!signal?.aborted) setLoading(false)
        }
    }, [admin, keyword, page, status])

    useEffect(() => {
        const controller = new AbortController()
        const timerId = window.setTimeout(() => void load(controller.signal), 0)
        return () => {
            window.clearTimeout(timerId)
            controller.abort()
        }
    }, [load])

    useEffect(() => {
        const refresh = () => void load()
        const intervalId = window.setInterval(refresh, 30_000)
        window.addEventListener('focus', refresh)
        window.addEventListener(REALTIME_EVENT, refresh)
        return () => {
            window.clearInterval(intervalId)
            window.removeEventListener('focus', refresh)
            window.removeEventListener(REALTIME_EVENT, refresh)
        }
    }, [load])

    if (creating && !admin) {
        return (
            <section className={managementPageClassName}>
                <Link className="mb-6 inline-flex items-center gap-2 text-xs font-bold underline underline-offset-4" to={basePath}>
                    <ArrowLeft className="size-4" /> 문의 목록
                </Link>
                <InquiryCreateForm
                    seller={role === 'ROLE_SELLER'}
                    submitting={submitting}
                    onCancel={() => navigate(basePath)}
                    onSubmit={async (request) => {
                        setSubmitting(true)
                        try {
                            const created = await createSupportInquiry(request)
                            navigate(`${basePath}/${created.inquiry.inquiryId}`, { replace: true })
                        } catch (createError) {
                            setError(createError instanceof Error ? createError.message : '문의를 등록하지 못했습니다.')
                        } finally {
                            setSubmitting(false)
                        }
                    }}
                />
                {error && <PageState compact variant="error" title={error} />}
            </section>
        )
    }

    const items = pageData?.content ?? []
    return (
        <section className={managementPageClassName}>
            <ManagementPageHeader
                eyebrow={admin ? 'SUPPORT OPERATIONS' : 'YMALL SUPPORT'}
                title={admin ? '고객센터 관리' : '고객센터'}
                action={!admin && (
                    <button className="inline-flex items-center gap-2 bg-ink px-5 py-3 text-sm font-bold text-paper" type="button" onClick={() => setSearchParams({ create: 'true' })}>
                        <Plus className="size-4" /> 새 문의
                    </button>
                )}
            />
            {admin && (
                <div className="mb-5 flex flex-wrap items-end gap-3">
                    <label className="grid min-w-52 gap-1.5 text-xs font-bold">
                        문의 상태
                        <select
                            className="h-11 border border-line bg-surface px-3 text-sm font-normal"
                            value={status}
                            onChange={(event) => {
                                const next = new URLSearchParams(searchParams)
                                if (event.target.value) next.set('status', event.target.value)
                                else next.delete('status')
                                next.set('page', '1')
                                setSearchParams(next)
                            }}
                        >
                            <option value="">전체 상태</option>
                            {Object.entries(STATUS_LABELS).map(([value, label]) => (
                                <option key={value} value={value}>{label}</option>
                            ))}
                        </select>
                    </label>
                </div>
            )}
            {admin && <ManagementListSearch placeholder="제목, 요청자 또는 담당자 검색" />}
            {error && <PageState compact variant="error" title={error} />}
            {loading ? (
                <div className="grid min-h-72 place-items-center">
                    <LoaderCircle className="size-6 animate-spin" aria-label="불러오는 중" />
                </div>
            ) : items.length === 0 ? (
                <ManagementEmpty>조회된 문의가 없습니다.</ManagementEmpty>
            ) : (
                <div className="overflow-x-auto border-y border-line">
                    <table className="w-full min-w-190 text-left text-sm">
                        <thead className="border-b border-ink text-xs">
                            <tr>
                                <th className="p-4">번호</th>
                                <th className="p-4">분류·제목</th>
                                {admin && <th className="p-4">요청자</th>}
                                <th className="p-4">상태</th>
                                {admin && <th className="p-4">담당자</th>}
                                <th className="p-4">최근 업데이트</th>
                            </tr>
                        </thead>
                        <tbody>
                            {items.map((inquiry) => (
                                <tr
                                    className="cursor-pointer border-b border-line transition-colors hover:bg-surface"
                                    key={inquiry.inquiryId}
                                    tabIndex={0}
                                    role="link"
                                    onClick={() => navigate(`${basePath}/${inquiry.inquiryId}`)}
                                    onKeyDown={(event) => {
                                        if (event.key === 'Enter' || event.key === ' ') {
                                            event.preventDefault()
                                            navigate(`${basePath}/${inquiry.inquiryId}`)
                                        }
                                    }}
                                >
                                    <td className="p-4 font-bold">#{inquiry.inquiryId}</td>
                                    <td className="p-4">
                                        <span className="block text-xs font-bold text-accent">{CATEGORY_LABELS[inquiry.category]}</span>
                                        <strong className="mt-1 block">{inquiry.title}</strong>
                                    </td>
                                    {admin && <td className="p-4">{inquiry.requesterName}</td>}
                                    <td className="p-4">
                                        <StatusBadge tone={supportStatusTones[inquiry.status]}>
                                            {STATUS_LABELS[inquiry.status]}
                                        </StatusBadge>
                                    </td>
                                    {admin && <td className="p-4">{inquiry.assignedAdminName ?? '-'}</td>}
                                    <td className="p-4 text-muted">{formatKoreanDateTime(inquiry.updatedAt)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
            <ManagementPagination page={pageData?.page ?? page} totalPages={pageData?.totalPages ?? 0} />
        </section>
    )
}

function parseStatus(value: string | null): SupportInquiryStatus | '' {
    return value && SUPPORT_STATUSES.has(value as SupportInquiryStatus)
        ? value as SupportInquiryStatus
        : ''
}

function positiveNumber(value: string | null, fallback: number) {
    const parsed = Number(value)
    return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback
}
