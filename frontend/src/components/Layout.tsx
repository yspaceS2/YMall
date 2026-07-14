import type { ReactNode } from 'react'
import { Heart, LogOut, Search, ShoppingBag, UserRound } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

export function Layout({ children }: { children: ReactNode }) {
    const { isAuthenticated, logout } = useAuth()

    return (
        <div className="app-shell">
            <header className="site-header">
                <Link className="brand" to="/" aria-label="YMall 홈">
                    Y<span>mall</span>
                </Link>
                <nav className="main-nav" aria-label="주요 메뉴">
                    <Link to="/">NEW</Link>
                    <Link to="/">BEST</Link>
                    <Link to="/">SHOP</Link>
                    <Link to="/">EVENT</Link>
                </nav>
                <div className="header-actions" aria-label="사용자 메뉴">
                    <Search aria-hidden="true" />
                    <Heart aria-hidden="true" />
                    <ShoppingBag aria-hidden="true" />
                    {isAuthenticated ? (
                        <button className="header-icon-button" type="button" onClick={logout} aria-label="로그아웃">
                            <LogOut aria-hidden="true" />
                        </button>
                    ) : (
                        <Link className="header-icon-button" to="/login" aria-label="로그인">
                            <UserRound aria-hidden="true" />
                        </Link>
                    )}
                </div>
            </header>
            <main>{children}</main>
            <footer className="site-footer">
                <div><strong>YMALL</strong><p>일상에 취향을 더하는 셀렉트 스토어</p></div>
                <p>© 2026 YMall. Portfolio project.</p>
            </footer>
        </div>
    )
}
