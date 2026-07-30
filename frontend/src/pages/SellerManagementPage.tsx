import { LoaderCircle, PackageCheck, Pencil, Store, Trash2 } from 'lucide-react'
import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { ApiError } from '../api/client'
import { getCategories } from '../api/products'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { SettlementAccountPanel } from '../components/seller/SettlementAccountPanel'
import { SettlementRequestPanel } from '../components/seller/SettlementRequestPanel'
import { ProductCategorySelector } from '../components/seller/ProductCategorySelector'
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
    SellerProfile,
} from '../types/seller'
import { formatPrice } from '../utils/product'
import { findFirstLeafCategoryId } from '../utils/productCategory'

const emptyProduct: SellerProductRequest = {
    categoryId: 0,
    name: '',
    description: '',
    brand: '',
    price: 0,
    discountPercentage: 0,
    stock: 0,
    thumbnailUrl: '',
    images: [],
    detailImages: [],
}

export function SellerManagementPage() {
    const [profile, setProfile] = useState<SellerProfile | null>(null)
    const [profileForm, setProfileForm] = useState({ storeName: '', businessNumber: '', description: '' })
    const [products, setProducts] = useState<SellerProductSummary[]>([])
    const [categories, setCategories] = useState<Category[]>([])
    const [productForm, setProductForm] = useState<SellerProductRequest>(emptyProduct)
    const [editingProductId, setEditingProductId] = useState<number | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [isSaving, setIsSaving] = useState(false)
    const [isLoadingMoreProducts, setIsLoadingMoreProducts] = useState(false)
    const [nextProductPage, setNextProductPage] = useState(2)
    const [hasMoreProducts, setHasMoreProducts] = useState(false)
    const [message, setMessage] = useState('')
    const [errorMessage, setErrorMessage] = useState('')
    const [productToDelete, setProductToDelete] = useState<SellerProductSummary | null>(null)

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
            return loadManagementData(controller.signal)
        }).catch((error: unknown) => {
            if (error instanceof Error && error.name === 'AbortError') return
            setErrorMessage(error instanceof ApiError ? error.message : '판매자 정보를 불러오지 못했습니다.')
        }).finally(() => {
            if (!controller.signal.aborted) setIsLoading(false)
        })
        return () => controller.abort()
    }, [])

    async function loadManagementData(signal?: AbortSignal) {
        const productResponse = await getSellerProducts({ signal })
        setProducts(productResponse.content)
        setHasMoreProducts(productResponse.hasNext)
        setNextProductPage(2)
    }

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
            if (!profile) await loadManagementData()
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '판매자 정보를 저장하지 못했습니다.')
        } finally {
            setIsSaving(false)
        }
    }

    async function saveProduct(event: FormEvent) {
        event.preventDefault()
        setIsSaving(true)
        setErrorMessage('')
        try {
            if (editingProductId) {
                await updateSellerProduct(editingProductId, productForm)
                setMessage('상품이 수정되었으며 재승인을 기다립니다.')
            } else {
                await createSellerProduct(productForm)
                setMessage('상품이 등록되었으며 승인을 기다립니다.')
            }
            setEditingProductId(null)
            setProductForm({
                ...emptyProduct,
                categoryId: findFirstLeafCategoryId(categories),
            })
            const response = await getSellerProducts()
            setProducts(response.content)
            setHasMoreProducts(response.hasNext)
            setNextProductPage(2)
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
            setEditingProductId(productId)
            setProductForm({
                categoryId: product.category.categoryId,
                name: product.name,
                description: product.description ?? '',
                brand: product.brand ?? '',
                price: product.price,
                discountPercentage: product.discountPercentage,
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
    }

    async function removeProduct(productId: number) {
        setIsSaving(true)
        setErrorMessage('')
        try {
            await deleteSellerProduct(productId)
            const response = await getSellerProducts()
            setProducts(response.content)
            setHasMoreProducts(response.hasNext)
            setNextProductPage(2)
            setProductToDelete(null)
            setMessage('상품이 삭제되었습니다.')
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '상품을 삭제하지 못했습니다.')
        } finally {
            setIsSaving(false)
        }
    }

    async function loadMoreProducts() {
        if (!hasMoreProducts || isLoadingMoreProducts) return
        setIsLoadingMoreProducts(true)
        setErrorMessage('')
        try {
            const response = await getSellerProducts({ page: nextProductPage })
            setProducts((current) => [...current, ...response.content])
            setHasMoreProducts(response.hasNext)
            setNextProductPage((current) => current + 1)
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '상품을 추가로 불러오지 못했습니다.')
        } finally {
            setIsLoadingMoreProducts(false)
        }
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
                    <SettlementAccountPanel />
                    <SettlementRequestPanel />
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
                            <Field label="대표 이미지 URL" value={productForm.thumbnailUrl} onChange={(value) => setProductForm({ ...productForm, thumbnailUrl: value })} />
                            <ImageUrlListField
                                label="상단 갤러리 이미지 URL"
                                description="한 줄에 하나씩 입력하면 상품 상단의 작은 썸네일 순서로 표시됩니다."
                                images={productForm.images}
                                onChange={(images) => setProductForm({ ...productForm, images })}
                            />
                            <ImageUrlListField
                                label="상세 설명 이미지 URL"
                                description="한 줄에 하나씩 입력한 순서대로 상품정보 탭에 세로로 이어집니다."
                                images={productForm.detailImages}
                                onChange={(detailImages) => setProductForm({ ...productForm, detailImages })}
                            />
                            <NumberField label="가격" value={productForm.price} onChange={(value) => setProductForm({ ...productForm, price: value })} min={1} />
                            <NumberField label="재고" value={productForm.stock} onChange={(value) => setProductForm({ ...productForm, stock: value })} min={0} />
                            <NumberField label="할인율" value={productForm.discountPercentage} onChange={(value) => setProductForm({ ...productForm, discountPercentage: value })} min={0} max={100} />
                            <label className="grid gap-2 text-xs font-bold min-[701px]:col-span-2">설명<textarea className="min-h-24 border border-line p-3 font-normal" value={productForm.description} onChange={(event) => setProductForm({ ...productForm, description: event.target.value })} /></label>
                            <div className="flex gap-2">
                                <button className="h-11 bg-ink px-6 text-xs font-bold text-white disabled:opacity-50" disabled={isSaving} type="submit">{editingProductId ? '상품 수정' : '상품 등록'}</button>
                                {editingProductId && <button className="h-11 border border-line px-5 text-xs font-bold" type="button" onClick={() => { setEditingProductId(null); setProductForm({ ...emptyProduct, categoryId: findFirstLeafCategoryId(categories) }) }}>취소</button>}
                            </div>
                        </form>
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
                        {hasMoreProducts && <button className="mx-auto mt-5 grid h-10 min-w-32 place-items-center border border-ink px-5 text-xs font-bold disabled:opacity-50" type="button" disabled={isLoadingMoreProducts} onClick={loadMoreProducts}>{isLoadingMoreProducts ? <LoaderCircle className="size-4 animate-spin" /> : '상품 더 보기'}</button>}
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

function ImageUrlListField({
    label,
    description,
    images,
    onChange,
}: {
    label: string
    description: string
    images: SellerProductRequest['images']
    onChange: (images: SellerProductRequest['images']) => void
}) {
    const value = images.map((image) => image.imageUrl).join('\n')

    const updateImages = (nextValue: string) => {
        onChange(
            nextValue
                .split('\n')
                .map((imageUrl) => imageUrl.trim())
                .filter(Boolean)
                .map((imageUrl, sortOrder) => ({
                    originalUrl: imageUrl,
                    imageUrl,
                    sortOrder,
                })),
        )
    }

    return (
        <label className="grid gap-2 text-xs font-bold min-[701px]:col-span-2">
            {label}
            <span className="font-normal text-muted">{description}</span>
            <textarea
                className="min-h-28 resize-y border border-line bg-surface p-3 font-mono text-xs font-normal text-ink"
                value={value}
                placeholder={'https://example.com/image-01.jpg\nhttps://example.com/image-02.jpg'}
                onChange={(event) => updateImages(event.target.value)}
            />
            {images.length > 0 && (
                <span className="font-normal text-muted">현재 {images.length}개 · 입력한 줄 순서대로 저장됩니다.</span>
            )}
        </label>
    )
}
