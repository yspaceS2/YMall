import { LoaderCircle, Store } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import {
    createSellerProfile,
    getSellerProfile,
    updateSellerProfile,
} from '../api/seller'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import type { SellerProfile } from '../types/seller'
import { useToast } from '../toast/useToast'

export function SellerProfilePage() {
    const { showToast } = useToast()
    const [profile, setProfile] = useState<SellerProfile | null>(null)
    const [profileForm, setProfileForm] = useState({ storeName: '', businessNumber: '', description: '' })
    const [isLoading, setIsLoading] = useState(true)
    const [isSaving, setIsSaving] = useState(false)
    const [errorMessage, setErrorMessage] = useState('')

    useEffect(() => {
        const controller = new AbortController()
        void getSellerProfile(controller.signal).then((response) => {
            setProfile(response)
            setProfileForm({
                storeName: response.storeName,
                businessNumber: response.businessNumber,
                description: response.description ?? '',
            })
        }).catch((error: unknown) => {
            if (error instanceof Error && error.name === 'AbortError') return
            if (error instanceof ApiError && error.code === 'SELLER_PROFILE_NOT_FOUND') return
            setErrorMessage(error instanceof ApiError ? error.message : '판매자 정보를 불러오지 못했습니다.')
        }).finally(() => {
            if (!controller.signal.aborted) setIsLoading(false)
        })
        return () => controller.abort()
    }, [])

    async function saveProfile(event: FormEvent) {
        event.preventDefault()
        setIsSaving(true)
        setErrorMessage('')
        try {
            const saved = profile
                ? await updateSellerProfile({
                    storeName: profileForm.storeName,
                    description: profileForm.description,
                })
                : await createSellerProfile(profileForm)
            setProfile(saved)
            showToast(
                profile ? '판매자 정보가 수정되었습니다.' : '판매자 등록이 완료되었습니다.',
                'success',
            )
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '판매자 정보를 저장하지 못했습니다.')
        } finally {
            setIsSaving(false)
        }
    }

    if (isLoading) {
        return <div className="grid min-h-100 place-content-center"><LoaderCircle className="size-6 animate-spin" /></div>
    }

    return (
        <section className="mx-auto max-w-350 px-4 py-10 min-[601px]:px-8 min-[601px]:py-14" id="management-overview">
            <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-accent">SELLER CENTER</p>
            <h1 className="mb-8 font-serif text-[clamp(40px,6vw,64px)] leading-none tracking-tighter">판매자 관리</h1>
            {errorMessage && <FeedbackMessage className="mb-5" tone="error">{errorMessage}</FeedbackMessage>}

            <section className="border-t-2 border-ink pt-5">
                <h2 className="mb-6 flex items-center gap-2 text-xl font-bold"><Store />판매자 정보</h2>
                <form className="grid gap-4 min-[701px]:grid-cols-2" onSubmit={saveProfile}>
                    <ProfileField label="상점명" value={profileForm.storeName} onChange={(value) => setProfileForm({ ...profileForm, storeName: value })} required />
                    <ProfileField label="사업자 번호" value={profileForm.businessNumber} onChange={(value) => setProfileForm({ ...profileForm, businessNumber: value })} required disabled={profile !== null} />
                    <label className="grid gap-2 text-xs font-bold min-[701px]:col-span-2">소개<textarea className="min-h-24 border border-line p-3 font-normal" value={profileForm.description} onChange={(event) => setProfileForm({ ...profileForm, description: event.target.value })} /></label>
                    <button className="h-11 bg-ink px-6 text-xs font-bold text-white disabled:opacity-50 min-[701px]:w-fit" disabled={isSaving} type="submit">{profile ? '정보 수정' : '판매자 등록'}</button>
                </form>
            </section>
        </section>
    )
}

function ProfileField({ label, value, onChange, required = false, disabled = false }: { label: string; value: string; onChange: (value: string) => void; required?: boolean; disabled?: boolean }) {
    return <label className="grid gap-2 text-xs font-bold"><span>{label}</span><input className="h-11 border border-line bg-surface px-3 font-normal text-ink disabled:bg-surface disabled:text-muted" value={value} onChange={(event) => onChange(event.target.value)} required={required} disabled={disabled} /></label>
}
