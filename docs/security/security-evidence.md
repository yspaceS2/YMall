# 보안 점검 증적

## 목적과 범위

이 문서는 YMall의 보안 통제와 검증 근거를 한곳에서 찾기 위한 인덱스다. KISA 가이드를 기준으로 수행한 자체 점검이며, 외부 전문기관의 보안 인증이나 침투 테스트 결과를 의미하지 않는다.

상세 판정과 잔여 위험은 다음 문서를 기준으로 관리한다.

- [KISA 2026 자체평가](./kisa-2026-assessment.md)
- [배포 전 보안 체크리스트](./pre-deployment-security-checklist.md)
- [보안 자동화 정책](./security-automation.md)

## 자동 검증 증적

| 검증 | 실행 시점 | 실패 기준 | 확인 위치 | 보존 |
| --- | --- | --- | --- | --- |
| Backend 테스트 | PR, `develop`·`main` push | Gradle 테스트 실패 | GitHub Actions `CI / Backend Test` | Actions 실행 기록 |
| Frontend 테스트·Lint·Build | PR, `develop`·`main` push | 테스트·Lint·Build 실패 | GitHub Actions `CI / Frontend Unit Test, Lint and Build` | Actions 실행 기록 |
| Frontend E2E | PR, `develop`·`main` push | Playwright 시나리오 실패 | GitHub Actions `CI / Frontend E2E Test` | 실패 artifact 7일 |
| Secret Scan | PR, `develop`·`main` push | Gitleaks 탐지 | GitHub Actions `Security Checks / Secret Scan` | Actions 실행 기록 |
| SAST | PR, `develop`·`main` push | Semgrep `ERROR` 탐지 | GitHub Actions `Security Checks / Semgrep SAST` | Actions 실행 기록 |
| 저장소 취약점 | PR, `develop`·`main` push | 수정 가능한 HIGH·CRITICAL 탐지 | GitHub Actions `Security Checks / Trivy Repository Scan` | Actions 실행 기록 |
| 컨테이너 취약점 | `develop`·`main` push, 수동 | 수정 가능한 HIGH·CRITICAL 탐지 | GitHub Actions `Security Checks / Trivy Container Image Scan` | Actions 실행 기록 |
| 익명 동적 점검 | 수동 | ZAP 정책상 Medium 이상 | GitHub Actions `OWASP ZAP Baseline` | 보고서 artifact 7일 |
| Backend 커버리지 | PR, `develop`·`main` push | 테스트 실패 | `backend-coverage-report` artifact | 14일 |
| Frontend 커버리지 | PR, `develop`·`main` push | 테스트 실패 | `frontend-coverage-report` artifact | 14일 |

커버리지는 테스트 누락을 찾기 위한 관찰 지표로 사용한다. 높은 수치만을 만들기 위한 의미 없는 테스트를 방지하기 위해 최초 도입 시 전역 실패 임계값을 두지 않고, 실제 결과를 확인한 뒤 주문·결제·환불·권한 같은 핵심 영역을 우선 관리한다.

## 통제와 구현 근거

| 통제 영역 | 구현 근거 | 대표 검증 |
| --- | --- | --- |
| 인증 토큰 | `global/security`, Redis Refresh Token 회전·폐기 | `JwtTokenProviderTest`, `RefreshTokenIntegrationTest`, `JwtAuthorizationIntegrationTest` |
| 역할과 소유권 | Spring Security, 관리자 Permission, 서비스 소유권 검사 | `HttpSecurityBoundaryIntegrationTest`, `AdminRoleApiIntegrationTest`, `SellerManagementApiIntegrationTest` |
| 로그인 공격 방어 | `LoginAttemptLimiter`, 비밀번호 정책 | `LoginAttemptLimiterTest`, `PasswordPolicyTest`, `MemberLoginIntegrationTest` |
| 결제 무결성 | 서버 금액 검증, 멱등키, Provider 상태 검증 | `PaymentApiIntegrationTest`, `PaymentConcurrencyIntegrationTest`, `PaymentWebhookIntegrationTest` |
| 환불 무결성 | 환불 가능 수량·금액, 중복·동시 요청 제어 | `PaymentRefundConcurrencyIntegrationTest`, `PaymentRefundFailureIntegrationTest`, `ReturnRequestTransactionServiceTest` |
| 파일 안전성 | 크기·MIME·실제 이미지·안전한 경로 검사 | `LocalFileStorageServiceTest`, `FileUploadIntegrationTest` |
| 메시지 정합성 | Transactional Outbox, 재시도·DLT·순서 보존 | `OrderOutboxIntegrationTest`, `KafkaRetryDltIntegrationTest`, `KafkaMalformedEventDltIntegrationTest` |
| 개인정보 보호 | 정산 계좌 암호화, 배송지 보존·마스킹 | `AesGcmSettlementAccountCipherTest`, `SellerDeliveryAddressPrivacyPolicyTest` |
| DB 최소 권한 | Flyway·런타임·백업 역할 분리, 내부 네트워크 | Jira `YMALL-132`, 배포 전 보안 체크리스트 |
| 웹 응답 보안 | HTTPS, HSTS, CSP, COOP, CORP, CORS | 운영 헤더 점검, ZAP Baseline, `HttpSecurityBoundaryIntegrationTest` |

## 결과 확인 절차

1. 대상 PR 또는 커밋의 GitHub Actions 실행을 연다.
2. `CI`와 `Security Checks`의 모든 필수 작업이 성공했는지 확인한다.
3. 필요한 경우 Backend·Frontend 커버리지 artifact를 내려받아 `index.html`을 확인한다.
4. 배포 전에는 수동 `OWASP ZAP Baseline`을 실행하고 결과 artifact를 확인한다.
5. 발견 사항은 실제 비밀값이나 개인정보를 복사하지 않고 Jira에 영향, 조치, 재검증 결과만 기록한다.

## 잔여 위험과 한계

- 익명 ZAP Baseline은 비파괴적 수동 검사이며 인증 사용자 전체 흐름의 능동 침투 테스트를 대체하지 않는다.
- CI 성공은 알려진 규칙과 작성된 테스트 범위 안에서의 결과이며 모든 취약점 부재를 보장하지 않는다.
- 보안 검사 결과는 도구의 규칙과 취약점 데이터베이스 갱신에 따라 달라질 수 있다.
- 운영 계정, 방화벽, SSH, DB 감사 로그 등 인프라 항목은 코드 검사 외에 배포 환경에서 별도로 확인한다.
- GitHub Actions artifact는 보존 기간이 있으므로 장기 근거는 이 문서와 관련 PR·Jira의 변경 이력으로 관리한다.

## 관련 작업

- [YMALL-79 보안 자체점검](https://yspace-labs.atlassian.net/browse/YMALL-79)
- [YMALL-123 PostgreSQL 통합 테스트](https://yspace-labs.atlassian.net/browse/YMALL-123)
- [YMALL-128 역할별 보안 회귀 검증](https://yspace-labs.atlassian.net/browse/YMALL-128)
- [YMALL-132 PostgreSQL 최소 권한](https://yspace-labs.atlassian.net/browse/YMALL-132)
- [YMALL-133 CI 커버리지 및 보안 점검 증적 문서화](https://yspace-labs.atlassian.net/browse/YMALL-133)
