# 데모 리뷰 fixture

`seed-review-fixtures.sql`은 홈 큐레이션 알고리즘이 반환하는 상품에 AI 리뷰 요약을 생성하기 위한
로컬 데모 데이터를 준비한다. 운영 Flyway migration이 아니며, 검증된 로컬 DB에 명시적으로 실행한
뒤 백업을 생성해 배포 환경에 복원한다.

## 데이터 원칙

- 실제 Toss Payments 결제 기록은 사용자가 직접 완료한 주문만 유지한다.
- fixture 주문에는 결제 레코드를 만들지 않아 실제 결제 검증 결과와 구분한다.
- fixture 회원은 무작위 비밀번호와 `RESTRICTED` 상태로 생성되어 로그인할 수 없다.
- 가상의 이름, 연락처, 배송지만 사용한다.
- 기존 대표 상품 6개의 마지막 리뷰는 회원 UI에서 등록하여 Kafka와 로컬 AI 모델을 포함한 실제 요약
  생성 흐름을 검증한다.
- 홈 최초 노출 상품 중 추가된 7개는 fixture 리뷰 10개를 생성하고 별도 갱신 이벤트로 AI 요약을 만든다.
- 동일한 환경에서 다시 실행해도 같은 주문과 리뷰가 중복 생성되지 않는다.

## 홈 최초 노출 상품

홈 API의 반환 순서를 기준으로 각 캐러셀의 첫 슬라이드에 표시되는 상품이다. 판매량과 최근 판매 시각,
승인 시각을 사용하는 기존 큐레이션 알고리즘은 변경하지 않는다.

| 상품 ID | 상품 |
| --- | --- |
| 103 | 테라 그립 트레일 러닝화 |
| 40 | 아쿠아 시트러스 오 드 뚜왈렛 |
| 111 | 노르웨이 생연어 필렛 500g |
| 110 | 완도 활전복 1kg |
| 88 | 모브 울 블렌드 숏 재킷 |
| 87 | 세렌 클래식 트렌치 코트 |
| 163 | 하드커버 그리드 노트 3권 세트 |
| 162 | 만년형 위클리 플래너 |

기존 시연 흐름에서 리뷰를 작성한 상품 95, 114, 123, 143, 161도 함께 유지한다.

## 실행

PostgreSQL 백업을 만든 뒤 프로젝트 루트에서 실행한다.

AI 요약은 다음 4B 모델을 4,096토큰 컨텍스트로 준비한다.

```powershell
docker model pull hf.co/Qwen/Qwen3-4B-GGUF:Q4_K_M
docker model package --from hf.co/Qwen/Qwen3-4B-GGUF:Q4_K_M `
  --context-size 4096 ymall/qwen3-4b-review:Q4_K_M
setx AI_REVIEW_MODEL "ymall/qwen3-4b-review:Q4_K_M"
setx AI_REVIEW_MAX_TOKENS 512
```

`setx` 실행 후에는 IntelliJ를 완전히 종료하고 다시 실행해야 사용자 환경변수가 적용된다.

```powershell
$postgresContainer = docker compose ps -q postgres
docker cp scripts/demo/seed-review-fixtures.sql `
  "${postgresContainer}:/tmp/seed-review-fixtures.sql"
docker compose exec -T postgres `
  psql -v ON_ERROR_STOP=1 -U ymall_user -d ymall `
  -f /tmp/seed-review-fixtures.sql
```

기존 대표 상품을 처음 구성할 때만 `user@ymall.cloud`로 로그인하여 fixture 주문의 마지막 리뷰를
작성한다. 홈 최초 노출 상품에 자동 생성된 리뷰는 Kafka 갱신 이벤트를 발행해 AI 요약을 비동기로
생성한다.

실제 DB 이름과 사용자는 로컬 `.env` 설정을 기준으로 바꾼다. 비밀번호와 API 키는 스크립트나 문서에
추가하지 않는다.
