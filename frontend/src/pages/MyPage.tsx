import { useEffect, useState, type FormEvent } from 'react'
import { changeMemberPassword, getMemberProfile, updateMemberProfile } from '../api/auth'
import { ApiError } from '../api/client'
import type { MemberProfile } from '../types/auth'
import { AddressManager } from '../components/AddressManager'

export function MyPage() {
    const [profile, setProfile] = useState<MemberProfile | null>(null)
    const [name, setName] = useState('')
    const [phone, setPhone] = useState('')
    const [currentPassword, setCurrentPassword] = useState('')
    const [newPassword, setNewPassword] = useState('')
    const [newPasswordConfirmation, setNewPasswordConfirmation] = useState('')
    const [profileMessage, setProfileMessage] = useState('')
    const [passwordMessage, setPasswordMessage] = useState('')
    const [errorMessage, setErrorMessage] = useState('')
    const [isLoading, setIsLoading] = useState(true)
    const [isSavingProfile, setIsSavingProfile] = useState(false)
    const [isChangingPassword, setIsChangingPassword] = useState(false)

    useEffect(() => {
        const controller = new AbortController()
        getMemberProfile(controller.signal)
            .then((response) => {
                setProfile(response)
                setName(response.name)
                setPhone(response.phone ?? '')
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(error instanceof ApiError ? error.message : '회원 정보를 불러오지 못했습니다.')
            })
            .finally(() => setIsLoading(false))
        return () => controller.abort()
    }, [])

    async function handleProfileSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setErrorMessage('')
        setProfileMessage('')
        setIsSavingProfile(true)
        try {
            const response = await updateMemberProfile({
                name: name.trim(),
                phone: phone.replace(/[\s-]/g, ''),
            })
            setProfile(response)
            setName(response.name)
            setPhone(response.phone ?? '')
            setProfileMessage('회원 정보가 저장되었습니다.')
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '회원 정보를 저장하지 못했습니다.')
        } finally {
            setIsSavingProfile(false)
        }
    }

    async function handlePasswordSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setErrorMessage('')
        setPasswordMessage('')
        if (newPassword !== newPasswordConfirmation) {
            setErrorMessage('새 비밀번호 확인이 일치하지 않습니다.')
            return
        }
        setIsChangingPassword(true)
        try {
            await changeMemberPassword({ currentPassword, newPassword, newPasswordConfirmation })
            setCurrentPassword('')
            setNewPassword('')
            setNewPasswordConfirmation('')
            setPasswordMessage('비밀번호가 변경되었습니다.')
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '비밀번호를 변경하지 못했습니다.')
        } finally {
            setIsChangingPassword(false)
        }
    }

    if (isLoading) {
        return <section className="mx-auto w-[calc(100%-40px)] max-w-240 py-16 text-sm text-muted">회원 정보를 불러오고 있습니다.</section>
    }

    if (!profile) {
        return <section className="mx-auto w-[calc(100%-40px)] max-w-240 py-16 text-sm text-[#b23b2f]">{errorMessage || '회원 정보를 불러오지 못했습니다.'}</section>
    }

    const passwordConfirmationVisible = newPasswordConfirmation.length > 0
    const isPasswordMatched = passwordConfirmationVisible && newPassword === newPasswordConfirmation

    return (
        <section className="mx-auto w-[calc(100%-40px)] max-w-240 py-14 min-[601px]:w-[calc(100%-48px)] min-[601px]:py-20">
            <p className="mb-4 text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">MY YMALL</p>
            <h1 className="font-serif text-[clamp(38px,5vw,62px)] leading-none font-medium tracking-[-.05em]">내 정보 관리</h1>
            <p className="mt-5 text-sm text-muted">회원 정보와 비밀번호를 안전하게 관리하세요.</p>

            {errorMessage && <p className="mt-8 border border-[#d9aaa4] bg-[#f9ecea] px-4 py-3 text-sm text-[#b23b2f]" role="alert">{errorMessage}</p>}

            <div className="mt-12 grid gap-8 min-[901px]:grid-cols-2">
                <form className="grid content-start gap-5 border border-line bg-white p-6 min-[601px]:p-8" onSubmit={handleProfileSubmit}>
                    <div>
                        <p className="text-[11px] font-extrabold tracking-[.16em] text-muted">PROFILE</p>
                        <h2 className="mt-2 font-serif text-3xl">기본 정보</h2>
                    </div>
                    <label className="grid gap-2 text-xs font-bold text-muted">
                        <span>이메일</span>
                        <input className="border-0 border-b border-line bg-[#f4f4ef] px-2 py-3.5 text-muted" value={profile.email} disabled />
                    </label>
                    <label className="grid gap-2 text-xs font-bold text-muted">
                        <span>이름</span>
                        <input className="border-0 border-b border-line bg-transparent px-0.5 py-3.5 text-ink outline-0 focus:border-ink" value={name} onChange={(event) => setName(event.target.value)} maxLength={50} required />
                    </label>
                    <label className="grid gap-2 text-xs font-bold text-muted">
                        <span>휴대전화 번호</span>
                        <input className="border-0 border-b border-line bg-transparent px-0.5 py-3.5 text-ink outline-0 focus:border-ink" type="tel" value={phone} onChange={(event) => setPhone(event.target.value)} pattern="01[016789]-?[0-9]{3,4}-?[0-9]{4}" maxLength={13} required />
                    </label>
                    {profileMessage && <p className="text-xs text-[#657617]" role="status">{profileMessage}</p>}
                    <button className="mt-2 h-12 border border-ink bg-ink font-extrabold text-white disabled:opacity-60" type="submit" disabled={isSavingProfile}>
                        {isSavingProfile ? '저장 중...' : '회원 정보 저장'}
                    </button>
                </form>

                <form className="grid content-start gap-5 border border-line bg-white p-6 min-[601px]:p-8" onSubmit={handlePasswordSubmit}>
                    <div>
                        <p className="text-[11px] font-extrabold tracking-[.16em] text-muted">SECURITY</p>
                        <h2 className="mt-2 font-serif text-3xl">비밀번호 변경</h2>
                    </div>
                    <label className="grid gap-2 text-xs font-bold text-muted">
                        <span>현재 비밀번호</span>
                        <input className="border-0 border-b border-line bg-transparent px-0.5 py-3.5 text-ink outline-0 focus:border-ink" type="password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} autoComplete="current-password" required />
                    </label>
                    <label className="grid gap-2 text-xs font-bold text-muted">
                        <span>새 비밀번호</span>
                        <input className="border-0 border-b border-line bg-transparent px-0.5 py-3.5 text-ink outline-0 focus:border-ink" type="password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} autoComplete="new-password" minLength={8} maxLength={64} required />
                    </label>
                    <label className="grid gap-2 text-xs font-bold text-muted">
                        <span>새 비밀번호 확인</span>
                        <input className="border-0 border-b border-line bg-transparent px-0.5 py-3.5 text-ink outline-0 focus:border-ink" type="password" value={newPasswordConfirmation} onChange={(event) => setNewPasswordConfirmation(event.target.value)} autoComplete="new-password" minLength={8} maxLength={64} required />
                        {passwordConfirmationVisible && <span className={isPasswordMatched ? 'text-[#657617]' : 'text-[#b23b2f]'}>{isPasswordMatched ? '새 비밀번호가 일치합니다.' : '새 비밀번호가 일치하지 않습니다.'}</span>}
                    </label>
                    {passwordMessage && <p className="text-xs text-[#657617]" role="status">{passwordMessage}</p>}
                    <button className="mt-2 h-12 border border-ink bg-ink font-extrabold text-white disabled:opacity-60" type="submit" disabled={isChangingPassword || !isPasswordMatched || currentPassword.length === 0}>
                        {isChangingPassword ? '변경 중...' : '비밀번호 변경'}
                    </button>
                </form>
            </div>
            <AddressManager defaultRecipientName={profile.name} defaultRecipientPhone={profile.phone ?? ''} />
        </section>
    )
}
