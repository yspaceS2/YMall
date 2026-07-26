const GOOGLE_IDENTITY_SCRIPT_ID = 'google-identity-services'
const GOOGLE_IDENTITY_SCRIPT_URL = 'https://accounts.google.com/gsi/client'

interface GoogleCredentialResponse {
    credential?: string
}

interface GoogleIdentityApi {
    initialize(options: {
        client_id: string
        callback: (response: GoogleCredentialResponse) => void
        nonce: string
        auto_select: boolean
        cancel_on_tap_outside: boolean
        itp_support: boolean
        use_fedcm_for_prompt: boolean
    }): void
    prompt(): void
    cancel(): void
    disableAutoSelect(): void
}

declare global {
    interface Window {
        google?: {
            accounts: {
                id: GoogleIdentityApi
            }
        }
    }
}

let scriptPromise: Promise<void> | null = null

export function loadGoogleIdentityScript() {
    if (window.google?.accounts.id) {
        return Promise.resolve()
    }
    if (scriptPromise !== null) {
        return scriptPromise
    }

    scriptPromise = new Promise<void>((resolve, reject) => {
        const existingScript = document.getElementById(GOOGLE_IDENTITY_SCRIPT_ID)
        const script = existingScript instanceof HTMLScriptElement
            ? existingScript
            : document.createElement('script')

        const handleLoad = () => {
            if (window.google?.accounts.id) {
                resolve()
            } else {
                scriptPromise = null
                reject(new Error('Google Identity Services를 초기화하지 못했습니다.'))
            }
        }
        const handleError = () => {
            scriptPromise = null
            reject(new Error('Google Identity Services를 불러오지 못했습니다.'))
        }

        script.addEventListener('load', handleLoad, { once: true })
        script.addEventListener('error', handleError, { once: true })
        if (!existingScript) {
            script.id = GOOGLE_IDENTITY_SCRIPT_ID
            script.src = GOOGLE_IDENTITY_SCRIPT_URL
            script.async = true
            script.defer = true
            document.head.appendChild(script)
        }
    })
    return scriptPromise
}

export function cancelGoogleOneTap() {
    window.google?.accounts.id.cancel()
}
