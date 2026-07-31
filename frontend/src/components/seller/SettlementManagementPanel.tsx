import { Banknote, CalendarRange, WalletCards } from 'lucide-react'
import { useState, type ComponentType } from 'react'

import { SettlementAccountPanel } from './SettlementAccountPanel'
import { SettlementRequestPanel } from './SettlementRequestPanel'

type SettlementTab = 'account' | 'request' | 'history'

const tabs: Array<{
    id: SettlementTab
    label: string
    icon: ComponentType<{ className?: string; 'aria-hidden'?: boolean }>
}> = [
    { id: 'account', label: '정산 계좌', icon: Banknote },
    { id: 'request', label: '정산 신청', icon: WalletCards },
    { id: 'history', label: '신청 이력', icon: CalendarRange },
]

export function SettlementManagementPanel() {
    const [activeTab, setActiveTab] = useState<SettlementTab>('account')

    return (
        <section aria-label="정산 관리">
            <div
                className="grid grid-cols-3 overflow-hidden border border-line bg-panel"
                role="tablist"
                aria-label="정산 관리 메뉴"
            >
                {tabs.map((tab) => {
                    const Icon = tab.icon
                    const isActive = activeTab === tab.id
                    return (
                        <button
                            className={`flex min-h-14 items-center justify-center gap-2 border-r border-line px-3 py-3 text-sm font-bold transition-colors last:border-r-0 ${
                                isActive
                                    ? 'bg-ink text-surface'
                                    : 'bg-surface text-muted hover:bg-panel hover:text-ink'
                            }`}
                            id={`settlement-tab-${tab.id}`}
                            key={tab.id}
                            type="button"
                            role="tab"
                            aria-controls={`settlement-panel-${tab.id}`}
                            aria-selected={isActive}
                            onClick={() => setActiveTab(tab.id)}
                        >
                            <Icon className="size-4 shrink-0" aria-hidden={true} />
                            <span>{tab.label}</span>
                        </button>
                    )
                })}
            </div>

            <div
                className="mt-6"
                id={`settlement-panel-${activeTab}`}
                role="tabpanel"
                aria-labelledby={`settlement-tab-${activeTab}`}
            >
                {activeTab === 'account' && <SettlementAccountPanel />}
                {activeTab === 'request' && <SettlementRequestPanel view="request" />}
                {activeTab === 'history' && <SettlementRequestPanel view="history" />}
            </div>
        </section>
    )
}
