import { Heart } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { ProductSummary } from '../types/product'
import { formatPrice, getDiscountedPrice, resolveImageUrl } from '../utils/product'

export function ProductCard({ product }: { product: ProductSummary }) {
    const discountedPrice = getDiscountedPrice(product.price, product.discountPercentage)

    return (
        <article className="group relative min-w-0">
            <Link className="relative block aspect-[.82] overflow-hidden bg-[#e9e9e3]" to={`/products/${product.productId}`}>
                {product.thumbnailUrl ? (
                    <img className="size-full object-cover transition-transform duration-500 group-hover:scale-[1.03]" src={resolveImageUrl(product.thumbnailUrl)} alt={product.name} />
                ) : (
                    <div className="grid size-full place-items-center bg-linear-to-br from-[#ebeae4] to-[#d8d9cf] font-serif text-lg font-bold tracking-[.2em] text-[#a2a298]">YMALL</div>
                )}
                {product.status === 'SOLD_OUT' && <span className="absolute inset-0 grid place-items-center bg-black/45 text-xs font-extrabold tracking-[.14em] text-white">SOLD OUT</span>}
            </Link>
            <button className="absolute top-3 right-3 grid size-9 place-items-center rounded-full border-0 bg-white/80" type="button" aria-label={`${product.name} 찜하기`}>
                <Heart className="size-4" aria-hidden="true" />
            </button>
            <Link className="block pt-4" to={`/products/${product.productId}`}>
                <span className="mb-1.5 block text-[11px] font-extrabold tracking-[.08em] text-muted uppercase">{product.brand}</span>
                <h2 className="mb-2.5 truncate text-[13px] font-medium sm:text-sm">{product.name}</h2>
                <div className="flex items-baseline gap-2">
                    {product.discountPercentage > 0 && <strong className="text-[#849b21]">{product.discountPercentage}%</strong>}
                    <b className="text-sm">{formatPrice(discountedPrice)}</b>
                    {product.discountPercentage > 0 && <del className="text-[11px] text-[#aaa]">{formatPrice(product.price)}</del>}
                </div>
                <span className="mt-3 inline-block border border-line px-2 py-1 text-[10px] text-muted">{product.categoryName}</span>
            </Link>
        </article>
    )
}
