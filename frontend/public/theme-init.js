(() => {
    try {
        const storedTheme = localStorage.getItem('ymall:theme')
        const preference = ['system', 'light', 'dark'].includes(storedTheme) ? storedTheme : 'system'
        const theme = preference === 'system'
            ? (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
            : preference
        document.documentElement.dataset.theme = theme
        document.documentElement.style.colorScheme = theme
    } catch {
        document.documentElement.dataset.theme = 'light'
        document.documentElement.style.colorScheme = 'light'
    }
})()
