import { LoaderCircle, PackageCheck, Pencil, Store, Trash2, Truck } from 'lucide-react'
import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { ApiError } from '../api/client'
import { getCategories } from '../api/products'
import {
    createSellerProduct,
    createSellerProfile,
    deleteSellerProduct,
    getSellerOrders,
    getSellerProduct,
    getSellerProducts,
    getSellerProfile,
    updateSellerOrderStatus,
    updateSellerProduct,
    updateSellerProfile,
} from '../api/seller'
import type { Category, ProductSummary } from '../types/product'
import type {
    FulfillmentStatus,
    SellerOrder,
    SellerProductRequest,
    SellerProfile,
} from '../types/seller'
import { formatPrice } from '../utils/product'

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
}

const nextStatus: Partial<Record<FulfillmentStatus, FulfillmentStatus>> = {
    PENDING: 'PREPARING',
    PREPARING: 'SHIPPED',
    SHIPPED: 'DELIVERED',
}

const statusLabel: Record<FulfillmentStatus, string> = {
    PENDING: '처리 대기',
    PREPARING: '상품 준비 중',
    SHIPPED: '배송 중',
    DELIVERED: '배송 완료',
}

export function SellerManagementPage() {
    const [profile, setProfile] = useState<SellerProfile | null>(null)
    const [profileForm, setProfileForm] = useState({ storeName: '', businessNumber: '', description: '' })
    const [products, setProducts] = useState<ProductSummary[]>([])
    const [orders, setOrders] = useState<SellerOrder[]>([])
    const [categories, setCategories] = useState<Category[]>([])
    const [productForm, setProductForm] = useState<SellerProductRequest>(emptyProduct)
    const [editingProductId, setEditingProductId] = useState<number | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [isSaving, setIsSaving] = useState(false)
    const [message, setMessage] = useState('')
    const [errorMessage, setErrorMessage] = useState('')

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
                categoryId: current.categoryId || categoryResponse[0]?.categoryId || 0,
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
        const [productResponse, orderResponse] = await Promise.all([
            getSellerProducts(signal),
            getSellerOrders(signal),
        ])
        setProducts(productResponse.content)
        setOrders(orderResponse.content)
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
            setProductForm({ ...emptyProduct, categoryId: categories[0]?.categoryId ?? 0 })
            const response = await getSellerProducts()
            setProducts(response.content)
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
                images: [],
            })
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '상품 정보를 불러오지 못했습니다.')
        }
    }

    async function removeProduct(productId: number) {
        if (!window.confirm('이 상품을 삭제하시겠습니까?')) return
        try {
            await deleteSellerProduct(productId)
            setProducts((current) => current.filter((product) => product.productId !== productId))
            setMessage('상품이 삭제되었습니다.')
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '상품을 삭제하지 못했습니다.')
        }
    }

    async function advanceOrder(order: SellerOrder) {
        const currentStatus = order.items[0]?.fulfillmentStatus
        const target = currentStatus ? nextStatus[currentStatus] : undefined
        if (!target) return
        try {
            const updated = await updateSellerOrderStatus(order.orderId, target)
            setOrders((current) => current.map((item) => item.orderId === updated.orderId ? updated : item))
            setMessage(`주문 #${order.orderId}의 배송 상태가 변경되었습니다.`)
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '배송 상태를 변경하지 못했습니다.')
        }
    }

    if (isLoading) {
        return <div className="grid min-h-100 place-content-center"><LoaderCircle className="size-6 animate-spin" /></div>
    }

    return (
        <section className="mx-auto max-w-300 px-4 py-12 min-[601px]:px-8 min-[601px]:py-18">
            <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">SELLER CENTER</p>
            <h1 className="mb-8 font-serif text-[clamp(40px,6vw,64px)] leading-none tracking-tighter">판매자 관리</h1>
            {message && <p className="mb-5 border border-[#cad39b] bg-[#f4f6e8] p-3 text-sm">{message}</p>}
            {errorMessage && <p className="mb-5 border border-[#e2b9b4] bg-[#fff5f3] p-3 text-sm text-[#a22e24]" role="alert">{errorMessage}</p>}

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
                            <label className="grid gap-2 text-xs font-bold">카테고리<select className="h-11 border border-line bg-white px-3 font-normal" value={productForm.categoryId} onChange={(event) => setProductForm({ ...productForm, categoryId: Number(event.target.value) })}>{categories.map((category) => <option key={category.categoryId} value={category.categoryId}>{category.name}</option>)}</select></label>
                            <Field label="상품명" value={productForm.name} onChange={(value) => setProductForm({ ...productForm, name: value })} required />
                            <Field label="브랜드" value={productForm.brand} onChange={(value) => setProductForm({ ...productForm, brand: value })} />
                            <Field label="대표 이미지 URL" value={productForm.thumbnailUrl} onChange={(value) => setProductForm({ ...productForm, thumbnailUrl: value })} />
                            <NumberField label="가격" value={productForm.price} onChange={(value) => setProductForm({ ...productForm, price: value })} min={1} />
                            <NumberField label="재고" value={productForm.stock} onChange={(value) => setProductForm({ ...productForm, stock: value })} min={0} />
                            <NumberField label="할인율" value={productForm.discountPercentage} onChange={(value) => setProductForm({ ...productForm, discountPercentage: value })} min={0} max={100} />
                            <label className="grid gap-2 text-xs font-bold min-[701px]:col-span-2">설명<textarea className="min-h-24 border border-line p-3 font-normal" value={productForm.description} onChange={(event) => setProductForm({ ...productForm, description: event.target.value })} /></label>
                            <div className="flex gap-2">
                                <button className="h-11 bg-ink px-6 text-xs font-bold text-white disabled:opacity-50" disabled={isSaving} type="submit">{editingProductId ? '상품 수정' : '상품 등록'}</button>
                                {editingProductId && <button className="h-11 border border-line px-5 text-xs font-bold" type="button" onClick={() => { setEditingProductId(null); setProductForm({ ...emptyProduct, categoryId: categories[0]?.categoryId ?? 0 }) }}>취소</button>}
                            </div>
                        </form>
                        <div className="grid gap-3">{products.length === 0 ? <p className="text-sm text-muted">등록한 상품이 없습니다.</p> : products.map((product) => <div className="flex flex-wrap items-center justify-between gap-3 border border-line p-4" key={product.productId}><div><strong>{product.name}</strong><p className="mt-1 text-xs text-muted">{formatPrice(product.price)} · 재고 {product.stock} · {product.status}</p></div><div className="flex gap-2"><button className="p-2" type="button" aria-label="상품 수정" onClick={() => startEditing(product.productId)}><Pencil className="size-4" /></button><button className="p-2 text-[#a22e24]" type="button" aria-label="상품 삭제" onClick={() => removeProduct(product.productId)}><Trash2 className="size-4" /></button></div></div>)}</div>
                    </Panel>

                    <Panel icon={<Truck />} title="주문·배송 관리">
                        <div className="grid gap-4">{orders.length === 0 ? <p className="text-sm text-muted">처리할 주문이 없습니다.</p> : orders.map((order) => { const current = order.items[0]?.fulfillmentStatus; const target = current ? nextStatus[current] : undefined; return <article className="border border-line p-4" key={order.orderId}><div className="flex flex-wrap items-center justify-between gap-3"><div><strong>주문 #{order.orderId}</strong><p className="mt-1 text-xs text-muted">{new Date(order.createdAt).toLocaleString('ko-KR')} · 판매 금액 {formatPrice(order.sellerAmount)}</p></div>{current && <span className="bg-[#eef0df] px-3 py-1 text-xs font-bold text-[#66751c]">{statusLabel[current]}</span>}</div><ul className="my-4 grid gap-1 text-sm">{order.items.map((item) => <li key={item.orderItemId}>{item.productName} × {item.quantity}</li>)}</ul>{target && <button className="h-10 border border-ink px-4 text-xs font-bold" type="button" onClick={() => advanceOrder(order)}>{statusLabel[target]}{target === 'DELIVERED' ? '로' : '으로'} 변경</button>}</article> })}</div>
                    </Panel>
                </>}
            </div>
        </section>
    )
}

function Panel({ icon, title, children }: { icon: ReactNode; title: string; children: ReactNode }) {
    return <section className="border-t-2 border-ink pt-5"><h2 className="mb-6 flex items-center gap-2 text-xl font-bold">{icon}{title}</h2>{children}</section>
}

function Field({ label, value, onChange, required = false, disabled = false }: { label: string; value: string; onChange: (value: string) => void; required?: boolean; disabled?: boolean }) {
    return <label className="grid gap-2 text-xs font-bold">{label}<input className="h-11 border border-line px-3 font-normal disabled:bg-[#f4f4f0]" value={value} onChange={(event) => onChange(event.target.value)} required={required} disabled={disabled} /></label>
}

function NumberField({ label, value, onChange, min, max }: { label: string; value: number; onChange: (value: number) => void; min: number; max?: number }) {
    return <label className="grid gap-2 text-xs font-bold">{label}<input className="h-11 border border-line px-3 font-normal" type="number" value={value} min={min} max={max} onChange={(event) => onChange(Number(event.target.value))} required /></label>
}
