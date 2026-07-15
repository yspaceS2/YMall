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
            .then((data) => {
                setError('')
                setProduct(data)
                setSelectedImage(data.images[0]?.imageUrl ?? data.thumbnailUrl ?? '')
            })
            .catch((requestError: unknown) => {
                if (requestError instanceof Error && requestError.name !== 'AbortError') setError(requestError.message)
            })
        return () => controller.abort()
    }, [id, invalidProductId])

    const discountedPrice = useMemo(() => product ? getDiscountedPrice(product.price, product.discountPercentage) : 0, [product])

    if (invalidProductId || error) return <div className="grid min-h-80 place-content-center gap-2 text-center text-muted"><strong className="text-ink">상품을 찾을 수 없습니다.</strong><p className="m-0">{invalidProductId ? '잘못된 상품 주소입니다.' : error}</p><Link className="mt-3 underline" to="/">상품 목록으로 돌아가기</Link></div>
    if (!product) return <div className="grid min-h-80 place-content-center gap-2 text-center text-muted"><strong className="text-ink">상품 정보를 불러오는 중입니다.</strong></div>

    return (
        <section className="mx-auto max-w-360 px-4 pt-12 pb-20 sm:px-[clamp(20px,5vw,72px)] sm:pt-18 sm:pb-27.5">
            <Link className="mb-7 inline-flex items-center gap-1 text-xs" to="/"><ChevronLeft className="size-4" /> 상품 목록</Link>
            <div className="grid grid-cols-1 gap-10 lg:grid-cols-[minmax(0,1.1fr)_minmax(360px,.9fr)] lg:gap-[clamp(40px,7vw,110px)]">
                <div className="min-w-0">
                    <div className="aspect-square overflow-hidden bg-[#e9e9e3]">{selectedImage ? <img className="size-full object-cover" src={resolveImageUrl(selectedImage)} alt={product.name} /> : <div className="grid size-full place-items-center bg-linear-to-br from-[#ebeae4] to-[#d8d9cf] font-serif text-lg font-bold tracking-[.2em] text-[#a2a298]">YMALL</div>}</div>
                    {product.images.length > 0 && <div className="mt-3 flex gap-2.5 overflow-x-auto">{product.images.map((image) => <button className={`size-18.5 shrink-0 border p-0 ${selectedImage === image.imageUrl ? 'border-ink' : 'border-transparent'}`} onClick={() => setSelectedImage(image.imageUrl)} key={image.imageId} type="button"><img className="size-full object-cover" src={resolveImageUrl(image.imageUrl)} alt="" /></button>)}</div>}
                </div>
                <div className="pt-3">
                    <div className="inline-block bg-lime px-2.5 py-1 text-[10px] font-extrabold tracking-[.08em]">{product.category.name}</div>
                    <p className="mt-7 mb-1.5 text-[11px] font-extrabold tracking-[.08em] text-muted uppercase">{product.brand}</p>
                    <h1 className="my-2 font-serif text-[clamp(34px,4vw,52px)] leading-[1.05] font-medium tracking-[-.04em]">{product.name}</h1>
                    <div className="flex items-center gap-1 text-[13px]"><Star className="size-4 text-[#8ca324]" fill="currentColor" /> {product.rating?.toFixed(1) ?? '0.0'} <span className="ml-1 text-muted">상품 평점</span></div>
                    <div className="my-8 flex items-baseline gap-2.5">{product.discountPercentage > 0 && <><del className="text-[#aaa]">{formatPrice(product.price)}</del><strong className="text-[#849b21]">{product.discountPercentage}%</strong></>}<b className="ml-auto text-2xl">{formatPrice(discountedPrice)}</b></div>
                    <p className="border-b border-line pb-7 text-sm leading-7 text-[#676761]">{product.description}</p>
                    <dl className="m-0 border-b border-line py-4.5 text-[13px]"><div className="grid grid-cols-[70px_1fr] py-2"><dt className="text-muted">배송</dt><dd className="m-0">무료배송 · 평균 2–3일 소요</dd></div><div className="grid grid-cols-[70px_1fr] py-2"><dt className="text-muted">재고</dt><dd className="m-0">{product.stock > 0 ? `${product.stock}개 남음` : '품절'}</dd></div></dl>
                    <div className="flex items-center justify-between py-6 text-[13px]"><span>수량</span><div className="flex items-center border border-line"><button className="grid h-9 w-9.5 place-items-center border-0 bg-transparent" onClick={() => setQuantity((value) => Math.max(1, value - 1))} type="button"><Minus className="size-3.5" /></button><b className="min-w-8.5 text-center">{quantity}</b><button className="grid h-9 w-9.5 place-items-center border-0 bg-transparent disabled:opacity-35" onClick={() => setQuantity((value) => Math.min(product.stock, value + 1))} disabled={product.stock === 0} type="button"><Plus className="size-3.5" /></button></div></div>
                    <div className="grid grid-cols-1 gap-2 sm:grid-cols-[120px_1fr]"><button className="h-13.5 border border-ink bg-transparent font-extrabold" type="button"><Heart className="inline size-4" /> 찜하기</button><button className="h-13.5 border border-ink bg-ink font-extrabold text-white disabled:border-[#ddd] disabled:bg-[#ddd] disabled:text-[#888]" disabled={product.stock === 0 || product.status === 'SOLD_OUT'} type="button">{product.stock === 0 ? '품절된 상품입니다' : `${formatPrice(discountedPrice * quantity)} · 장바구니 담기`}</button></div>
                    <div className="mt-5.5 flex gap-6.5 text-[11px] text-muted"><span className="flex items-center gap-1.5"><Truck className="size-4" /> 무료 배송</span><span className="flex items-center gap-1.5"><ShieldCheck className="size-4" /> 안전 결제</span></div>
                </div>
            </div>
        </section>
    )
}
