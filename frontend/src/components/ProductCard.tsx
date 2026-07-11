import { Heart } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { ProductSummary } from '../types/product'
import { formatPrice, getDiscountedPrice, resolveImageUrl } from '../utils/product'

export function ProductCard({ product }: { product: ProductSummary }) {
    const discountedPrice = getDiscountedPrice(product.price, product.discountPercentage)

    return (
        <article className="product-card">
            <Link className="product-image" to={`/products/${product.productId}`}>
                {product.thumbnailUrl ? (
                    <img src={resolveImageUrl(product.thumbnailUrl)} alt={product.name} />
                ) : (
                    <div className="image-placeholder">YMALL</div>
                )}
                {product.status === 'SOLD_OUT' && <span className="sold-out">SOLD OUT</span>}
            </Link>
            <button className="wish-button" type="button" aria-label={`${product.name} 찜하기`}>
                <Heart aria-hidden="true" />
            </button>
            <Link className="product-info" to={`/products/${product.productId}`}>
                <span className="product-brand">{product.brand}</span>
                <h2>{product.name}</h2>
                <div className="price-line">
                    {product.discountPercentage > 0 && <strong>{product.discountPercentage}%</strong>}
                    <b>{formatPrice(discountedPrice)}</b>
                    {product.discountPercentage > 0 && <del>{formatPrice(product.price)}</del>}
                </div>
                <span className="category-chip">{product.categoryName}</span>
            </Link>
        </article>
    )
}
