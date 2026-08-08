import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { getApiErrorMessage, isAbortError } from '../api/errors'
import { getCategories } from '../api/products'
import { deleteSellerProduct, getSellerProducts } from '../api/seller'
import type { Category, ProductSummary } from '../types/product'
import type { SellerProductStockCondition } from '../types/seller'
import { getCategoryChildren } from '../utils/productCategory'
import { parsePositiveInteger } from '../utils/searchParams'

const PAGE_SIZE = 20

export function useSellerProductList() {
    const [searchParams, setSearchParams] = useSearchParams()
    const [products, setProducts] = useState<ProductSummary[]>([])
    const [categories, setCategories] = useState<Category[]>([])
    const [pagination, setPagination] = useState({ page: 1, totalPages: 0, totalElements: 0 })
    const [isLoading, setIsLoading] = useState(true)
    const [errorMessage, setErrorMessage] = useState('')
    const [productToDelete, setProductToDelete] = useState<ProductSummary | null>(null)
    const [isDeleting, setIsDeleting] = useState(false)
    const page = parsePositiveInteger(searchParams.get('page'), 1)
    const keyword = searchParams.get('keyword') ?? ''
    const rootCategoryId = parsePositiveInteger(searchParams.get('rootCategoryId'), 0) || undefined
    const middleCategoryId = parsePositiveInteger(searchParams.get('middleCategoryId'), 0) || undefined
    const leafCategoryId = parsePositiveInteger(searchParams.get('categoryId'), 0) || undefined
    const selectedCategoryId = leafCategoryId ?? middleCategoryId ?? rootCategoryId
    const stockCondition = parseProductStockCondition(searchParams.get('stockCondition'))
    const stockQuantity = nonNegativeNumber(searchParams.get('stockQuantity'))

    useEffect(() => {
        const controller = new AbortController()
        getCategories(controller.signal)
            .then(setCategories)
            .catch((error: unknown) => {
                if (isAbortError(error)) return
                setErrorMessage(getApiErrorMessage(error, '카테고리를 불러오지 못했습니다.'))
            })
        return () => controller.abort()
    }, [])

    useEffect(() => {
        const controller = new AbortController()
        getSellerProducts({
            page,
            size: PAGE_SIZE,
            keyword,
            categoryId: selectedCategoryId,
            stockCondition,
            stockQuantity,
            signal: controller.signal,
        })
            .then((response) => {
                setProducts(response.content)
                setPagination({
                    page: response.page,
                    totalPages: response.totalPages,
                    totalElements: response.totalElements,
                })
                setErrorMessage('')
            })
            .catch((error: unknown) => {
                if (isAbortError(error)) return
                setErrorMessage(getApiErrorMessage(error, '상품 목록을 불러오지 못했습니다.'))
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [keyword, page, selectedCategoryId, stockCondition, stockQuantity])

    function updateFilter(name: string, value: string, childNames: string[] = []) {
        const next = new URLSearchParams(searchParams)
        if (value) next.set(name, value)
        else next.delete(name)
        childNames.forEach((childName) => next.delete(childName))
        next.set('page', '1')
        setIsLoading(true)
        setSearchParams(next)
    }

    async function removeProduct() {
        if (!productToDelete) return
        setIsDeleting(true)
        try {
            await deleteSellerProduct(productToDelete.productId)
            setProducts((current) => current.filter(
                (product) => product.productId !== productToDelete.productId,
            ))
            setPagination((current) => ({
                ...current,
                totalElements: Math.max(0, current.totalElements - 1),
            }))
            setProductToDelete(null)
        } catch (error) {
            setErrorMessage(getApiErrorMessage(error, '상품을 삭제하지 못했습니다.'))
        } finally {
            setIsDeleting(false)
        }
    }

    return {
        products,
        pagination,
        isLoading,
        errorMessage,
        productToDelete,
        isDeleting,
        categoryFilter: {
            rootCategoryId,
            middleCategoryId,
            leafCategoryId,
            rootCategories: getCategoryChildren(categories, null),
            middleCategories: rootCategoryId ? getCategoryChildren(categories, rootCategoryId) : [],
            leafCategories: middleCategoryId ? getCategoryChildren(categories, middleCategoryId) : [],
        },
        stockCondition,
        stockQuantity,
        updateFilter,
        setProductToDelete,
        removeProduct,
    }
}

function parseProductStockCondition(value: string | null): SellerProductStockCondition {
    return value === 'LTE' ? 'LTE' : 'GTE'
}

function nonNegativeNumber(value: string | null) {
    if (value == null || value.trim() === '') return undefined
    const parsed = Number(value)
    return Number.isInteger(parsed) && parsed >= 0 ? parsed : undefined
}
