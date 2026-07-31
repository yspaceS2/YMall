import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
    deleteAllNotifications,
    deleteNotification,
    getNotifications,
    notifyNotificationsChanged,
} from '../api/notifications'
import { NotificationPage } from './NotificationPage'

vi.mock('../api/notifications', () => ({
    deleteAllNotifications: vi.fn(),
    deleteNotification: vi.fn(),
    getNotifications: vi.fn(),
    markAllNotificationsAsRead: vi.fn(),
    markNotificationAsRead: vi.fn(),
    notifyNotificationsChanged: vi.fn(),
}))

describe('NotificationPage', () => {
    beforeEach(() => {
        vi.mocked(getNotifications).mockResolvedValue({
            content: [{
                notificationId: 10,
                type: 'PRODUCT_QUESTION_ANSWERED',
                title: '문의 답변',
                message: '등록한 상품 문의에 답변이 등록되었습니다.',
                targetUrl: '/products/1?tab=qna',
                readAt: null,
                createdAt: '2026-07-30T12:00:00',
            }],
            page: 1,
            size: 20,
            totalElements: 1,
            totalPages: 1,
            hasNext: false,
            hasPrevious: false,
        })
        vi.mocked(deleteNotification).mockResolvedValue(undefined)
        vi.mocked(deleteAllNotifications).mockResolvedValue(undefined)
    })

    it('본인 알림을 확인 후 삭제하고 미읽음 배지를 갱신한다', async () => {
        const user = userEvent.setup()
        render(
            <MemoryRouter initialEntries={['/mypage/notifications']}>
                <NotificationPage />
            </MemoryRouter>,
        )

        await user.click(await screen.findByRole('button', {
            name: '문의 답변 알림 삭제',
        }))
        await user.click(screen.getByRole('button', { name: '삭제' }))

        await waitFor(() => {
            expect(deleteNotification).toHaveBeenCalledWith(10)
        })
        expect(screen.queryByText('문의 답변')).not.toBeInTheDocument()
        expect(notifyNotificationsChanged).toHaveBeenCalledOnce()
        expect(screen.getByText('알림을 삭제했습니다.')).toBeInTheDocument()
    })

    it('개인회원 알림 화면에서 모든 알림을 삭제한다', async () => {
        const user = userEvent.setup()
        render(
            <MemoryRouter initialEntries={['/mypage/notifications']}>
                <NotificationPage />
            </MemoryRouter>,
        )

        await user.click(await screen.findByRole('button', { name: '모두 삭제' }))
        await user.click(
            within(screen.getByRole('alertdialog')).getByRole('button', {
                name: '모두 삭제',
            }),
        )

        await waitFor(() => {
            expect(deleteAllNotifications).toHaveBeenCalledOnce()
        })
        expect(screen.queryByText('문의 답변')).not.toBeInTheDocument()
        expect(notifyNotificationsChanged).toHaveBeenCalledOnce()
        expect(screen.getByText('모든 알림을 삭제했습니다.')).toBeInTheDocument()
    })

    it('판매자 알림 화면에는 모두 삭제 버튼을 표시하지 않는다', async () => {
        render(
            <MemoryRouter initialEntries={['/seller/notifications']}>
                <NotificationPage />
            </MemoryRouter>,
        )

        expect(await screen.findByText('문의 답변')).toBeInTheDocument()
        expect(screen.queryByRole('button', { name: '모두 삭제' })).not.toBeInTheDocument()
        expect(screen.queryByRole('button', {
            name: '문의 답변 알림 삭제',
        })).not.toBeInTheDocument()
    })
})
