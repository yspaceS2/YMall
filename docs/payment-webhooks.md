# Toss Payments 웹훅 운영 가이드

## 목적

브라우저 결제 완료 콜백이 누락되거나 결제 상태가 비동기로 변경되어도 YMall의 주문·결제 상태를 Toss Payments의 현재 상태와 일치시킨다.

## 웹훅 등록

Toss Payments 개발자센터에서 아래 웹훅을 등록한다.

- URL: `https://{backend-domain}/api/payments/webhooks/toss`
- 이벤트: `PAYMENT_STATUS_CHANGED`
- 로컬 테스트는 외부에서 접근 가능한 HTTPS 터널 또는 테스트 서버를 사용한다.

일반 결제 웹훅에는 요청 서명이 제공되지 않는다. YMall은 웹훅 본문을 그대로 신뢰하지 않고, 전달받은 `paymentKey`로 Toss Payments 결제 조회 API를 다시 호출한다. 조회 결과의 결제 키, 주문번호, 금액과 상태가 서버 주문 정보에 맞을 때만 상태를 반영한다.

- [Toss Payments 웹훅 이벤트](https://docs.tosspayments.com/reference/using-api/webhook-events)
- [paymentKey로 결제 조회](https://docs.tosspayments.com/reference#paymentkey로-결제-조회)

## 처리 규칙

| Toss 상태 | YMall 처리 |
| --- | --- |
| `READY`, `IN_PROGRESS`, `WAITING_FOR_DEPOSIT` | 중간 상태로 기록만 확인하고 주문 상태는 변경하지 않는다. |
| `DONE` | 재고를 확보하고 주문을 `PAID`로 변경한다. 누락된 결제 이력을 생성하거나 실패 이력을 성공으로 정정한다. |
| `ABORTED`, `EXPIRED` | 배송 시작 전 주문을 `PAYMENT_FAILED`로 변경하고 확보한 재고를 복구한다. |
| `CANCELED` | 배송 시작 전 주문을 `CANCELED`로 변경하고 확보한 재고를 복구한다. |
| `PARTIAL_CANCELED` | 결제사 상태만 기록한다. 주문 항목별 부분 환불은 YMALL-64에서 처리한다. |

`PREPARING`, `SHIPPED`, `DELIVERED`, `CANCELED` 같은 내부 확정 상태는 늦게 도착한 이벤트만으로 되돌리지 않는다. 웹훅 본문 상태와 결제 조회 결과가 다르면 조회 결과를 기준으로 처리하고 해당 웹훅은 `STALE_EVENT`로 기록한다.

## 중복과 재시도

- `tosspayments-webhook-transmission-id`를 `payment_webhook_events` 테이블의 고유 키로 저장한다.
- 같은 전송 ID가 재수신되면 결제사 조회와 주문 상태 변경을 반복하지 않고 `200 OK`를 반환한다.
- 검증 또는 처리 중 예외가 발생하면 웹훅 이력을 저장하지 않고 오류 응답을 반환한다.
- Toss Payments가 같은 전송 ID로 재시도하면 정상적으로 다시 처리할 수 있다.
- 처리 실패 로그에는 안전한 문자와 길이로 정제한 전송 ID와 이벤트 타입만 남기며 결제 키, API 키, 고객정보는 기록하지 않는다.

## 장애 확인과 수동 재처리

1. 애플리케이션 로그에서 `Payment webhook processing failed`와 전송 ID를 확인한다.
2. Toss Payments 개발자센터 웹훅 전송 기록에서 응답 코드와 재전송 횟수를 확인한다.
3. `payment_webhook_events`에 전송 ID가 있으면 이미 처리가 완료된 이벤트이므로 주문·결제 상태를 확인한다.
4. 이력이 없다면 Toss Payments에서 웹훅을 재전송한다.
5. 반복 실패 시 `paymentKey` 조회 결과와 YMall 주문번호·금액을 비교한다. 실제 키 값이나 개인정보를 Jira, PR, 로그에 복사하지 않는다.
6. 배송이 시작된 주문과 결제사 상태가 다르면 자동으로 주문 상태를 되돌리지 말고 운영 확인 대상으로 처리한다.

## 로컬 요청 형식

실제 키 대신 테스트 더블 또는 Toss Payments 테스트 결제로 검증한다.

```http
POST /api/payments/webhooks/toss
tosspayments-webhook-transmission-id: test-transmission-id
Content-Type: application/json

{
  "eventType": "PAYMENT_STATUS_CHANGED",
  "createdAt": "2026-07-24T12:00:00",
  "data": {
    "paymentKey": "test-payment-key",
    "orderId": "YMALL-test-order",
    "status": "DONE",
    "totalAmount": 10000
  }
}
```
