import type { MemberRole } from '../types/auth'

function isSafeInternalPath(path: string | undefined): path is string {
    return Boolean(path?.startsWith('/') && !path.startsWith('//'))
}

export function resolveLoginDestination(
    requestedPath: string | undefined,
    role: MemberRole | null,
) {
    if (!isSafeInternalPath(requestedPath) || !role || requestedPath === '/forbidden') {
        return '/'
    }

    if (requestedPath.startsWith('/admin') && role !== 'ROLE_ADMIN') {
        return '/'
    }

    if (
        requestedPath.startsWith('/seller') &&
        role !== 'ROLE_SELLER' &&
        role !== 'ROLE_ADMIN'
    ) {
        return '/'
    }

    return requestedPath
}
