import { ChevronLeft, Heart, Minus, Plus, ShieldCheck, Star, Truck } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getProduct } from '../api/products'
import type { ProductDetail } from '../types/product'
import { formatPrice, getDiscountedPrice, resolveImageUrl } from '../utils/product'

export function ProductDetailPage() {
    const { productId } = useParams()
    const id = Number(productId)
    const invalidProductId = !Number.isInteger(id)
    const [product, setProduct] = useState<ProductDetail | null>(null)
    const [error, setError] = useState('')
    const [quantity, setQuantity] = useState(1)
    const [selectedImage, setSelectedImage] = useState('')

    useEffect(() => {
        const controller = new AbortController()
        if (invalidProductId) return () => controller.abort()

        getProduct(id, controller.signal)
            .then((data) => { setProduct(data); setSelectedImage(data.images[0]?.imageUrl ?? data.thumbnailUrl ?? '') })
            .catch((requestError: unknown) => {
                if (requestError instanceof Error && requestError.name !== 'AbortError') setError(requestError.message)
            })
        return () => controller.abort()
    }, [id, invalidProductId])

    const discountedPrice = useMemo(() => product ? getDiscountedPrice(product.price, product.discountPercentage) : 0, [product])

    if (invalidProductId || error) return <div className="detail-state"><strong>상품을 찾을 수 없습니다.</strong><p>{invalidProductId ? '잘못된 상품 주소입니다.' : error}</p><Link to="/">상품 목록으로 돌아가기</Link></div>
    if (!product) return <div className="detail-state"><strong>상품 정보를 불러오는 중입니다.</strong></div>

    return (
        <section className="detail-page">
            <Link className="back-link" to="/"><ChevronLeft /> 상품 목록</Link>
            <div className="detail-layout">
                <div className="detail-gallery">
                    <div className="detail-main-image">{selectedImage ? <img src={resolveImageUrl(selectedImage)} alt={product.name} /> : <div className="image-placeholder">YMALL</div>}</div>
                    {product.images.length > 0 && <div className="thumbnail-list">{product.images.map((image) => <button className={selectedImage === image.imageUrl ? 'active' : ''} onClick={() => setSelectedImage(image.imageUrl)} key={image.imageId} type="button"><img src={resolveImageUrl(image.imageUrl)} alt="" /></button>)}</div>}
                </div>
                <div className="detail-info">
                    <div className="detail-category">{product.category.name}</div>
                    <p className="product-brand">{product.brand}</p>
                    <h1>{product.name}</h1>
                    <div className="rating"><Star fill="currentColor" /> {product.rating?.toFixed(1) ?? '0.0'} <span>상품 평점</span></div>
                    <div className="detail-price">{product.discountPercentage > 0 && <><del>{formatPrice(product.price)}</del><strong>{product.discountPercentage}%</strong></>}<b>{formatPrice(discountedPrice)}</b></div>
                    <p className="detail-description">{product.description}</p>
                    <dl className="product-meta"><div><dt>배송</dt><dd>무료배송 · 평균 2–3일 소요</dd></div><div><dt>재고</dt><dd>{product.stock > 0 ? `${product.stock}개 남음` : '품절'}</dd></div></dl>
                    <div className="quantity-row"><span>수량</span><div><button onClick={() => setQuantity((value) => Math.max(1, value - 1))} type="button"><Minus /></button><b>{quantity}</b><button onClick={() => setQuantity((value) => Math.min(product.stock, value + 1))} disabled={product.stock === 0} type="button"><Plus /></button></div></div>
                    <div className="purchase-actions"><button className="wish-detail" type="button"><Heart /> 찜하기</button><button className="buy-button" disabled={product.stock === 0 || product.status === 'SOLD_OUT'} type="button">{product.stock === 0 ? '품절된 상품입니다' : `${formatPrice(discountedPrice * quantity)} · 장바구니 담기`}</button></div>
                    <div className="benefit-row"><span><Truck /> 무료 배송</span><span><ShieldCheck /> 안전 결제</span></div>
                </div>
            </div>
        </section>
    )
}
