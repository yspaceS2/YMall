import { History, LogOut, ShieldBan, ShieldCheck } from 'lucide-react'
import { useEffect, useState } from 'react'
import {
    getAdminMemberAuditLogs,
    revokeAdminMemberSessions,
    updateAdminMemberRestriction,
} from '../../api/admin'
import { ApiError } from '../../api/client'
import { useAdminAuthorization } from '../../auth/useAdminAuthorization'
import type { AdminAuditLog, AdminMember } from '../../types/admin'
import { formatKoreanDateTime } from '../../utils/dateTime'
import { FeedbackMessage } from '../ui/FeedbackMessage'

export function AdminMemberOperationsPanel({
    member,
    onChanged,
}: {
    member: AdminMember
    onChanged: (member: AdminMember) => void
}) {
    const { authorization, hasPermission } = useAdminAuthorization()
    const [reason, setReason] = useState('')
    const [logs, setLogs] = useState<AdminAuditLog[]>([])
    const [isProcessing, setIsProcessing] = useState(false)
    const [message, setMessage] = useState('')
    const [errorMessage, setErrorMessage] = useState('')
    const canManageAll = hasPermission('MEMBER_RESTRICT_ALL')
    const canManageLimited = hasPermission('MEMBER_RESTRICT_LIMITED')
    const canManageTarget = authorization?.memberId !== member.memberId
        && (canManageAll || (canManageLimited && member.role === 'ROLE_USER'))
    const canReadAudit = hasPermission('AUDIT_OWN_READ') || hasPermission('AUDIT_ALL_READ')

    useEffect(() => {
        if (!canReadAudit) return
        const controller = new AbortController()
        getAdminMemberAuditLogs(member.memberId, controller.signal)
            .then(setLogs)
            .catch(() => undefined)
        return () => controller.abort()
    }, [canReadAudit, member.memberId])

    async function changeRestriction() {
        const normalizedReason = reason.trim()
        if (!normalizedReason) {
            setErrorMessage('처리 사유를 입력해 주세요.')
            return
        }
        const restrict = member.accessStatus === 'ACTIVE'
        const label = restrict ? '이용 제한' : '제한 해제'
        if (!window.confirm(`${member.name} 회원을 ${label}하시겠습니까?`)) return
        await process(async () => {
            const response = await updateAdminMemberRestriction(
                member.memberId,
                restrict,
                normalizedReason,
            )
            onChanged(response)
            setMessage(`${member.name} 회원의 ${label} 처리가 완료되었습니다.`)
        })
    }

    async function revokeSessions() {
        const normalizedReason = reason.trim()
        if (!normalizedReason) {
            setErrorMessage('처리 사유를 입력해 주세요.')
            return
        }
        if (!window.confirm(`${member.name} 회원의 모든 로그인 세션을 종료하시겠습니까?`)) return
        await process(async () => {
            await revokeAdminMemberSessions(member.memberId, normalizedReason)
            setMessage(`${member.name} 회원의 모든 로그인 세션을 종료했습니다.`)
        })
    }

    async function process(action: () => Promise<void>) {
        setIsProcessing(true)
        setMessage('')
        setErrorMessage('')
        try {
            await action()
            setReason('')
            if (canReadAudit) setLogs(await getAdminMemberAuditLogs(member.memberId))
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '회원 업무를 처리하지 못했습니다.')
        } finally {
            setIsProcessing(false)
        }
    }

    return (
        <div className="grid gap-8 min-[1001px]:grid-cols-[minmax(0,.8fr)_minmax(0,1.2fr)]">
            <section className="border-t-2 border-ink pt-5">
                <h2 className="mb-5 flex items-center gap-2 text-xl font-bold">
                    {member.accessStatus === 'ACTIVE'
                        ? <ShieldCheck className="size-5" aria-hidden="true" />
                        : <ShieldBan className="size-5" aria-hidden="true" />}
                    회원 상태 관리
                </h2>
                {message && <FeedbackMessage className="mb-4" tone="success">{message}</FeedbackMessage>}
                {errorMessage && <FeedbackMessage className="mb-4" tone="error">{errorMessage}</FeedbackMessage>}
                {!canManageTarget ? (
                    <p className="text-sm text-muted">현재 권한으로 이 회원의 상태를 변경할 수 없습니다.</p>
                ) : (
                    <div className="grid gap-4">
                        <label className="grid gap-2 text-xs font-bold text-muted">
                            처리 사유
                            <textarea
                                className="min-h-24 resize-y border border-line bg-transparent p-3 text-sm font-normal text-ink outline-0 focus:border-ink"
                                maxLength={500}
                                placeholder="감사 로그에 남길 사유를 입력하세요"
                                value={reason}
                                onChange={(event) => setReason(event.target.value)}
                            />
                        </label>
                        <div className="flex flex-wrap gap-2">
                            <button
                                className={member.accessStatus === 'ACTIVE'
                                    ? 'h-11 border border-danger px-4 text-xs font-bold text-danger disabled:opacity-50'
                                    : 'h-11 bg-ink px-4 text-xs font-bold text-white disabled:opacity-50'}
                                disabled={isProcessing}
                                type="button"
                                onClick={() => void changeRestriction()}
                            >
                                {isProcessing ? '처리 중' : member.accessStatus === 'ACTIVE' ? '이용 제한' : '제한 해제'}
                            </button>
                            <button
                                className="inline-flex h-11 items-center gap-2 border border-line px-4 text-xs font-bold disabled:opacity-50"
                                disabled={isProcessing}
                                type="button"
                                onClick={() => void revokeSessions()}
                            >
                                <LogOut className="size-4" aria-hidden="true" />
                                전체 세션 종료
                            </button>
                        </div>
                    </div>
                )}
            </section>
            <section className="border-t-2 border-ink pt-5">
                <h2 className="mb-5 flex items-center gap-2 text-xl font-bold">
                    <History className="size-5" aria-hidden="true" />
                    최근 변경 이력
                </h2>
                {!canReadAudit ? (
                    <p className="text-sm text-muted">감사 로그 조회 권한이 없습니다.</p>
                ) : logs.length === 0 ? (
                    <p className="text-sm text-muted">기록된 변경 이력이 없습니다.</p>
                ) : (
                    <ol className="grid gap-3">
                        {logs.map((log) => (
                            <li className="border border-line p-4 text-sm" key={log.auditLogId}>
                                <div className="flex flex-wrap items-center justify-between gap-2">
                                    <strong>{auditActionLabel(log.action)}</strong>
                                    <span className="text-xs text-muted">{formatKoreanDateTime(log.createdAt)}</span>
                                </div>
                                <p className="mt-2 text-xs text-muted">
                                    {log.actorName} · {log.actorGrade} · {log.reason}
                                </p>
                            </li>
                        ))}
                    </ol>
                )}
            </section>
        </div>
    )
}

function auditActionLabel(action: AdminAuditLog['action']) {
    if (action === 'ADMIN_ROLE_CHANGED') return '관리자 역할 변경'
    if (action === 'MEMBER_RESTRICTION_CHANGED') return '회원 이용 상태 변경'
    return '전체 로그인 세션 종료'
}
