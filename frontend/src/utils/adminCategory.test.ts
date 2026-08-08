import { describe, expect, it } from 'vitest'
import type { AdminCategory } from '../types/admin'
import {
    flattenAdminCategoryTree,
    isAdminCategoryDescendant,
    normalizeAdminCategorySlug,
} from './adminCategory'

const category = (
    categoryId: number,
    name: string,
    parentId: number | null,
    depth: number,
    displayOrder: number,
): AdminCategory => ({
    categoryId,
    name,
    slug: name.toLowerCase(),
    parentId,
    parentName: null,
    depth,
    displayOrder,
    active: true,
    hasChildren: false,
    hasProducts: false,
    createdAt: '2026-08-08T00:00:00',
    updatedAt: '2026-08-08T00:00:00',
})

describe('adminCategory', () => {
    const categories = [
        category(3, '원피스', 2, 3, 1),
        category(4, '가구', null, 1, 2),
        category(2, '여성의류', 1, 2, 1),
        category(1, '패션', null, 1, 1),
    ]

    it('노출 순서에 따라 부모 다음에 자식을 배치한다', () => {
        expect(flattenAdminCategoryTree(categories).map(({ categoryId }) => categoryId))
            .toEqual([1, 2, 3, 4])
    })

    it('직계 및 여러 단계 후손을 판정한다', () => {
        expect(isAdminCategoryDescendant(categories, 2, 1)).toBe(true)
        expect(isAdminCategoryDescendant(categories, 3, 1)).toBe(true)
        expect(isAdminCategoryDescendant(categories, 4, 1)).toBe(false)
    })

    it('슬러그를 소문자 영문·숫자·하이픈으로 정규화한다', () => {
        expect(normalizeAdminCategorySlug('Fashion--Women 한글!')).toBe('fashion-women')
    })
})
