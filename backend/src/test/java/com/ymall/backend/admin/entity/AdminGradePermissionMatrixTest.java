package com.ymall.backend.admin.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminGradePermissionMatrixTest {

    @Test
    void managerHasOperationalPermissionsWithoutApprovalOrAssignmentAuthority() {
        assertThat(AdminGrade.MANAGER.permissions())
            .contains(
                AdminPermission.DASHBOARD_READ,
                AdminPermission.MEMBER_READ,
                AdminPermission.MEMBER_RESTRICT_LIMITED,
                AdminPermission.SELLER_APPLICATION_REVIEW,
                AdminPermission.REFUND_STANDARD,
                AdminPermission.SETTLEMENT_REVIEW,
                AdminPermission.TASK_SELF,
                AdminPermission.AUDIT_OWN_READ
            )
            .doesNotContain(
                AdminPermission.MEMBER_RESTRICT_ALL,
                AdminPermission.SELLER_APPLICATION_DECIDE,
                AdminPermission.REFUND_ALL,
                AdminPermission.SETTLEMENT_APPROVE,
                AdminPermission.TASK_ASSIGN,
                AdminPermission.ADMIN_MANAGER_MANAGE,
                AdminPermission.ADMIN_ALL_MANAGE,
                AdminPermission.AUDIT_ALL_READ
            );
    }

    @Test
    void supervisorCanApproveAndManageManagersWithoutSuperAdminAuthority() {
        assertThat(AdminGrade.SUPERVISOR.permissions())
            .contains(
                AdminPermission.MEMBER_RESTRICT_ALL,
                AdminPermission.SELLER_APPLICATION_DECIDE,
                AdminPermission.REFUND_ALL,
                AdminPermission.SETTLEMENT_APPROVE,
                AdminPermission.CATEGORY_MANAGE_PARTIAL,
                AdminPermission.ADMIN_MANAGER_MANAGE,
                AdminPermission.AUDIT_ALL_READ
            )
            .doesNotContain(
                AdminPermission.TASK_ASSIGN,
                AdminPermission.CATEGORY_MANAGE_ALL,
                AdminPermission.ADMIN_ALL_MANAGE
            );
    }

    @Test
    void superAdminHasEveryPermission() {
        assertThat(AdminGrade.SUPER_ADMIN.permissions())
            .containsExactlyInAnyOrder(AdminPermission.values());
    }
}
