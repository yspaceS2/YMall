import { AdminDashboard } from '../components/dashboard/AdminDashboard'

export function AdminManagementPage() {
    return (
        <section
            className="mx-auto max-w-350 px-4 py-3 min-[601px]:px-8"
            id="management-overview"
        >
            <AdminDashboard />
        </section>
    )
}
