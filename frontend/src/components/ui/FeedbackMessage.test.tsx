import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { FeedbackMessage } from './FeedbackMessage'

describe('FeedbackMessage', () => {
    it.each([
        ['error', 'alert', 'border-danger/35', 'bg-danger-soft', 'text-danger'],
        ['success', 'status', 'border-success/35', 'bg-success-soft', 'text-success'],
        ['info', 'status', 'border-line', 'bg-subtle', 'text-muted'],
    ] as const)('%s 상태에 의미 기반 색상 토큰을 적용한다', (tone, role, borderClass, backgroundClass, textClass) => {
        render(<FeedbackMessage tone={tone}>{tone} message</FeedbackMessage>)

        const message = screen.getByRole(role)
        expect(message).toHaveClass(borderClass, backgroundClass, textClass)
    })
})
