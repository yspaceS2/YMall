import { LoaderCircle, PackageCheck } from 'lucide-react'
import { SellerProductForm } from '../components/seller/SellerProductForm'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { useSellerProductEditor } from '../hooks/useSellerProductEditor'

export function SellerProductEditorPage({
    initialProductId,
}: {
    initialProductId?: number
} = {}) {
    const editor = useSellerProductEditor(initialProductId)

    if (editor.isLoading) {
        return (
            <div className="grid min-h-100 place-content-center">
                <LoaderCircle className="size-6 animate-spin" />
            </div>
        )
    }

    return (
        <section
            className="mx-auto max-w-350 px-4 py-10 min-[601px]:px-8 min-[601px]:py-14"
            id="management-overview"
        >
            <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-accent">
                SELLER CENTER
            </p>
            <h1 className="mb-8 font-serif text-[clamp(40px,6vw,64px)] leading-none tracking-tighter">
                판매자 관리
            </h1>
            {editor.errorMessage && (
                <FeedbackMessage className="mb-5" tone="error">
                    {editor.errorMessage}
                </FeedbackMessage>
            )}
            {!editor.hasProfile ? (
                <FeedbackMessage tone="error">
                    판매자 정보를 먼저 등록해야 이 관리 기능을 사용할 수 있습니다.
                </FeedbackMessage>
            ) : (
                <section className="border-t-2 border-ink pt-5">
                    <h2 className="mb-6 flex items-center gap-2 text-xl font-bold">
                        <PackageCheck />
                        상품 관리
                    </h2>
                    <SellerProductForm
                        categories={editor.categories}
                        productForm={editor.productForm}
                        imageInputVersion={editor.imageInputVersion}
                        editingProductId={editor.editingProductId}
                        isSaving={editor.isSaving}
                        setProductForm={editor.setProductForm}
                        onThumbnailFilesChange={editor.setThumbnailFiles}
                        onProductImageFilesChange={editor.setProductImageFiles}
                        onDetailImageFilesChange={editor.setDetailImageFiles}
                        onCancel={editor.resetEditor}
                        onSubmit={editor.saveProduct}
                    />
                </section>
            )}
        </section>
    )
}
