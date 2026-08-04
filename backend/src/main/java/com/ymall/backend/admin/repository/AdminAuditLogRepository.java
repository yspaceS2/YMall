package com.ymall.backend.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ymall.backend.admin.entity.AdminAuditLog;
import com.ymall.backend.admin.entity.AdminAuditTargetType;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    List<AdminAuditLog> findTop20ByTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(
        AdminAuditTargetType targetType,
        Long targetId
    );

    List<AdminAuditLog> findTop20ByActorIdAndTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(
        Long actorMemberId,
        AdminAuditTargetType targetType,
        Long targetId
    );
}
