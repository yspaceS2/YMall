import { apiRequest } from './client'

export interface FileUploadResponse {
    originalFileName: string
    storedFileName: string
    fileUrl: string
    thumbnailFileName: string
    thumbnailUrl: string
    size: number
    contentType: string
}

export function uploadProductImage(file: File) {
    const formData = new FormData()
    formData.append('file', file)

    return apiRequest<FileUploadResponse>('/files/images', {
        method: 'POST',
        body: formData,
    })
}
