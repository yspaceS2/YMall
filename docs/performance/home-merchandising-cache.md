# 홈 큐레이션 캐시 성능 검증

## 대상

- API: `GET /api/home/merchandising`
- 캐시: Redis `homeMerchandisingV1::all`
- TTL: 5분 (`HOME_MERCHANDISING_CACHE_TTL`로 조정 가능)
- 측정일: 2026-08-01

## 측정 방법

`HomeMerchandisingCacheIntegrationTest`에서 동일한 홈 큐레이션 조회를 각각 20회 실행했다.

- 비캐시: 매 요청 전 캐시를 무효화하여 DB 조회와 응답 조립 시간을 측정
- 캐시 적중: 최초 조회로 캐시를 생성한 뒤 동일 요청 시간을 측정
- 환경: 로컬 Windows, Java 17, Spring Boot 통합 테스트, H2 데이터베이스, Docker Redis

실행 명령:

```powershell
cd backend
.\gradlew.bat test --tests "com.ymall.backend.integration.home.HomeMerchandisingCacheIntegrationTest"
```

## 결과

| 구분 | 평균 응답 시간 | 비교 |
| --- | ---: | ---: |
| 비캐시 | 31.775 ms | 기준 |
| Redis 캐시 적중 | 3.753 ms | 88.2% 감소 |

## 해석과 제한

- 캐시 적중 시 집계 쿼리와 응답 조립을 생략하여 로컬 통합 테스트에서 응답 시간이 감소했다.
- 이 결과는 H2와 소량의 테스트 데이터를 사용한 로컬 측정값이므로 운영 PostgreSQL의 절대 성능을 의미하지 않는다.
- 상품 등록·수정·삭제·승인 상태 변경 시 캐시를 즉시 무효화한다.
- 주문·결제·환불에 따른 판매량 순위 변화는 최대 5분의 TTL 안에 반영한다. 요청마다 전체 홈 캐시를 삭제해 캐시 효율이 떨어지는 것을 피하기 위한 선택이다.
- Redis 연결 실패 시 캐시 오류 처리기가 예외를 흡수하고 PostgreSQL 조회 결과를 반환한다.

운영 배포 후에는 실제 데이터 규모와 PostgreSQL 환경에서 p95·p99 응답 시간, 캐시 적중률, DB 부하를 추가 측정해야 한다.
