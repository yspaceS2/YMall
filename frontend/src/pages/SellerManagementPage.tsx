import { LoaderCircle, PackageCheck, Store } from 'lucide-react'
import { useCallback, useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { ApiError } from '../api/client'
import { uploadProductImage } from '../api/files'
import { getCategories } from '../api/products'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { ProductImageUploadField } from '../components/seller/ProductImageUploadField'
import { SellerDashboard } from '../components/dashboard/SellerDashboard'
import {
    ProductCategorySelector,
} from '../components/seller/ProductCategorySelector'
import {
    createSellerProduct,
    createSellerProfile,
    getSellerProduct,
    getSellerProfile,
    updateSellerProduct,
    updateSellerProfile,
} from '../api/seller'
import type { Category } from '../types/product'
import type { SellerProductRequest, SellerProfile } from '../types/seller'
import { findFirstLeafCategoryId } from '../utils/productCategory'
import { useToast } from '../toast/useToast'

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

type SellerSection = 'dashboard' | 'profile' | 'products'

export function SellerManagementPage({
    section = 'dashboard',
    initialProductId,
}: {
    section?: SellerSection
    initialProductId?: number
} = {}) {
    const { showToast } = useToast()
    const activeSection = section
    const [profile, setProfile] = useState<SellerProfile | null>(null)
    const [profileForm, setProfileForm] = useState({ storeName: '', businessNumber: '', description: '' })
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

    useEffect(() => {
        const controller = new AbortController()
        async function loadPage() {
            const profileResponse = await getSellerProfile(controller.signal).catch((error: unknown) => {
                if (error instanceof ApiError && error.code === 'SELLER_PROFILE_NOT_FOUND') return null
                throw error
            })
            if (!profileResponse) return
            setProfile(profileResponse)
            setProfileForm({
                storeName: profileResponse.storeName,
                businessNumber: profileResponse.businessNumber,
                description: profileResponse.description ?? '',
            })

            if (activeSection !== 'products') return
            const categoryResponse = await getCategories(controller.signal)
            setCategories(categoryResponse)
            setProductForm((current) => ({
                ...current,
                categoryId: current.categoryId || findFirstLeafCategoryId(categoryResponse),
            }))
        }

        void loadPage().catch((error: unknown) => {
            if (error instanceof Error && error.name === 'AbortError') return
            setErrorMessage(error instanceof ApiError ? error.message : '판매자 정보를 불러오지 못했습니다.')
        }).finally(() => {
            if (!controller.signal.aborted) setIsLoading(false)
        })
        return () => controller.abort()
    }, [activeSection])

    async function saveProfile(event: FormEvent) {
        event.preventDefault()
        setIsSaving(true)
        setErrorMessage('')
        try {
            const saved = profile
                ? await updateSellerProfile({
                    storeName: profileForm.storeName,
                    description: profileForm.description,
                })
                : await createSellerProfile(profileForm)
            setProfile(saved)
            showToast(
                profile ? '판매자 정보가 수정되었습니다.' : '판매자 등록이 완료되었습니다.',
                'success',
            )
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '판매자 정보를 저장하지 못했습니다.')
        } finally {
            setIsSaving(false)
        }
    }

    async function uploadImages(files: File[]) {
        const uploads = []
        for (const file of files) {
            uploads.push(await uploadProductImage(file))
        }
        return uploads
    }

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
                showToast('상품 정보가 저장되었습니다. 콘텐츠 변경사항은 심사 후 반영됩니다.', 'success')
            } else {
                await createSellerProduct(request)
                showToast('상품이 등록되었으며 승인을 기다립니다.', 'success')
            }
            setEditingProductId(null)
            setProductForm({
                ...emptyProduct,
                categoryId: findFirstLeafCategoryId(categories),
            })
            resetPendingImages()
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '상품을 저장하지 못했습니다.')
        } finally {
            setIsSaving(false)
        }
    }

    const startEditing = useCallback(async (productId: number) => {
        setErrorMessage('')
        try {
            const product = await getSellerProduct(productId)
            resetPendingImages()
            setEditingProductId(productId)
            setProductForm({
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
            })
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '상품 정보를 불러오지 못했습니다.')
        }
    }, [resetPendingImages])

    useEffect(() => {
        if (!initialProductId) return
        const timeoutId = window.setTimeout(() => {
            void startEditing(initialProductId)
        }, 0)
        return () => window.clearTimeout(timeoutId)
    }, [initialProductId, startEditing])

    if (isLoading) {
        return <div className="grid min-h-100 place-content-center"><LoaderCircle className="size-6 animate-spin" /></div>
    }

    return (
        <section
            className={`mx-auto max-w-350 px-4 min-[601px]:px-8 ${
                activeSection === 'dashboard' ? 'py-3' : 'py-10 min-[601px]:py-14'
            }`}
            id="management-overview"
        >
            {activeSection !== 'dashboard' && <>
                <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-accent">SELLER CENTER</p>
                <h1 className="mb-8 font-serif text-[clamp(40px,6vw,64px)] leading-none tracking-tighter">판매자 관리</h1>
            </>}
            {errorMessage && <FeedbackMessage className="mb-5" tone="error">{errorMessage}</FeedbackMessage>}

            <div className="grid gap-8">
                {activeSection === 'dashboard' && (
                    profile
                        ? <SellerDashboard />
                        : <FeedbackMessage tone="error">판매자 정보를 등록하면 통계를 확인할 수 있습니다.</FeedbackMessage>
                )}

                {activeSection === 'profile' && (
                <Panel icon={<Store />} title="판매자 정보">
                    <form className="grid gap-4 min-[701px]:grid-cols-2" onSubmit={saveProfile}>
                        <Field label="상점명" value={profileForm.storeName} onChange={(value) => setProfileForm({ ...profileForm, storeName: value })} required />
                        <Field label="사업자 번호" value={profileForm.businessNumber} onChange={(value) => setProfileForm({ ...profileForm, businessNumber: value })} required disabled={profile !== null} />
                        <label className="grid gap-2 text-xs font-bold min-[701px]:col-span-2">소개<textarea className="min-h-24 border border-line p-3 font-normal" value={profileForm.description} onChange={(event) => setProfileForm({ ...profileForm, description: event.target.value })} /></label>
                        <button className="h-11 bg-ink px-6 text-xs font-bold text-white disabled:opacity-50 min-[701px]:w-fit" disabled={isSaving} type="submit">{profile ? '정보 수정' : '판매자 등록'}</button>
                    </form>
                </Panel>
                )}

                {!profile && activeSection === 'products' && (
                    <FeedbackMessage tone="error">
                        판매자 정보를 먼저 등록해야 이 관리 기능을 사용할 수 있습니다.
                    </FeedbackMessage>
                )}

                {profile && activeSection === 'products' && (
                    <Panel icon={<PackageCheck />} title="상품 관리">
                        <form className="mb-8 grid grid-cols-1 overflow-hidden border-y-2 border-ink min-[701px]:grid-cols-2" onSubmit={saveProduct}>
                            <div className={`${productFormRowClassName} min-[701px]:col-span-2`}>
                                <span>카테고리</span>
                                <ProductCategorySelector
                                    categories={categories}
                                    value={productForm.categoryId}
                                    onChange={(categoryId) => setProductForm({
                                        ...productForm,
                                        categoryId,
                                    })}
                                />
                            </div>
                            <Field label="상품명" value={productForm.name} onChange={(value) => setProductForm({ ...productForm, name: value })} required row />
                            <Field label="브랜드" value={productForm.brand} onChange={(value) => setProductForm({ ...productForm, brand: value })} row />
                            <div className="border-b border-line bg-surface px-4 py-3 text-xs text-muted min-[701px]:col-span-2 min-[701px]:pl-[180px]">
                                선택한 이미지는 상품을 저장할 때 함께 업로드됩니다. 업로드가 끝날 때까지
                                창을 닫지 마세요.
                            </div>
                            <ProductImageUploadField
                                key={`thumbnail-${imageInputVersion}`}
                                label="대표 이미지"
                                description="상품 목록과 상세 화면에서 가장 먼저 보이는 대표 사진입니다."
                                existingImages={productForm.thumbnailUrl
                                    ? [{
                                        imageUrl: productForm.thumbnailUrl,
                                    }]
                                    : []}
                                maxFiles={1}
                                multiple={false}
                                onFilesChange={setThumbnailFiles}
                            />
                            <ProductImageUploadField
                                key={`gallery-${imageInputVersion}`}
                                label="추가 상품 이미지"
                                description="상품 상세 상단에서 작은 썸네일로 보여 줄 사진입니다."
                                existingImages={productForm.images}
                                maxFiles={10}
                                onFilesChange={setProductImageFiles}
                            />
                            <ProductImageUploadField
                                key={`detail-${imageInputVersion}`}
                                label="상세 설명 이미지"
                                description="상품정보 탭에 위에서 아래 순서로 이어지는 상세 설명 사진입니다."
                                existingImages={productForm.detailImages}
                                maxFiles={20}
                                onFilesChange={setDetailImageFiles}
                            />
                            <NumberField label="가격" value={productForm.price} onChange={(value) => setProductForm({ ...productForm, price: value })} min={1} />
                            <NumberField label="재고" value={productForm.stock} onChange={(value) => setProductForm({ ...productForm, stock: value })} min={0} />
                            <NumberField
                                label="할인율"
                                value={productForm.discountPercentage}
                                onChange={(value) => setProductForm({
                                    ...productForm,
                                    discountPercentage: value,
                                    discountStartDate: value > 0 ? productForm.discountStartDate : null,
                                    discountEndDate: value > 0 ? productForm.discountEndDate : null,
                                })}
                                min={0}
                                max={100}
                                fullWidth
                                compact
                            />
                            <DateField
                                label="할인 시작일"
                                value={productForm.discountStartDate ?? ''}
                                onChange={(value) => setProductForm({
                                    ...productForm,
                                    discountStartDate: value || null,
                                })}
                                max={productForm.discountEndDate ?? undefined}
                                disabled={productForm.discountPercentage <= 0}
                            />
                            <DateField
                                label="할인 종료일"
                                value={productForm.discountEndDate ?? ''}
                                onChange={(value) => setProductForm({
                                    ...productForm,
                                    discountEndDate: value || null,
                                })}
                                min={getDiscountEndMinimum(productForm.discountStartDate)}
                                disabled={productForm.discountPercentage <= 0}
                            />
                            <label className={productFormRowClassName}>
                                <span>배송 방식</span>
                                <span className="flex min-h-11 items-center gap-3 border border-line bg-surface px-3 font-normal text-ink">
                                    <input
                                        type="checkbox"
                                        checked={productForm.freeShipping}
                                        onChange={(event) => setProductForm({
                                            ...productForm,
                                            freeShipping: event.target.checked,
                                            shippingFee: event.target.checked ? 0 : productForm.shippingFee,
                                        })}
                                    />
                                    무료배송
                                </span>
                            </label>
                            <NumberField
                                label="예상 배송기간(일)"
                                value={productForm.estimatedDeliveryDays}
                                onChange={(value) => setProductForm({ ...productForm, estimatedDeliveryDays: value })}
                                min={1}
                                max={30}
                            />
                            {!productForm.freeShipping && (
                                <NumberField
                                    label="배송비"
                                    value={productForm.shippingFee}
                                    onChange={(value) => setProductForm({ ...productForm, shippingFee: value })}
                                    min={1}
                                    fullWidth
                                />
                            )}
                            <label className={`${productFormRowClassName} min-[701px]:col-span-2`}><span>설명</span><textarea className="min-h-28 resize-y border border-line bg-surface p-3 font-normal text-ink" value={productForm.description} onChange={(event) => setProductForm({ ...productForm, description: event.target.value })} /></label>
                            <div className="flex flex-wrap gap-2 border-b border-line px-4 py-4 last:border-b-0 min-[701px]:col-span-2 min-[701px]:pl-[180px]">
                                <button className="h-11 bg-ink px-6 text-xs font-bold text-white disabled:opacity-50" disabled={isSaving} type="submit">{editingProductId ? '상품 수정' : '상품 등록'}</button>
                                {editingProductId && <button className="h-11 border border-line px-5 text-xs font-bold" type="button" onClick={() => { setEditingProductId(null); setProductForm({ ...emptyProduct, categoryId: findFirstLeafCategoryId(categories) }); resetPendingImages() }}>취소</button>}
                            </div>
                        </form>
                    </Panel>
                )}

            </div>
        </section>
    )
}

