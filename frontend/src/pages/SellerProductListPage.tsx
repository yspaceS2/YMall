import { LoaderCircle, PackagePlus } from 'lucide-react'
import { Link } from 'react-router-dom'
import {
    ManagementEmpty,
    ManagementListSearch,
    ManagementPageHeader,
    ManagementPagination,
    managementPageClassName,
} from '../components/management/ManagementListUi'
import { SellerProductListFilters } from '../components/seller/SellerProductListFilters'
import { SellerProductTable } from '../components/seller/SellerProductTable'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { useSellerProductList } from '../hooks/useSellerProductList'

export function SellerProductListPage() {
    const list = useSellerProductList()

    return (
        <section className={managementPageClassName}>
            <ManagementPageHeader
                eyebrow="SELLER PRODUCTS"
                title="상품 관리"
                description={`등록 상품 ${list.pagination.totalElements.toLocaleString()}개`}
                action={(
                    <Link
                        className="flex h-11 items-center gap-2 bg-ink px-5 text-xs font-bold text-white"
                        to="/seller/products/new"
                    >
                        <PackagePlus className="size-4" />
                        상품 등록
                    </Link>
                )}
            />
            <SellerProductListFilters
                {...list.categoryFilter}
                stockCondition={list.stockCondition}
                stockQuantity={list.stockQuantity}
                onFilterChange={list.updateFilter}
            />
            <ManagementListSearch placeholder="상품명 또는 브랜드를 검색하세요" />
            {list.errorMessage && (
                <FeedbackMessage className="mb-5" tone="error">
                    {list.errorMessage}
                </FeedbackMessage>
            )}
            {list.isLoading ? (
                <div className="grid min-h-72 place-items-center">
                    <LoaderCircle className="size-6 animate-spin" aria-label="불러오는 중" />
                </div>
            ) : list.products.length === 0 ? (
                <ManagementEmpty>조건에 맞는 상품이 없습니다.</ManagementEmpty>
            ) : (
                <SellerProductTable
                    products={list.products}
                    onDelete={list.setProductToDelete}
                />
            )}
            <ManagementPagination
                page={list.pagination.page}
                totalPages={list.pagination.totalPages}
            />
            <ConfirmDialog
                open={list.productToDelete !== null}
                title="상품을 삭제할까요?"
                description={list.productToDelete
                    ? `'${list.productToDelete.name}' 상품을 삭제합니다.`
                    : ''}
                confirmLabel="상품 삭제"
                isPending={list.isDeleting}
                onCancel={() => list.setProductToDelete(null)}
                onConfirm={() => void list.removeProduct()}
            />
        </section>
    )
}
