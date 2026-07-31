import { LoaderCircle, PackageCheck, Pencil, Store, Trash2 } from 'lucide-react'
import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { useSearchParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { uploadProductImage } from '../api/files'
import { getCategories } from '../api/products'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { ProductCategorySelector } from '../components/seller/ProductCategorySelector'
import { ProductImageUploadField } from '../components/seller/ProductImageUploadField'
import {
    ManagementListSearch,
    ManagementPagination,
} from '../components/management/ManagementListUi'
import {
    createSellerProduct,
    createSellerProfile,
    deleteSellerProduct,
    getSellerProduct,
    getSellerProducts,
    getSellerProfile,
    updateSellerProduct,
    updateSellerProfile,
} from '../api/seller'
import type { Category } from '../types/product'
import type {
    SellerProductSummary,
    SellerProductRequest,
    SellerProductStockCondition,
    SellerProfile,
} from '../types/seller'
import { formatPrice } from '../utils/product'
import {
    findFirstLeafCategoryId,
    getCategoryChildren,
} from '../utils/productCategory'

const emptyProduct: SellerProductRequest = {
    categoryId: 0,
    name: '',
    description: '',
    brand: '',
    price: 0,
    discountPercentage: 0,
    discountStartDate: null,
    discountEndDate: null,
    stock: 0,
    thumbnailUrl: '',
    freeShipping: true,
    shippingFee: 0,
    estimatedDeliveryDays: 3,
    images: [],
    detailImages: [],
}

export function SellerManagementPage() {
    const [searchParams, setSearchParams] = useSearchParams()
    const [profile, setProfile] = useState<SellerProfile | null>(null)
    const [profileForm, setProfileForm] = useState({ storeName: '', businessNumber: '', description: '' })
    const [products, setProducts] = useState<SellerProductSummary[]>([])
    const [categories, setCategories] = useState<Category[]>([])
    const [productForm, setProductForm] = useState<SellerProductRequest>(emptyProduct)
    const [thumbnailFiles, setThumbnailFiles] = useState<File[]>([])
    const [productImageFiles, setProductImageFiles] = useState<File[]>([])
    const [detailImageFiles, setDetailImageFiles] = useState<File[]>([])
    const [imageInputVersion, setImageInputVersion] = useState(0)
    const [editingProductId, setEditingProductId] = useState<number | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [isSaving, setIsSaving] = useState(false)
    const [productTotalPages, setProductTotalPages] = useState(0)
    const [productTotalElements, setProductTotalElements] = useState(0)
    const [message, setMessage] = useState('')
    const [errorMessage, setErrorMessage] = useState('')
    const [productToDelete, setProductToDelete] = useState<SellerProductSummary | null>(null)
    const productPage = positivePage(searchParams.get('page'))
    const productKeyword = searchParams.get('keyword') ?? ''
    const rootCategoryId = positiveId(searchParams.get('rootCategoryId'))
    const middleCategoryId = positiveId(searchParams.get('middleCategoryId'))
    const leafCategoryId = positiveId(searchParams.get('categoryId'))
    const selectedCategoryId = leafCategoryId ?? middleCategoryId ?? rootCategoryId
    const stockCondition = parseStockCondition(searchParams.get('stockCondition'))
    const stockQuantity = nonNegativeInteger(searchParams.get('stockQuantity'))
    const rootCategories = getCategoryChildren(categories, null)
    const middleCategories = rootCategoryId
        ? getCategoryChildren(categories, rootCategoryId)
        : []
    const leafCategories = middleCategoryId
        ? getCategoryChildren(categories, middleCategoryId)
        : []

    useEffect(() => {
        const controller = new AbortController()
        Promise.all([
            getSellerProfile(controller.signal).catch((error: unknown) => {
                if (error instanceof ApiError && error.code === 'SELLER_PROFILE_NOT_FOUND') return null
                throw error
            }),
            getCategories(controller.signal),
        ]).then(([profileResponse, categoryResponse]) => {
            setCategories(categoryResponse)
            setProductForm((current) => ({
                ...current,
                categoryId: current.categoryId || findFirstLeafCategoryId(categoryResponse),
            }))
            if (!profileResponse) return
            setProfile(profileResponse)
            setProfileForm({
                storeName: profileResponse.storeName,
                businessNumber: profileResponse.businessNumber,
                description: profileResponse.description ?? '',
            })
        }).catch((error: unknown) => {
            if (error instanceof Error && error.name === 'AbortError') return
            setErrorMessage(error instanceof ApiError ? error.message : '판매자 정보를 불러오지 못했습니다.')
        }).finally(() => {
            if (!controller.signal.aborted) setIsLoading(false)
        })
        return () => controller.abort()
    }, [])

    useEffect(() => {
        if (!profile) return
        const controller = new AbortController()
        getSellerProducts({
            page: productPage,
            keyword: productKeyword,
            categoryId: selectedCategoryId,
            stockCondition,
            stockQuantity,
            signal: controller.signal,
        }).then((response) => {
            setProducts(response.content)
            setProductTotalPages(response.totalPages)
            setProductTotalElements(response.totalElements)
        }).catch((error: unknown) => {
            if (error instanceof Error && error.name === 'AbortError') return
            setErrorMessage(error instanceof ApiError
                ? error.message
                : '상품 목록을 불러오지 못했습니다.')
        })
        return () => controller.abort()
    }, [
        productKeyword,
        productPage,
        profile,
        selectedCategoryId,
        stockCondition,
        stockQuantity,
    ])

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
            setMessage(profile ? '판매자 정보가 수정되었습니다.' : '판매자 등록이 완료되었습니다.')
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '판매자 정보를 저장하지 못했습니다.')
        } finally {
            setIsSaving(false)
        }
    }

    function resetPendingImages() {
        setThumbnailFiles([])
        setProductImageFiles([])
        setDetailImageFiles([])
        setImageInputVersion((current) => current + 1)
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
                setMessage('상품이 수정되었으며 재승인을 기다립니다.')
            } else {
                await createSellerProduct(request)
                setMessage('상품이 등록되었으며 승인을 기다립니다.')
            }
            setEditingProductId(null)
            setProductForm({
                ...emptyProduct,
                categoryId: findFirstLeafCategoryId(categories),
            })
            resetPendingImages()
            const response = await getSellerProducts({
                page: productPage,
                keyword: productKeyword,
                categoryId: selectedCategoryId,
                stockCondition,
                stockQuantity,
            })
            setProducts(response.content)
            setProductTotalPages(response.totalPages)
            setProductTotalElements(response.totalElements)
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '상품을 저장하지 못했습니다.')
        } finally {
            setIsSaving(false)
        }
    }

    async function startEditing(productId: number) {
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
                stock: product.stock,
                thumbnailUrl: product.thumbnailUrl ?? '',
                freeShipping: product.freeShipping,
                shippingFee: product.shippingFee,
                estimatedDeliveryDays: product.estimatedDeliveryDays,
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
    }

    async function removeProduct(productId: number) {
        setIsSaving(true)
        setErrorMessage('')
        try {
            await deleteSellerProduct(productId)
            const response = await getSellerProducts({
                page: productPage,
                keyword: productKeyword,
                categoryId: selectedCategoryId,
                stockCondition,
                stockQuantity,
            })
            setProducts(response.content)
            setProductTotalPages(response.totalPages)
            setProductTotalElements(response.totalElements)
            setProductToDelete(null)
            setMessage('상품이 삭제되었습니다.')
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '상품을 삭제하지 못했습니다.')
        } finally {
            setIsSaving(false)
        }
    }

    function updateCategoryFilter(
        name: 'rootCategoryId' | 'middleCategoryId' | 'categoryId',
        value: string,
    ) {
        const next = new URLSearchParams(searchParams)
        if (value) next.set(name, value)
        else next.delete(name)
        if (name === 'rootCategoryId') {
            next.delete('middleCategoryId')
            next.delete('categoryId')
        } else if (name === 'middleCategoryId') {
            next.delete('categoryId')
        }
        next.set('page', '1')
        setSearchParams(next)
    }

    function updateStockFilter(
        name: 'stockCondition' | 'stockQuantity',
        value: string,
    ) {
        const next = new URLSearchParams(searchParams)
        if (value) next.set(name, value)
        else next.delete(name)
        next.set('page', '1')
        setSearchParams(next)
    }

    if (isLoading) {
        return <div className="grid min-h-100 place-content-center"><LoaderCircle className="size-6 animate-spin" /></div>
    }

    return (
        <section className="mx-auto max-w-300 px-4 py-12 min-[601px]:px-8 min-[601px]:py-18">
            <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">SELLER CENTER</p>
            <h1 className="mb-8 font-serif text-[clamp(40px,6vw,64px)] leading-none tracking-tighter">판매자 관리</h1>
            {message && <FeedbackMessage className="mb-5" tone="success">{message}</FeedbackMessage>}
            {errorMessage && <FeedbackMessage className="mb-5" tone="error">{errorMessage}</FeedbackMessage>}

            <div className="grid gap-8">
                <Panel icon={<Store />} title="판매자 정보">
                    <form className="grid gap-4 min-[701px]:grid-cols-2" onSubmit={saveProfile}>
                        <Field label="상점명" value={profileForm.storeName} onChange={(value) => setProfileForm({ ...profileForm, storeName: value })} required />
                        <Field label="사업자 번호" value={profileForm.businessNumber} onChange={(value) => setProfileForm({ ...profileForm, businessNumber: value })} required disabled={profile !== null} />
                        <label className="grid gap-2 text-xs font-bold min-[701px]:col-span-2">소개<textarea className="min-h-24 border border-line p-3 font-normal" value={profileForm.description} onChange={(event) => setProfileForm({ ...profileForm, description: event.target.value })} /></label>
                        <button className="h-11 bg-ink px-6 text-xs font-bold text-white disabled:opacity-50 min-[701px]:w-fit" disabled={isSaving} type="submit">{profile ? '정보 수정' : '판매자 등록'}</button>
                    </form>
                </Panel>

                {profile && <>
                    <Panel icon={<PackageCheck />} title="상품 관리">
                        <form className="mb-8 grid gap-4 border-b border-line pb-8 min-[701px]:grid-cols-2" onSubmit={saveProduct}>
                            <div className="grid gap-2 text-xs font-bold">
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
                            <Field label="상품명" value={productForm.name} onChange={(value) => setProductForm({ ...productForm, name: value })} required />
                            <Field label="브랜드" value={productForm.brand} onChange={(value) => setProductForm({ ...productForm, brand: value })} />
                            <p className="text-xs text-muted min-[701px]:col-span-2">
                                선택한 이미지는 상품을 저장할 때 업로드됩니다. 업로드가 끝날 때까지 창을 닫지 마세요.
                            </p>
                            <ProductImageUploadField
                                key={`thumbnail-${imageInputVersion}`}
                                label="대표 이미지"
                                description="상품 목록과 상세 화면에서 가장 먼저 보이는 대표 사진입니다."
                                existingImages={productForm.thumbnailUrl
                                    ? [{ imageUrl: productForm.thumbnailUrl }]
                                    : []}
                                maxFiles={1}
                                multiple={false}
                                onFilesChange={setThumbnailFiles}
                                onExistingImageRemove={() => setProductForm({
                                    ...productForm,
                                    thumbnailUrl: '',
                                })}
                            />
                            <ProductImageUploadField
                                key={`gallery-${imageInputVersion}`}
                                label="추가 상품 이미지"
                                description="상품 상세 상단에서 작은 썸네일로 보여 줄 사진입니다."
                                existingImages={productForm.images}
                                maxFiles={10}
                                onFilesChange={setProductImageFiles}
                                onExistingImageRemove={(index) => setProductForm({
                                    ...productForm,
                                    images: productForm.images
                                        .filter((_, imageIndex) => imageIndex !== index)
                                        .map((image, sortOrder) => ({ ...image, sortOrder })),
                                })}
                            />
                            <ProductImageUploadField
                                key={`detail-${imageInputVersion}`}
                                label="상세 설명 이미지"
                                description="상품정보 탭에 위에서 아래 순서로 이어지는 상세 설명 사진입니다."
                                existingImages={productForm.detailImages}
                                maxFiles={20}
                                onFilesChange={setDetailImageFiles}
                                onExistingImageRemove={(index) => setProductForm({
                                    ...productForm,
                                    detailImages: productForm.detailImages
                                        .filter((_, imageIndex) => imageIndex !== index)
                                        .map((image, sortOrder) => ({ ...image, sortOrder })),
                                })}
                            />
                            <NumberField label="가격" value={productForm.price} onChange={(value) => setProductForm({ ...productForm, price: value })} min={1} />
                            <NumberField label="재고" value={productForm.stock} onChange={(value) => setProductForm({ ...productForm, stock: value })} min={0} />
                            <NumberField label="할인율" value={productForm.discountPercentage} onChange={(value) => setProductForm({ ...productForm, discountPercentage: value })} min={0} max={100} />
                            <DateField label="할인 시작" value={productForm.discountStartDate} onChange={(value) => setProductForm({ ...productForm, discountStartDate: value })} required={productForm.discountPercentage > 0} />
                            <DateField label="할인 종료" value={productForm.discountEndDate} onChange={(value) => setProductForm({ ...productForm, discountEndDate: value })} required={productForm.discountPercentage > 0} />
                            <label className="flex h-11 items-center gap-2 text-xs font-bold">
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
                            </label>
                            {!productForm.freeShipping && <NumberField label="배송비" value={productForm.shippingFee} onChange={(value) => setProductForm({ ...productForm, shippingFee: value })} min={1} />}
                            <NumberField label="예상 배송기간(일)" value={productForm.estimatedDeliveryDays} onChange={(value) => setProductForm({ ...productForm, estimatedDeliveryDays: value })} min={1} max={30} />
                            <label className="grid gap-2 text-xs font-bold min-[701px]:col-span-2">설명<textarea className="min-h-24 border border-line p-3 font-normal" value={productForm.description} onChange={(event) => setProductForm({ ...productForm, description: event.target.value })} /></label>
                            <div className="flex gap-2">
                                <button className="h-11 bg-ink px-6 text-xs font-bold text-white disabled:opacity-50" disabled={isSaving} type="submit">{editingProductId ? '상품 수정' : '상품 등록'}</button>
                                {editingProductId && <button className="h-11 border border-line px-5 text-xs font-bold" type="button" onClick={() => { setEditingProductId(null); setProductForm({ ...emptyProduct, categoryId: findFirstLeafCategoryId(categories) }); resetPendingImages() }}>취소</button>}
                            </div>
                        </form>
                        <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
                            <strong className="text-sm">
                                등록 상품 {productTotalElements.toLocaleString()}개
                            </strong>
                            <span className="text-xs text-muted">
                                카테고리·재고 조건과 검색어를 함께 적용할 수 있습니다.
                            </span>
                        </div>
                        <div className="mb-5 grid gap-3 md:grid-cols-2 xl:grid-cols-[1fr_1fr_1fr_1.2fr]">
                            <CategoryFilter
                                label="대분류"
                                emptyLabel="전체 대분류"
                                categories={rootCategories}
                                value={rootCategoryId}
                                onChange={(value) => updateCategoryFilter(
                                    'rootCategoryId',
                                    value,
                                )}
                            />
                            <CategoryFilter
                                label="중분류"
                                emptyLabel="전체 중분류"
                                categories={middleCategories}
                                value={middleCategoryId}
                                disabled={!rootCategoryId}
                                onChange={(value) => updateCategoryFilter(
                                    'middleCategoryId',
                                    value,
                                )}
                            />
                            <CategoryFilter
                                label="소분류"
                                emptyLabel="전체 소분류"
                                categories={leafCategories}
                                value={leafCategoryId}
                                disabled={!middleCategoryId}
                                onChange={(value) => updateCategoryFilter(
                                    'categoryId',
                                    value,
                                )}
                            />
                            <label className="grid gap-1.5 text-xs font-bold">
                                재고 수량
                                <span className="flex">
                                    <input
                                        className="h-11 min-w-0 flex-1 border border-r-0 border-line bg-surface px-3 text-sm font-normal text-ink"
                                        min="0"
                                        placeholder="수량 입력"
                                        type="number"
                                        value={stockQuantity ?? ''}
                                        onChange={(event) => updateStockFilter(
                                            'stockQuantity',
                                            event.target.value,
                                        )}
                                    />
                                    <select
                                        aria-label="재고 수량 비교 조건"
                                        className="h-11 border border-line bg-surface px-3 text-sm font-normal text-ink"
                                        value={stockCondition}
                                        onChange={(event) => updateStockFilter(
                                            'stockCondition',
                                            event.target.value,
                                        )}
                                    >
                                        <option value="GTE">개 이상</option>
                                        <option value="LTE">개 이하</option>
                                    </select>
                                </span>
                            </label>
                        </div>
                        <ManagementListSearch
                            key={productKeyword}
                            placeholder="상품명 또는 브랜드를 검색하세요"
                        />
                        <div className="grid gap-3">
                            {products.length === 0 ? (
                                <p className="text-sm text-muted">등록한 상품이 없습니다.</p>
                            ) : products.map((product) => (
                                <div className="flex flex-wrap items-center justify-between gap-3 border border-line p-4" key={product.productId}>
                                    <div>
                                        <strong>{product.name}</strong>
                                        <p className="mt-1 text-xs text-muted">
                                            {formatPrice(product.price)} · 재고 {product.stock} · {product.status}
                                        </p>
                                        {product.status === 'REJECTED' && product.rejectionReason && (
                                            <p className="mt-2 text-xs font-bold text-[#a22e24]">
                                                반려 사유: {product.rejectionReason}
                                            </p>
                                        )}
                                    </div>
                                    <div className="flex gap-2">
                                        <button className="p-2" type="button" aria-label="상품 수정" onClick={() => startEditing(product.productId)}><Pencil className="size-4" /></button>
                                        <button className="p-2 text-[#a22e24]" type="button" aria-label="상품 삭제" onClick={() => setProductToDelete(product)}><Trash2 className="size-4" /></button>
                                    </div>
                                </div>
                            ))}
                        </div>
                        <ManagementPagination
                            page={productPage}
                            totalPages={productTotalPages}
                        />
                    </Panel>

                </>}
            </div>
            <ConfirmDialog
                open={productToDelete !== null}
                title="상품을 삭제할까요?"
                description={`삭제한 상품은 복구할 수 없습니다.${productToDelete ? ` '${productToDelete.name}' 상품을 삭제합니다.` : ''}`}
                confirmLabel="상품 삭제"
                isPending={isSaving}
                onCancel={() => setProductToDelete(null)}
                onConfirm={() => {
                    if (productToDelete) void removeProduct(productToDelete.productId)
                }}
            />
        </section>
    )
}

