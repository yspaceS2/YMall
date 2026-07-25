import execution from 'k6/execution'

import { config } from './config.js'
import { expectStatus, post, responseData } from './api.js'

export function validateCredentials() {
    if (config.userEmails.length === 0 || config.userPasswords.length === 0) {
        throw new Error(
            'LOAD_TEST_USER_EMAILS and LOAD_TEST_USER_PASSWORDS are required for the mixed scenario.',
        )
    }

    if (config.userEmails.length !== config.userPasswords.length) {
        throw new Error('The load-test email and password lists must contain the same number of values.')
    }

    if (config.userEmails.length < config.targetVus) {
        throw new Error(
            'Provide at least one dedicated account per target VU to avoid refresh-token and cart collisions.',
        )
    }
}

export function loginForCurrentVu() {
    const credentialIndex = (execution.vu.idInTest - 1) % config.userEmails.length
    const response = post(
        '/members/login',
        {
            email: config.userEmails[credentialIndex],
            password: config.userPasswords[credentialIndex],
        },
        null,
        { name: 'POST /members/login' },
    )

    if (!expectStatus(response, 200, 'login')) {
        return null
    }

    return responseData(response)?.accessToken ?? null
}

export function refreshAccessToken() {
    const response = post('/members/tokens/refresh', null, null, {
        name: 'POST /members/tokens/refresh',
    })

    if (!expectStatus(response, 200, 'token refresh')) {
        return null
    }

    return responseData(response)?.accessToken ?? null
}
