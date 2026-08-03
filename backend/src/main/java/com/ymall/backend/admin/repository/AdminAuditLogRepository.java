package com.ymall.backend.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ymall.backend.admin.entity.AdminAuditLog;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
}
