import { sleep } from 'k6'

import { config, spikeOptions } from '../lib/config.js'
import { discoverProduct, expectStatus, get } from '../lib/api.js'

export const options = spikeOptions()

export function setup() {
    return {
        productId: discoverProduct(),
    }
}

export default function (data) {
    const response = get(`/products/${data.productId}`, null, {
        name: 'GET /products/{productId}',
    })
    expectStatus(response, 200, 'spike product detail')
    sleep(config.thinkTime)
}
