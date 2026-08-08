import { Pencil, Trash2 } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import type { ProductStatus, ProductSummary } from '../../types/product'
import { formatPrice, getProductStatusLabel, resolveImageUrl } from '../../utils/product'
import { StatusBadge, type StatusBadgeTone } from '../ui/StatusBadge'

const productStatusTones: Record<ProductStatus, StatusBadgeTone> = {
    DRAFT: 'neutral',
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    SOLD_OUT: 'neutral',
    DELETED: 'danger',
}

export function SellerProductTable({
    products,
    onDelete,
}: {
    products: ProductSummary[]
    onDelete: (product: ProductSummary) => void
}) {
    const navigate = useNavigate()

    function openProduct(productId: number) {
        navigate(`/seller/products/${productId}`)
    }

    return (
        <div className="overflow-x-auto border-t-2 border-ink">
            <table className="w-full min-w-190 text-left text-sm">
                <thead className="border-b border-line bg-surface text-xs">
                    <tr>
                        <th className="p-4">상품</th>
                        <th className="p-4">카테고리</th>
                        <th className="p-4">가격</th>
                        <th className="p-4">재고</th>
                        <th className="p-4">상태</th>
                        <th className="p-4 text-right">관리</th>
                    </tr>
                </thead>
                <tbody>
                    {products.map((product) => (
                        <tr
                            className="cursor-pointer border-b border-line bg-paper transition-colors hover:bg-surface focus-visible:bg-surface focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-ink"
                            key={product.productId}
                            onClick={() => openProduct(product.productId)}
                            onKeyDown={(event) => {
                                if (event.target !== event.currentTarget) return
                                if (event.key === 'Enter' || event.key === ' ') {
                                    event.preventDefault()
                                    openProduct(product.productId)
                                }
                            }}
                            role="link"
                            tabIndex={0}
                        >
                            <td className="p-4">
                                <div className="flex min-w-60 items-center gap-3">
                                    <div className="size-14 shrink-0 overflow-hidden border border-line bg-surface">
                                        {product.thumbnailUrl ? (
                                            <img
                                                alt=""
                                                className="size-full object-cover"
                                                loading="lazy"
                                                src={resolveImageUrl(product.thumbnailUrl)}
                                            />
                                        ) : (
                                            <div className="grid size-full place-items-center text-[9px] font-bold tracking-[.12em] text-muted">
                                                YMALL
                                            </div>
                                        )}
                                    </div>
                                    <div className="min-w-0">
                                        <strong className="block truncate">{product.name}</strong>
                                        <p className="mt-1 truncate text-xs text-muted">
                                            {product.brand || '브랜드 미입력'}
                                        </p>
                                    </div>
                                </div>
                            </td>
                            <td className="p-4">{product.categoryName}</td>
                            <td className="p-4">{formatPrice(product.price)}</td>
                            <td className="p-4">{product.stock.toLocaleString()}</td>
                            <td className="p-4">
                                <StatusBadge tone={productStatusTones[product.status]}>
                                    {getProductStatusLabel(product.status)}
                                </StatusBadge>
                            </td>
                            <td className="p-4">
                                <div className="flex justify-end gap-1">
                                    <Link
                                        aria-label={`${product.name} 수정`}
                                        className="grid size-9 place-items-center"
                                        to={`/seller/products/${product.productId}`}
                                        onClick={(event) => event.stopPropagation()}
                                    >
                                        <Pencil className="size-4" />
                                    </Link>
                                    <button
                                        aria-label={`${product.name} 삭제`}
                                        className="grid size-9 place-items-center text-danger"
                                        type="button"
                                        onClick={(event) => {
                                            event.stopPropagation()
                                            onDelete(product)
                                        }}
                                    >
                                        <Trash2 className="size-4" />
                                    </button>
                                </div>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    )
}
