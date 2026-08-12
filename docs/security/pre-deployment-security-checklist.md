# 배포 전 인증·인가 보안 체크리스트

## 범위와 한계

이 문서는 YMall의 배포 전 자체 보안 점검 결과를 기록합니다. 외부 보안 인증이나
KISA 공식 심사 결과가 아닙니다. 실제 운영 도메인, HTTPS/TLS, 운영 CORS와 외부
노출 포트는 배포 후 별도로 확인합니다.

실제 개인정보와 운영 시크릿은 사용하지 않습니다. 자동 검증은 `test` 프로필의
가상 회원과 폐기 가능한 데이터만 사용합니다.

## 인증과 세션

| 점검 항목 | 결과 | 자동 검증 근거 |
| --- | --- | --- |
| 인증이 필요한 API의 토큰 누락 차단 | 통과 | `JwtAuthorizationIntegrationTest.missingTokenReturnsUnauthorized` |
| 변조된 액세스 토큰 차단 | 통과 | `JwtAuthorizationIntegrationTest.invalidTokenReturnsUnauthorized` |
| 로그인·Refresh Token 회전·로그아웃 | 통과 | `RefreshTokenIntegrationTest.loginRefreshRotationAndLogoutManageDeviceSession` |
| 역할 변경 전 Refresh Token 무효화 | 통과 | `RefreshTokenIntegrationTest.refreshTokenIssuedBeforeRoleChangeCannotBeRotated` |
| 회원 제한 후 기존 액세스 토큰 무효화 | 통과 | `AdminManagementApiIntegrationTest.superAdminRestrictsMemberAndImmediatelyInvalidatesExistingToken` |
| 로그인 반복 시도 제한과 계정 존재 여부 비노출 | 통과 | `MemberLoginIntegrationTest.repeatedLoginFailuresAreRateLimitedAndSuccessfulLoginResetsAttempts`, `LoginAttemptLimiterTest` |
| 회원가입·변경·재설정의 공통 비밀번호 정책 | 통과 | `PasswordPolicyTest`, `MemberServiceTest`, `PasswordResetIntegrationTest` |

Refresh Token은 브라우저가 자동 전송하는 Cookie이므로 배포 후 HTTPS 환경에서
`Secure`, `HttpOnly`, `SameSite` 속성을 다시 확인합니다. 상태 변경 API는 액세스
토큰을 `Authorization` 헤더로 요구하며 Refresh Token endpoint는 토큰 회전과
서버 저장 상태 검증을 수행합니다.

## 역할과 관리자 등급

| 점검 항목 | 결과 | 자동 검증 근거 |
| --- | --- | --- |
| 일반 회원의 판매자 API 접근 차단 | 통과 | `JwtAuthorizationIntegrationTest.sellerEndpointAllowsSellerAndAdminOnly` |
| 판매자의 관리자 API 접근 차단 | 통과 | `JwtAuthorizationIntegrationTest.adminEndpointAllowsAdminOnly` |
| 매니저의 조회 권한과 고위험 변경 권한 분리 | 통과 | `JwtAuthorizationIntegrationTest.managerCanReadDashboardButCannotManageCategoriesOrApproveSettlements` |
| 슈퍼바이저의 부분 관리·정산 승인 허용과 카테고리 삭제 차단 | 통과 | `JwtAuthorizationIntegrationTest.supervisorCanPartiallyManageCategoriesAndApproveSettlementsButCannotDeleteCategories` |
| 슈퍼관리자의 전체 카테고리 관리 권한 | 통과 | `JwtAuthorizationIntegrationTest.superAdminCanUseCategoryDeletionPermission` |

프론트엔드 메뉴 노출 여부와 관계없이 위 권한은 Spring Security에서 서버 측으로
검사합니다.

## 리소스 소유권과 개인정보

| 점검 항목 | 결과 | 자동 검증 근거 |
| --- | --- | --- |
| 다른 회원의 배송지로 주문 생성 차단 | 통과 | `OrderApiIntegrationTest.rejectsDeliveryAddressOwnedByAnotherMember` |
| 다른 회원의 리뷰 수정·삭제 차단 | 통과 | `ReviewApiIntegrationTest.onlyOwnerUpdatesAndDeletesReviewAndRatingIsRecalculated` |
| 다른 회원의 알림 조회·변경 차단 | 통과 | `NotificationApiIntegrationTest.recordsOrderPaymentAndFulfillmentEventsAndProtectsOwnership` |
| 다른 판매자의 상품 변경 차단 | 통과 | `SellerManagementApiIntegrationTest.sellerCannotUpdateAnotherSellersProduct` |
| 혼합 판매자 주문에서 자기 상품만 변경 | 통과 | `SellerManagementApiIntegrationTest.sellerUpdatesOnlyOwnItemsInMixedSellerOrder` |
| 다른 판매자의 정산계좌 조회 차단 | 통과 | `SellerSettlementAccountApiIntegrationTest.sellerCannotReadAnotherSellersAccountAndBuyerCannotAccessSellerApi` |
| 정산계좌 응답 마스킹 | 통과 | `SellerSettlementAccountApiIntegrationTest.sellerRegistersAndReadsOnlyMaskedSettlementAccount` |

소유하지 않은 리소스는 가능한 경우 `404`로 응답해 리소스 존재 여부를 노출하지
않습니다.

## 주문·결제·환불

