import { describe, expect, it } from 'vitest'
import type { AdminAuthorization, AdminMember } from '../../types/admin'
import {
    availableAdminRoleChanges,
    formatMemberAuthority,
} from './adminRolePresentation'

const user: AdminMember = {
    memberId: 2,
    email: 'user@example.test',
    name: '회원',
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

describe('adminRolePresentation', () => {
    it('관리자 등급을 한국어 권한명으로 표시한다', () => {
        expect(formatMemberAuthority({
            ...user,
            role: 'ROLE_ADMIN',
            adminGrade: 'SUPER_ADMIN',
        })).toBe('최고 관리자')
    })

    it('슈퍼바이저는 일반 회원을 매니저로만 승급할 수 있다', () => {
        const actor: AdminAuthorization = {
            memberId: 1,
            adminGrade: 'SUPERVISOR',
            permissions: [],
        }

        expect(availableAdminRoleChanges(actor, user)).toEqual([{
            role: 'ROLE_ADMIN',
            adminGrade: 'MANAGER',
            label: '매니저',
        }])
    })

    it('최고 관리자는 매니저를 일반 회원 또는 슈퍼바이저로 변경할 수 있다', () => {
        const actor: AdminAuthorization = {
            memberId: 1,
            adminGrade: 'SUPER_ADMIN',
            permissions: [],
        }
        const manager: AdminMember = {
            ...user,
            role: 'ROLE_ADMIN',
            adminGrade: 'MANAGER',
        }

        expect(availableAdminRoleChanges(actor, manager).map((option) => option.label))
            .toEqual(['일반 회원', '슈퍼바이저'])
    })
})
