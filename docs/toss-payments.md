# Toss Payments 테스트 환경

## 키 구성

Toss Payments 개발자센터에서 같은 세트로 발급된 테스트 클라이언트 키와 테스트 시크릿 키를 사용한다.

- `TOSS_CLIENT_KEY`: 브라우저 SDK에서 사용할 `test_ck` 또는 `test_gck` 키
- `TOSS_SECRET_KEY`: Backend API 인증에 사용할 짝이 맞는 `test_sk` 또는 `test_gsk` 키
- `TOSS_API_BASE_URL`: 기본값 `https://api.tosspayments.com`

시크릿 키는 `application-local.yaml` 또는 루트 `.env`에만 저장하고 커밋하지 않는다. 공유 파일에는 실제 키를 입력하지 않는다.

로컬 Backend 설정 예시:

```yaml
ymall:
  payment:
    toss:
      client-key: ${TOSS_CLIENT_KEY}
      secret-key: ${TOSS_SECRET_KEY}
```

Docker Compose를 사용할 때는 루트 `.env.example`을 `.env`로 복사한 다음 값을 입력한다.

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

현재 모의 결제 API는 유지한다. 실제 승인 흐름 연결은 후속 작업에서 `PaymentGateway`를 통해 진행한다.
