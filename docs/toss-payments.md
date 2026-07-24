# Toss Payments 테스트 환경

YMall의 실제 결제 흐름은 브라우저의 Toss Payments SDK와 Backend의
`PaymentGateway`를 통해 승인·조회·취소 API에 연결된다. 모의 결제 API는 로컬 기능 확인용으로
유지하지만, 실제 결제 검증은 테스트 키만 사용한다.

## 키 구성

Toss Payments 개발자센터에서 같은 세트로 발급된 테스트 클라이언트 키와 테스트 시크릿 키를 사용한다.

- `TOSS_CLIENT_KEY`: 브라우저 SDK에서 사용할 `test_ck` 또는 `test_gck` 키
- `TOSS_SECRET_KEY`: Backend API 인증에 사용할 짝이 맞는 `test_sk` 또는 `test_gsk` 키
- `TOSS_API_BASE_URL`: 기본값 `https://api.tosspayments.com`

시크릿 키는 `application-local.yaml` 또는 루트 `.env`에만 저장하고 커밋하지 않는다. 공유 파일에는
실제 키를 입력하지 않는다. 브라우저에서 사용하는 클라이언트 키는 공개 가능한 식별자지만 실제 값은
`frontend/.env.local`에서 관리한다.

로컬 Backend 설정 예시:

```yaml
ymall:
  payment:
    toss:
      client-key: ${TOSS_CLIENT_KEY}
      secret-key: ${TOSS_SECRET_KEY}
```

Docker Compose를 사용할 때는 루트 `.env.example`을 `.env`로 복사한 다음 값을 입력한다.

## 로컬 실행과 사용자 흐름

1. PostgreSQL, Redis, Kafka와 Backend를 실행한다.
2. Frontend를 실행하고 사용자 계정으로 로그인한다.
3. 상품을 장바구니에 담고 주문을 생성한다.
4. 결제 화면에서 Toss Payments 테스트 결제 수단으로 승인한다.
5. 주문 결과와 주문 내역에서 `PAID` 상태를 확인한다.
6. 환불 가능한 주문에서 전체 또는 일부 수량을 환불한다.
7. 환불 이력, 주문 상태, 환불 수량과 재고 복원을 확인한다.

실제 테스트 카드 정보와 인증 방식은 Toss Payments 개발자 문서를 따른다. 테스트 과정에서 생성된
`paymentKey`, 주문번호, 사용자 정보는 문서·Jira·PR·스크린샷에 복사하지 않는다.

## 자동화 테스트

외부 결제사 상태와 무관하게 CI에서 재현되도록 계층별 테스트를 분리한다.

| 범위 | 테스트 | 검증 내용 |
| --- | --- | --- |
| HTTP 어댑터 | `TossPaymentAdapterTest` | 승인·조회·취소 요청, 멱등키, 오류와 타임아웃 매핑 |
| 결제 API | `PaymentApiIntegrationTest` | 승인 성공·실패·금액 위변조·재시도·부분 환불 |
| 동시 요청 | `PaymentConcurrencyIntegrationTest`, `PaymentRefundConcurrencyIntegrationTest` | 중복 승인·환불 방지와 잠금 |
| 웹훅 | `PaymentWebhookIntegrationTest`, `PaymentWebhookConcurrencyIntegrationTest` | 중복·역순·검증 실패·재시도 |
| 환불 실패 | `PaymentRefundFailureIntegrationTest` | 거절 후 재시도와 결과 미확정 차단 |

Backend 전체 검증:

```powershell
cd backend
.\gradlew.bat test
```

결제 관련 테스트만 실행:

```powershell
cd backend
.\gradlew.bat test --tests "*Payment*Test"
```

자동화 테스트에서는 실제 API 키를 사용하지 않는다. Toss HTTP 요청은 `MockRestServiceServer`로,
도메인 통합 테스트의 결제사 응답은 `PaymentGateway` 테스트 더블로 대체한다.

## API 연결 확인

승인된 테스트 결제의 `paymentKey`가 있다면 PowerShell에서 조회 API로 인증과 연결 상태를 확인할 수 있다.

```powershell
$credential = [Convert]::ToBase64String(
    [Text.Encoding]::UTF8.GetBytes("$env:TOSS_SECRET_KEY`:")
)

Invoke-RestMethod `
    -Uri "https://api.tosspayments.com/v1/payments/{paymentKey}" `
    -Headers @{ Authorization = "Basic $credential" }
```

- Payment 객체가 응답되면 연결과 키 구성이 정상이다.
- `NOT_FOUND_PAYMENT`이면 API 연결과 인증은 되었지만 입력한 결제 키가 존재하지 않는 것이다.
- `INVALID_API_KEY`이면 클라이언트 키와 시크릿 키가 같은 세트인지 확인한다.

## 코드 구조

- `PaymentGateway`: 결제 도메인이 사용하는 승인·조회·취소 계약
- `TossPaymentAdapter`: Toss Payments HTTP API 호출 구현
- `TossPaymentMapper`: Toss 응답을 YMall 공통 결제 결과로 변환
- `TossPaymentExceptionMapper`: 결제사 오류를 YMall `PaymentException`으로 변환

## 장애 확인과 복구

| 상황 | 확인 | 조치 |
| --- | --- | --- |
| `INVALID_API_KEY` | 클라이언트 키와 시크릿 키의 테스트 세트 일치 여부 | 키를 다시 발급하거나 로컬 환경변수를 수정한 뒤 Backend 재시작 |
| 승인 타임아웃 | 결제 조회 API와 관리자 콘솔의 실제 결제 상태 | 새 결제를 즉시 승인하지 말고 기존 결제 상태를 먼저 대사 |
| 환불 `FAILED` | 환불 이력의 실패 코드와 결제사 거절 사유 | 원인을 수정하고 새 멱등키로 재요청 |
| 환불 `UNKNOWN` | 결제사 `balanceAmount`와 내부 성공 환불 합계 | 정합성 확인 전 추가 환불 차단, 수동 대사 후 보정 |
| 웹훅 반복 실패 | 전송 ID, 응답 코드, 결제 조회 결과 | 처리 이력이 없을 때만 Toss 개발자센터에서 재전송 |

환불 상세 정책은 [결제 취소·환불 운영 가이드](payment-refunds.md), 웹훅 재처리는
[Toss Payments 웹훅 운영 가이드](payment-webhooks.md)를 따른다.
