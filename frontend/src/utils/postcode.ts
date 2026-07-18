interface KakaoPostcodeResult {
    zonecode: string
    roadAddress: string
    jibunAddress: string
}

interface KakaoPostcodeConstructor {
    new (options: { oncomplete: (data: KakaoPostcodeResult) => void; width?: string; height?: string }): {
        embed: (element: HTMLElement) => void
    }
}

declare global {
    interface Window {
        kakao?: { Postcode: KakaoPostcodeConstructor }
    }
}

let scriptPromise: Promise<void> | null = null

function loadPostcodeScript() {
    if (window.kakao?.Postcode) return Promise.resolve()
    if (scriptPromise) return scriptPromise
    scriptPromise = new Promise((resolve, reject) => {
        const script = document.createElement('script')
        script.src = 'https://t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'
        script.async = true
        script.onload = () => resolve()
        script.onerror = () => reject(new Error('주소 검색 서비스를 불러오지 못했습니다.'))
        document.head.appendChild(script)
    })
    return scriptPromise
}

export async function embedPostcodeSearch(
    element: HTMLElement,
    onComplete: (postalCode: string, address: string) => void,
) {
    await loadPostcodeScript()
    if (!window.kakao?.Postcode) throw new Error('주소 검색 서비스를 사용할 수 없습니다.')
    new window.kakao.Postcode({
        oncomplete: (data) => {
            const address = data.roadAddress || data.jibunAddress
            onComplete(data.zonecode, address)
        },
        width: '100%',
        height: '100%',
    }).embed(element)
}
