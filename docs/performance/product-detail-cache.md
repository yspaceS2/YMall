# 상품 상세 Redis 캐시 성능 검증

## 적용 범위

- 캐시 이름: `productDetails`
- 캐시 키: `productDetails::{productId}`
- 기본 TTL: 10분 (`PRODUCT_DETAIL_CACHE_TTL`로 변경 가능)
- 무효화: 상품 수정·삭제, 판매자 수정·재심사, 관리자 승인·반려, 리뷰 평점 변경
- 장애 정책: Redis 읽기·쓰기·삭제 실패를 기록하고 PostgreSQL 조회 결과를 반환

## 측정 방법


`ProductCacheIntegrationTest.comparesUncachedAndCachedLookupTime`에서 동일 상품을 30회 조회한다.

1. 비캐시 측정은 매 조회 전에 상품 상세 캐시를 제거한다.
2. 캐시 측정은 최초 1회 적재 후 같은 상품을 반복 조회한다.
3. JVM 준비 상태와 로컬 장비 부하에 따라 수치가 달라질 수 있으므로 절대 성능이 아닌 적용 전후 경향을 확인한다.

실행 명령:

```shell
./gradlew test --tests "com.ymall.backend.integration.product.ProductCacheIntegrationTest.comparesUncachedAndCachedLookupTime" --info
```

## 측정 결과

측정 환경과 결과는 테스트 실행 후 아래 표에 기록한다.

| 환경 | 반복 | 비캐시 평균 | 캐시 평균 | 개선율 |
| --- | ---: | ---: | ---: | ---: |
| 로컬 테스트 프로필(H2 + Redis) | 30 | 4.501 ms | 1.478 ms | 67.2% |

측정일: 2026-07-21. 캐시 적용 후 동일 상품 상세 조회의 평균 응답 시간이 테스트
서비스 계층 기준 약 67.2% 감소했다.

운영과 유사한 PostgreSQL 환경의 최종 수치는 배포 환경 확정 후 같은 상품 상세 API를 기준으로 추가 측정한다.
