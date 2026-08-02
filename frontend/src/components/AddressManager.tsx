import { useEffect, useRef, useState, type FormEvent } from 'react'
import { createMemberAddress, deleteMemberAddress, getMemberAddresses, updateMemberAddress } from '../api/auth'
import { ApiError } from '../api/client'
import type { MemberAddress, MemberAddressRequest } from '../types/auth'
import { embedPostcodeSearch } from '../utils/postcode'

function createEmptyForm(recipientName: string, recipientPhone: string): MemberAddressRequest {
    return {
    addressName: '', recipientName, recipientPhone, postalCode: '',
    roadAddress: '', detailAddress: '', isDefault: false,
    }
}

export function AddressManager({ defaultRecipientName, defaultRecipientPhone }: { defaultRecipientName: string; defaultRecipientPhone: string }) {
    const [addresses, setAddresses] = useState<MemberAddress[]>([])
    const [form, setForm] = useState<MemberAddressRequest>(() => createEmptyForm(defaultRecipientName, defaultRecipientPhone))
    const [editingId, setEditingId] = useState<number | null>(null)
    const [message, setMessage] = useState('')
    const [error, setError] = useState('')
    const [isSaving, setIsSaving] = useState(false)
    const [isPostcodeOpen, setIsPostcodeOpen] = useState(false)
    const postcodeContainerRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        const controller = new AbortController()
        getMemberAddresses(controller.signal).then(setAddresses).catch((requestError: unknown) => {
            if (requestError instanceof Error && requestError.name === 'AbortError') return
            setError(requestError instanceof ApiError ? requestError.message : '배송지를 불러오지 못했습니다.')
        })
        return () => controller.abort()
    }, [])

    function selectAddress(address: MemberAddress) {
        setEditingId(address.addressId)
        setForm({
            addressName: address.addressName, recipientName: address.recipientName,
            recipientPhone: address.recipientPhone, postalCode: address.postalCode,
            roadAddress: address.roadAddress, detailAddress: address.detailAddress,
            isDefault: address.isDefault,
        })
        setMessage('')
        setError('')
    }

    async function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setIsSaving(true)
        setMessage('')
        setError('')
        try {
            const request = { ...form, recipientPhone: form.recipientPhone.replace(/[\s-]/g, '') }
            if (editingId === null) await createMemberAddress(request)
            else await updateMemberAddress(editingId, request)
            setAddresses(await getMemberAddresses())
            setForm(createEmptyForm(defaultRecipientName, defaultRecipientPhone))
            setEditingId(null)
            setMessage('배송지가 저장되었습니다.')
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : '배송지를 저장하지 못했습니다.')
        } finally {
            setIsSaving(false)
        }
    }

    async function remove(addressId: number) {
        setError('')
        try {
            await deleteMemberAddress(addressId)
            setAddresses(await getMemberAddresses())
            if (editingId === addressId) {
                setEditingId(null)
                setForm(createEmptyForm(defaultRecipientName, defaultRecipientPhone))
            }
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : '배송지를 삭제하지 못했습니다.')
        }
    }

    useEffect(() => {
        if (!isPostcodeOpen || !postcodeContainerRef.current) return
        const container = postcodeContainerRef.current
        setError('')
        embedPostcodeSearch(container, (postalCode, roadAddress) => {
                setForm((current) => ({ ...current, postalCode, roadAddress, detailAddress: '' }))
                setIsPostcodeOpen(false)
            })
            .catch((requestError: unknown) => {
                setError(requestError instanceof Error ? requestError.message : '주소 검색을 시작하지 못했습니다.')
                setIsPostcodeOpen(false)
            })
        return () => { container.replaceChildren() }
    }, [isPostcodeOpen])

    const field = (key: keyof MemberAddressRequest, label: string, options?: { maxLength?: number; pattern?: string }) => (
        <label className="grid gap-2 text-xs font-bold text-muted">
            <span>{label}</span>
            <input className="border-0 border-b border-line bg-transparent px-0.5 py-3 text-ink outline-0 focus:border-ink"
                value={String(form[key])} onChange={(event) => setForm({ ...form, [key]: event.target.value })}
                maxLength={options?.maxLength} pattern={options?.pattern} required />
        </label>
    )

    return (
        <div className="mt-8 scroll-mt-24 border border-line bg-surface p-6 min-[601px]:p-8" id="addresses">
            <p className="text-[11px] font-extrabold tracking-[.16em] text-muted">DELIVERY ADDRESS</p>
            <h2 className="mt-2 font-serif text-3xl">배송지 관리</h2>
            {error && <p className="mt-4 text-sm text-danger" role="alert">{error}</p>}
            <div className="mt-6 grid gap-6 min-[901px]:grid-cols-[1fr_1.2fr]">
                <div className="grid content-start gap-3">
                    {addresses.length === 0 && <p className="text-sm text-muted">등록된 배송지가 없습니다.</p>}
                    {addresses.map((address) => (
                        <article className="border border-line p-4" key={address.addressId}>
                            <div className="flex items-center justify-between gap-3">
                                <strong>{address.addressName}</strong>
                                {address.isDefault && <span className="text-[11px] font-bold text-success">기본 배송지</span>}
                            </div>
                            <p className="mt-2 text-sm">{address.recipientName} · {address.recipientPhone}</p>
                            <p className="mt-1 text-xs leading-5 text-muted">[{address.postalCode}] {address.roadAddress} {address.detailAddress}</p>
                            <div className="mt-3 flex gap-3 text-xs underline">
                                <button type="button" onClick={() => selectAddress(address)}>수정</button>
                                <button type="button" onClick={() => void remove(address.addressId)}>삭제</button>
                            </div>
                        </article>
                    ))}
                </div>
                <form className="grid gap-4" onSubmit={submit}>
                    <label className="grid gap-2 text-xs font-bold text-muted">
                        <span>배송지 별칭</span>
                        <input className="border-0 border-b border-line bg-transparent px-0.5 py-3 text-ink outline-0 focus:border-ink" value={form.addressName} onChange={(event) => setForm({ ...form, addressName: event.target.value })} maxLength={30} placeholder="예: 집, 회사, 부모님 댁" required />
                    </label>
                    {field('recipientName', '받는 분', { maxLength: 50 })}
                    {field('recipientPhone', '연락처', { maxLength: 13, pattern: '01[016789]-?[0-9]{3,4}-?[0-9]{4}' })}
                    <div className="grid grid-cols-[1fr_auto] items-end gap-2">
                        <label className="grid gap-2 text-xs font-bold text-muted"><span>우편번호</span><input className="border-0 border-b border-line bg-surface px-0.5 py-3 text-ink" value={form.postalCode} readOnly required /></label>
                        <button className="h-11 border border-ink px-4 text-xs font-bold" type="button" onClick={() => setIsPostcodeOpen(true)}>주소 찾기</button>
                    </div>
                    <label className="grid gap-2 text-xs font-bold text-muted"><span>도로명 주소</span><input className="border-0 border-b border-line bg-surface px-0.5 py-3 text-ink" value={form.roadAddress} readOnly required /></label>
                    {field('detailAddress', '상세 주소', { maxLength: 255 })}
                    <label className="flex items-center gap-2 text-xs font-bold"><input type="checkbox" checked={form.isDefault} onChange={(event) => setForm({ ...form, isDefault: event.target.checked })} />기본 배송지로 설정</label>
                    {message && <p className="text-xs text-success" role="status">{message}</p>}
                    <div className="flex gap-2">
                        <button className="h-11 flex-1 bg-ink text-sm font-extrabold text-white disabled:opacity-60" disabled={isSaving}>{isSaving ? '저장 중...' : editingId === null ? '배송지 추가' : '배송지 수정'}</button>
                        {editingId !== null && <button className="h-11 border border-line px-4 text-xs" type="button" onClick={() => { setEditingId(null); setForm(createEmptyForm(defaultRecipientName, defaultRecipientPhone)) }}>취소</button>}
                    </div>
                </form>
            </div>
            {isPostcodeOpen && (
                <div className="fixed inset-0 z-50 grid place-items-center bg-black/50 p-4" role="dialog" aria-modal="true" aria-label="주소 검색">
                    <div className="w-full max-w-130 bg-surface p-3 shadow-xl">
                        <div className="mb-2 flex items-center justify-between px-1">
                            <strong className="text-sm">주소 검색</strong>
                            <button className="px-2 py-1 text-xs underline" type="button" onClick={() => setIsPostcodeOpen(false)}>닫기</button>
                        </div>
                        <div className="h-[min(70vh,520px)] border border-line" ref={postcodeContainerRef} />
                    </div>
                </div>
            )}
        </div>
    )
}
