import { Banknote, LoaderCircle, ShieldCheck } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import {
    getSellerSettlementAccount,
    upsertSellerSettlementAccount,
} from '../../api/seller'
import { ApiError } from '../../api/client'
import type {
    SellerSettlementAccount,
    SellerSettlementAccountUpsertRequest,
} from '../../types/seller'
import { FeedbackMessage } from '../ui/FeedbackMessage'

const banks = [
    { code: '004', name: 'KB국민은행' },
    { code: '088', name: '신한은행' },
    { code: '081', name: '하나은행' },
    { code: '020', name: '우리은행' },
    { code: '011', name: 'NH농협은행' },
    { code: '003', name: 'IBK기업은행' },
    { code: '090', name: '카카오뱅크' },
    { code: '092', name: '토스뱅크' },
] as const

const initialForm: SellerSettlementAccountUpsertRequest = {
    bankCode: banks[0].code,
    accountHolder: '',
    accountNumber: '',
    currentPassword: '',
}

export function SettlementAccountPanel() {
    const [account, setAccount] = useState<SellerSettlementAccount | null>(null)
    const [form, setForm] = useState<SellerSettlementAccountUpsertRequest>(initialForm)
    const [isLoading, setIsLoading] = useState(true)
    const [isSaving, setIsSaving] = useState(false)
    const [message, setMessage] = useState('')
    const [error, setError] = useState('')

    useEffect(() => {
        const controller = new AbortController()
        getSellerSettlementAccount(controller.signal)
            .then((response) => {
                setAccount(response)
                setForm((current) => ({
                    ...current,
                    bankCode: response.bankCode,
                    accountHolder: response.accountHolder,
                }))
            })
            .catch((value: unknown) => {
                if (value instanceof Error && value.name === 'AbortError') return
                if (value instanceof ApiError
                    && value.code === 'SELLER_SETTLEMENT_ACCOUNT_NOT_FOUND') {
                    return
                }
                setError(value instanceof ApiError
                    ? value.message
                    : '정산 계좌 정보를 불러오지 못했습니다.')
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [])

    async function saveAccount(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setIsSaving(true)
        setMessage('')
        setError('')
        try {
            const saved = await upsertSellerSettlementAccount(form)
            setAccount(saved)
            setForm((current) => ({
                ...current,
                accountNumber: '',
                currentPassword: '',
            }))
            setMessage(account
                ? '정산 계좌 정보가 변경되었습니다.'
                : '정산 계좌 정보가 등록되었습니다.')
        } catch (value) {
            setError(value instanceof ApiError
                ? value.message
                : '정산 계좌 정보를 저장하지 못했습니다.')
        } finally {
            setIsSaving(false)
        }
    }

    return (
        <section className="border-t-2 border-ink pt-5" aria-labelledby="settlement-account-title">
            <h2 className="mb-3 flex items-center gap-2 text-xl font-bold" id="settlement-account-title">
                <Banknote aria-hidden="true" />
                정산 계좌
            </h2>
            <p className="mb-6 max-w-180 text-sm leading-6 text-muted">
                판매 대금의 모의 정산에 사용할 계좌입니다. 계좌번호는 암호화해 저장하며,
                화면에는 마지막 네 자리만 표시합니다.
            </p>

            {isLoading ? (
                <div className="grid min-h-32 place-content-center" aria-label="정산 계좌 불러오는 중">
                    <LoaderCircle className="size-5 animate-spin" />
                </div>
            ) : (
                <div className="grid gap-5">
                    {account && (
                        <div className="flex flex-wrap items-center justify-between gap-4 border border-line bg-surface p-4">
                            <div>
                                <strong>{account.bankName} {account.maskedAccountNumber}</strong>
                                <p className="mt-1 text-xs text-muted">
                                    예금주 {account.accountHolder}
                                </p>
                            </div>
                            <span className="flex items-center gap-1.5 text-xs font-bold text-muted">
                                <ShieldCheck className="size-4" aria-hidden="true" />
                                {account.verificationStatus === 'VERIFIED'
                                    ? '계좌 확인 완료'
                                    : '모의 정산용 · 미인증'}
                            </span>
                        </div>
                    )}

                    <form className="grid gap-4 min-[701px]:grid-cols-2" onSubmit={saveAccount}>
                        <label className="grid gap-2 text-xs font-bold">
                            은행
                            <select
                                className="h-11 border border-line bg-surface px-3 font-normal text-ink"
                                value={form.bankCode}
                                onChange={(event) => setForm({
                                    ...form,
                                    bankCode: event.target.value,
                                })}
                            >
                                {banks.map((bank) => (
                                    <option key={bank.code} value={bank.code}>{bank.name}</option>
                                ))}
                            </select>
                        </label>
                        <label className="grid gap-2 text-xs font-bold">
                            예금주
                            <input
                                className="h-11 border border-line bg-surface px-3 font-normal text-ink"
                                value={form.accountHolder}
                                onChange={(event) => setForm({
                                    ...form,
                                    accountHolder: event.target.value,
                                })}
                                autoComplete="name"
                                maxLength={50}
                                required
                            />
                        </label>
                        <label className="grid gap-2 text-xs font-bold">
                            {account ? '새 계좌번호' : '계좌번호'}
                            <input
                                className="h-11 border border-line bg-surface px-3 font-normal text-ink"
                                value={form.accountNumber}
                                onChange={(event) => setForm({
                                    ...form,
                                    accountNumber: event.target.value.replace(/\D/g, '').slice(0, 20),
                                })}
                                inputMode="numeric"
                                autoComplete="off"
                                minLength={8}
                                maxLength={20}
                                placeholder="숫자만 입력"
                                required
                            />
                        </label>
                        <label className="grid gap-2 text-xs font-bold">
                            현재 비밀번호
                            <input
                                className="h-11 border border-line bg-surface px-3 font-normal text-ink"
                                type="password"
                                value={form.currentPassword}
                                onChange={(event) => setForm({
                                    ...form,
                                    currentPassword: event.target.value,
                                })}
                                autoComplete="current-password"
                                minLength={8}
                                maxLength={100}
                                required
                            />
                        </label>
                        <div className="grid gap-2 min-[701px]:col-span-2 min-[701px]:justify-start">
                            <p className="text-xs leading-5 text-muted">
                                실제 계좌 인증과 은행 송금은 지원하지 않으며, 포트폴리오의 정산 흐름 검증에만 사용합니다.
                            </p>
                            <button
                                className="h-11 bg-ink px-6 text-xs font-bold text-white disabled:opacity-50"
                                type="submit"
                                disabled={isSaving}
                            >
                                {isSaving
                                    ? '저장 중...'
                                    : account
                                        ? '정산 계좌 변경'
                                        : '정산 계좌 등록'}
                            </button>
                        </div>
                    </form>
                    {message && <FeedbackMessage tone="success">{message}</FeedbackMessage>}
                    {error && <FeedbackMessage tone="error">{error}</FeedbackMessage>}
                </div>
            )}
        </section>
    )
}
