import type { ReactNode } from 'react'
import { Heart, Search, ShoppingBag, UserRound } from 'lucide-react'
import { Link } from 'react-router-dom'

export function Layout({ children }: { children: ReactNode }) {
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
          <UserRound aria-hidden="true" />
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
