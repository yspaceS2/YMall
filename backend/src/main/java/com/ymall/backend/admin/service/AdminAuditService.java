package com.ymall.backend.admin.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.admin.entity.AdminAuditAction;
import com.ymall.backend.admin.entity.AdminAuditLog;
import com.ymall.backend.admin.entity.AdminAuditTargetType;
import com.ymall.backend.admin.repository.AdminAuditLogRepository;
import com.ymall.backend.member.entity.Member;

@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AdminAuditLogRepository auditLogRepository;

    public void record(
        Member actor,
        AdminAuditTargetType targetType,
        Long targetId,
        AdminAuditAction action,
        String beforeValue,
        String afterValue,
        String reason
    ) {
        auditLogRepository.save(new AdminAuditLog(
            actor,
            targetType,
            targetId,
            action,
            beforeValue,
            afterValue,
            reason
        ));
    }
}
