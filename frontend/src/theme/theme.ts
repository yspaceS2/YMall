import type { ResolvedTheme, ThemePreference } from './ThemeContext'

export const THEME_STORAGE_KEY = 'ymall:theme'
export const DARK_MODE_QUERY = '(prefers-color-scheme: dark)'

export function readThemePreference(storage?: Pick<Storage, 'getItem'>): ThemePreference {
    try {
        const storedPreference = (storage ?? globalThis.localStorage).getItem(THEME_STORAGE_KEY)
        return isThemePreference(storedPreference) ? storedPreference : 'system'
    } catch {
        return 'system'
    }
}

export function writeThemePreference(
    preference: ThemePreference,
    storage?: Pick<Storage, 'setItem'>,
): boolean {
    try {
        (storage ?? globalThis.localStorage).setItem(THEME_STORAGE_KEY, preference)
        return true
    } catch {
        return false
    }
}

export function resolveTheme(
    preference: ThemePreference,
    systemPrefersDark: boolean,
): ResolvedTheme {
    if (preference === 'system') {
        return systemPrefersDark ? 'dark' : 'light'
    }
    return preference
}

export function applyTheme(theme: ResolvedTheme) {
    document.documentElement.dataset.theme = theme
    document.documentElement.style.colorScheme = theme
}

function isThemePreference(value: string | null): value is ThemePreference {
    return value === 'system' || value === 'light' || value === 'dark'
}
