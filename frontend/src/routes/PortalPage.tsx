import type { ReactNode } from 'react'

export function PortalPage({ children }: { children: ReactNode }) {
    return (
        <div className="mx-auto max-w-350 px-4 py-10 min-[601px]:px-8 min-[601px]:py-14">
            {children}
        </div>
    )
}
