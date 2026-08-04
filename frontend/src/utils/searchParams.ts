export function parsePositiveInteger(
    value: string | null,
    fallback: number,
) {
    const parsed = Number(value)
    return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback
}
