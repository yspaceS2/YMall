import type {
    AdminAuthorization,
    AdminGrade,
    AdminMember,
    AdminRoleUpdateRequest,
} from '../../types/admin'
import type { MemberRole } from '../../types/auth'

const MEMBER_ROLE_LABELS: Record<MemberRole, string> = {
    ROLE_USER: '일반 회원',
    ROLE_SELLER: '판매자',
    ROLE_ADMIN: '관리자',
}

const ADMIN_GRADE_LABELS: Record<AdminGrade, string> = {
    MANAGER: '매니저',
    SUPERVISOR: '슈퍼바이저',
    SUPER_ADMIN: '최고 관리자',
}

export interface AdminRoleChangeOption extends Omit<AdminRoleUpdateRequest, 'reason'> {
    label: string
}

export function formatMemberRole(role: MemberRole) {
    return MEMBER_ROLE_LABELS[role]
}

export function formatAdminGrade(grade: AdminGrade | null) {
    return grade ? ADMIN_GRADE_LABELS[grade] : '-'
}

export function formatMemberAuthority(member: AdminMember) {
    return member.role === 'ROLE_ADMIN'
        ? formatAdminGrade(member.adminGrade)
        : formatMemberRole(member.role)
}

export function availableAdminRoleChanges(
    actor: AdminAuthorization,
    target: AdminMember,
): AdminRoleChangeOption[] {
    if (actor.memberId === target.memberId
        || actor.adminGrade === 'MANAGER'
        || target.role === 'ROLE_SELLER'
        || target.adminGrade === 'SUPER_ADMIN') {
        return []
    }

    if (actor.adminGrade === 'SUPERVISOR') {
        if (target.role === 'ROLE_USER') {
            return [adminOption('MANAGER')]
        }
        if (target.adminGrade === 'MANAGER') {
            return [userOption()]
        }
        return []
    }

    if (target.role === 'ROLE_USER') {
        return [adminOption('MANAGER'), adminOption('SUPERVISOR')]
    }
    if (target.adminGrade === 'MANAGER') {
        return [userOption(), adminOption('SUPERVISOR')]
    }
    if (target.adminGrade === 'SUPERVISOR') {
        return [userOption(), adminOption('MANAGER')]
    }
    return []
}

function adminOption(adminGrade: Extract<AdminGrade, 'MANAGER' | 'SUPERVISOR'>) {
    return {
        role: 'ROLE_ADMIN' as const,
        adminGrade,
        label: formatAdminGrade(adminGrade),
    }
}

function userOption() {
    return {
        role: 'ROLE_USER' as const,
        adminGrade: null,
        label: formatMemberRole('ROLE_USER'),
    }
}
