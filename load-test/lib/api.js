import http from 'k6/http'
import { check } from 'k6'

import { config } from './config.js'

const JSON_HEADERS = {
    'Content-Type': 'application/json',
}

export function get(path, token = null, tags = {}) {
    return http.get(`${config.baseUrl}${path}`, requestParams(token, tags))
}

export function post(path, body = null, token = null, tags = {}) {
    return http.post(
        `${config.baseUrl}${path}`,
        body === null ? null : JSON.stringify(body),
        requestParams(token, tags),
    )
}

export function del(path, token = null, tags = {}) {
    return http.del(`${config.baseUrl}${path}`, null, requestParams(token, tags))
}

export function expectStatus(response, expectedStatus, name) {
    return check(response, {
        [`${name}: status ${expectedStatus}`]: (result) => result.status === expectedStatus,
    })
}

export function responseData(response) {
    try {
        const body = response.json()
        return body && body.success ? body.data : null
    } catch (error) {
        return null
    }
}

export function discoverProduct() {
    if (config.productId !== '') {
        const response = get(`/products/${encodeURIComponent(config.productId)}`, null, {
            name: 'GET /products/{productId} setup',
        })

        if (!expectStatus(response, 200, 'configured product lookup')) {
            throw new Error('LOAD_TEST_PRODUCT_ID does not identify a readable product.')
        }

        return String(config.productId)
    }

    const response = get('/products?page=1&size=100', null, {
        name: 'GET /products setup',
    })

    if (!expectStatus(response, 200, 'product discovery')) {
        throw new Error('Unable to query products during load-test setup.')
    }

    const page = responseData(response)
    const product = page?.content?.find((item) => item.status === 'APPROVED' && item.stock > 0)

    if (!product) {
        throw new Error(
            'No approved in-stock product was found. Prepare test data or set LOAD_TEST_PRODUCT_ID.',
        )
    }

    return String(product.productId)
}

function requestParams(token, tags) {
    const headers = { ...JSON_HEADERS }

    if (token) {
        headers.Authorization = `Bearer ${token}`
    }

    return {
        headers,
        tags,
        redirects: 0,
    }
}
