import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { ManagementListSearch, ManagementPagination } from './ManagementListUi'

function LocationProbe() {
    const location = useLocation()
    return <output aria-label="현재 주소">{`${location.pathname}${location.search}`}</output>
}

describe('ManagementListUi', () => {
    it('검색하면 검색어를 URL에 남기고 페이지를 1로 초기화한다', async () => {
        const user = userEvent.setup()
        render(
            <MemoryRouter initialEntries={['/admin/members?page=3']}>
                <ManagementListSearch placeholder="회원 검색" />
                <LocationProbe />
            </MemoryRouter>,
        )

        await user.type(screen.getByPlaceholderText('회원 검색'), 'tester')
        await user.click(screen.getByRole('button', { name: '검색' }))

        expect(screen.getByLabelText('현재 주소')).toHaveTextContent(
            '/admin/members?page=1&keyword=tester',
        )
    })

    it('페이지 링크에 현재 검색 조건을 유지한다', () => {
        render(
            <MemoryRouter initialEntries={['/admin/orders?keyword=keyboard&page=2']}>
                <ManagementPagination page={2} totalPages={4} />
            </MemoryRouter>,
        )

        expect(screen.getByRole('link', { name: '3' })).toHaveAttribute(
            'href',
            '/admin/orders?keyword=keyboard&page=3',
        )
        expect(screen.getByRole('link', { name: '이전 페이지' })).toHaveAttribute(
            'href',
            '/admin/orders?keyword=keyboard&page=1',
        )
    })
})
