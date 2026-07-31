import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { ToastProvider } from './ToastProvider'
import { useToast } from './useToast'

function ToastTrigger() {
    const { showToast } = useToast()
    return (
        <button type="button" onClick={() => showToast('저장했습니다.', 'success')}>
            알림 표시
        </button>
    )
}

describe('ToastProvider', () => {
    it('요청한 메시지를 표시하고 닫는다', async () => {
        const user = userEvent.setup()
        render(
            <ToastProvider>
                <ToastTrigger />
            </ToastProvider>,
        )

        await user.click(screen.getByRole('button', { name: '알림 표시' }))
        expect(screen.getByRole('status')).toHaveTextContent('저장했습니다.')

        await user.click(screen.getByRole('button', { name: '알림 닫기' }))
        expect(screen.queryByText('저장했습니다.')).not.toBeInTheDocument()
    })
})
