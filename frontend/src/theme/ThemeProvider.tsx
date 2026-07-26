import { useEffect, useLayoutEffect, useMemo, useState, type ReactNode } from 'react'
import { ThemeContext, type ThemePreference } from './ThemeContext'
import {
    applyTheme,
    DARK_MODE_QUERY,
    readThemePreference,
    resolveTheme,
    writeThemePreference,
} from './theme'

export function ThemeProvider({ children }: { children: ReactNode }) {
    const [preference, setPreferenceState] = useState<ThemePreference>(readThemePreference)
    const [systemPrefersDark, setSystemPrefersDark] = useState(
        () => window.matchMedia?.(DARK_MODE_QUERY).matches ?? false,
    )
    const resolvedTheme = resolveTheme(preference, systemPrefersDark)

    useEffect(() => {
        const mediaQuery = window.matchMedia?.(DARK_MODE_QUERY)
        if (!mediaQuery) return

        const handleChange = (event: MediaQueryListEvent) => {
            setSystemPrefersDark(event.matches)
        }
        mediaQuery.addEventListener('change', handleChange)
        return () => mediaQuery.removeEventListener('change', handleChange)
    }, [])

    useLayoutEffect(() => {
        applyTheme(resolvedTheme)
    }, [resolvedTheme])

    const value = useMemo(() => ({
        preference,
        resolvedTheme,
        setPreference: (nextPreference: ThemePreference) => {
            writeThemePreference(nextPreference)
            setPreferenceState(nextPreference)
        },
    }), [preference, resolvedTheme])

    return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}