| 점검 항목 | 결과 | 자동 검증 근거 |
| --- | --- | --- |
| 서버 주문 금액과 결제 승인 금액 대조 | 통과 | `PaymentApiIntegrationTest.confirmsTossPaymentWithServerOrderAmount` |
| 결제 금액 위변조 시 Gateway 호출 전 차단 | 통과 | `PaymentApiIntegrationTest.rejectsChangedAmountBeforeCallingGateway` |
| 동일 결제 승인 요청 중복 처리 방지 | 통과 | `PaymentConcurrencyIntegrationTest.confirmsPaymentOnceForConcurrentRequestsWithSameIdempotencyKey` |
| 동일 환불 요청 중복 처리 방지와 환불 한도 적용 | 통과 | `PaymentApiIntegrationTest.processesPartialAndRemainingRefundIdempotently` |
| Webhook 재전송 중복 처리 방지 | 통과 | `PaymentWebhookIntegrationTest.processesSameTransmissionOnlyOnce` |
| Webhook 역순 도착 시 Provider 현재 상태 확인 | 통과 | `PaymentWebhookIntegrationTest.usesCurrentProviderStatusForOutOfOrderWebhook` |
| 판매자는 자기 상품 금액만 환불 | 통과 | `SellerManagementApiIntegrationTest.sellerRefundsOnlyOwnedItemsAndAdminRefundsRemainingOrder` |

## 배포 전 수동 점검

- [x] 업로드 파일의 서명과 실제 디코딩, 손상된 PNG, 변조·초대형 WebP 및 경로 이동 파일명을 확인
- [x] 애플리케이션 로그가 비밀번호, Authorization Header, Cookie, OAuth 오류 메시지와 결제 요청 본문을 기록하지 않는지 확인
- [x] 운영 Compose에서 PostgreSQL·Redis·Kafka 포트가 호스트에 노출되지 않는지 확인
- [x] 운영 Compose의 필수 환경변수 누락 시 안전하지 않은 기본값 없이 구성 단계에서 실패하는지 확인
- [x] KISA 적용 항목, N/A 사유와 배포 후 항목을 [`kisa-2026-assessment.md`](./kisa-2026-assessment.md)에 연결

검증 근거:

- `LocalFileStorageServiceTest`: 손상 이미지, WebP 컨테이너·해상도, 경로 이동 파일명 차단
- Spring multipart 제한: 파일과 요청 크기 각각 기본 10MB
- `OAuth2AuthenticationFailureHandler`: 예외 메시지 대신 예외 유형만 기록
- `PaymentWebhookService`: 결제 요청 본문과 stack trace 없이 제한된 식별자와 예외 유형만 기록
- `docker compose -f compose.yaml -f compose.prod.yaml config --quiet`: 필수 변수 누락 실패와 완전한 구성 성공 확인
- `compose.prod.yaml`: Redis 호스트 포트 제거, PostgreSQL·Kafka 내부 네트워크 전용, Frontend loopback 기본 바인딩, SMTP 인증·STARTTLS 강제
- `PasswordPolicy`: 조합별 최소 길이, 허용 문자 종류, 예측 문자열, 이메일 유사값, 연속·반복 패턴과 현재 비밀번호 재사용 차단
- `LoginAttemptLimiter`: 정규화된 이메일의 SHA-256 해시 키와 Redis TTL을 이용한 반복 로그인 제한

## 배포 후 점검

- [x] 운영 HTTPS/TLS와 인증서 구성
- [x] 운영 응답의 CSP, COOP, CORP, Permissions Policy
- [ ] Refresh Token Cookie의 `Secure`, `HttpOnly`, `SameSite`
- [ ] 운영 CORS 허용 출처
- [ ] PostgreSQL, Redis, Kafka, Actuator와 모니터링 포트의 외부 노출 여부
- [x] 실제 배포 URL에 대한 ZAP Baseline 또는 Passive Scan
- [x] 운영 PostgreSQL 역할 분리, 최소 GRANT와 접속·DDL 감사 로그 구성
- [ ] 운영 배포 후 실제 역할 속성·권한 거부와 감사 로그 표본 재검증

2026-08-11 운영 점검에서 HTTP 요청의 HTTPS `308 Permanent Redirect`, 유효한 인증서와
`Strict-Transport-Security: max-age=31536000; includeSubDomains`를 확인했습니다. 운영 URL에
비파괴적인 ZAP Passive Baseline을 실행한 결과 High 0건, Medium 0건, Low 3종,
Informational 5종이었습니다.

Low 항목 중 COEP 미적용과 COOP `same-origin-allow-popups`는 Google One Tap, OAuth 팝업과
Toss 결제의 교차 출처 연동을 유지하기 위한 의도된 예외입니다. Backend API와 공개 이미지의
CORP 누락은 Caddy Backend 경로에서 `Cross-Origin-Resource-Policy: same-origin`을 추가해
보완했습니다.

2026-08-11 재배포 후 API와 이미지 응답의 CORP 헤더를 직접 확인하고 Passive Scan을
재실행했습니다. 최종 결과는 High 0건, Medium 0건, Low 2종, Informational 5종이며 CORP
경고는 제거됐습니다. 남은 Low 2종은 위에서 기록한 COEP·COOP 호환성 예외입니다.

전체 KISA 판정과 N/A 사유는
[`kisa-2026-assessment.md`](./kisa-2026-assessment.md)를 기준으로 관리합니다.

