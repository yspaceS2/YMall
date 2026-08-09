import { fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ProductImageUploadField } from './ProductImageUploadField'

describe('ProductImageUploadField', () => {
    beforeEach(() => {
        vi.stubGlobal('URL', {
            createObjectURL: vi.fn(() => 'blob:preview'),
            revokeObjectURL: vi.fn(),
        })
    })

    afterEach(() => {
        vi.unstubAllGlobals()
    })

    it('shows a selected image preview and removes it', () => {
        const onFilesChange = vi.fn()
        render(
            <ProductImageUploadField
                label="상품 이미지"
                description="상품 이미지를 선택합니다."
                onFilesChange={onFilesChange}
            />,
        )

        const input = screen.getByLabelText('상품 이미지 파일 선택')
        const file = new File(['image'], 'product.webp', { type: 'image/webp' })

        expect(screen.getByText('상품 이미지').parentElement).toHaveClass(
            'border-b',
            'px-4',
            'py-5',
        )

        fireEvent.change(input, { target: { files: [file] } })

        expect(screen.getByText('product.webp')).toBeInTheDocument()
        expect(onFilesChange).toHaveBeenLastCalledWith([file])
        expect(screen.getByAltText('상품 이미지 미리보기 1')).toHaveAttribute(
            'src',
            'blob:preview',
        )

        fireEvent.click(screen.getByRole('button', { name: 'product.webp 삭제' }))

        expect(screen.queryByText('product.webp')).not.toBeInTheDocument()
        expect(onFilesChange).toHaveBeenLastCalledWith([])
        expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:preview')
    })

    it('does not add a non-image file to the preview list', () => {
        render(
            <ProductImageUploadField
                label="상품 이미지"
                description="상품 이미지를 선택합니다."
            />,
        )

        const input = screen.getByLabelText('상품 이미지 파일 선택')
        const file = new File(['text'], 'memo.txt', { type: 'text/plain' })

        fireEvent.change(input, { target: { files: [file] } })

        expect(screen.queryByText('memo.txt')).not.toBeInTheDocument()
        expect(screen.getByRole('alert')).toHaveTextContent(
            'JPG, PNG, WEBP 이미지 파일만 선택할 수 있습니다.',
        )
        expect(URL.createObjectURL).not.toHaveBeenCalled()
    })

    it('removes an existing image through the provided callback', () => {
        const onExistingImageRemove = vi.fn()
        render(
            <ProductImageUploadField
                label="대표 이미지"
                description="대표 이미지를 선택합니다."
                existingImages={[{ imageUrl: '/images/product.jpg' }]}
                onExistingImageRemove={onExistingImageRemove}
            />,
        )

        fireEvent.click(screen.getByRole('button', {
            name: '대표 이미지 현재 이미지 1 삭제',
        }))

        expect(onExistingImageRemove).toHaveBeenCalledWith(0)
    })
})
