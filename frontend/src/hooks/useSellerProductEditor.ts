import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { uploadProductImage } from '../api/files'
import { getCategories } from '../api/products'
import {
    createSellerProduct,
    getSellerProduct,
    getSellerProfile,
    updateSellerProduct,
} from '../api/seller'
import { useToast } from '../toast/useToast'
import type { Category, ProductDetail } from '../types/product'
import type { SellerProductRequest } from '../types/seller'
import { findFirstLeafCategoryId } from '../utils/productCategory'

const emptyProduct: SellerProductRequest = {
    categoryId: 0,
    name: '',
    description: '',
    brand: '',
    price: 0,
    discountPercentage: 0,
    discountStartDate: null,
    discountEndDate: null,
    freeShipping: true,
    shippingFee: 0,
    estimatedDeliveryDays: 3,
    stock: 0,
    thumbnailUrl: '',
    images: [],
    detailImages: [],
}

export function useSellerProductEditor(initialProductId?: number) {
    const { showToast } = useToast()
    const [hasProfile, setHasProfile] = useState(false)
    const [categories, setCategories] = useState<Category[]>([])
    const [productForm, setProductForm] = useState<SellerProductRequest>(emptyProduct)
    const [thumbnailFiles, setThumbnailFiles] = useState<File[]>([])
    const [productImageFiles, setProductImageFiles] = useState<File[]>([])
    const [detailImageFiles, setDetailImageFiles] = useState<File[]>([])
    const [imageInputVersion, setImageInputVersion] = useState(0)
    const [editingProductId, setEditingProductId] = useState<number | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [isSaving, setIsSaving] = useState(false)
    const [errorMessage, setErrorMessage] = useState('')

    const resetPendingImages = useCallback(() => {
        setThumbnailFiles([])
        setProductImageFiles([])
        setDetailImageFiles([])
        setImageInputVersion((current) => current + 1)
    }, [])

    const resetEditor = useCallback(() => {
        setEditingProductId(null)
        setProductForm({
            ...emptyProduct,
            categoryId: findFirstLeafCategoryId(categories),
        })
        resetPendingImages()
    }, [categories, resetPendingImages])

    useEffect(() => {
        const controller = new AbortController()
        async function loadPage() {
            const profileResponse = await getSellerProfile(controller.signal)
                .catch((error: unknown) => {
                    if (error instanceof ApiError && error.code === 'SELLER_PROFILE_NOT_FOUND') {
                        return null
                    }
                    throw error
                })
            if (!profileResponse) return
            setHasProfile(true)
            const categoryResponse = await getCategories(controller.signal)
            setCategories(categoryResponse)
            setProductForm((current) => ({
                ...current,
                categoryId: current.categoryId || findFirstLeafCategoryId(categoryResponse),
            }))
        }

        void loadPage()
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(
                    error instanceof ApiError
                        ? error.message
                        : '판매자 정보를 불러오지 못했습니다.',
                )
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [])

    const startEditing = useCallback(async (productId: number) => {
        setErrorMessage('')
        try {
            const product = await getSellerProduct(productId)
            resetPendingImages()
            setEditingProductId(productId)
            setProductForm(toProductRequest(product))
        } catch (error) {
            setErrorMessage(
                error instanceof ApiError ? error.message : '상품 정보를 불러오지 못했습니다.',
            )
        }
    }, [resetPendingImages])

    useEffect(() => {
        if (!initialProductId) return
        const timeoutId = window.setTimeout(() => {
            void startEditing(initialProductId)
        }, 0)
        return () => window.clearTimeout(timeoutId)
    }, [initialProductId, startEditing])

    async function saveProduct(event: FormEvent) {
        event.preventDefault()
        setIsSaving(true)
        setErrorMessage('')
        try {
            const [thumbnailUpload, imageUploads, detailImageUploads] = await Promise.all([
                thumbnailFiles[0]
                    ? uploadProductImage(thumbnailFiles[0])
                    : Promise.resolve(null),
                uploadImages(productImageFiles),
                uploadImages(detailImageFiles),
            ])
            const request: SellerProductRequest = {
                ...productForm,
                thumbnailUrl: thumbnailUpload?.thumbnailUrl ?? productForm.thumbnailUrl,
                images: [
                    ...productForm.images,
                    ...imageUploads.map((upload, index) => ({
                        originalUrl: upload.fileUrl,
                        imageUrl: upload.fileUrl,
                        sortOrder: productForm.images.length + index,
                    })),
                ],
                detailImages: [
                    ...productForm.detailImages,
                    ...detailImageUploads.map((upload, index) => ({
                        originalUrl: upload.fileUrl,
                        imageUrl: upload.fileUrl,
                        sortOrder: productForm.detailImages.length + index,
                    })),
                ],
            }
            if (editingProductId) {
                await updateSellerProduct(editingProductId, request)
                showToast(
                    '상품 정보가 저장되었습니다. 콘텐츠 변경사항은 심사 후 반영됩니다.',
                    'success',
                )
            } else {
                await createSellerProduct(request)
                showToast('상품이 등록되었으며 승인을 기다립니다.', 'success')
            }
            resetEditor()
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '상품을 저장하지 못했습니다.')
        } finally {
            setIsSaving(false)
        }
    }

    return {
        hasProfile,
        categories,
        productForm,
        imageInputVersion,
        editingProductId,
        isLoading,
        isSaving,
        errorMessage,
        setProductForm,
        setThumbnailFiles,
        setProductImageFiles,
        setDetailImageFiles,
        resetEditor,
        saveProduct,
    }
}

async function uploadImages(files: File[]) {
    const uploads = []
    for (const file of files) {
        uploads.push(await uploadProductImage(file))
    }
    return uploads
}

function toProductRequest(product: ProductDetail): SellerProductRequest {
    return {
        categoryId: product.category.categoryId,
        name: product.name,
        description: product.description ?? '',
        brand: product.brand ?? '',
        price: product.price,
        discountPercentage: product.discountPercentage,
        discountStartDate: product.discountStartDate,
        discountEndDate: product.discountEndDate,
        freeShipping: product.freeShipping,
        shippingFee: product.shippingFee,
        estimatedDeliveryDays: product.estimatedDeliveryDays,
        stock: product.stock,
        thumbnailUrl: product.thumbnailUrl ?? '',
        images: product.images.map((image) => ({
            originalUrl: image.originalUrl,
            imageUrl: image.imageUrl,
            sortOrder: image.sortOrder,
        })),
        detailImages: (product.detailImages ?? []).map((image) => ({
            originalUrl: image.originalUrl,
            imageUrl: image.imageUrl,
            sortOrder: image.sortOrder,
        })),
    }
}
