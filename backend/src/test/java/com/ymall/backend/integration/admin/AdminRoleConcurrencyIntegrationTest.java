package com.ymall.backend.integration.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ymall.backend.admin.dto.AdminRoleUpdateRequest;
import com.ymall.backend.admin.entity.AdminGrade;
import com.ymall.backend.admin.repository.AdminAuditLogRepository;
import com.ymall.backend.admin.service.AdminRoleService;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.RefreshTokenService;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.testsupport.PostgresIntegrationTestSupport;

@SpringBootTest
@ActiveProfiles("test")
class AdminRoleConcurrencyIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired private AdminRoleService adminRoleService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AdminAuditLogRepository auditLogRepository;

    @MockitoBean private RefreshTokenService refreshTokenService;

    private final List<Long> createdMemberIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        auditLogRepository.deleteAll(auditLogRepository.findAll().stream()
            .filter(log -> createdMemberIds.contains(log.getActor().getId())
                || createdMemberIds.contains(log.getTargetId()))
            .toList());
        memberRepository.deleteAllById(createdMemberIds);
    }

    @Test
    void concurrentPromotionsForSameMemberAllowOnlyOneChange() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Member firstSupervisor = saveAdmin("first-" + suffix, AdminGrade.SUPERVISOR);
        Member secondSupervisor = saveAdmin("second-" + suffix, AdminGrade.SUPERVISOR);
        Member target = memberRepository.saveAndFlush(new Member(
            "target-" + suffix + "@example.test",
            "password",
            "Target",
            MemberRole.ROLE_USER
        ));
        createdMemberIds.add(target.getId());
        AdminRoleUpdateRequest request = new AdminRoleUpdateRequest(
            MemberRole.ROLE_ADMIN,
            AdminGrade.MANAGER,
            "Concurrent role change verification"
        );
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<ChangeResult> first = executor.submit(() -> changeRole(
                firstSupervisor.getId(),
                target.getId(),
                request,
                ready,
                start
            ));
            Future<ChangeResult> second = executor.submit(() -> changeRole(
                secondSupervisor.getId(),
                target.getId(),
                request,
                ready,
                start
            ));
            ready.await();
            start.countDown();

            assertThat(List.of(first.get(), second.get()))
                .containsExactlyInAnyOrder(ChangeResult.SUCCESS, ChangeResult.FORBIDDEN);
            Member updated = memberRepository.findById(target.getId()).orElseThrow();
            assertThat(updated.getRole()).isEqualTo(MemberRole.ROLE_ADMIN);
            assertThat(updated.getAdminGrade()).isEqualTo(AdminGrade.MANAGER);
            assertThat(auditLogRepository.findAll())
                .filteredOn(log -> log.getTargetId().equals(target.getId()))
                .hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private ChangeResult changeRole(
        Long actorMemberId,
        Long targetMemberId,
        AdminRoleUpdateRequest request,
        CountDownLatch ready,
        CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            adminRoleService.changeRole(actorMemberId, targetMemberId, request);
            return ChangeResult.SUCCESS;
        } catch (BusinessException exception) {
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ADMIN_ROLE_CHANGE_FORBIDDEN);
            return ChangeResult.FORBIDDEN;
        }
    }

    private Member saveAdmin(String emailPrefix, AdminGrade grade) {
        Member member = new Member(
            emailPrefix + "@example.test",
            "password",
            emailPrefix,
            MemberRole.ROLE_ADMIN
        );
        member.changeAdminRole(MemberRole.ROLE_ADMIN, grade);
        Member saved = memberRepository.saveAndFlush(member);
        createdMemberIds.add(saved.getId());
        return saved;
    }

    private enum ChangeResult {
        SUCCESS,
        FORBIDDEN
    }
}
