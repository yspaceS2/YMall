import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { Bell, ClipboardList, MapPin, MessageSquareText, UserRound } from 'lucide-react'
import { changeMemberPassword, getMemberProfile, getOAuthAccounts, getOAuthAuthorizationUrl, startOAuthAccountLink, updateMemberProfile } from '../api/auth'
import { ApiError } from '../api/client'
import type { MemberProfile, OAuthProvider } from '../types/auth'
import { AddressManager } from '../components/AddressManager'
import { EmailChangePanel } from '../components/member/EmailChangePanel'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { PageState } from '../components/ui/PageState'

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
    const [linkedProviders, setLinkedProviders] = useState<OAuthProvider[]>([])
    const [linkingProvider, setLinkingProvider] = useState<OAuthProvider | null>(null)
    const [retryKey, setRetryKey] = useState(0)

    useEffect(() => {
        const controller = new AbortController()
        Promise.all([getMemberProfile(controller.signal), getOAuthAccounts(controller.signal)])
            .then(([profileResponse, accountResponse]) => {
                setProfile(profileResponse)
                setName(profileResponse.name)
                setPhone(profileResponse.phone ?? '')
                setLinkedProviders(accountResponse.map((account) => account.provider))
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(error instanceof ApiError ? error.message : '회원 정보를 불러오지 못했습니다.')
            })
            .finally(() => setIsLoading(false))
        return () => controller.abort()
    }, [retryKey])

    async function handleOAuthLink(provider: OAuthProvider) {
        setErrorMessage('')
        setLinkingProvider(provider)
        try {
            await startOAuthAccountLink(provider)
            window.location.assign(getOAuthAuthorizationUrl(provider))
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '소셜 계정 연결을 시작하지 못했습니다.')
            setLinkingProvider(null)
        }
    }

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
        return <PageState variant="loading" title="회원 정보를 불러오는 중입니다" description="잠시만 기다려 주세요." />
    }

    if (!profile) {
        return <PageState variant="error" title="회원 정보를 불러오지 못했습니다" description={errorMessage || '잠시 후 다시 시도해 주세요.'} action={<button className="border border-ink bg-white px-5 py-2.5 text-xs font-bold" type="button" onClick={() => { setErrorMessage(''); setIsLoading(true); setRetryKey((value) => value + 1) }}>다시 시도</button>} />
    }

    const passwordConfirmationVisible = newPasswordConfirmation.length > 0
    const isPasswordMatched = passwordConfirmationVisible && newPassword === newPasswordConfirmation
    const quickLinks = [
        { label: '프로필', description: '회원 정보와 보안 설정', href: '#profile', icon: UserRound },
        { label: '배송지', description: '받는 주소 추가·수정', href: '#addresses', icon: MapPin },
        { label: '주문', description: '주문 및 배송 현황', href: '/orders', icon: ClipboardList },
        { label: '리뷰', description: '구매 상품 리뷰 관리', href: '/orders', icon: MessageSquareText },
        { label: '알림', description: '주문 상태 알림 확인', href: '/notifications', icon: Bell },
    ]

    return (
        <section className="mx-auto w-[calc(100%-40px)] max-w-240 py-14 min-[601px]:w-[calc(100%-48px)] min-[601px]:py-20">
            <p className="mb-4 text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">MY YMALL</p>
            <h1 className="font-serif text-[clamp(38px,5vw,62px)] leading-none font-medium tracking-[-.05em]">내 정보 관리</h1>
            <p className="mt-5 text-sm leading-7 text-muted">회원 정보부터 배송지, 주문과 알림까지 한곳에서 관리하세요.</p>

            <nav className="mt-10 grid grid-cols-2 gap-3 min-[701px]:grid-cols-5" aria-label="마이페이지 바로가기">
                {quickLinks.map(({ label, description, href, icon: Icon }) => {
                    const className = 'group flex min-h-28 flex-col justify-between border border-line bg-white p-4 text-left transition-colors hover:border-ink focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ink'
                    const content = <>
                        <Icon className="size-5 text-[#71801e]" aria-hidden="true" />
                        <span>
                            <strong className="block text-sm">{label}</strong>
                            <span className="mt-1 block text-[11px] leading-4 text-muted">{description}</span>
                        </span>
                    </>
                    return href.startsWith('#')
                        ? <a className={className} href={href} key={label}>{content}</a>
                        : <Link className={className} to={href} key={label}>{content}</Link>
                })}
            </nav>

            {errorMessage && <FeedbackMessage className="mt-8" tone="error">{errorMessage}</FeedbackMessage>}

            <div className="mt-8 grid scroll-mt-24 gap-8 min-[901px]:grid-cols-2" id="profile">
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
                    {profileMessage && <FeedbackMessage tone="success">{profileMessage}</FeedbackMessage>}
                    <button className="mt-2 h-12 border border-ink bg-ink font-extrabold text-white disabled:opacity-60" type="submit" disabled={isSavingProfile}>
                        {isSavingProfile ? '저장 중...' : '회원 정보 저장'}
                    </button>
                </form>

                {profile.hasPassword ? <form className="grid content-start gap-5 border border-line bg-white p-6 min-[601px]:p-8" onSubmit={handlePasswordSubmit}>
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
                    {passwordMessage && <FeedbackMessage tone="success">{passwordMessage}</FeedbackMessage>}
                    <button className="mt-2 h-12 border border-ink bg-ink font-extrabold text-white disabled:opacity-60" type="submit" disabled={isChangingPassword || !isPasswordMatched || currentPassword.length === 0}>
                        {isChangingPassword ? '변경 중...' : '비밀번호 변경'}
                    </button>
                </form> : <div className="grid content-start gap-5 border border-line bg-surface p-6 min-[601px]:p-8">
                    <div>
                        <p className="text-[11px] font-extrabold tracking-[.16em] text-muted">SECURITY</p>
                        <h2 className="mt-2 font-serif text-3xl">소셜 로그인 계정</h2>
                    </div>
                    <p className="text-sm leading-7 text-muted">이 계정은 비밀번호 없이 연결된 소셜 계정으로 로그인합니다. 비밀번호 변경은 제공되지 않습니다.</p>
                </div>}
            </div>
            <div className="mt-8">
                <EmailChangePanel currentEmail={profile.email} hasPassword={profile.hasPassword} />
            </div>
            <div className="mt-8 scroll-mt-24 border border-line bg-surface p-6 min-[601px]:p-8" id="social-accounts">
                <p className="text-[11px] font-extrabold tracking-[.16em] text-muted">SOCIAL LOGIN</p>
                <h2 className="mt-2 font-serif text-3xl">연결된 소셜 계정</h2>
                <p className="mt-3 text-sm text-muted">소셜 계정을 연결하면 해당 계정으로도 같은 YMall 회원에 로그인할 수 있습니다.</p>
                <div className="mt-6 grid gap-3 min-[701px]:grid-cols-3">
                    {(['GOOGLE', 'KAKAO', 'NAVER'] as OAuthProvider[]).map((provider) => {
                        const isLinked = linkedProviders.includes(provider)
                        return (
                            <button
                                className="flex h-13 items-center justify-between border border-line px-4 text-sm font-bold disabled:bg-paper disabled:text-muted"
                                type="button"
                                key={provider}
                                disabled={isLinked || linkingProvider !== null}
                                onClick={() => handleOAuthLink(provider)}
                            >
                                <span>{provider === 'GOOGLE' ? 'Google' : provider === 'KAKAO' ? '카카오' : '네이버'}</span>
                                <span className={isLinked ? 'text-[#657617]' : 'text-ink'}>
                                    {isLinked ? '연결됨' : linkingProvider === provider ? '연결 중...' : '연결하기'}
                                </span>
                            </button>
                        )
                    })}
                </div>
            </div>
            <AddressManager
                key={`${profile.name}:${profile.phone ?? ''}`}
                defaultRecipientName={profile.name}
                defaultRecipientPhone={profile.phone ?? ''}
            />
        </section>
    )
}
