import { render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getCategories } from '../api/products'
import {
    getSellerOrders,
    getSellerProducts,
    getSellerProfile,
} from '../api/seller'
import type { SellerProfile } from '../types/seller'
import { AdminManagementPage } from './AdminManagementPage'
import { SellerManagementPage } from './SellerManagementPage'

vi.mock('../api/products', () => ({
    getCategories: vi.fn(),
}))

vi.mock('../api/seller', () => ({
    createSellerProduct: vi.fn(),
    createSellerProfile: vi.fn(),
    getSellerOrders: vi.fn(),
    getSellerProduct: vi.fn(),
    getSellerProducts: vi.fn(),
    getSellerProfile: vi.fn(),
    updateSellerProduct: vi.fn(),
    updateSellerProfile: vi.fn(),
}))

vi.mock('../api/files', () => ({
    uploadProductImage: vi.fn(),
}))

vi.mock('../toast/useToast', () => ({
    useToast: () => ({ showToast: vi.fn() }),
}))

vi.mock('../components/dashboard/AdminDashboard', () => ({
    AdminDashboard: () => <div>관리자 통계 대시보드</div>,
}))

vi.mock('../components/dashboard/SellerDashboard', () => ({
    SellerDashboard: () => <div>판매자 통계 대시보드</div>,
}))

vi.mock('../components/seller/ProductCategorySelector', () => ({
    ProductCategorySelector: () => <div>상품 카테고리 선택</div>,
}))

vi.mock('../components/seller/ProductImageUploadField', () => ({
    ProductImageUploadField: () => <div>상품 이미지 선택</div>,
}))

const sellerProfile: SellerProfile = {
    sellerProfileId: 1,
    memberId: 2,
    storeName: '테스트 상점',
    businessNumber: '000-00-00000',
    description: '테스트 판매자',
    createdAt: '2026-08-01T00:00:00+09:00',
    updatedAt: '2026-08-01T00:00:00+09:00',
}

describe('management pages', () => {
    beforeEach(() => {
        vi.mocked(getSellerProfile).mockResolvedValue(sellerProfile)
        vi.mocked(getCategories).mockResolvedValue([
            { categoryId: 1, name: '식품', slug: 'food', depth: 1 },
        ])
    })

    it('관리자 진입 시 대시보드만 렌더링한다', () => {
        render(<AdminManagementPage />)

        expect(screen.getByText('관리자 통계 대시보드')).toBeInTheDocument()
        expect(screen.queryByText('관리자 운영')).not.toBeInTheDocument()
    })

    it('판매자 대시보드에서는 프로필 외 관리 목록을 조회하지 않는다', async () => {
        render(<SellerManagementPage section="dashboard" />)

        expect(await screen.findByText('판매자 통계 대시보드')).toBeInTheDocument()
        expect(getSellerProfile).toHaveBeenCalledOnce()
        expect(getCategories).not.toHaveBeenCalled()
        expect(getSellerProducts).not.toHaveBeenCalled()
        expect(getSellerOrders).not.toHaveBeenCalled()
    })

    it('상품 편집 화면에서는 프로필과 카테고리만 준비한다', async () => {
        render(<SellerManagementPage section="products" />)

        expect(await screen.findByText('상품 관리')).toBeInTheDocument()
        await waitFor(() => expect(getCategories).toHaveBeenCalledOnce())
        expect(screen.getByLabelText('할인 시작일')).toBeDisabled()
        expect(screen.getByLabelText('할인 종료일')).toBeDisabled()
        expect(getSellerProducts).not.toHaveBeenCalled()
        expect(getSellerOrders).not.toHaveBeenCalled()
    })
})
