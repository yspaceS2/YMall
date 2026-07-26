import { Check, Monitor, Moon, Sun } from 'lucide-react'
import { useState, type FocusEvent } from 'react'
import type { ThemePreference } from '../theme/ThemeContext'
import { useTheme } from '../theme/useTheme'

const themeOptions: Array<{
    value: ThemePreference
    label: string
    icon: typeof Monitor
}> = [
    { value: 'system', label: '시스템 설정', icon: Monitor },
    { value: 'light', label: '라이트 모드', icon: Sun },
    { value: 'dark', label: '다크 모드', icon: Moon },
]

export function ThemeSelector() {
    const { preference, resolvedTheme, setPreference } = useTheme()
    const [isOpen, setIsOpen] = useState(false)
    const ActiveIcon = resolvedTheme === 'dark' ? Moon : Sun

    const closeWhenFocusLeaves = (event: FocusEvent<HTMLDivElement>) => {
        if (!event.currentTarget.contains(event.relatedTarget)) {
            setIsOpen(false)
        }
    }

    return (
        <div className="relative" onBlur={closeWhenFocusLeaves}>
            <button
                className="inline-grid size-8 place-items-center border border-line bg-surface transition-colors hover:border-ink"
                type="button"
                aria-label={`테마 선택: ${themeOptions.find((option) => option.value === preference)?.label}`}
                aria-expanded={isOpen}
                aria-haspopup="menu"
                onClick={() => setIsOpen((open) => !open)}
            >
                <ActiveIcon className="size-4" aria-hidden="true" />
            </button>
            {isOpen && (
                <div
                    className="absolute top-[calc(100%+10px)] right-0 z-30 grid min-w-40 border border-line bg-surface p-1 shadow-xl"
                    role="menu"
                    aria-label="화면 테마"
                >
                    {themeOptions.map((option) => {
                        const Icon = option.icon
                        const selected = preference === option.value
                        return (
                            <button
                                className="flex items-center gap-2.5 px-3 py-2.5 text-left text-xs font-bold transition-colors hover:bg-paper"
                                type="button"
                                role="menuitemradio"
                                aria-checked={selected}
                                key={option.value}
                                onClick={() => {
                                    setPreference(option.value)
                                    setIsOpen(false)
                                }}
                            >
                                <Icon className="size-4" aria-hidden="true" />
                                <span className="flex-1">{option.label}</span>
                                {selected && <Check className="size-3.5" aria-hidden="true" />}
                            </button>
                        )
                    })}
                </div>
            )}
        </div>
    )
}
