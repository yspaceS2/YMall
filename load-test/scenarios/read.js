import { sleep } from 'k6'

import { config, rampingOptions } from '../lib/config.js'
import { discoverProduct, expectStatus, get, responseData } from '../lib/api.js'

export const options = rampingOptions('read')

export function setup() {
    return {
        productId: discoverProduct(),
    }
}

export default function (data) {
    const listResponse = get('/products?page=1&size=20', null, {
        name: 'GET /products',
    })
    expectStatus(listResponse, 200, 'product list')

    const detailResponse = get(`/products/${data.productId}`, null, {
        name: 'GET /products/{productId}',
    })
    expectStatus(detailResponse, 200, 'product detail')

    const product = responseData(detailResponse)
    const keyword = config.searchKeyword || product?.name || ''

    if (keyword !== '') {
        const searchResponse = get(
            `/products/search?keyword=${encodeURIComponent(keyword)}&page=1&size=20`,
            null,
            { name: 'GET /products/search' },
        )
        expectStatus(searchResponse, 200, 'product search')
    }

    sleep(config.thinkTime)
}
