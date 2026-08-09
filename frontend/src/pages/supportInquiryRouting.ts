export function supportBasePath(admin: boolean, role: string | null) {
    if (admin) return '/admin/support'
    return role === 'ROLE_SELLER' ? '/seller/support' : '/mypage/support'
}
