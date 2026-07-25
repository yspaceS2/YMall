# 리뷰 요약 Backend 연동

## 처리 흐름

1. 리뷰 생성·수정·삭제 트랜잭션이 완료되면 상품 ID를 포함한 갱신 이벤트를 발행한다.
2. 이벤트는 `ymall.review-summary.refresh.v1` Kafka 토픽으로 전달된다.
3. Consumer는 Redis 분산 잠금을 획득해 같은 상품의 중복 생성을 막는다.
4. 리뷰가 기본 10개 이상이면 최신 리뷰를 모델 입력 제한 안에서 수집한다.
5. Docker Model Runner의 OpenAI 호환 `/chat/completions` API를 호출한다.
6. 결과와 모델 버전, 생성 시각, 원본 리뷰 수·최종 변경 시각을 PostgreSQL에 저장한다.
7. 공개 조회 API는 Redis 캐시를 먼저 사용하고 Redis 장애 시 PostgreSQL로 대체한다.

리뷰 변경 트랜잭션에서는 AI를 직접 호출하지 않는다. 따라서 모델의 응답 지연이나 장애가
리뷰 작성과 상품 상세·리뷰 목록 조회에 전파되지 않는다.

## 공개 API

```text
GET /api/products/{productId}/review-summary
```

요약이 준비된 경우:

```json
{
    "success": true,
    "data": {
        "available": true,
        "reviewCount": 12,
        "pros": ["연결이 빠릅니다."],
        "cons": ["무게가 무겁습니다."],
        "commonOpinions": ["키감이 부드럽다는 의견이 반복됩니다."],
        "modelVersion": "huggingface.co/qwen/qwen3-0.6b-gguf:Q8_0",
        "generatedAt": "2026-07-25T13:00:00"
    }
}
```

리뷰가 기준보다 적거나 아직 생성되지 않은 경우 `available`은 `false`이며 요약 목록은
빈 배열이다. 기존 요약이 있는 상태에서 AI 호출이 실패하면 기존 요약을 유지한다.

## 생성 및 입력 정책

- 기본 생성 기준: 리뷰 10개 이상
- 모델에 전달할 최대 리뷰: 100개
- 리뷰 한 건의 최대 입력 길이: 1,000자
- 전체 입력 최대 길이: 6,000자
- 모델 출력 배열: 장점·단점·공통 의견 각각 최대 3개
- 동일한 리뷰 수와 최종 변경 시각으로 생성된 요약은 다시 만들지 않는다.

## 장애 처리

- AI 타임아웃·5xx: Kafka Consumer 재시도 후 DLT로 이동하며 기존 요약은 유지한다.
- 동일 상품 생성 잠금 경합: 이벤트를 완료 처리하지 않고 Kafka 재시도를 요청한다.
- Redis 잠금 장애: 단일 Backend 인스턴스 안의 로컬 잠금으로 대체한다.
- Redis 캐시 장애: PostgreSQL에서 요약을 조회한다.
- 생성 도중 리뷰 변경: 결과를 저장하지 않고 재시도한다.
- 리뷰 수가 기준 미만으로 감소: 기존 요약을 제거한다.

## 주요 환경변수

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `AI_REVIEW_ENABLED` | `false` | Backend 요약 생성을 활성화한다. Compose에서는 `true`이다. |
| `AI_REVIEW_MINIMUM_REVIEWS` | `10` | 요약 생성에 필요한 최소 리뷰 수 |
| `AI_REVIEW_MAXIMUM_REVIEWS` | `100` | 모델에 전달할 최대 리뷰 수 |
| `AI_REVIEW_MAXIMUM_REVIEW_LENGTH` | `1000` | 리뷰 한 건의 최대 입력 길이 |
| `AI_REVIEW_MAXIMUM_TOTAL_LENGTH` | `6000` | 전체 리뷰 입력 최대 길이 |
| `AI_REVIEW_MAX_TOKENS` | `192` | 최대 생성 토큰 |
| `AI_REVIEW_CONNECT_TIMEOUT` | `3s` | 모델 연결 제한 시간 |
| `AI_REVIEW_READ_TIMEOUT` | `180s` | 모델 응답 제한 시간 |
| `AI_REVIEW_LOCK_TTL` | `5m` | Redis 생성 잠금 만료 시간 |
| `REVIEW_SUMMARY_CACHE_TTL` | `30m` | 조회 캐시 만료 시간 |

모델 endpoint와 모델 이름은 Compose의 `models` 연결이 `AI_REVIEW_BASE_URL`,
`AI_REVIEW_MODEL`로 Backend에 주입한다.
