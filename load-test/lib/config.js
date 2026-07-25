function numberFromEnv(name, fallback, minimum = 0) {
    const rawValue = __ENV[name]
    const value = rawValue === undefined || rawValue === '' ? fallback : Number(rawValue)

    if (!Number.isFinite(value) || value < minimum) {
        throw new Error(`${name} must be a number greater than or equal to ${minimum}.`)
    }

    return value
}

function integerFromEnv(name, fallback, minimum = 0) {
    const value = numberFromEnv(name, fallback, minimum)

    if (!Number.isInteger(value)) {
        throw new Error(`${name} must be an integer.`)
    }

    return value
}

function stringFromEnv(name, fallback = '') {
    const value = __ENV[name]
    return value === undefined ? fallback : value.trim()
}

function listFromEnv(name) {
    const value = stringFromEnv(name)
    return value === '' ? [] : value.split(',').map((item) => item.trim())
}

function rateFromEnv(name, fallback) {
    const value = numberFromEnv(name, fallback, 0)

    if (value > 1) {
        throw new Error(`${name} must be less than or equal to 1.`)
    }

    return value
}

export const config = {
    baseUrl: stringFromEnv('LOAD_TEST_BASE_URL', 'http://backend:8080/api').replace(/\/+$/, ''),
    productId: stringFromEnv('LOAD_TEST_PRODUCT_ID'),
    searchKeyword: stringFromEnv('LOAD_TEST_SEARCH_KEYWORD'),
    userEmails: listFromEnv('LOAD_TEST_USER_EMAILS'),
    userPasswords: listFromEnv('LOAD_TEST_USER_PASSWORDS'),
    addressId: stringFromEnv('LOAD_TEST_ADDRESS_ID'),
    enableWrites: stringFromEnv('LOAD_TEST_ENABLE_WRITES', 'false').toLowerCase() === 'true',
    targetVus: integerFromEnv('LOAD_TEST_TARGET_VUS', 10, 1),
    rampUp: stringFromEnv('LOAD_TEST_RAMP_UP', '30s'),
    hold: stringFromEnv('LOAD_TEST_HOLD', '1m'),
    rampDown: stringFromEnv('LOAD_TEST_RAMP_DOWN', '30s'),
    spikeBaseVus: integerFromEnv('LOAD_TEST_SPIKE_BASE_VUS', 5, 1),
    spikeVus: integerFromEnv('LOAD_TEST_SPIKE_VUS', 50, 1),
    spikeWarmUp: stringFromEnv('LOAD_TEST_SPIKE_WARM_UP', '20s'),
    spikeDuration: stringFromEnv('LOAD_TEST_SPIKE_DURATION', '30s'),
    spikeRecovery: stringFromEnv('LOAD_TEST_SPIKE_RECOVERY', '30s'),
    p95Ms: integerFromEnv('LOAD_TEST_P95_MS', 500, 1),
    failureRate: rateFromEnv('LOAD_TEST_FAILURE_RATE', 0.01),
    thinkTime: numberFromEnv('LOAD_TEST_THINK_TIME', 1, 0),
    refreshEvery: integerFromEnv('LOAD_TEST_REFRESH_EVERY', 20, 1),
    writeEvery: integerFromEnv('LOAD_TEST_WRITE_EVERY', 10, 1),
}

export function rampingOptions(name) {
    return {
        scenarios: {
            [name]: {
                executor: 'ramping-vus',
                startVUs: 0,
                stages: [
                    { duration: config.rampUp, target: config.targetVus },
                    { duration: config.hold, target: config.targetVus },
                    { duration: config.rampDown, target: 0 },
                ],
                gracefulRampDown: '10s',
                tags: { workload: name },
            },
        },
        thresholds: thresholds(),
    }
}

export function spikeOptions() {
    return {
        scenarios: {
            spike: {
                executor: 'ramping-vus',
                startVUs: 0,
                stages: [
                    { duration: config.spikeWarmUp, target: config.spikeBaseVus },
                    { duration: '5s', target: config.spikeVus },
                    { duration: config.spikeDuration, target: config.spikeVus },
                    { duration: '5s', target: config.spikeBaseVus },
                    { duration: config.spikeRecovery, target: config.spikeBaseVus },
                    { duration: '10s', target: 0 },
                ],
                gracefulRampDown: '10s',
                tags: { workload: 'spike' },
            },
        },
        thresholds: thresholds(),
    }
}

function thresholds() {
    return {
        http_req_failed: [`rate<${config.failureRate}`],
        http_req_duration: [`p(95)<${config.p95Ms}`],
        checks: ['rate>0.99'],
    }
}