function Panel({ icon, title, children }: { icon: ReactNode; title: string; children: ReactNode }) {
    return <section className="border-t-2 border-ink pt-5"><h2 className="mb-6 flex items-center gap-2 text-xl font-bold">{icon}{title}</h2>{children}</section>
}

function Field({ label, value, onChange, required = false, disabled = false }: { label: string; value: string; onChange: (value: string) => void; required?: boolean; disabled?: boolean }) {
    return <label className="grid gap-2 text-xs font-bold">{label}<input className="h-11 border border-line bg-surface px-3 font-normal text-ink disabled:bg-surface disabled:text-muted" value={value} onChange={(event) => onChange(event.target.value)} required={required} disabled={disabled} /></label>
}

function NumberField({ label, value, onChange, min, max }: { label: string; value: number; onChange: (value: number) => void; min: number; max?: number }) {
    return <label className="grid gap-2 text-xs font-bold">{label}<input className="h-11 border border-line px-3 font-normal" type="number" value={value} min={min} max={max} onChange={(event) => onChange(Number(event.target.value))} required /></label>
}

function DateField({ label, value, onChange, required }: { label: string; value: string | null; onChange: (value: string | null) => void; required: boolean }) {
    return <label className="grid gap-2 text-xs font-bold">{label}<input className="h-11 border border-line bg-surface px-3 font-normal text-ink" type="date" value={value ?? ''} required={required} onChange={(event) => onChange(event.target.value || null)} /></label>
}

