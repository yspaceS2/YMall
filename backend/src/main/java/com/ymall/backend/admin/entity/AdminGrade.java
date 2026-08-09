package com.ymall.backend.admin.entity;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum AdminGrade {
    MANAGER(
        1,
        AdminPermission.DASHBOARD_READ,
        AdminPermission.MEMBER_READ,
        AdminPermission.MEMBER_RESTRICT_LIMITED,
        AdminPermission.SELLER_READ,
        AdminPermission.SELLER_APPLICATION_REVIEW,
        AdminPermission.SUPPORT_REPLY,
        AdminPermission.PRODUCT_REVIEW,
        AdminPermission.REFUND_STANDARD,
        AdminPermission.SETTLEMENT_REVIEW,
        AdminPermission.TASK_SELF,
        AdminPermission.CATEGORY_READ,
        AdminPermission.AUDIT_OWN_READ
    ),
    SUPERVISOR(
        2,
        AdminPermission.DASHBOARD_READ,
        AdminPermission.MEMBER_READ,
        AdminPermission.MEMBER_RESTRICT_LIMITED,
        AdminPermission.MEMBER_RESTRICT_ALL,
        AdminPermission.SELLER_READ,
        AdminPermission.SELLER_APPLICATION_REVIEW,
        AdminPermission.SELLER_APPLICATION_DECIDE,
        AdminPermission.SUPPORT_REPLY,
        AdminPermission.PRODUCT_REVIEW,
        AdminPermission.REFUND_STANDARD,
        AdminPermission.REFUND_ALL,
        AdminPermission.SETTLEMENT_REVIEW,
        AdminPermission.SETTLEMENT_APPROVE,
        AdminPermission.TASK_SELF,
        AdminPermission.CATEGORY_READ,
        AdminPermission.CATEGORY_MANAGE_PARTIAL,
        AdminPermission.ADMIN_MANAGER_MANAGE,
        AdminPermission.AUDIT_OWN_READ,
        AdminPermission.AUDIT_ALL_READ
    ),
    SUPER_ADMIN(3, AdminPermission.values());

    private final int level;
    private final Set<AdminPermission> permissions;

    AdminGrade(int level, AdminPermission... permissions) {
        this.level = level;
        EnumSet<AdminPermission> permissionSet = EnumSet.noneOf(AdminPermission.class);
        Collections.addAll(permissionSet, permissions);
        this.permissions = Collections.unmodifiableSet(permissionSet);
    }

    public int level() {
        return level;
    }

    public Set<AdminPermission> permissions() {
        return permissions;
    }

    public boolean hasPermission(AdminPermission permission) {
        return permissions.contains(permission);
    }
}
