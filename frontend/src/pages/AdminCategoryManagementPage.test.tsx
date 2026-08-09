import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AdminAuthorizationContext } from '../auth/AdminAuthorizationContext'
import {
    createAdminCategory,
    deleteAdminCategory,
    getAdminCategories,
    updateAdminCategory,
} from '../api/admin'
import type { AdminCategory } from '../types/admin'
import { AdminCategoryManagementPage } from './AdminCategoryManagementPage'

vi.mock('../api/admin', () => ({
    createAdminCategory: vi.fn(),
    deleteAdminCategory: vi.fn(),
    getAdminCategories: vi.fn(),
    updateAdminCategory: vi.fn(),
}))

const categories: AdminCategory[] = [
    {
        categoryId: 1,
        name: '패션',
        slug: 'fashion',
        parentId: null,
        parentName: null,
        depth: 1,
        displayOrder: 1,
        active: true,
        hasChildren: true,
        hasProducts: false,
        createdAt: '2026-07-30T00:00:00',
        updatedAt: '2026-07-30T00:00:00',
    },
    {
        categoryId: 2,
        name: '원피스',
        slug: 'dresses',
        parentId: 1,
        parentName: '패션',
        depth: 2,
        displayOrder: 1,
        active: true,
        hasChildren: false,
        hasProducts: true,
        createdAt: '2026-07-30T00:00:00',
        updatedAt: '2026-07-30T00:00:00',
    },
]

function renderPage(path = '/admin/categories/2') {
    return render(
        <AdminAuthorizationContext.Provider value={{
            authorization: {
                memberId: 1,
                adminGrade: 'SUPER_ADMIN',
                permissions: ['CATEGORY_READ', 'CATEGORY_MANAGE_ALL'],
            },
            hasPermission: (...permissions) => permissions.some((permission) =>
                permission === 'CATEGORY_READ' || permission === 'CATEGORY_MANAGE_ALL'),
        }}>
            <MemoryRouter initialEntries={[path]}>
                <Routes>
                    <Route
                        path="/admin/categories/:categoryId"
                        element={<AdminCategoryManagementPage mode="detail" />}
                    />
                    <Route
                        path="/admin/categories/new"
                        element={<AdminCategoryManagementPage mode="new" />}
                    />
                    <Route
                        path="/admin/categories"
                        element={<AdminCategoryManagementPage mode="list" />}
                    />
                </Routes>
            </MemoryRouter>
        </AdminAuthorizationContext.Provider>,
    )
}

describe('AdminCategoryManagementPage', () => {
    beforeEach(() => {
        vi.mocked(getAdminCategories).mockResolvedValue(categories)
        vi.mocked(createAdminCategory).mockReset()
        vi.mocked(updateAdminCategory).mockReset()
        vi.mocked(deleteAdminCategory).mockReset()
    })

    it('계층형 카테고리를 표시하고 선택한 상세 정보를 채운다', async () => {
        renderPage()

        expect(await screen.findByRole('link', { name: /패션/ }))
            .toHaveAttribute('href', '/admin/categories/1')
        expect(screen.getByRole('link', { name: /원피스/ }))
            .toHaveAttribute('aria-current', 'page')
        expect(screen.getByDisplayValue('원피스')).toBeInTheDocument()
        expect(screen.getByDisplayValue('dresses')).toBeInTheDocument()
    })

    it('상품이 연결된 카테고리의 삭제를 비활성화한다', async () => {
        renderPage()

        await waitFor(() => {
            expect(getAdminCategories).toHaveBeenCalled()
        })

        expect(screen.getByRole('button', { name: '삭제' })).toBeDisabled()
    })

    it('새 카테고리 입력을 정규화해 등록한다', async () => {
        const user = userEvent.setup()
        const savedCategory: AdminCategory = {
            ...categories[1],
            categoryId: 3,
            name: '여름 원피스',
            slug: 'summer-dresses',
            hasProducts: false,
        }
        vi.mocked(createAdminCategory).mockResolvedValue(savedCategory)

        renderPage('/admin/categories/new')

        await user.type(
            await screen.findByRole('textbox', { name: '카테고리명' }),
            '여름 원피스',
        )
        await user.type(screen.getByRole('textbox', { name: '슬러그' }), 'Summer--Dresses')
        await user.click(screen.getByRole('button', { name: '카테고리 등록' }))

        await waitFor(() => expect(createAdminCategory).toHaveBeenCalledWith(
            expect.objectContaining({
                name: '여름 원피스',
                slug: 'summer-dresses',
                parentId: null,
                displayOrder: 0,
                active: true,
            }),
        ))
    })
})
