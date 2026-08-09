export const SEOUL_TIME_ZONE = 'Asia/Seoul'

const koreanDateTimeFormatter = new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: SEOUL_TIME_ZONE,
})

const koreanDateFormatter = new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeZone: SEOUL_TIME_ZONE,
})

export function formatKoreanDateTime(value: string) {
    return format(value, koreanDateTimeFormatter)
}

export function formatKoreanDate(value: string) {
    return format(value, koreanDateFormatter)
}

function format(value: string, formatter: Intl.DateTimeFormat) {
    const date = new Date(value)
    return Number.isNaN(date.getTime()) ? '' : formatter.format(date)
}
