import { FileText, Image as ImageIcon } from 'lucide-react'
import { useEffect, useState } from 'react'
import { getAccessToken } from '../../auth/tokenStorage'
import type { SupportAttachment } from '../../types/support'

export function SupportAttachmentItem({ attachment }: { attachment: SupportAttachment }) {
    const [objectUrl, setObjectUrl] = useState<string | null>(null)
    const isImage = attachment.contentType.startsWith('image/')

    useEffect(() => {
        if (!isImage) return
        const controller = new AbortController()
        void fetch(attachment.downloadUrl, {
            headers: { Authorization: `Bearer ${getAccessToken() ?? ''}` },
            signal: controller.signal,
        })
            .then((response) => {
                if (!response.ok) throw new Error('첨부 이미지를 불러오지 못했습니다.')
                return response.blob()
            })
            .then((blob) => setObjectUrl(URL.createObjectURL(blob)))
            .catch((error: unknown) => {
                if (!(error instanceof Error) || error.name !== 'AbortError') setObjectUrl(null)
            })
        return () => controller.abort()
    }, [attachment.downloadUrl, isImage])

    useEffect(() => () => {
        if (objectUrl) URL.revokeObjectURL(objectUrl)
    }, [objectUrl])

    async function openAttachment() {
        const response = await fetch(attachment.downloadUrl, {
            headers: { Authorization: `Bearer ${getAccessToken() ?? ''}` },
        })
        if (!response.ok) return
        const url = URL.createObjectURL(await response.blob())
        window.open(url, '_blank', 'noopener,noreferrer')
        window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
    }

    return (
        <button
            className="overflow-hidden border border-line bg-surface text-left"
            type="button"
            onClick={() => void openAttachment()}
        >
            {isImage && objectUrl && (
                <img
                    alt={attachment.fileName}
                    className="max-h-72 w-full object-contain"
                    src={objectUrl}
                />
            )}
            <span className="flex items-center gap-2 px-3 py-2 text-xs">
                {isImage
                    ? <ImageIcon className="size-4 shrink-0" />
                    : <FileText className="size-4 shrink-0" />}
                <span className="min-w-0 flex-1 truncate">{attachment.fileName}</span>
                <span className="text-muted">{formatFileSize(attachment.fileSize)}</span>
            </span>
        </button>
    )
}

function formatFileSize(bytes: number) {
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${Math.ceil(bytes / 1024)} KB`
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
