import { spawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { createServer } from 'vite'

const root = fileURLToPath(new URL('../', import.meta.url))
const playwrightCli = fileURLToPath(
    new URL('../node_modules/@playwright/test/cli.js', import.meta.url),
)
const server = await createServer({
    root,
    server: {
        host: '127.0.0.1',
        port: 4173,
        strictPort: true,
    },
})

await server.listen()

try {
    const exitCode = await new Promise((resolve, reject) => {
        const child = spawn(
            process.execPath,
            [playwrightCli, 'test', ...process.argv.slice(2)],
            {
                cwd: root,
                env: process.env,
                stdio: 'inherit',
            },
        )
        child.once('error', reject)
        child.once('exit', (code, signal) => {
            if (signal) {
                reject(new Error(`Playwright가 ${signal} 신호로 종료되었습니다.`))
                return
            }
            resolve(code ?? 1)
        })
    })
    process.exitCode = exitCode
} finally {
    await server.close()
}