function Panel({ icon, title, children }: { icon: ReactNode; title: string; children: ReactNode }) {
    return <section className="border-t-2 border-ink pt-5"><h2 className="mb-6 flex items-center gap-2 text-xl font-bold">{icon}{title}</h2>{children}</section>
}

function Field({ label, value, onChange, required = false, disabled = false, row = false }: { label: string; value: string; onChange: (value: string) => void; required?: boolean; disabled?: boolean; row?: boolean }) {
    return <label className={row ? productFormRowClassName : 'grid gap-2 text-xs font-bold'}><span>{label}</span><input className="h-11 border border-line bg-surface px-3 font-normal text-ink disabled:bg-surface disabled:text-muted" value={value} onChange={(event) => onChange(event.target.value)} required={required} disabled={disabled} /></label>
}

function NumberField({ label, value, onChange, min, max, fullWidth = false, compact = false }: { label: string; value: number; onChange: (value: number) => void; min: number; max?: number; fullWidth?: boolean; compact?: boolean }) {
    const className = fullWidth
        ? `${productFormRowClassName} min-[701px]:col-span-2`
        : productFormRowClassName
    const inputClassName = compact
        ? 'h-11 border border-line bg-surface px-3 font-normal text-ink min-[701px]:w-40'
        : 'h-11 border border-line bg-surface px-3 font-normal text-ink'
    return <label className={className}><span>{label}</span><input className={inputClassName} type="number" value={value} min={min} max={max} onChange={(event) => onChange(Number(event.target.value))} required /></label>
}

function DateField({ label, value, onChange, min, max, disabled = false }: { label: string; value: string; onChange: (value: string) => void; min?: string; max?: string; disabled?: boolean }) {
    return <label className={productFormRowClassName}><span>{label}</span><input className="h-11 border border-line bg-surface px-3 font-normal text-ink disabled:text-muted" type="date" value={value} min={min} max={max} onChange={(event) => onChange(event.target.value)} required={!disabled} disabled={disabled} /></label>
}

const productFormRowClassName =
    'grid gap-2 border-b border-line px-4 py-4 text-xs font-bold last:border-b-0 min-[701px]:grid-cols-[140px_minmax(0,1fr)] min-[701px]:items-start min-[701px]:gap-6'

function getSeoulDate() {
    const parts = new Intl.DateTimeFormat('en-CA', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        timeZone: 'Asia/Seoul',
    }).formatToParts(new Date())
    const values = Object.fromEntries(parts.map((part) => [part.type, part.value]))
    return `${values.year}-${values.month}-${values.day}`
}

function getDiscountEndMinimum(discountStartDate: string | null) {
    const today = getSeoulDate()
    return discountStartDate && discountStartDate > today ? discountStartDate : today
}
