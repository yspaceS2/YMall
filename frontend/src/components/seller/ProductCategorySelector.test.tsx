import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { Category } from '../../types/product'
import { findFirstLeafCategoryId } from '../../utils/productCategory'
import { ProductCategorySelector } from './ProductCategorySelector'

const categories: Category[] = [
    {
        categoryId: 1,
        name: '패션',
        slug: 'fashion',
        parentId: null,
        depth: 1,
        displayOrder: 1,
    },
    {
        categoryId: 2,
        name: '뷰티',
        slug: 'beauty',
        parentId: null,
        depth: 1,
        displayOrder: 2,
    },
    {
        categoryId: 10,
        name: '여성패션',
        slug: 'women-fashion',
        parentId: 1,
        depth: 2,
        displayOrder: 1,
    },
    {
        categoryId: 20,
        name: '원피스',
        slug: 'dresses',
        parentId: 10,
        depth: 3,
        displayOrder: 1,
    },
    {
        categoryId: 21,
        name: '여성 상의',
        slug: 'women-tops',
        parentId: 10,
        depth: 3,
        displayOrder: 2,
    },
    {
        categoryId: 30,
        name: '스킨케어',
        slug: 'skin-care',
        parentId: 2,
        depth: 2,
        displayOrder: 1,
    },
    {
        categoryId: 31,
        name: '클렌징',
        slug: 'cleansing',
        parentId: 30,
        depth: 3,
        displayOrder: 1,
    },
]

describe('ProductCategorySelector', () => {
    it('기존 소분류의 전체 카테고리 경로를 복원한다', () => {
        render(
            <ProductCategorySelector
                categories={categories}
                value={21}
                onChange={vi.fn()}
            />,
        )

        expect(screen.getByRole('combobox', { name: '대분류' })).toHaveValue('1')
        expect(screen.getByRole('combobox', { name: '중분류' })).toHaveValue('10')
        expect(screen.getByRole('combobox', { name: '소분류' })).toHaveValue('21')
        expect(screen.getByText('선택: 패션 › 여성패션 › 여성 상의')).toBeInTheDocument()
    })

    it('대분류를 변경하면 첫 번째 최종 카테고리를 선택한다', () => {
        const onChange = vi.fn()
        render(
            <ProductCategorySelector
                categories={categories}
                value={20}
                onChange={onChange}
            />,
        )

        fireEvent.change(screen.getByRole('combobox', { name: '대분류' }), {
            target: { value: '2' },
        })

        expect(onChange).toHaveBeenCalledWith(31)
    })

    it('첫 번째 최종 카테고리 ID를 찾는다', () => {
        expect(findFirstLeafCategoryId(categories)).toBe(20)
        expect(findFirstLeafCategoryId(categories, 2)).toBe(31)
    })
})
