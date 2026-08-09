import { LoaderCircle, ShieldCheck } from 'lucide-react'
import { useEffect, useMemo, useState, type FormEvent } from 'react'
import {
    getAdminAuthorization,
    updateAdminMemberRole,
} from '../../api/admin'
import { ApiError } from '../../api/client'
import type { AdminMember, AdminRoleUpdateResponse } from '../../types/admin'
import { FeedbackMessage } from '../ui/FeedbackMessage'
import {
    availableAdminRoleChanges,
    type AdminRoleChangeOption,
} from './adminRolePresentation'

export function AdminMemberRolePanel({
    member,
    onChanged,
}: {
    member: AdminMember
    onChanged: (response: AdminRoleUpdateResponse) => void
}) {
    const [authorization, setAuthorization] = useState<Awaited<ReturnType<typeof getAdminAuthorization>> | null>(null)
    const [selectedValue, setSelectedValue] = useState('')
    const [reason, setReason] = useState('')
    const [isLoading, setIsLoading] = useState(true)
    const [isProcessing, setIsProcessing] = useState(false)
    const [message, setMessage] = useState('')
    const [errorMessage, setErrorMessage] = useState('')

    useEffect(() => {
        const controller = new AbortController()
        getAdminAuthorization(controller.signal)
            .then(setAuthorization)
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(
                    error instanceof ApiError
                        ? error.message
                        : '관리자 권한 정보를 불러오지 못했습니다.',
                )
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [])

    const options = useMemo(
        () => authorization
            ? availableAdminRoleChanges(authorization, member)
            : [],
        [authorization, member],
    )

    async function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        const option = options.find((candidate) => optionValue(candidate) === selectedValue)
        const normalizedReason = reason.trim()
        if (!option || !normalizedReason) {
            setErrorMessage('변경할 권한과 변경 사유를 입력해 주세요.')
            return
        }

        setIsProcessing(true)
        setMessage('')
        setErrorMessage('')
        try {
            const response = await updateAdminMemberRole(member.memberId, {
                role: option.role,
                adminGrade: option.adminGrade,
                reason: normalizedReason,
            })
            onChanged(response)
            setSelectedValue('')
            setReason('')
            setMessage(`${member.name}의 권한을 ${option.label}(으)로 변경했습니다.`)
        } catch (error) {
            setErrorMessage(
                error instanceof ApiError
                    ? error.message
                    : '회원 권한을 변경하지 못했습니다.',
            )
        } finally {
            setIsProcessing(false)
        }
    }

    return (
        <section className="border-t-2 border-ink pt-5">
            <h2 className="mb-5 flex items-center gap-2 text-xl font-bold">
                <ShieldCheck className="size-5" aria-hidden="true" />
                권한 변경
            </h2>
            {message && <FeedbackMessage className="mb-4" tone="success">{message}</FeedbackMessage>}
            {errorMessage && <FeedbackMessage className="mb-4" tone="error">{errorMessage}</FeedbackMessage>}
            {isLoading ? (
                <LoaderCircle className="size-5 animate-spin" aria-label="관리자 권한 로딩 중" />
            ) : options.length === 0 ? (
                <p className="text-sm text-muted">현재 계정으로 변경할 수 있는 권한이 없습니다.</p>
            ) : (
                <form className="grid gap-4 min-[801px]:grid-cols-[minmax(180px,.6fr)_minmax(260px,1fr)_auto] min-[801px]:items-end" onSubmit={(event) => void submit(event)}>
                    <label className="grid gap-2 text-xs font-bold text-muted">
                        변경할 권한
                        <select
                            className="h-11 border border-line bg-surface px-3 text-sm font-normal text-ink outline-0 focus:border-ink"
                            value={selectedValue}
                            onChange={(event) => setSelectedValue(event.target.value)}
                        >
                            <option value="">선택</option>
                            {options.map((option) => (
                                <option key={optionValue(option)} value={optionValue(option)}>
                                    {option.label}
                                </option>
                            ))}
                        </select>
                    </label>
                    <label className="grid gap-2 text-xs font-bold text-muted">
                        변경 사유
                        <input
                            className="h-11 border border-line bg-transparent px-3 text-sm font-normal text-ink outline-0 focus:border-ink"
                            maxLength={500}
                            placeholder="변경 사유를 입력하세요"
                            value={reason}
                            onChange={(event) => setReason(event.target.value)}
                        />
                    </label>
                    <button
                        className="h-11 bg-ink px-6 text-xs font-bold text-white disabled:opacity-50"
                        disabled={isProcessing}
                        type="submit"
                    >
                        {isProcessing ? '변경 중' : '권한 변경'}
                    </button>
                </form>
            )}
        </section>
    )
}

function optionValue(option: AdminRoleChangeOption) {
    return `${option.role}:${option.adminGrade ?? ''}`
}
