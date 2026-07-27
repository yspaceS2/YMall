# 월별 정산 신청과 모의 지급

YMALL의 정산 기능은 포트폴리오 환경에서 판매 금액, 수수료, 환불 조정을 추적하고
관리자 지급 절차를 재현한다. `PAID` 처리는 실제 은행 송금이 아닌 모의 지급이다.

## 신청 기준

- `Asia/Seoul` 기준으로 종료된 월만 신청할 수 있다.
- 배송 완료로 `AVAILABLE` 상태가 된 판매·환불 원장 항목만 합산한다.
- 정산 계좌가 등록되어 있어야 한다.
- 합산 정산액이 0원보다 커야 한다.
- 같은 판매자와 대상 월에는 신청서가 하나만 존재한다.
- 판매자 단위 비관적 잠금과 DB 고유 제약으로 동시 중복 신청을 차단한다.

## 상태 전이

```text
REQUESTED -> APPROVED -> PAID
     |
     +-> REJECTED -> REQUESTED
```

- 관리자는 신청 상태에서만 승인 또는 반려할 수 있다.
- 반려하면 연결된 원장 항목을 `AVAILABLE`로 되돌려 같은 신청서로 재신청할 수 있다.
- 승인 후 모의 지급을 완료하면 연결된 원장 항목도 `PAID`로 변경한다.
- 모든 상태 변화는 처리 회원, 이전·이후 상태, 사유, 처리 시각과 함께 감사 이력에 기록한다.

## 신청 이후 환불

이미 신청되었거나 지급된 판매 건에서 환불이 완료되면 기존 신청 금액을 과거 시점으로
수정하지 않는다. 환불 금액은 별도의 음수 조정 원장으로 즉시 `AVAILABLE` 처리하며,
환불 발생 월의 다음 정산 신청에 반영한다. 이 방식으로 지급 완료 이력의 불변성을
유지하면서 이후 지급액에서 환불액을 차감한다.

## API

판매자:

- `GET /api/seller/settlement-requests/availability?period=YYYY-MM`
- `GET /api/seller/settlement-requests`
- `POST /api/seller/settlement-requests`

관리자:

- `GET /api/admin/settlement-requests`
- `PATCH /api/admin/settlement-requests/{settlementRequestId}/approval`
- `PATCH /api/admin/settlement-requests/{settlementRequestId}/rejection`
- `POST /api/admin/settlement-requests/{settlementRequestId}/mock-payments`
- `GET /api/admin/settlement-requests/{settlementRequestId}/histories`
