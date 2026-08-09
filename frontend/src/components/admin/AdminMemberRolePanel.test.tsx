import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AdminMember } from '../../types/admin'
import { AdminMemberRolePanel } from './AdminMemberRolePanel'

const mocks = vi.hoisted(() => ({
    getAdminAuthorization: vi.fn(),
    updateAdminMemberRole: vi.fn(),
}))

vi.mock('../../api/admin', () => ({
    getAdminAuthorization: mocks.getAdminAuthorization,
    updateAdminMemberRole: mocks.updateAdminMemberRole,
}))

const member: AdminMember = {
    memberId: 2,
    email: 'user@example.test',
    name: '테스트 회원',
    role: 'ROLE_USER',
    adminGrade: null,
    accessStatus: 'ACTIVE',
    lastLoginAt: null,
    restrictionReason: null,
    restrictedAt: null,
    restrictedByMemberId: null,
    orderCount: 0,
    totalPaidAmount: 0,
    createdAt: '2026-08-03T00:00:00',
}

describe('AdminMemberRolePanel', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mocks.getAdminAuthorization.mockResolvedValue({
            memberId: 1,
            adminGrade: 'SUPER_ADMIN',
            permissions: [],
        })
        mocks.updateAdminMemberRole.mockResolvedValue({
            memberId: member.memberId,
            role: 'ROLE_ADMIN',
            adminGrade: 'MANAGER',
            permissions: [],
        })
    })

    it('변경 사유와 함께 일반 회원을 매니저로 승급한다', async () => {
        const user = userEvent.setup()
        const onChanged = vi.fn()
        render(<AdminMemberRolePanel member={member} onChanged={onChanged} />)

        await user.selectOptions(await screen.findByLabelText('변경할 권한'), 'ROLE_ADMIN:MANAGER')
        await user.type(screen.getByLabelText('변경 사유'), '운영 담당자 지정')
        await user.click(screen.getByRole('button', { name: '권한 변경' }))

        await waitFor(() => {
            expect(mocks.updateAdminMemberRole).toHaveBeenCalledWith(member.memberId, {
                role: 'ROLE_ADMIN',
                adminGrade: 'MANAGER',
                reason: '운영 담당자 지정',
            })
        })
        expect(onChanged).toHaveBeenCalledWith(expect.objectContaining({
            adminGrade: 'MANAGER',
        }))
        expect(await screen.findByText('테스트 회원의 권한을 매니저(으)로 변경했습니다.'))
            .toBeInTheDocument()
    })
})
