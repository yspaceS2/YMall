import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AdminAuthorizationContext } from '../../auth/AdminAuthorizationContext'
import type { AdminMember } from '../../types/admin'
import { AdminMemberOperationsPanel } from './AdminMemberOperationsPanel'

const mocks = vi.hoisted(() => ({
    getAdminMemberAuditLogs: vi.fn(),
    revokeAdminMemberSessions: vi.fn(),
    updateAdminMemberRestriction: vi.fn(),
}))

vi.mock('../../api/admin', () => mocks)

const member: AdminMember = {
    memberId: 2,
    email: 'u***@example.test',
    name: '테스트 회원',
    role: 'ROLE_USER',
    adminGrade: null,
    accessStatus: 'ACTIVE',
    lastLoginAt: null,
    restrictionReason: null,
    restrictedAt: null,
    restrictedByMemberId: null,
    orderCount: 2,
    totalPaidAmount: 25000,
    createdAt: '2026-08-03T00:00:00',
}

describe('AdminMemberOperationsPanel', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.spyOn(window, 'confirm').mockReturnValue(true)
        mocks.getAdminMemberAuditLogs.mockResolvedValue([])
        mocks.updateAdminMemberRestriction.mockResolvedValue({
            ...member,
            accessStatus: 'RESTRICTED',
            restrictionReason: '운영 정책 위반',
        })
        mocks.revokeAdminMemberSessions.mockResolvedValue(undefined)
    })

    it('사유와 확인 단계를 거쳐 회원 이용을 제한한다', async () => {
        const user = userEvent.setup()
        const onChanged = vi.fn()
        renderPanel(onChanged)

        await user.type(screen.getByLabelText('처리 사유'), '운영 정책 위반')
        await user.click(screen.getByRole('button', { name: '이용 제한' }))

        await waitFor(() => {
            expect(mocks.updateAdminMemberRestriction).toHaveBeenCalledWith(
                member.memberId,
                true,
                '운영 정책 위반',
            )
        })
        expect(onChanged).toHaveBeenCalledWith(expect.objectContaining({
            accessStatus: 'RESTRICTED',
        }))
    })

    it('전체 세션 종료도 사유를 필수로 전달한다', async () => {
        const user = userEvent.setup()
        renderPanel(vi.fn())

        await user.type(screen.getByLabelText('처리 사유'), '계정 보호 요청')
        await user.click(screen.getByRole('button', { name: '전체 세션 종료' }))

        await waitFor(() => {
            expect(mocks.revokeAdminMemberSessions).toHaveBeenCalledWith(
                member.memberId,
                '계정 보호 요청',
            )
        })
    })
})

function renderPanel(onChanged: (member: AdminMember) => void) {
    render(
        <AdminAuthorizationContext.Provider value={{
            authorization: {
                memberId: 1,
                adminGrade: 'SUPERVISOR',
                permissions: ['MEMBER_RESTRICT_LIMITED', 'MEMBER_RESTRICT_ALL', 'AUDIT_ALL_READ'],
            },
            hasPermission: (...permissions) => permissions.some((permission) => [
                'MEMBER_RESTRICT_LIMITED',
                'MEMBER_RESTRICT_ALL',
                'AUDIT_ALL_READ',
            ].includes(permission)),
        }}>
            <AdminMemberOperationsPanel member={member} onChanged={onChanged} />
        </AdminAuthorizationContext.Provider>,
    )
}
