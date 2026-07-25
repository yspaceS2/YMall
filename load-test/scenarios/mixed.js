import execution from 'k6/execution'
import { sleep } from 'k6'

import { loginForCurrentVu, refreshAccessToken, validateCredentials } from '../lib/auth.js'
import { config, rampingOptions } from '../lib/config.js'
import { del, discoverProduct, expectStatus, get, post, responseData } from '../lib/api.js'

export const options = rampingOptions('mixed')

let accessToken = null
let addressId = config.addressId

export function setup() {
    validateCredentials()

    return {
        productId: discoverProduct(),
    }
}

export default function (data) {
    if (accessToken === null) {
        accessToken = loginForCurrentVu()
    }

    if (accessToken === null) {
        return
    }

    const iteration = execution.vu.iterationInScenario + 1

    if (iteration % config.refreshEvery === 0) {
        accessToken = refreshAccessToken()

        if (accessToken === null) {
            return
        }
    }

    expectStatus(
        get('/cart', accessToken, { name: 'GET /cart' }),
        200,
        'cart lookup',
    )
    expectStatus(
        get('/orders?page=1&size=20', accessToken, { name: 'GET /orders' }),
        200,
        'order list',
    )

    if (config.enableWrites && iteration % config.writeEvery === 0) {
        createOrder(data.productId)
    }

    sleep(config.thinkTime)
}

function createOrder(productId) {
    const cartResponse = post(
        '/cart/items',
        { productId: Number(productId), quantity: 1 },
        accessToken,
        { name: 'POST /cart/items' },
    )

    if (!expectStatus(cartResponse, 201, 'cart item creation')) {
        return
    }

    const cartItemId = responseData(cartResponse)?.cartItemId
    const selectedAddressId = resolveAddressId()

    if (selectedAddressId === null) {
        cleanupCartItem(cartItemId)
        throw new Error(
            'Write flow requires a member delivery address or LOAD_TEST_ADDRESS_ID.',
        )
    }

    const orderResponse = post(
        '/orders',
        {
            idempotencyKey: [
                'k6',
                execution.vu.idInTest,
                execution.scenario.iterationInTest,
                Date.now(),
            ].join('-'),
            addressId: Number(selectedAddressId),
        },
        accessToken,
        { name: 'POST /orders' },
    )

    if (!expectStatus(orderResponse, 201, 'order creation')) {
        cleanupCartItem(cartItemId)
    }
}

function resolveAddressId() {
    if (addressId !== '') {
        return addressId
    }

    const response = get('/members/me/addresses', accessToken, {
        name: 'GET /members/me/addresses',
    })

    if (!expectStatus(response, 200, 'address lookup')) {
        return null
    }

    const addresses = responseData(response)
    const address = addresses?.find((item) => item.isDefault) ?? addresses?.[0]
    addressId = address ? String(address.addressId) : ''

    return addressId === '' ? null : addressId
}

function cleanupCartItem(cartItemId) {
    if (!cartItemId) {
        return
    }

    const response = del(`/cart/items/${cartItemId}`, accessToken, {
        name: 'DELETE /cart/items/{cartItemId}',
    })
    expectStatus(response, 204, 'cart cleanup')
}
