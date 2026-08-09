import type { Dispatch, FormEvent, SetStateAction } from 'react'
import type { Category } from '../../types/product'
import type { SellerProductRequest } from '../../types/seller'
import { ProductCategorySelector } from './ProductCategorySelector'
import { ProductImageUploadField } from './ProductImageUploadField'

interface SellerProductFormProps {
    categories: Category[]
    productForm: SellerProductRequest
    imageInputVersion: number
    editingProductId: number | null
    isSaving: boolean
    setProductForm: Dispatch<SetStateAction<SellerProductRequest>>
    onThumbnailFilesChange: (files: File[]) => void
    onProductImageFilesChange: (files: File[]) => void
    onDetailImageFilesChange: (files: File[]) => void
    onCancel: () => void
    onSubmit: (event: FormEvent) => void
}

export function SellerProductForm({
    categories,
    productForm,
    imageInputVersion,
    editingProductId,
    isSaving,
    setProductForm,
    onThumbnailFilesChange,
    onProductImageFilesChange,
    onDetailImageFilesChange,
    onCancel,
    onSubmit,
}: SellerProductFormProps) {
    function updateForm(values: Partial<SellerProductRequest>) {
        setProductForm((current) => ({ ...current, ...values }))
    }

    return (
        <form
            className="mb-8 grid grid-cols-1 overflow-hidden border-y-2 border-ink min-[701px]:grid-cols-2"
            onSubmit={onSubmit}
        >
            <div className={`${productFormRowClassName} min-[701px]:col-span-2`}>
                <span>카테고리</span>
                <ProductCategorySelector
                    categories={categories}
                    value={productForm.categoryId}
                    onChange={(categoryId) => updateForm({ categoryId })}
                />
            </div>
            <TextField
                label="상품명"
                value={productForm.name}
                onChange={(name) => updateForm({ name })}
                required
            />
            <TextField
                label="브랜드"
                value={productForm.brand}
                onChange={(brand) => updateForm({ brand })}
            />
            <div className="border-b border-line bg-surface px-4 py-3 text-xs text-muted min-[701px]:col-span-2 min-[701px]:pl-[180px]">
                선택한 이미지는 상품을 저장할 때 함께 업로드됩니다. 업로드가 끝날 때까지
                창을 닫지 마세요.
            </div>
            <ProductImageUploadField
                key={`thumbnail-${imageInputVersion}`}
                label="대표 이미지"
                description="상품 목록과 상세 화면에서 가장 먼저 보이는 대표 사진입니다."
                existingImages={productForm.thumbnailUrl ? [{ imageUrl: productForm.thumbnailUrl }] : []}
                maxFiles={1}
                multiple={false}
                onFilesChange={onThumbnailFilesChange}
            />
            <ProductImageUploadField
                key={`gallery-${imageInputVersion}`}
                label="추가 상품 이미지"
                description="상품 상세 상단에서 작은 썸네일로 보여 줄 사진입니다."
                existingImages={productForm.images}
                maxFiles={10}
                onFilesChange={onProductImageFilesChange}
            />
            <ProductImageUploadField
                key={`detail-${imageInputVersion}`}
                label="상세 설명 이미지"
                description="상품정보 탭에 위에서 아래 순서로 이어지는 상세 설명 사진입니다."
                existingImages={productForm.detailImages}
                maxFiles={20}
                onFilesChange={onDetailImageFilesChange}
            />
            <NumberField
                label="가격"
                value={productForm.price}
                onChange={(price) => updateForm({ price })}
                min={1}
            />
            <NumberField
                label="재고"
                value={productForm.stock}
                onChange={(stock) => updateForm({ stock })}
                min={0}
            />
            <NumberField
                label="할인율"
                value={productForm.discountPercentage}
                onChange={(discountPercentage) => updateForm({
                    discountPercentage,
                    discountStartDate: discountPercentage > 0
                        ? productForm.discountStartDate
                        : null,
                    discountEndDate: discountPercentage > 0 ? productForm.discountEndDate : null,
                })}
                min={0}
                max={100}
                fullWidth
                compact
            />
            <DateField
                label="할인 시작일"
                value={productForm.discountStartDate ?? ''}
                onChange={(value) => updateForm({ discountStartDate: value || null })}
                max={productForm.discountEndDate ?? undefined}
                disabled={productForm.discountPercentage <= 0}
            />
            <DateField
                label="할인 종료일"
                value={productForm.discountEndDate ?? ''}
                onChange={(value) => updateForm({ discountEndDate: value || null })}
                min={getDiscountEndMinimum(productForm.discountStartDate)}
                disabled={productForm.discountPercentage <= 0}
            />
            <label className={productFormRowClassName}>
                <span>배송 방식</span>
                <span className="flex min-h-11 items-center gap-3 border border-line bg-surface px-3 font-normal text-ink">
                    <input
                        type="checkbox"
                        checked={productForm.freeShipping}
                        onChange={(event) => updateForm({
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
                onChange={(estimatedDeliveryDays) => updateForm({ estimatedDeliveryDays })}
                min={1}
                max={30}
            />
            {!productForm.freeShipping && (
                <NumberField
                    label="배송비"
                    value={productForm.shippingFee}
                    onChange={(shippingFee) => updateForm({ shippingFee })}
                    min={1}
                    fullWidth
                />
            )}
            <label className={`${productFormRowClassName} min-[701px]:col-span-2`}>
                <span>설명</span>
                <textarea
                    className="min-h-28 resize-y border border-line bg-surface p-3 font-normal text-ink"
                    value={productForm.description}
                    onChange={(event) => updateForm({ description: event.target.value })}
                />
            </label>
            <div className="flex flex-wrap gap-2 border-b border-line px-4 py-4 last:border-b-0 min-[701px]:col-span-2 min-[701px]:pl-[180px]">
                <button
                    className="h-11 bg-ink px-6 text-xs font-bold text-white disabled:opacity-50"
                    disabled={isSaving}
                    type="submit"
                >
                    {editingProductId ? '상품 수정' : '상품 등록'}
                </button>
                {editingProductId && (
                    <button
                        className="h-11 border border-line px-5 text-xs font-bold"
                        type="button"
                        onClick={onCancel}
                    >
                        취소
                    </button>
                )}
            </div>
        </form>
    )
}

function TextField({
    label,
    value,
    onChange,
    required = false,
}: {
    label: string
    value: string
    onChange: (value: string) => void
    required?: boolean
}) {
    return (
        <label className={productFormRowClassName}>
            <span>{label}</span>
            <input
                className="h-11 border border-line bg-surface px-3 font-normal text-ink"
                value={value}
                onChange={(event) => onChange(event.target.value)}
                required={required}
            />
        </label>
    )
}

function NumberField({
    label,
    value,
    onChange,
    min,
    max,
    fullWidth = false,
    compact = false,
}: {
    label: string
    value: number
    onChange: (value: number) => void
    min: number
    max?: number
    fullWidth?: boolean
    compact?: boolean
}) {
    const className = fullWidth
        ? `${productFormRowClassName} min-[701px]:col-span-2`
        : productFormRowClassName
    const inputClassName = compact
        ? 'h-11 border border-line bg-surface px-3 font-normal text-ink min-[701px]:w-40'
        : 'h-11 border border-line bg-surface px-3 font-normal text-ink'
    return (
        <label className={className}>
            <span>{label}</span>
            <input
                className={inputClassName}
                type="number"
                value={value}
                min={min}
                max={max}
                onChange={(event) => onChange(Number(event.target.value))}
                required
            />
        </label>
    )
}

function DateField({
    label,
    value,
    onChange,
    min,
    max,
    disabled = false,
}: {
    label: string
    value: string
    onChange: (value: string) => void
    min?: string
    max?: string
    disabled?: boolean
}) {
    return (
        <label className={productFormRowClassName}>
            <span>{label}</span>
            <input
                className="h-11 border border-line bg-surface px-3 font-normal text-ink disabled:text-muted"
                type="date"
                value={value}
                min={min}
                max={max}
                onChange={(event) => onChange(event.target.value)}
                required={!disabled}
                disabled={disabled}
            />
        </label>
    )
}

const productFormRowClassName =
    'grid gap-2 border-b border-line px-4 py-4 text-xs font-bold last:border-b-0 min-[701px]:grid-cols-[140px_minmax(0,1fr)] min-[701px]:items-start min-[701px]:gap-6'

function getDiscountEndMinimum(discountStartDate: string | null) {
    const parts = new Intl.DateTimeFormat('en-CA', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        timeZone: 'Asia/Seoul',
    }).formatToParts(new Date())
    const values = Object.fromEntries(parts.map((part) => [part.type, part.value]))
    const today = `${values.year}-${values.month}-${values.day}`
    return discountStartDate && discountStartDate > today ? discountStartDate : today
}