function CategoryFilter({
    label,
    emptyLabel,
    categories,
    value,
    disabled = false,
    onChange,
}: {
    label: string
    emptyLabel: string
    categories: Category[]
    value?: number
    disabled?: boolean
    onChange: (value: string) => void
}) {
    return (
        <label className="grid gap-1.5 text-xs font-bold">
            {label}
            <select
                className="h-11 border border-line bg-surface px-3 text-sm font-normal text-ink"
                disabled={disabled}
                value={value ?? ''}
                onChange={(event) => onChange(event.target.value)}
            >
                <option value="">{emptyLabel}</option>
                {categories.map((category) => (
                    <option key={category.categoryId} value={category.categoryId}>
                        {category.name}
                    </option>
                ))}
            </select>
        </label>
    )
}

function positivePage(value: string | null) {
    const parsed = Number(value)
    return Number.isInteger(parsed) && parsed > 0 ? parsed : 1
}

function positiveId(value: string | null) {
    const parsed = Number(value)
    return Number.isInteger(parsed) && parsed > 0 ? parsed : undefined
}

function nonNegativeInteger(value: string | null) {
    if (value == null || value.trim() === '') return undefined
    const parsed = Number(value)
    return Number.isInteger(parsed) && parsed >= 0 ? parsed : undefined
}

function parseStockCondition(value: string | null): SellerProductStockCondition {
    return value === 'LTE' ? 'LTE' : 'GTE'
}
