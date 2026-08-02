import { ImagePlus, MoveLeft, MoveRight, Trash2 } from 'lucide-react'
import {
    useEffect,
    useRef,
    useState,
    type ChangeEvent,
    type DragEvent,
} from 'react'
import { resolveImageUrl } from '../../utils/product'

interface PendingImage {
    id: number
    file: File
    previewUrl: string
}

interface ExistingImage {
    imageUrl: string
}

interface ProductImageUploadFieldProps {
    label: string
    description: string
    existingImages?: ExistingImage[]
    maxFiles?: number
    multiple?: boolean
    onFilesChange?: (files: File[]) => void
    onExistingImageRemove?: (index: number) => void
}

let pendingImageId = 0

export function ProductImageUploadField({
    label,
    description,
    existingImages = [],
    maxFiles = 10,
    multiple = true,
    onFilesChange,
    onExistingImageRemove,
}: ProductImageUploadFieldProps) {
    const [pendingImages, setPendingImages] = useState<PendingImage[]>([])
    const [validationMessage, setValidationMessage] = useState('')
    const objectUrls = useRef(new Set<string>())

    useEffect(() => () => {
        objectUrls.current.forEach((url) => URL.revokeObjectURL(url))
    }, [])

    const replacePendingImages = (next: PendingImage[]) => {
        setPendingImages(next)
        onFilesChange?.(next.map(({ file }) => file))
    }

    const addFiles = (files: FileList | File[]) => {
        const selectedFiles = Array.from(files)
        const imageFiles = selectedFiles.filter((file) =>
            ['image/jpeg', 'image/png', 'image/webp'].includes(file.type),
        )
        const availableCount = multiple
            ? Math.max(0, maxFiles - existingImages.length - pendingImages.length)
            : 1
        const acceptedFiles = imageFiles.slice(0, availableCount)
        if (imageFiles.length !== selectedFiles.length) {
            setValidationMessage('JPG, PNG, WEBP 이미지 파일만 선택할 수 있습니다.')
        } else if (acceptedFiles.length !== imageFiles.length) {
            setValidationMessage(`이미지는 최대 ${maxFiles}장까지 등록할 수 있습니다.`)
        } else {
            setValidationMessage('')
        }
        const nextImages = acceptedFiles.map((file) => {
            const previewUrl = URL.createObjectURL(file)
            objectUrls.current.add(previewUrl)

            return {
                id: pendingImageId++,
                file,
                previewUrl,
            }
        })

        if (!multiple) {
            pendingImages.forEach(({ previewUrl }) => {
                URL.revokeObjectURL(previewUrl)
                objectUrls.current.delete(previewUrl)
            })
            replacePendingImages(nextImages)
            return
        }

        replacePendingImages([...pendingImages, ...nextImages])
    }

    const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
        if (event.target.files) addFiles(event.target.files)
        event.target.value = ''
    }

    const handleDrop = (event: DragEvent<HTMLLabelElement>) => {
        event.preventDefault()
        addFiles(event.dataTransfer.files)
    }

    const removeImage = (id: number) => {
        const target = pendingImages.find((image) => image.id === id)
        if (target) {
            URL.revokeObjectURL(target.previewUrl)
            objectUrls.current.delete(target.previewUrl)
        }
        replacePendingImages(pendingImages.filter((image) => image.id !== id))
    }

    const moveImage = (index: number, direction: -1 | 1) => {
        const nextIndex = index + direction
        if (nextIndex < 0 || nextIndex >= pendingImages.length) return

        const next = [...pendingImages]
        ;[next[index], next[nextIndex]] = [next[nextIndex], next[index]]
        replacePendingImages(next)
    }

    return (
        <div className="grid gap-3 text-xs font-bold min-[701px]:col-span-2">
            <span>{label}</span>
            <div className="grid gap-4">
                <div>
                    <p className="font-normal text-muted">{description}</p>
                    <p className="mt-1 font-normal text-muted">
                        JPG, PNG, WEBP · 파일당 최대 10MB · 최대 {maxFiles}장
                    </p>
                </div>

                <label
                    className="grid min-h-32 cursor-pointer place-items-center border border-dashed border-line bg-surface px-5 py-6 text-center transition-colors hover:border-ink"
                    onDragOver={(event) => event.preventDefault()}
                    onDrop={handleDrop}
                >
                    <span className="grid justify-items-center gap-2">
                        <ImagePlus className="size-7" aria-hidden="true" />
                        <strong>이미지를 선택하거나 여기에 끌어다 놓으세요</strong>
                        <span className="font-normal text-muted">내 컴퓨터에서 이미지 파일 선택</span>
                    </span>
                    <input
                        className="sr-only"
                        type="file"
                        accept="image/jpeg,image/png,image/webp"
                        multiple={multiple}
                        aria-label={`${label} 파일 선택`}
                        onChange={handleFileChange}
                    />
                </label>

                {validationMessage && (
                    <p className="text-xs font-normal text-danger" role="alert">
                        {validationMessage}
                    </p>
                )}

                {existingImages.length > 0 && (
                    <div>
                        <p className="mb-2">현재 등록된 이미지</p>
                        <div className="flex flex-wrap gap-3">
                            {existingImages.map((image, index) => (
                                <figure
                                    className="w-28 overflow-hidden border border-line bg-surface"
                                    key={`${image.imageUrl}-${index}`}
                                >
                                    <img
                                        className="aspect-square w-full object-cover"
                                        src={resolveImageUrl(image.imageUrl)}
                                        alt={`${label} 현재 이미지 ${index + 1}`}
                                    />
                                    <figcaption className="px-2 py-1.5 font-normal text-muted">
                                        <span className="flex items-center justify-between gap-1">
                                            <span>등록됨</span>
                                            {onExistingImageRemove && (
                                                <button
                                                    className="p-1 text-danger"
                                                    type="button"
                                                    aria-label={`${label} 현재 이미지 ${index + 1} 삭제`}
                                                    onClick={() => onExistingImageRemove(index)}
                                                >
                                                    <Trash2 className="size-3.5" />
                                                </button>
                                            )}
                                        </span>
                                    </figcaption>
                                </figure>
                            ))}
                        </div>
                    </div>
                )}

                {pendingImages.length > 0 && (
                    <div>
                        <p className="mb-2">새로 선택한 이미지</p>
                        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                            {pendingImages.map((image, index) => (
                                <article
                                    className="overflow-hidden border border-line bg-surface"
                                    key={image.id}
                                >
                                    <img
                                        className="aspect-[4/3] w-full object-cover"
                                        src={image.previewUrl}
                                        alt={`${label} 미리보기 ${index + 1}`}
                                    />
                                    <div className="flex items-center gap-1 border-t border-line p-2">
                                        <span className="min-w-0 flex-1 truncate font-normal">
                                            {image.file.name}
                                        </span>
                                        {multiple && (
                                            <>
                                                <button
                                                    className="p-1.5 disabled:opacity-30"
                                                    type="button"
                                                    disabled={index === 0}
                                                    aria-label={`${image.file.name} 왼쪽으로 이동`}
                                                    onClick={() => moveImage(index, -1)}
                                                >
                                                    <MoveLeft className="size-4" />
                                                </button>
                                                <button
                                                    className="p-1.5 disabled:opacity-30"
                                                    type="button"
                                                    disabled={index === pendingImages.length - 1}
                                                    aria-label={`${image.file.name} 오른쪽으로 이동`}
                                                    onClick={() => moveImage(index, 1)}
                                                >
                                                    <MoveRight className="size-4" />
                                                </button>
                                            </>
                                        )}
                                        <button
                                            className="p-1.5 text-danger"
                                            type="button"
                                            aria-label={`${image.file.name} 삭제`}
                                            onClick={() => removeImage(image.id)}
                                        >
                                            <Trash2 className="size-4" />
                                        </button>
                                    </div>
                                </article>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </div>
    )
}
